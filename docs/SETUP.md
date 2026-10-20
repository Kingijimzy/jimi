# Jimi Setup Guide

## Requirements
- Android Studio (recent)
- JDK 17
- Phone or emulator on Android 9+ (minSdk 28)
- Optional: free API keys (Groq / Gemini / OpenRouter)

## Build

```bash
git clone https://github.com/Kingijimzy/jimi.git
cd jimi
# Open folder in Android Studio, sync Gradle, Run
```

## First run checklist

1. **Install the app**
2. **Enable Accessibility**
   - Settings → Accessibility → Installed apps → Jimi → On
   - Needed for tap / swipe / read screen / macros / FC Mobile
3. **Microphone** (for voice)
   - Allow when prompted
4. **API keys** (for full AI)
   - Open Jimi → Settings gear
   - Paste Groq and/or Gemini key first

## Quick tests

| Command | Expected |
|---------|----------|
| `hi` | Local greeting |
| `time` / `battery` | Instant local answer |
| `network status` | Network Radar |
| `open Chrome` | App launches |
| `add task test` / `my tasks` | Task list |
| Mic button | Listening → transcript → agent |
| `run macro fc daily` | Needs Accessibility + FC Mobile installed |

## Notes for Redmi / MIUI

- Lock Jimi in Recent apps so the system does not kill it
- Allow Autostart / Background activity if macros stop
- Battery saver can delay background work

## Safety

- Jimi does not store passwords or OTPs in memory by design
- Sensitive tool confirmations can be expanded further
- Review Accessibility access; revoke anytime in system settings
