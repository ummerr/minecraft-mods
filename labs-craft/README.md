# LabsCraft

A Minecraft Fabric mod (1.21.4) where you play as an APM intern at "Labs" learning to use Flow, an AI-powered image and video generation platform.

## Overview

LabsCraft introduces a quest-driven gameplay experience centered around collecting TPUs (Tensor Processing Units) and building AI generation consoles. Meet Josh Woodward, your PM guide, and progress through the internship by crafting increasingly powerful generation hardware.

## Features

### NPCs

**Josh Woodward**
- Your PM guide who introduces you to Labs
- Gives TPU rewards at quest milestones (5 TPUs to start, 5 more after first generation)
- Speaks in corporate jargon when idle
- Invulnerable (he has meetings to attend)

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
```

## File Structure

```
src/main/java/com/labscraft/
├── LabsCraft.java              # Main mod initializer
├── LabsCraftClient.java        # Client-side initialization
├── block/                      # Block classes
│   ├── ModBlocks.java
│   ├── FlowConsoleBlock.java
│   ├── FlowCraftingTableBlock.java
│   ├── NanoBananaConsoleBlock.java
│   └── VeoConsoleBlock.java
├── block/entity/               # Block entities
├── entity/                     # Entity classes (Josh Woodward)
├── item/                       # Items (TPU, spawn eggs)
├── network/                    # Client-server packets
├── quest/                      # Quest system
├── screen/                     # GUI screens and handlers
└── world/                      # World generation (ore spawning)
```

## Credits

- Mod created for Minecraft Montreal
- Built with Fabric API 0.110.5 for Minecraft 1.21.4
