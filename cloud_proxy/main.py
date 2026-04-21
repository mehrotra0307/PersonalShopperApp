"""
Cloud Run proxy: Android app → this → Vertex AI Agent Engine
"""

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


def extract_text(raw: str) -> str:
    """
    Extract only the synthesizer's final answer from the NDJSON event stream.
    The parallel sub-agents emit intermediate text during tool execution.
    We only want text from AFTER the last tool response (= synthesizer output).
    """
    print(f"[extract] body len={len(raw)} | first 600:\n{raw[:600]}\n---")

    # Parse all events (handles both NDJSON and JSON array)
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

    print(f"[extract] parsed {len(events)} events")

    # Find the index of the last event containing a tool response/call
    last_tool_idx = -1
    for i, ev in enumerate(events):
        parts = ev.get("content", {}).get("parts", [])
        for part in parts:
            # camelCase (Vertex AI JSON serialization)
            if "functionResponse" in part or "functionCall" in part:
                last_tool_idx = i
            # snake_case fallback
            if "function_response" in part or "function_call" in part:
                last_tool_idx = i

    print(f"[extract] last tool event index: {last_tool_idx} of {len(events)-1}")

    # Collect text only from events AFTER the last tool event (synthesizer output)
    synth_texts = []
    all_texts = []
    for i, ev in enumerate(events):
        for part in ev.get("content", {}).get("parts", []):
            txt = part.get("text", "")
            if txt:
                all_texts.append(txt)
                if i > last_tool_idx:
                    synth_texts.append(txt)

    # Prefer synthesizer-only text; fall back to all text if nothing found
    texts = synth_texts if synth_texts else all_texts
    result = "".join(texts)
    print(f"[extract] synth chunks={len(synth_texts)} all chunks={len(all_texts)} result len={len(result)}")
    if result:
        print(f"[extract] preview: {result[:300]}")
    return result if result else "Agent returned no text. Please try again."


async def create_session(token: str) -> tuple[str, str]:
    """Create a fresh session. Returns (session_id, user_id)."""
    user_id = f"user_{int(time.time())}"
    async with httpx.AsyncClient(timeout=30.0) as client:
        r = await client.post(
            f"{BASE}/sessions",
            headers=auth_headers(token),
            json={"userId": user_id},
        )
    if r.status_code != 200:
        raise HTTPException(500, f"Session creation failed ({r.status_code}): {r.text[:200]}")

    data = r.json()
    print(f"[session] create response: {json.dumps(data)[:300]}")
    parts = data.get("name", "").split("/")
    try:
        session_id = parts[parts.index("sessions") + 1]
    except (ValueError, IndexError):
        raise HTTPException(500, f"Could not parse session_id from: {data.get('name')}")

    print(f"[session] created session_id={session_id} user_id={user_id}, waiting 8s...")
    await asyncio.sleep(8)
    return session_id, user_id


@app.on_event("startup")
async def startup():
    print("Proxy started — session will be created on first request.")


@app.post("/query")
async def query_agent(request: QueryRequest):
    token = get_token()

    # Fresh session per query — avoids stale conversation state
    session_id, user_id = await create_session(token)
    print(f"[query] session_id={session_id} | message={request.message[:80]}")

    async with httpx.AsyncClient(timeout=180.0) as client:
        r = await client.post(
            f"{BASE}:streamQuery",
            headers=auth_headers(token),
            json={
                "input": {
                    "user_id": user_id,
                    "session_id": session_id,
                    "message": request.message,
                }
            },
        )

    print(f"[query] streamQuery status={r.status_code}")

    if r.status_code != 200:
        raise HTTPException(r.status_code, f"Agent error: {r.text[:300]}")

    return {"result": extract_text(r.text)}


@app.get("/health")
async def health():
    return {"status": "ok"}
