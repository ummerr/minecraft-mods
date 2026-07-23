# PRD: Googleplex Office (LabsCraft v2, gate G6)

The Labs office the player works in: a recognizable multi-room Googleplex
building containing the lobby, meeting room, micro-kitchen, open-plan desk
pods, and the Flow Lab — the natural home of the Flow Crafting Table and the
three generation consoles.

This replaces v1's 550-line `GoogleplexGenerator`, which mixed layout math and
block placement in one Minecraft-coupled class; its 413-line test could only
exercise trivia because the test classpath cannot import Minecraft classes.

## Architecture

Two layers, following the established `com.labscraft.logic` / `com.labscraft.quest`
pattern (pure core + thin Minecraft shell):

| Layer | Package | Minecraft imports | Tested by |
|---|---|---|---|
| Blueprint planner | `com.labscraft.world.structure.plan` | none | 39 JUnit tests, plain JVM |
| Placer + command | `com.labscraft.world.structure`, `com.labscraft.command.GoogleplexCommands` | yes | dedicated-server runtime verification |

### Pure layer (`world.structure.plan`)

- **`BlockSpec`** — abstract materials (WALL, GLASS, DESK, CONSOLE_FLOW,
  CARPET_BLUE, ...) with a `passable()` walkability contract.
- **`Room`, `RoomType`, `Door`** — records for the floor-plan metadata.
- **`GoogleplexBlueprint`** — immutable 31 x 7 x 27 grid of `BlockSpec`s plus
  room/door lists. y=0 floor, y=1..4 interior, y=5 ceiling, y=6 parapet.
- **`GoogleplexPlanner.plan(long seed)`** — deterministic planner. The
  structural skeleton (rooms, walls, doors) is fixed; the seed drives cosmetic
  variation only (desk-pod colors, plant species, desk-lamp corners, corridor
  carpet phase). Same seed, same blueprint, always.
- **`BlueprintValidator.validate(blueprint)`** — structural invariants:
  rooms in bounds and pairwise disjoint, solid floor, covered ceiling, sealed
  perimeter (no holes except the declared entrance), walkable doors that
  actually sit between their two declared rooms, and BFS reachability of
  **every room from the entrance**. The placer runs this before touching the
  world and refuses to place an invalid blueprint.

### Minecraft layer

- **`GoogleplexPlacer`** — maps `BlockSpec` to `BlockState` (the palette),
  places bottom-up so supports precede carpets/cake/lamps, fills a
  stone-brick foundation under every footprint column until solid ground
  (max 12 deep — nothing floats), and deliberately clears 6 blocks above the
  roof inside the footprint only.
- **`GoogleplexCommands`** — Brigadier subtree merged into the shared
  `/labscraft` root.

## Floor plan

31 wide x 27 deep x 7 tall. Entrance on the blueprint z=0 face (world north).

```
 z=26 +------------------------------------------+
      |                FLOW LAB                  |  3 consoles, Flow Crafting
 z=19 +-------------[glass door]----------------+  Table + Google-dot carpet
      | MICRO-       |          |               |  ring, iron server racks
      | KITCHEN      |          |  DESK PODS    |
 z=13 +--------------+ CORRIDOR |  4 pods, 2x2  |  open plan: low wall with
      | MEETING ROOM |          |  desks, seeded|  planters toward corridor
      | (glass wall) |          |  pod colors   |
 z=7  +------------[door]-----------------------+
      |                 LOBBY                   |  reception, 4 Google carpet
 z=0  +===========[3-wide entrance]=============+  dots, corner plants
       x=0          x=13..17                x=30
```

- **Lobby** (29x6): quartz reception counter, receptionist chair facing the
  door, the four Google dots (blue/red/yellow/green carpet), corner plants.
- **Meeting room** (11x5): full-glass wall toward the corridor, dark-oak
  conference table, chairs both sides plus head chair, black wall screen.
- **Micro-kitchen** (11x5): smooth-stone counter run, cake, barrel,
  two-block iron fridge, cafe table for two.
- **Desk pods** (11x11): four 2x2 quartz-slab desk pods, each with two
  chairs, an end-rod desk lamp on a seeded corner, and carpet accents in a
  seeded permutation of the four Google colors. Separated from the corridor
  by a waist-high wall with planters (open-plan sightline).
- **Corridor** (5x11): colored carpet runner with seeded color phase,
  connects every room; glass bands in both cross walls.
- **Flow Lab** (29x6): dark floor, glass double door, `flow_console`,
  `nano_banana_console` and `veo_console` along the back wall, the
  `flow_crafting_table` centered in a ring of Google-dot carpet, and
  sea-lantern-topped iron server racks along both side walls.
- Shell: white concrete with window bands, Google-colored entrance columns
  and lintel, roof parapet with a four-color run above the entrance.

## Command

Op level 2. Position is an explicit argument, so it works from the
dedicated-server console (and scripts) with no player context:

```
/labscraft googleplex generate <x> <y> <z> [<seed>] [force]
/labscraft googleplex info
```

- `<x y z>` is the structure's minimum (north-west, floor-level) corner;
  relative `~` coordinates also work in-game.
- `seed` defaults to a hash of the position: re-running at the same spot
  rebuilds the identical office (verified: a second run places 0 blocks).
- **Anti-grief:** without `force`, the command scans the target volume
  (structure box + overhead clearance) and refuses if it finds any block
  entity — chests, furnaces, consoles are the strongest signal of a player
  build. `force` makes overwriting an explicit choice.

There is intentionally **no natural worldgen placement**: the office is
summoned by command (Josh or a quest step can do this programmatically), so
it can never disturb G1's ore generation or spawn on top of player builds.

## Wiring

`GoogleplexCommands.register()` (static, no arguments) must be called once
from `LabsCraft.onInitialize()`. Nothing else needs registering — the feature
adds no blocks, items, or worldgen features of its own.

## Verified

- `./gradlew build` green; 163 tests pass repo-wide, 39 of them covering the
  blueprint layer (determinism, seed-only cosmetic variation, non-overlap,
  bounds, door connectivity, sealed shell, per-room reachability, and
  validator sabotage tests proving each check actually fires).
- On a dedicated server (`run-server/`, superflat): refusal over a planted
  chest, force-generate (2676 blocks, 38 ms), block queries confirming
  consoles/table/walls/entrance/desks/lights materialized at the expected
  world coordinates, idempotent re-generate (0 blocks), and a second site
  built with an explicit seed.
