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
| add task ... | Task list |
| my tasks | Show open tasks |
| done task ... | Complete task |
| help | Capability summary |

## AI + tools

If a request is not matched locally, Jimi asks the free/paid model.
The model may reply with:

```
TOOL: open_app {"name":"Chrome"}
TOOL: web_search {"query":"FC Mobile tips"}
TOOL: click_text {"text":"Claim"}
```

Jimi runs the tool and can continue up to a few steps.

## Setup tips

1. Enable **Accessibility** for Jimi
2. Add free API keys in **Settings** (Groq or Gemini first)
3. For FC Mobile: `run macro fc daily` after Accessibility is on
