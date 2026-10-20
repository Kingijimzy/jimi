# Jimi

**Your personal hybrid AI assistant for Android**  
Local-first • Free APIs • Phone control • Browser automation • Evolving skills  
Built for a real Jarvis-like experience on your phone.

---

## Vision

Jimi is designed to be a practical, evolving personal AI that can:

- **Reason** about complex multi-step goals
- **Act** on your phone (apps, settings, screen, browser)
- **Learn** new skills automatically (AI-Generated Skills)
- **Connect** to your real accounts (Gmail, YouTube, GitHub, etc.)
- **Code**, research, and solve problems
- **Remember** and improve over time
- Run well even on lower-end devices (optimized hybrid mode)

Later you can upgrade to paid models (DeepSeek, etc.) with almost zero code changes.

---

## Core Features (Planned)

### 1. Hybrid Intelligence
- **Local first** → rules + macros + tiny on-device model
- **Free APIs** (Groq, Gemini Flash-Lite, OpenRouter, etc.) when needed
- **Paid upgrade path** (DeepSeek and others)
- Smart routing so most tasks finish before rate limits are hit

### 2. Phone Control
- Full Accessibility Service control (tap, swipe, type, open apps, keys...)
- Screen reading + vision when available
- Safe confirmation for sensitive actions

### 3. Browser Automation
- Open browser, navigate, search, fill forms, extract content
- Multi-step web workflows

### 4. Evolving Skills System
- **AI-Generated Skills** — Jimi can create new skills from examples
- Skills can improve themselves over time
- Built-in advanced skills:
  - Game Sentinel (game monitoring & automation)
  - Network Radar (network status, speed, diagnostics)
  - Research Agent
  - Coding Assistant
  - Personal workflow skills

### 5. Account Connections
- Gmail (read, draft, summarize)
- YouTube (search, play, summarize)
- GitHub (repos, issues, PRs, code)
- More connectors later

### 6. Memory & Self-Improvement
- Long-term memory with user control
- Task tracking across sessions
- Learns from your corrections and preferences

### 7. Beautiful Jarvis-style UI
- Floating orb / status states (idle, listening, thinking, speaking, acting)
- Clean dark elegant theme
- Voice + text input
- Transparent progress of what Jimi is doing

---

## Architecture (High Level)

```
Jimi/
├── Local Engine          (rules, macros, tiny model)
├── Agent Core            (reasoning + planning)
├── Model Router          (Local → Free APIs → Paid)
├── Tools &
│   ├── Phone Control
│   ├── Browser Automation
│   ├── Account Connectors (Gmail, YouTube, GitHub...)
│   ├── Skills System (AI-generated + evolving)
│   └── Game Sentinel, Network Radar, Research, Coding
├── Memory & Tasks
├── Voice & Avatar UI
└── Safety Layer         (confirmations, limits, privacy)
```

---

## Target Device

Optimized for real-world phones, including lower-end devices like Redmi A3 Pro (4 GB RAM).

---

## Status

🚧 Project just started.  
Core structure and hybrid foundation are being built now.

---

## License

Private / Personal use for now.

---

**Jimi is being built step by step to become a real personal Jarvis.**
