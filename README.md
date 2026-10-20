# Jimi

**Your personal hybrid AI assistant for Android**  
Local-first • Free APIs • Phone control • Browser automation • Evolving skills  
Built for a real Jarvis-like experience on your phone.

**Repo:** https://github.com/Kingijimzy/jimi

---

## What works now

| Feature | Status |
|---------|--------|
| Beautiful animated Orb UI (6 states) | ✅ |
| Hybrid Model Router (Groq / Gemini / OpenRouter / DeepSeek) | ✅ |
| Settings screen for API keys | ✅ |
| Local commands (time, battery, open app, search, volume, flashlight...) | ✅ |
| Memory ("remember ...") | ✅ |
| Accessibility Service (tap, swipe, click text, type, read screen) | ✅ |
| Game Sentinel (FC Mobile open + help) | ✅ |
| Macros / teach-a-task (Grok Bot style) + sample `fc daily` | ✅ |
| Network Radar (Wi‑Fi / mobile status) | ✅ |
| Screen commands (go home, go back, tap text, read screen) | ✅ |

### Example commands
- `Open FC Mobile`
- `Run macro fc daily`
- `Network status`
- `Tap Claim`
- `Go home` / `Go back`
- `Read screen`
- `Search for weather today`
- `Remember my favorite team is Chelsea`
- `Volume up` / `Flashlight on`

---

## Vision

Practical evolving personal AI that can:

- **Reason** about multi-step goals
- **Act** on the phone (apps, screen, browser)
- **Learn** skills (AI-Generated + macros)
- **Connect** accounts later (Gmail, YouTube, GitHub)
- **Code** and research
- **Remember** with user control
- Run well on lower-end devices (Redmi A3 Pro class)

Upgrade path: free APIs now → DeepSeek / paid later with almost no code change.

---

## Architecture

```
Jimi/
├── Local Engine          (rules, macros)
├── Agent Core            (reasoning)
├── Model Router          (Local → Free → Paid)
├── Tools
│   ├── Phone + Screen control
│   ├── Browser open / search
│   ├── Skills (Game Sentinel, Network Radar, ...)
│   └── Macros (teach once, run forever)
├── Memory
├── Orb UI
└── Safety / confirmations
```

---

## Setup (developer)

1. Clone the repo
2. Open in Android Studio
3. Build & install on device
4. Enable **Accessibility** for Jimi
5. Open **Settings** in the app and paste a free API key (Groq or Gemini)

---

## Status

🚧 Active development. Core hybrid agent + phone control + FC Mobile macros are working.

---

**Jimi is being built step by step into a real personal Jarvis.**
