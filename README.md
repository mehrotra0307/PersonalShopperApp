# Personal Shopper — AI-Powered Shopping Assistant

An Android app that uses **3 parallel AI agents** to research any product and give you a synthesized recommendation — pulling from Google Shopping, Reddit reviews, and YouTube reviews simultaneously.

---

## What It Does

1. You type a product query (e.g. "best wireless headphones under 2000 INR")
2. Three AI agents run in parallel on Google Cloud:
   - **Google Shopping agent** — finds prices, listings, availability
   - **Reddit agent** — finds real user reviews and community opinions
   - **YouTube agent** — finds video reviews and expert opinions
3. A **Synthesizer agent** reads all three results and writes a final recommendation
4. The recommendation is streamed back to the app with a live typing effect

---

## Architecture

```
Android App (Kotlin / Jetpack Compose)
        │
        │  HTTP POST /query
        ▼
Cloud Run Proxy (Python / FastAPI)
        │
        │  Vertex AI Agent Engine API (streamQuery)
        ▼
Vertex AI Agent Engine (Google ADK)
        ├── ParallelAgent
        │       ├── Google Shopping Tool
        │       ├── Reddit Search Tool
        │       └── YouTube Search Tool
        └── Synthesizer (Gemini 2.0 Flash)
```

**Why a proxy?** The Vertex AI Agent Engine API requires a GCP auth token. You can't put GCP credentials in an Android app (security risk). The Cloud Run proxy runs with a GCP service account identity, gets the token server-side, and forwards requests securely.

---

## Project Structure

```
PersonalShopperApp/
├── app/                          # Android app (Kotlin)
│   └── src/main/java/com/ashish/personalshopperagent/
│       ├── MainActivity.kt       # Entry point, navigation
│       ├── data/
│       │   ├── api/              # Retrofit HTTP client + API interface
│       │   └── model/            # Request/response data classes
│       ├── viewmodel/
│       │   └── ShopperViewModel.kt  # State management, API calls, animation timeline
│       └── ui/
│           ├── screens/
│           │   ├── HomeScreen.kt      # Search bar
│           │   ├── SearchingScreen.kt # Live agent progress view
│           │   └── ResultScreen.kt    # Typed recommendation view
│           ├── components/
│           │   └── MarkdownText.kt    # Markdown renderer with clickable links
│           └── theme/                 # Colors, typography
│
└── cloud_proxy/                  # Cloud Run backend (Python)
    ├── main.py                   # FastAPI server — auth, session, NDJSON parsing
    ├── requirements.txt          # Python dependencies
    └── Dockerfile                # Container definition for Cloud Run
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Android UI | Jetpack Compose + Material 3 |
| Android State | ViewModel + StateFlow + Coroutines |
| Android HTTP | Retrofit + Gson |
| Backend | Python + FastAPI + Uvicorn |
| Backend HTTP | httpx (async) |
| Cloud Runtime | Google Cloud Run (serverless containers) |
| AI Agents | Google ADK (Agent Development Kit) |
| AI Hosting | Vertex AI Agent Engine (Reasoning Engine) |
| AI Model | Gemini 2.0 Flash |

---

## Cloud Infrastructure

- **Cloud Run service**: `personal-shopper-proxy` (us-central1)
- **Vertex AI Agent Engine resource ID**: `1786156639322112`
- **GCP project**: `personal-shopper-agent`
- **Agent uses**: Session-based `:streamQuery` API (ADK 0.5+)

The proxy pre-warms a session at container startup to avoid the 12-second session initialization delay on the first user request.

---

## Running Locally

### Android App
Open the project in Android Studio and run on a device or emulator. The app connects to the deployed Cloud Run proxy by default.

### Cloud Proxy (local test)
```bash
cd cloud_proxy
pip install -r requirements.txt
gcloud auth application-default login
uvicorn main:app --reload --port 8080
```

### Deploy Proxy to Cloud Run
```bash
gcloud run deploy personal-shopper-proxy \
  --source cloud_proxy \
  --region us-central1 \
  --project personal-shopper-agent \
  --allow-unauthenticated \
  --memory 512Mi \
  --timeout 300
```

---

## Security Notes

- `local.properties` is excluded from git (contains local SDK path)
- No API keys or credentials are hardcoded anywhere
- The Cloud Run proxy uses GCP Application Default Credentials (service account identity)
- The Android app only talks to the Cloud Run URL — no GCP credentials on device
