"""
Cloud Run proxy: Android app → this → Vertex AI Agent Engine (ADK multi-agent)

Each streamQuery call advances the ADK agent one "round" (tool call or response).
We loop collecting substantial text from all agents, stop when synthesis is done.
"""

import re
import json
import time
import asyncio
import httpx
import google.auth
import google.auth.transport.requests
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="Personal Shopper Proxy")

app.add_middleware(
    CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"]
)

PROJECT = "personal-shopper-agent"
REGION = "us-central1"
RESOURCE_ID = "1786156639322112"
BASE = (
    f"https://{REGION}-aiplatform.googleapis.com/v1"
    f"/projects/{PROJECT}/locations/{REGION}"
    f"/reasoningEngines/{RESOURCE_ID}"
)

MAX_ROUNDS = 12
ROUND_WAIT_SECS = 5
# Only count text as real synthesis if it's at least this long
MIN_SYNTHESIS_LEN = 250

# Patterns that indicate intermediate agent status, not real synthesis
_INTERMEDIATE_PATTERNS = [
    r"^please wait",
    r"^i am (currently )?(?:searching|looking|fetching|gathering)",
    r"^i'm (currently )?(?:searching|looking|fetching|gathering)",
    r"^i am still waiting",
    r"^i'm sorry.*(?:cannot|can't) fulfill",
    r"^i am sorry.*(?:cannot|can't) fulfill",
    r"^i (?:cannot|can't) fulfill this request",
    r"^i have already executed",
    r"^i'm unable to",
    r"^searching for",
    r"^fetching",
    r"^gathering",
]
_INTERMEDIATE_RE = re.compile(
    "|".join(_INTERMEDIATE_PATTERNS), re.IGNORECASE
)


class QueryRequest(BaseModel):
    message: str


def get_token() -> str:
    creds, _ = google.auth.default(
        scopes=["https://www.googleapis.com/auth/cloud-platform"]
    )
    creds.refresh(google.auth.transport.requests.Request())
    return creds.token


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}


def parse_events(raw: str) -> list:
    events = []
    for line in raw.splitlines():
        line = line.strip()
        if not line:
            continue
        try:
            parsed = json.loads(line)
            if isinstance(parsed, list):
                events.extend(parsed)
            else:
                events.append(parsed)
        except Exception:
            pass
    if not events:
        try:
            parsed = json.loads(raw.strip())
            events = parsed if isinstance(parsed, list) else [parsed]
        except Exception:
            pass
    return events


def extract_synthesis_text(events: list) -> str:
    """
    Extract only genuine synthesis text from a round's events.
    Returns empty string if this round only has tool calls / intermediate messages.
    """
    text_chunks = []
    for ev in events:
        content = ev.get("content", {})
        if not isinstance(content, dict):
            continue
        parts = content.get("parts", [])
        # Skip any event that contains tool calls or tool responses
        has_tool = any("function_call" in p or "function_response" in p for p in parts)
        if has_tool:
            continue
        for p in parts:
            txt = p.get("text", "").strip()
            if not txt:
                continue
            # Skip known intermediate status messages
            if _INTERMEDIATE_RE.match(txt):
                print(f"[extract] skip intermediate: {txt[:80]}")
                continue
            text_chunks.append(txt)

    return "".join(text_chunks)


def has_tool_calls(events: list) -> bool:
    for ev in events:
        content = ev.get("content", {})
        if not isinstance(content, dict):
            continue
        for p in content.get("parts", []):
            if "function_call" in p:
                return True
    return False


async def create_session(client: httpx.AsyncClient, token: str) -> tuple[str, str]:
    user_id = f"user_{int(time.time())}"
    r = await client.post(
        f"{BASE}/sessions",
        headers=auth_headers(token),
        json={"userId": user_id},
    )
    if r.status_code != 200:
        raise HTTPException(500, f"Session creation failed ({r.status_code}): {r.text[:200]}")

    data = r.json()
    print(f"[session] response: {json.dumps(data)[:300]}")
    parts = data.get("name", "").split("/")
    try:
        session_id = parts[parts.index("sessions") + 1]
    except (ValueError, IndexError):
        raise HTTPException(500, f"Could not parse session_id from: {data.get('name')}")

    print(f"[session] created session_id={session_id}, waiting 12s...")
    await asyncio.sleep(12)
    return session_id, user_id


@app.on_event("startup")
async def startup():
    print("Proxy started.")


@app.post("/query")
async def query_agent(request: QueryRequest):
    token = get_token()

    async with httpx.AsyncClient(timeout=300.0) as client:
        session_id, user_id = await create_session(client, token)
        print(f"[query] session_id={session_id} | message={request.message[:80]}")

        payload = {
            "input": {
                "user_id": user_id,
                "session_id": session_id,
                "message": request.message,
            }
        }

        accumulated_text = []
        last_text_round = -1

        for round_num in range(1, MAX_ROUNDS + 1):
            print(f"[round {round_num}] calling streamQuery...")
            r = await client.post(
                f"{BASE}:streamQuery",
                headers=auth_headers(token),
                json=payload,
            )
            print(f"[round {round_num}] status={r.status_code}, bytes={len(r.text)}")

            if r.status_code != 200:
                raise HTTPException(r.status_code, f"Agent error: {r.text[:300]}")

            events = parse_events(r.text)
            print(f"[round {round_num}] events={len(events)}")

            for ev in events:
                author = ev.get("author", "?")
                content = ev.get("content", {})
                parts = content.get("parts", []) if isinstance(content, dict) else []
                part_types = [list(p.keys()) for p in parts]
                print(f"  {author}: {part_types}")

            text = extract_synthesis_text(events)
            if text and len(text) >= MIN_SYNTHESIS_LEN:
                print(f"[round {round_num}] got synthesis text len={len(text)}: {text[:150]}")
                accumulated_text.append(text)
                last_text_round = round_num

            # Stop early if: we have substantial text AND this round had no new tool calls
            if accumulated_text and not has_tool_calls(events):
                print(f"[round {round_num}] no more tool calls, synthesis complete")
                break

            if round_num < MAX_ROUNDS:
                print(f"[round {round_num}] waiting {ROUND_WAIT_SECS}s...")
                await asyncio.sleep(ROUND_WAIT_SECS)

        if accumulated_text:
            combined = "\n\n".join(accumulated_text)
            print(f"[query] returning combined text len={len(combined)} from {len(accumulated_text)} round(s)")
            return {"result": combined}

    print(f"[query] exhausted {MAX_ROUNDS} rounds with no synthesis text")
    return {"result": "Agent returned no text. Please try again."}


@app.get("/health")
async def health():
    return {"status": "ok"}
