# Research Notes — Good Features & Optimizations for Jimi

Based on current open-source Android agents, hybrid AI systems, and personal assistant trends (2026).

## High-Value Features to Prioritize

### 1. Hybrid Routing (already started)
- Local rules / macros first (unlimited)
- Tiny on-device model for classification (later)
- Free APIs for medium tasks
- Paid (DeepSeek) for hard reasoning
- Confidence-based escalation

### 2. Self-Planning + Re-evaluation
- Break goals into steps
- After each tool result, check if goal is closer
- Replan if a step fails (instead of looping blindly)
- Max steps lower on free tiers

### 3. Strong Safety Layer
- Always confirm before: send message, delete, pay, post, change security settings
- Visible stop button
- Action log (tool name + success/fail only — privacy friendly)

### 4. Skills / Routines that evolve
- Built-in: Game Sentinel, Network Radar, Research, Coding
- User can teach by example (record sequence)
- AI can propose new skills from repeated patterns
- Success/fail counters so good skills get preferred

### 5. Memory that improves
- Explicit "remember this"
- Automatic preference extraction (with user control)
- Never store passwords / OTPs / cards
- User can view / edit / delete in Settings

### 6. Habit & Routine Detection (future)
- Notice repeated sequences (e.g. open Gmail → Calendar every morning)
- Offer to turn them into one-tap routines

### 7. Screen Understanding Priority
- Prefer Accessibility tree (fast, private, low cost)
- Fall back to vision/screenshot only when needed
- Good for low-RAM phones

### 8. Optimizations for Redmi A3 Pro / low-end devices
- Keep conversation history short
- Aggressive prompt compression
- Lower max tokens on free providers
- Avoid always-on wake word (battery)
- Lazy load vision models
- Limit concurrent background work
- Macros that need zero AI calls

## Inspiration Sources
- OpenDroid, CellClaw, Omni, Aries AI, Google ARTEMIS, LiteRT-LM hybrid patterns
- Personal Jarvis-style agents with plugins and confirmation gates

## Next Implementation Order (suggested)
1. Wire real Agent into UI + Orb reactions
2. Expand Accessibility control (tap, type, swipe, open app)
3. Settings screen for API keys
4. Browser open + basic navigation
5. Simple tool-calling loop
6. Memory UI
7. First AI-generated skill prototype
