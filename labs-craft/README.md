# LabsCraft

A Minecraft Fabric mod (1.21.4) where you play as an APM intern at "Labs" learning to use Flow, an AI-powered image and video generation platform.

## Overview

LabsCraft introduces a quest-driven gameplay experience centered around collecting TPUs (Tensor Processing Units) and building AI generation consoles. Meet Josh Woodward, your PM guide, and progress through the internship by crafting increasingly powerful generation hardware.

## Custom Map

LabsCraft includes an auto-generating Googleplex office campus built on a superflat world. On first world load, the mod generates a 200×200 block structure centered on the world spawn with:

- **Lobby** with reception desk, Flow Crafting Table, and Josh Woodward NPC
- **Flow Lab**, **Genie Lab**, and **Veo Lab** with their respective consoles
- **Cafeteria**, **Server Room**, **Mine Entrance**, and exterior courtyard
- **Superflat terrain**: bedrock → deepslate → stone → dirt → grass (65 blocks) with ore generation enabled for TPU mining

The map generates automatically on first server start, or manually via `/labscraft build`. Generation is idempotent — restarting the server won't duplicate the structure.

### Superflat World Preset

For the intended experience, create a new world using the **LabsCraft Googleplex** superflat preset, which provides a clean flat terrain with underground ores for TPU mining.

## Features

### Agentic NPC System (v0.2.0)

Josh Woodward is powered by an LLM-backed agent system. Instead of cycling through hardcoded dialogue, Josh perceives the game world, reasons about what to do, and generates dynamic responses in character.

**How it works:**
- A separate Node.js agent server receives world state from the Fabric mod every second
- The server runs an agentic loop: perceive (world state) → decide (should Josh act?) → reason (LLM) → act (SAY, WALK_TO, EMOTE, GIVE_ITEM, ADVANCE_QUEST)
- Josh remembers previous conversations across sessions (SQLite)
- Josh proactively speaks when you're stuck, in danger, or idle near an objective
- Conversation history is automatically summarized to manage context window

**Supported LLM providers** (in priority order):
1. Claude API (Sonnet 4.5) — best character consistency
2. Gemini API (2.5 Flash) — fast alternative
3. Ollama (local) — free, offline play
4. Hardcoded fallback — works with no server running at all

**Trigger system — Josh speaks when:**
- You send a chat message near him
- You right-click (interact with) him
- You've been idle near a quest objective for too long
- You complete a quest objective
- You're in danger (low health + hostile mobs nearby)
- You're standing close and he hasn't spoken in a while

### NPCs

**Josh Woodward**
- Your PM guide who introduces you to Labs
- Speaks in deadpan corporate jargon ("That's a P0 for Q1", "I have a hard stop in 30")
- Gives TPU rewards at quest milestones
- Invulnerable (he has meetings to attend)
- With agent server: generates dynamic, context-aware dialogue via LLM
- Without agent server: falls back to static dialogue seamlessly

### Resources

**TPU (Tensor Processing Unit)**
- Core resource for building consoles
- Obtain by:
  - Mining TPU Ore (Y: -64 to 32, ~6 per vein, 8 veins/chunk)
  - Crafting: Gold Nuggets + Redstone + Iron Ingot
  - Quest rewards from Josh

**TPU Ore**
- Spawns in stone (Y > 0) and deepslate (Y < 0) variants
- Drops 1-2 TPUs when mined (Fortune compatible)
- Silk Touch returns the ore block

### Blocks

**Flow Crafting Table**
- Custom crafting station for building consoles
- Insert TPUs (up to 10 slots)
- Craft Nano Banana Console (5 TPUs) or Veo Console (10 TPUs)
- Recipe: Crafting Table + Iron Ingots + Redstone

**Nano Banana Console**
- Image generation console (yellow/gold themed)
- Requires 5 TPUs to craft
- Faster generation time (3 seconds)

**Veo Console**
- Video generation console (purple themed)
- Requires 10 TPUs to craft
- Standard generation time (5 seconds)

**Flow Console** (Legacy)
- Original video generation console
- Still functional but superseded by the TPU-based system

### Quest Progression

1. **NOT_STARTED** - Find and interact with Josh Woodward
2. **FLOW_INTRO** - Receive 5 TPUs, learn about the crafting system
3. **LEARNING_PIPELINE** - Build a console and use it
4. **FIRST_GENERATION** - Complete your first generation
5. **COMPLETED** - Talk to Josh for 5 bonus TPUs, full access unlocked

## Running the Agent Server

```bash
# 1. Set your API key (pick one)
export ANTHROPIC_API_KEY="sk-ant-..."   # Claude
export GEMINI_API_KEY="..."             # or Gemini

# 2. Start the agent server
cd agent-server
npm install
npm run dev

# 3. Verify it's running
curl http://localhost:3001/health
# → {"status":"ok","version":"0.2.0","llm":"claude"}

# 4. Start Minecraft with the mod — Josh will use the agent server automatically
```

The mod works fine without the agent server running — Josh falls back to static dialogue.

## Recipes

### TPU
```
G R G
R I R
G R G

G = Gold Nugget, R = Redstone, I = Iron Ingot
```

### Flow Crafting Table
```
I R I
R C R
I R I

I = Iron Ingot, R = Redstone, C = Crafting Table
```

### Consoles
Built in the Flow Crafting Table:
- **Nano Banana Console**: 5 TPUs
- **Veo Console**: 10 TPUs

### Commands

**`/labscraft build`**
- Generates the Googleplex office at the player's position
- Spawns Josh Woodward NPC in the lobby
- Marks the world as generated (prevents auto-gen duplication)
- Requires operator permissions

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.4
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Place the mod JAR in your `mods` folder

## Development

```bash
# Build the mod
./gradlew build

# Run the client
./gradlew runClient

# Run tests (174+ unit tests)
./gradlew test

# Run the agent server (separate terminal)
cd agent-server && npm run dev
```

## Architecture

```
┌──────────────────────────────────────────┐
│          Minecraft (Fabric 1.21.4)       │
│                                          │
│  JoshWoodwardEntity ◄──► AgentBridge     │
│       │                    │             │
│  WorldStateCollector   ActionExecutor    │
│       │                    ▲             │
└───────┼────────────────────┼─────────────┘
        │ HTTP POST          │ JSON actions
        ▼                    │
┌──────────────────────────────────────────┐
│        Agent Server (Node.js)            │
│                                          │
│  triggers.ts → context.ts → LLM → parse │
│       │                                  │
│  memory.ts    summarizer.ts    SQLite    │
└──────────────────────────────────────────┘
```

### Testing

The project includes 174+ unit tests using JUnit 5 and Mockito. Tests use pure-Java logic mirrors to avoid Minecraft class bootstrap dependencies, enabling fast isolated testing of:

- Quest stage transitions and serialization
- Crafting table TPU counting and cost validation
- Console generation state machines
- Googleplex structural layout and room positioning
- Auto-generation origin math and spawn positions
- Persistent state tracking

## File Structure

```
src/main/java/com/labscraft/
├── LabsCraft.java              # Main mod initializer
├── LabsCraftClient.java        # Client-side initialization
├── agent/                      # Agentic NPC bridge
│   ├── AgentBridge.java        # Async HTTP client to agent server
│   ├── AgentConfig.java        # Config loader
│   ├── WorldStateCollector.java # Gathers world state for agent
│   ├── ActionExecutor.java     # Executes agent actions in-game
│   ├── RecentEventsTracker.java # Captures events between ticks
│   └── ChatListener.java       # Server chat → agent events
├── block/                      # Block classes
├── block/entity/               # Block entities
├── command/                    # Commands (/labscraft build)
├── entity/                     # Entity classes (Josh Woodward)
├── item/                       # Items (TPU, spawn eggs)
├── network/                    # Client-server packets
├── quest/                      # Quest system
├── screen/                     # GUI screens and handlers
└── world/                      # World generation, Googleplex map
    ├── GoogleplexGenerator.java      # 200×200 structure builder
    ├── GoogleplexAutoGenerator.java  # First-load auto-generation
    └── GoogleplexState.java          # Persistent generation state

agent-server/
├── src/
│   ├── index.ts                # Express server + tick endpoint
│   ├── types.ts                # WorldState, AgentAction types
│   ├── config.ts               # Config loader
│   ├── database.ts             # SQLite setup
│   ├── context.ts              # LLM context builder
│   ├── triggers.ts             # should_respond() logic
│   ├── memory.ts               # Memory extraction
│   ├── summarizer.ts           # Conversation compression
│   ├── llm/
│   │   ├── provider.ts         # Abstract LLM interface
│   │   ├── claude.ts           # Claude API provider
│   │   ├── gemini.ts           # Gemini API provider
│   │   └── ollama.ts           # Ollama local provider
│   └── prompts/
│       └── josh.ts             # Josh's system prompt
├── config.json                 # Server configuration
└── data/
    └── memory.db               # SQLite (created at runtime)
```

## Credits

- Mod created for Minecraft Montreal
- Built with Fabric API 0.110.5 for Minecraft 1.21.4
