# Minecraft Mods Collection

A collection of custom Minecraft mods.

## Mods

### [LabsCraft](./labs-craft/)

A Fabric mod (1.21.4) where you play as an APM intern at "Labs" learning to use Flow, an AI-powered generation platform. Features an auto-generating Googleplex office campus on a superflat world, quest-driven progression with NPC guide Josh Woodward, TPU resource mining, and AI console crafting.

**Key Features:**
- Auto-generating 200×200 Googleplex office with labs, cafeteria, and mine
- Quest system with TPU rewards and AI console crafting
- Superflat world preset with ore generation for TPU mining
- 174+ unit tests

[See the LabsCraft README for full details →](./labs-craft/README.md)

### [MTL Memories](./montreal-mod/)

A Minecraft mod celebrating Montreal and McGill University memories from 2004-2008. Features custom items like poutine, Montreal bagels, and Habs jerseys, along with quest storylines about frosh week, academic life, and Montreal culture.

**Key Features:**
- Montreal-themed items (poutine, smoked meat, bagels, metro tickets, etc.)
- Quest system with storylines about McGill and Montreal life
- Custom item effects and interactions

[See the Montreal mod README for full details →](./montreal-mod/README.md)

## Requirements

| Mod | Minecraft | Loader | Java |
|-----|-----------|--------|------|
| LabsCraft | 1.21.4 | Fabric | 21 |
| MTL Memories | 1.20.1 | Forge | 17 |

## Quick Start

Navigate to a mod directory and run:
```bash
./gradlew runClient
```

## Building Mods

To build a mod as a .jar file:
```bash
cd <mod-directory>
./gradlew build
```

The compiled mod will be in `build/libs/`

## Development

Each mod is a separate Forge mod project with its own build configuration and source code. See individual mod directories for specific setup and development instructions.

---

Made with love for Minecraft and Montreal.
