# LabsCraft

A Minecraft Fabric mod (1.21.4) where you play as an APM intern at "Labs", learning to
use Flow — an AI image and video generation platform. Your PM is Josh Woodward, an NPC
backed by an LLM who perceives the world, reasons about it, and acts.

**v2** is a ground-up rebuild. See [Rebuild notes](#rebuild-notes-v1--v2) for what changed and why.

## Quick start

```bash
# Build and run the game
./gradlew build
./gradlew runClient

# Optional: run the agent server for LLM-driven Josh (separate terminal)
cd agent-server
npm install
npm run dev
```

**The mod is fully playable with the agent server stopped.** Josh falls back to static,
stage-aware dialogue and the entire quest line is completable. The agent server is an
enhancement, not a dependency.

The server itself also works with **no API keys at all** — it ships a static provider
that returns sensible in-character actions. Add a key to `agent-server/config.json`
(copy `config.example.json`) or set `ANTHROPIC_API_KEY` / `GEMINI_API_KEY` to get real
LLM behavior.

## Gameplay

Mine **TPU Ore** (below Y 32) → craft **TPUs** → build a **Flow Crafting Table** →
assemble **consoles** → generate artifacts.

| Console | Cost | Time | Produces |
|---|---|---|---|
| Flow Console | 1 TPU | 100 ticks | Flow Sketch |
| Nano Banana Console | 1 TPU | 140 ticks | Generated Image |
| Veo Console | 3 TPU | 200 ticks | Generated Video |

Each artifact gets a distinct generated name, prompt, model, and seed in its lore.

### Quest line

Six stages, each with tracked objectives that advance from real game events. A stage
completes only when its objectives are genuinely satisfied.

| Stage | Objectives |
|---|---|
| Orientation Day | Meet Josh |
| Compute Procurement | Mine 3 TPU Ore · Craft 2 TPU |
| Infrastructure Buildout | Craft Flow Crafting Table · Craft Nano Banana Console |
| First Launch | Generate an image · Demo it to Josh |
| Scale to Video | Mine 5 TPU Ore · Craft Veo Console · Generate a video |
| Return Offer | — |

### Commands

```
/labscraft quest                                  # status
/labscraft quest set|advance|reset                # op 2; advance goes through validation
/labscraft josh spawn|emote <e>|say <text>        # op 2
/labscraft googleplex generate <x> <y> <z> [seed] [force]
/labscraft googleplex info
```

`/labscraft googleplex generate` builds the Labs office: lobby, meeting room with a glass
wall, micro-kitchen, open-plan desk pods, and a Flow Lab pre-stocked with all three
consoles and a crafting table. It refuses to overwrite player builds unless you pass `force`.

## Architecture

```
┌──────────────────────────────────────────────┐
│           Minecraft (Fabric 1.21.4)          │
│                                              │
│  JoshWoodwardEntity ◄──► AgentBridge         │
│         │                    │               │
│  WorldStateCollector    ActionExecutor       │
│         │                    ▲               │
│  QuestManager  ◄─ authority ─┘               │
└─────────┼────────────────────┼───────────────┘
          │ POST /tick         │ validated actions
          ▼                    │
┌──────────────────────────────────────────────┐
│           Agent Server (Node + TS)           │
│                                              │
│  triggers → prompt → LLM (tool-call) → actions│
│      │                                       │
│  memory.ts   summarizer   SQLite             │
└──────────────────────────────────────────────┘
```

Providers are tried in order: **Claude → Gemini → Ollama → static fallback**.

### Design rules

These are enforced, not aspirational:

1. **The mod never depends on the server.** Connection refused, timeout, malformed JSON,
   or a protocol mismatch all degrade silently to static dialogue. Logged once on
   transition, never per-poll.
2. **The LLM is untrusted input.** Every action is validated before execution — unknown
   types, non-allowlisted items, out-of-range quantities, absurd coordinates, and
   over-length messages are dropped individually while the rest still run.
3. **The quest system is the authority.** `ADVANCE_QUEST` is a *request*. An LLM cannot
   skip the player ahead; refusals name the unmet objectives.
4. **Nothing blocks the game thread.** Polling is async, once per second per player.

The wire contract lives in `PROTOCOL-V2` and both halves implement it independently.

## Testing

163 JVM tests + 51 agent-server tests.

```bash
./gradlew test                      # mod
cd agent-server && npm test         # server
```

Minecraft's test classpath isn't remapped, so game logic lives in pure-Java layers
(`com.labscraft.logic`, `com.labscraft.quest`'s state machine, `com.labscraft.agent`'s
validation/scheduling, `...world.structure.plan`) with thin Minecraft-facing wrappers.
That split is what makes the coverage meaningful rather than decorative.

There are also env-gated runtime harnesses that drive a fake player through real server
code paths:

```bash
LABSCRAFT_SMOKETEST=true ./gradlew runServer        # quest line end to end
LABSCRAFT_AGENT_SMOKETEST=true ./gradlew runServer  # chat → POST /tick → SAY
```

## Rebuild notes (v1 → v2)

v1 was deleted and rebuilt from a spec. Full accounting — approach, system map, token
costs, and an honest read of what the numbers do and don't prove — is in
**[docs/REBUILD.md](docs/REBUILD.md)**.

Headline: **6,785 → 12,105 LOC, 148 → 214 tests, 10 defects fixed, ~100 minutes**, built
by 7 parallel agents against a frozen wire contract, integrating with zero breaking
mismatches.

The rebuild targeted specific defects that made the core loop hollow:

- **Consoles produced nothing.** v1 ran a 100-tick progress bar and incremented a
  counter. No item, no reward. Now they consume TPU and emit real artifacts.
- **Quest objectives were fiction.** The wire type carried `objectives_completed` /
  `objectives_remaining` and the "player is stuck" trigger read them — but nothing ever
  populated them. Now they're first-class and event-driven.
- **The quest could not be finished.** `FIRST_GENERATION` was unreachable: the advance
  hook was an empty `if` block containing only a comment.
- **Ore dropped nothing.** Ores called `requiresTool()` but the mod shipped no block
  tags, so mining them yielded no items.
- **`delay_ticks` was unreliable.** Two unrelated clocks — a caller-supplied base tick
  and a private counter. Now one monotonic tick source.
- **LLM output was hand-parsed prose.** Now structured tool-calling with a schema.
- **Trigger state leaked.** Module-global maps keyed by player *name*, never evicted.
  Now UUID-keyed with TTL eviction and session-change reset.
- **A mixin config was declared with no mixins.** Now exactly one mixin, and it's used —
  Fabric ships no crafting event, so crafting detection needs it.

Also new in v2: a sixth quest stage (`Scale to Video`), real emote animations instead of
italic chat text, console screens using vanilla button clicks instead of four custom
packet classes, and a Googleplex generator built on a validated blueprint layer
(non-overlap, sealed shell, BFS reachability of every room).

## Requirements

- Java 21 · Minecraft 1.21.4 · Fabric Loader 0.16.9 · Fabric API 0.110.5
- Node 22+ for the agent server (optional)
