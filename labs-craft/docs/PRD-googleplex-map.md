# PRD: Googleplex Office Map

**Author:** LabsCraft Team
**Status:** Implemented
**Last Updated:** 2024-02-04

---

## Overview

### Problem Statement
Players need a themed environment that reinforces the narrative of being an APM intern at a tech company ("Labs"). A bare Minecraft world doesn't provide the immersive office experience needed for the mod's storyline.

### Solution
A procedurally-generated 200x200 block office building inspired by Google's campus (Googleplex), featuring dedicated product lab areas, a central TPU mining facility, and common spaces.

### Goals
- Provide an immersive "tech office" environment for gameplay
- Create dedicated spaces for each in-game product (Flow, Genie, etc.)
- Integrate the TPU resource system into the environment
- Support the quest/progression narrative

### Non-Goals
- Multiplayer optimization (single-player focus for v1)
- Dynamic/changing building layout
- Multiple building variations

---

## User Stories

1. **As a player**, I want to spawn in a recognizable office environment so that I feel immersed in the "tech intern" narrative.

2. **As a player**, I want distinct areas for each product so that I can explore and understand what each team works on.

3. **As a player**, I want a central TPU mine so that I have a reliable source of TPUs beyond random world generation.

4. **As a player**, I want common spaces (lobby, cafeteria) so that the office feels realistic and lived-in.

---

## Functional Requirements

### FR-1: Building Generation Command
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1.1 | Command `/labscraft build` generates the structure | P0 |
| FR-1.2 | Command requires operator permissions (level 2) | P0 |
| FR-1.3 | Building generates at player's current position | P0 |
| FR-1.4 | Feedback message confirms generation start/completion | P1 |

### FR-2: Building Dimensions & Structure
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-2.1 | Total footprint: 200x200 blocks | P0 |
| FR-2.2 | Wall height: 12 blocks | P0 |
| FR-2.3 | Exterior walls with windows | P0 |
| FR-2.4 | Ceiling enclosure | P0 |
| FR-2.5 | Foundation/floor throughout | P0 |

### FR-3: Entrance & Lobby
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-3.1 | North-facing entrance with open doorway | P0 |
| FR-3.2 | Google-colored floor stripes (blue, red, yellow, green) | P1 |
| FR-3.3 | Reception desk | P1 |
| FR-3.4 | "LABS" signage on back wall | P2 |

### FR-4: Product Labs (x6)
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-4.1 | Six separate lab rooms around perimeter | P0 |
| FR-4.2 | Each lab: 40x45 blocks | P0 |
| FR-4.3 | Unique accent color per lab | P1 |
| FR-4.4 | Doorway entrance to each lab | P0 |
| FR-4.5 | Workstation furniture in each lab | P2 |
| FR-4.6 | Relevant console placed in applicable labs | P1 |

**Lab Assignments:**
| Lab | Color | Location | Special Items |
|-----|-------|----------|---------------|
| Flow | Blue | NW | Veo Console |
| Genie | Magenta | NE | Nano Banana Console |
| Doppl | Cyan | W | - |
| NotebookLM | Orange | E | - |
| Opal | Green | SW | - |
| Mixboard | Purple | SE | - |

### FR-5: Central TPU Mine
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-5.1 | 50x50 block pit in building center | P0 |
| FR-5.2 | Depth: 15 blocks below floor level | P0 |
| FR-5.3 | TPU ore veins in pit walls (~15% density) | P0 |
| FR-5.4 | Ladder access to pit floor | P0 |
| FR-5.5 | Safety railing around pit edge | P1 |
| FR-5.6 | Glass floor viewing area around pit | P2 |
| FR-5.7 | Stone transitions to deepslate at lower depths | P2 |

### FR-6: Cafeteria
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-6.1 | Located at south end of building | P0 |
| FR-6.2 | Dimensions: 90x25 blocks | P1 |
| FR-6.3 | Tables and seating | P1 |
| FR-6.4 | Counter/kitchen area | P2 |
| FR-6.5 | Warm lighting (lanterns) | P2 |

### FR-7: Decoration & Atmosphere
| ID | Requirement | Priority |
|----|-------------|----------|
| FR-7.1 | Ceiling lighting throughout | P1 |
| FR-7.2 | Potted plants in common areas | P2 |
| FR-7.3 | Benches in hallways | P2 |
| FR-7.4 | Consistent block palette | P1 |

---

## Technical Specifications

### Architecture
```
com.labscraft.command.ModCommands
    └── Registers /labscraft build command

com.labscraft.world.GoogleplexGenerator
    └── Contains all structure generation logic
    └── Methods for each building section
```

### Block Palette
| Element | Block Type |
|---------|------------|
| Exterior Walls | White Concrete |
| Exterior Accent | Light Gray Concrete |
| Windows | Light Gray Stained Glass |
| Floor Primary | Polished Granite |
| Floor Secondary | Polished Diorite |
| Ceiling | Smooth Quartz |
| Interior Walls | White Concrete |

### Lab Accent Colors
| Lab | Block |
|-----|-------|
| Flow | Blue Concrete |
| Genie | Magenta Concrete |
| Doppl | Cyan Concrete |
| NotebookLM | Orange Concrete |
| Opal | Green Concrete (Lime) |
| Mixboard | Purple Concrete |

### Performance Considerations
- Structure generates synchronously (blocks game briefly)
- Approximately 400,000+ block placements
- Recommended: Generate on superflat world for best results

---

## Testing Instructions

### Setup
1. Run `./gradlew runClient`
2. Create new Superflat world with cheats enabled
3. Switch to Creative mode: `/gamemode creative`
4. Fly up ~20 blocks

### Test Cases

| Test | Steps | Expected Result |
|------|-------|-----------------|
| TC-1: Basic Generation | Run `/labscraft build` | Building generates without errors |
| TC-2: Permissions | Run command without op | Command rejected |
| TC-3: Lobby Access | Walk through north entrance | Can enter lobby, see reception desk |
| TC-4: Lab Access | Navigate to each lab | All 6 labs accessible with correct colors |
| TC-5: TPU Mine | Descend ladder into mine | Can reach bottom, TPU ore visible in walls |
| TC-6: TPU Mining | Mine ore from pit walls | Drops TPU items |
| TC-7: Cafeteria | Navigate to south area | Tables, chairs, lighting present |

---

## Future Enhancements

### Phase 2 (Potential)
- [ ] Josh Woodward NPC spawns in lobby automatically
- [ ] Signs/labels for each lab area
- [ ] Working doors (iron doors with buttons)
- [ ] More detailed furniture (armor stands as "employees")
- [ ] Ambient sounds

### Phase 3 (Potential)
- [ ] Multiple floors
- [ ] Elevator system
- [ ] Outdoor courtyard/campus area
- [ ] Parking lot with "bikes"
- [ ] Rooftop access

---

## Appendix

### Layout Diagram
```
    ┌─────────────────────────────────────────────┐
    │              ENTRANCE / LOBBY               │
    ├─────────┬───────────────────────┬───────────┤
    │  FLOW   │                       │  GENIE    │
    │  LAB    │                       │   LAB     │
    ├─────────┤    ┌───────────┐      ├───────────┤
    │ DOPPL   │    │  TPU MINE │      │ NOTEBOOKLM│
    │  LAB    │    │  (50x50)  │      │    LAB    │
    ├─────────┤    └───────────┘      ├───────────┤
    │  OPAL   │                       │ MIXBOARD  │
    │  LAB    │                       │   LAB     │
    ├─────────┴───────────────────────┴───────────┤
    │              CAFETERIA / LOUNGE              │
    └─────────────────────────────────────────────┘
```

### Command Reference
| Command | Description | Permission |
|---------|-------------|------------|
| `/labscraft build` | Generate Googleplex at current position | Op Level 2 |

### File Locations
- Command: `src/main/java/com/labscraft/command/ModCommands.java`
- Generator: `src/main/java/com/labscraft/world/GoogleplexGenerator.java`
