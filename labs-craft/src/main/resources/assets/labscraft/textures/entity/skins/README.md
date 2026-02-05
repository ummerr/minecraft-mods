# LabsCraft Entity Skins

This folder contains custom skins for LabsCraft NPC entities.

## Skin Format

All skins must follow the **Minecraft 1.8+ skin format**:

- **Dimensions**: 64x64 pixels (PNG format, RGBA)
- **Layout**: Standard Minecraft player skin UV mapping

### UV Layout Reference

```
64x64 Skin Layout:

HEAD (8x8x8):
  Top:    (8,0)   8x8     Bottom: (16,0)  8x8
  Front:  (8,8)   8x8     Back:   (24,8)  8x8
  Right:  (0,8)   8x8     Left:   (16,8)  8x8

BODY (8x12x4):
  Top:    (20,16) 8x4     Bottom: (28,16) 8x4
  Front:  (20,20) 8x12    Back:   (32,20) 8x12
  Right:  (16,20) 4x12    Left:   (28,20) 4x12

RIGHT ARM (4x12x4):
  Top:    (44,16) 4x4     Bottom: (48,16) 4x4
  Front:  (44,20) 4x12    Back:   (52,20) 4x12
  Outer:  (40,20) 4x12    Inner:  (48,20) 4x12

LEFT ARM (4x12x4):
  Top:    (36,48) 4x4     Bottom: (40,48) 4x4
  Front:  (36,52) 4x12    Back:   (44,52) 4x12
  Outer:  (32,52) 4x12    Inner:  (40,52) 4x12

RIGHT LEG (4x12x4):
  Top:    (4,16)  4x4     Bottom: (8,16)  4x4
  Front:  (4,20)  4x12    Back:   (12,20) 4x12
  Outer:  (0,20)  4x12    Inner:  (8,20)  4x12

LEFT LEG (4x12x4):
  Top:    (20,48) 4x4     Bottom: (24,48) 4x4
  Front:  (20,52) 4x12    Back:   (28,52) 4x12
  Outer:  (16,52) 4x12    Inner:  (24,52) 4x12
```

## Current Skins

| File | Entity | Description |
|------|--------|-------------|
| `josh_woodward.png` | Josh Woodward | Google blue shirt, gray pants, beige skin |

## Adding New Skins

1. Create a 64x64 PNG following the UV layout above
2. Save it in this folder with a descriptive name (e.g., `entity_name.png`)
3. Update the entity's renderer to reference the new skin path:

```java
// In the entity renderer class:
private static final Identifier TEXTURE = Identifier.of(
    LabsCraft.MOD_ID,
    "textures/entity/skins/your_skin_name.png"
);
```

## Tools for Creating Skins

- [Blockbench](https://www.blockbench.net/) - Free 3D modeling tool with skin editor
- [Nova Skin](https://minecraft.novaskin.me/) - Online Minecraft skin editor
- [miners-need-cool-shoes](https://www.minecraftskins.com/skin-editor/) - Another online editor

## Notes

- Use transparent backgrounds (RGBA PNG) for proper rendering
- The overlay layer (rows 32-63) can add hats, jackets, etc.
- Test skins in-game to verify appearance
