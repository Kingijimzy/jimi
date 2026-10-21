# Jimi

**Your personal hybrid AI assistant for Android**  
Local-first • Free APIs • Phone control • Voice • Macros • Evolving skills  
Built for a real Jarvis-like experience on your phone.

**Repo:** https://github.com/Kingijimzy/jimi

---

## What works now

| Feature | Status |
|---------|--------|
| Animated Orb UI (idle / listening / thinking / speaking / acting / error) | ✅ |
| Hybrid Model Router (Groq / Gemini / OpenRouter / DeepSeek) | ✅ |
| Real HTTP chat + tool-calling loop | ✅ |
| Settings (API keys, TTS toggle, safety toggle) | ✅ |
| Local commands (apps, search, volume, flashlight, dial...) | ✅ |
| Memory + Tasks | ✅ |
| Accessibility (tap, swipe, click text, type, read screen) | ✅ |
| Game Sentinel (FC Mobile) | ✅ |
| Macros + **TeachMode** (teach once, run forever) | ✅ |
| Network Radar | ✅ |
| Voice input (STT) + TTS speak | ✅ |
| Stop button while busy | ✅ |

### Example commands

```
Open FC Mobile
Run macro fc daily
Teach daily rewards → open FC Mobile → tap Claim → stop teaching
Network status
Tap Claim
Go home
Add task buy data
Remember my team is Chelsea
```

See [docs/COMMANDS.md](docs/COMMANDS.md) and [docs/SETUP.md](docs/SETUP.md).

---

## Architecture

```
Jimi/
├── Local Engine          rules + macros + teach mode
├── Agent Core            reasoning + tool loop
├── Model Router          Local → Free APIs → Paid
├── Tools                 phone, screen, browser open
├── Skills                Game Sentinel, Network Radar
├── Memory & Tasks
├── Voice                 STT + TTS
└── Orb UI + Settings
```

**Hybrid design:** everyday commands stay local (unlimited). Free APIs are used only when needed, with short context and limited tool steps so tasks finish before rate limits.

---

## Setup

1. Clone → open in **Android Studio**
2. Build & install on device (Android 9+)
3. Enable **Accessibility** for Jimi
4. Allow **Microphone** for voice
5. Settings → paste free API key (Groq or Gemini recommended)

Target: real phones including **Redmi A3 Pro** (4 GB class).

---

## Status

🚧 Active development. Core hybrid agent, phone control, voice, and FC Mobile macros are in place.

**Next:** stronger confirmations for dial, richer teach capture, connectors (Gmail / YouTube / GitHub).

---

**Jimi is being built step by step into a real personal Jarvis.**
