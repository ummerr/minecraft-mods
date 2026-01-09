# Minecraft Mods Collection

A collection of custom Minecraft mods built with Minecraft Forge.

## Mods

### [MTL Memories](./montreal-mod/)

A Minecraft mod celebrating Montreal and McGill University memories from 2004-2008. Features custom items like poutine, Montreal bagels, and Habs jerseys, along with quest storylines about frosh week, academic life, and Montreal culture.

**Key Features:**
- Montreal-themed items (poutine, smoked meat, bagels, metro tickets, etc.)
- Quest system with storylines about McGill and Montreal life
- Custom item effects and interactions

[See the Montreal mod README for full details →](./montreal-mod/README.md)

## Requirements

- Java 17
- Minecraft 1.20.1
- Forge 47.4.10

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
