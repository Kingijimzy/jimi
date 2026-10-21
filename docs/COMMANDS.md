# Jimi Commands Reference

## Instant local (no API)

| Say | Action |
|-----|--------|
| hi / hello | Greeting |
| time | Current time |
| battery | Battery % |
| device info | Phone model |
| open WhatsApp | Launch app |
| search for ... | Google search |
| open settings / wifi settings | System settings |
| volume up / mute / set volume 50 | Media volume |
| flashlight on/off | Torch |
| call 0803... | Open dialer |
| remember ... | Save memory |
| what do you remember | List memories |
| network status | Network Radar |
| go home / go back | Navigation |
| tap Claim / click Collect | Screen tap by text |
| read screen | Accessibility text dump |
| open FC Mobile | Game Sentinel |
| list macros | Saved macros |
| run macro fc daily | Replay macro |
| teach daily rewards | Start teach-a-task |
| stop teaching | Save recorded macro |
| cancel teaching | Discard recording |
| teaching status | Steps so far |
| add task ... | Task list |
| my tasks | Show open tasks |
| done task ... | Complete task |
| help | Capability summary |

## Voice

- Tap **mic** → speak → same as typing a command
- Replies can be spoken (TTS) — toggle in Settings
- **Stop** button cancels agent + speech while busy

## Teach-a-task (Grok Bot style)

```
teach daily rewards
open FC Mobile
tap Claim
tap Collect
stop teaching
```

Then later: `run macro daily rewards`

While teaching, open app / tap / home / back are recorded automatically.

## AI + tools

If a request is not matched locally, Jimi asks the free/paid model.
The model may reply with:

```
TOOL: open_app {"name":"Chrome"}
TOOL: web_search {"query":"FC Mobile tips"}
TOOL: click_text {"text":"Claim"}
```

Jimi runs the tool and can continue up to a few steps (protects free tier).

## Setup tips

1. Enable **Accessibility** for Jimi
2. Allow **Microphone** for voice
3. Add free API keys in **Settings** (Groq or Gemini first)
4. For FC Mobile: Accessibility on, then `run macro fc daily`
5. On MIUI/Redmi: lock app in Recents, allow autostart
