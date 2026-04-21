# Personal Shopper — AI-Powered Shopping Assistant

An Android app that uses **3 parallel AI agents** to research any product and give you a synthesized recommendation — pulling from Google Shopping, Reddit reviews, and YouTube reviews simultaneously.

---

## What It Does

1. You type a product query (e.g. "best wireless headphones under 5000 INR")
2. Three AI agents run in parallel on Google Cloud:
   - **Google Shopping agent** — finds prices, listings, and availability
   - **Reddit agent** — finds real user reviews and community opinions
   - **YouTube agent** — finds video reviews and expert opinions
3. A **Synthesizer agent** (Gemini 2.0 Flash) reads all three results and writes one final recommendation
4. The recommendation streams back to the app with a live typing effect and clickable links

---

## How It Works — Full Flow

```
You type a query
        │
        ▼
Android App (Kotlin / Jetpack Compose)
        │  sends HTTP POST /query via Retrofit
        ▼
Cloud Run Proxy  ◄── Docker container running Python + FastAPI
        │  authenticates with Google Cloud using a service account
        │  creates a session on Vertex AI Agent Engine
        │  calls the streaming query API
        ▼
Vertex AI Agent Engine  ◄── hosted on Google ADK
        │
        ├── ParallelAgent (all 3 run simultaneously)
        │       ├── Google Shopping Tool  ──► searches product listings
        │       ├── Reddit Search Tool    ──► finds community reviews
        │       └── YouTube Search Tool   ──► finds video reviews
        │
        └── Synthesizer Agent (Gemini 2.0 Flash)
                reads all 3 results and writes a single recommendation
        │
        ▼
Cloud Run Proxy parses the streaming response
        │
        ▼
Android App displays the recommendation with typewriter animation
```

**Why Docker?** The Cloud Run proxy is packaged as a Docker container — a self-contained box that includes Python, all libraries, and the server. Docker ensures the code runs identically whether on your laptop or Google's servers. The `Dockerfile` is the recipe that builds this box.

**Why a proxy?** The Vertex AI Agent Engine API requires a Google Cloud auth token. You cannot embed credentials in an Android app safely. Instead, the Cloud Run proxy runs on Google's infrastructure with a service account identity, fetches the auth token automatically, and forwards the request on behalf of the app.

---

## Project Structure

```
PersonalShopperApp/
├── app/                          # Android app (Kotlin)
│   └── src/main/java/com/ashish/personalshopperagent/
│       ├── MainActivity.kt           # Entry point, navigation
│       ├── data/api/                 # Retrofit HTTP client
│       ├── data/model/               # Request/response data classes
│       ├── viewmodel/
│       │   └── ShopperViewModel.kt   # State, API calls, animation timeline
│       └── ui/
│           ├── screens/
│           │   ├── HomeScreen.kt         # Search bar
│           │   ├── SearchingScreen.kt    # Live agent progress animation
│           │   └── ResultScreen.kt       # Typewriter result view
│           ├── components/
│           │   └── MarkdownText.kt       # Markdown renderer with clickable links
│           └── theme/                    # Colors, typography
│
└── cloud_proxy/                  # Cloud Run backend (Python)
    ├── main.py                   # FastAPI server — auth, sessions, response parsing
    ├── requirements.txt          # Python dependencies
    └── Dockerfile                # Container recipe for Cloud Run
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Android UI | Jetpack Compose + Material 3 |
| Android State | ViewModel + StateFlow + Coroutines |
| Android HTTP | Retrofit + Gson |
| Backend Language | Python |
| Backend Framework | FastAPI + Uvicorn |
| Backend HTTP | httpx (async) |
| Containerization | Docker |
| Cloud Runtime | Google Cloud Run (serverless) |
| AI Agent Framework | Google ADK (Agent Development Kit) |
| AI Hosting | Vertex AI Agent Engine |
| AI Model | Gemini 2.0 Flash |
