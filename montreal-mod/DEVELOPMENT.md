# Montreal Mod - Development Guide

This guide explains how the Montreal mod works internally and how to add new content.

## Table of Contents
- [Mod Architecture](#mod-architecture)
- [Adding New Items](#adding-new-items)
- [Adding Assets](#adding-assets)
- [Creating Custom Item Behaviors](#creating-custom-item-behaviors)
- [Quest System](#quest-system)
- [Localization](#localization)

---

## Mod Architecture

### Core Components

The mod has a clear structure that follows Minecraft Forge conventions:

```
src/main/
├── java/com/mtlmod/
│   ├── MtlMod.java              # Main mod entry point
│   ├── item/
│   │   ├── ModItems.java        # Item registry
│   │   ├── TextbookItem.java    # Custom item: Textbook
│   │   └── QuestJournalItem.java # Custom item: Quest Journal
│   ├── quest/
│   │   ├── Quest.java           # Quest data structure
│   │   └── QuestManager.java    # Quest system + definitions
│   └── network/
│       └── ModNetwork.java      # Networking (for multiplayer)
└── resources/
    └── assets/mtlmod/
        ├── lang/                # Translations
        ├── textures/            # PNG images
        └── models/              # JSON model definitions
```

### How the Mod Loads

1. **MtlMod.java** is the entry point (marked with `@Mod` annotation)
2. In the constructor:
   - Registers items via `ModItems.register()`
   - Sets up event listeners for `commonSetup` and `clientSetup`
3. During `commonSetup`:
   - Network system initializes
   - Quest manager loads all quests
4. During `clientSetup`:
   - Client-side rendering setup (currently minimal)

### The MOD_ID

Everything in the mod is namespaced with the `MOD_ID = "mtlmod"`. This shows up everywhere:
- Item registry names: `mtlmod:poutine`
- Texture paths: `assets/mtlmod/textures/item/bagel.png`
- Translation keys: `item.mtlmod.bagel`

---

## Adding New Items

### Step-by-Step Guide

#### 1. Register the Item in ModItems.java

Open `src/main/java/com/mtlmod/item/ModItems.java` and add your item:

```java
// Add to the appropriate section (FOOD ITEMS or COLLECTIBLES)
public static final RegistryObject<Item> YOUR_ITEM = ITEMS.register("your_item",
        () -> new Item(new Item.Properties()
                .stacksTo(64)));  // How many can stack in one slot
```

#### 2. Add to Creative Tab

In the same file, scroll to the `MTL_TAB` section and add your item to the display list:

```java
.displayItems((parameters, output) -> {
    // ... existing items ...
    output.accept(YOUR_ITEM.get());
})
```

#### 3. Done! (for basic items)

That's it for registration. Now you need to add assets (see next section).

### Item Properties Explained

```java
new Item(new Item.Properties()
    .stacksTo(64)           // Max stack size (1-64)
    .rarity(Rarity.UNCOMMON) // COMMON, UNCOMMON, RARE, EPIC
    .food(...)              // Makes it edible (see Food Items below)
)
```

### Food Items

Food items need a `FoodProperties` builder:

```java
public static final RegistryObject<Item> YOUR_FOOD = ITEMS.register("your_food",
        () -> new Item(new Item.Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(6)           // Hunger points restored (1-20)
                        .saturationMod(0.6f)    // Saturation (0.0-1.0)
                        .fast()                 // Quick eating animation
                        .alwaysEat()            // Can eat even when full
                        .meat()                 // It's meat (for wolves, etc.)
                        .effect(() -> new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED,  // Effect type
                            1200,                       // Duration in ticks (20 ticks = 1 second)
                            1                           // Amplifier (0 = level 1)
                        ), 1.0f)                       // Probability (1.0 = 100%)
                        .build())
                .rarity(Rarity.UNCOMMON)));
```

**Common Effects:**
- `MobEffects.MOVEMENT_SPEED` - Speed boost
- `MobEffects.DAMAGE_BOOST` - Strength
- `MobEffects.REGENERATION` - Health regen
- `MobEffects.DIG_SPEED` - Mining speed (Haste)
- `MobEffects.JUMP` - Jump boost
- `MobEffects.ABSORPTION` - Extra hearts

**Duration Examples:**
- 600 ticks = 30 seconds
- 1200 ticks = 1 minute
- 2400 ticks = 2 minutes

---

## Adding Assets

Every item needs visual assets. Here's the complete workflow:

### 1. Create the Texture (PNG Image)

**Location:** `src/main/resources/assets/mtlmod/textures/item/your_item.png`

**Requirements:**
- Size: 16x16 pixels (standard Minecraft item size)
- Format: PNG with transparency
- Filename must match your item's registry name

**Tools:**
- Pixaki (iPad)
- Aseprite
- Piskel (browser-based)
- Photoshop/GIMP (resize to 16x16)

**Example:** For the item registered as `"bagel"`, create:
```
src/main/resources/assets/mtlmod/textures/item/bagel.png
```

### 2. Create the Model (JSON)

**Location:** `src/main/resources/assets/mtlmod/models/item/your_item.json`

For most items, use this simple template:

```json
{
    "parent": "minecraft:item/generated",
    "textures": {
        "layer0": "mtlmod:item/your_item"
    }
}
```

**Explanation:**
- `parent`: Tells Minecraft to render this as a standard 2D item
- `layer0`: Points to your texture file
  - `mtlmod:` = namespace (your MOD_ID)
  - `item/your_item` = path from `textures/` folder (no .png extension)

**Advanced:** For handheld items (tools, swords):
```json
{
    "parent": "minecraft:item/handheld",
    "textures": {
        "layer0": "mtlmod:item/your_tool"
    }
}
```

### 3. Add the Display Name

**Location:** `src/main/resources/assets/mtlmod/lang/en_us.json`

Add your item's display name:

```json
{
    "item.mtlmod.your_item": "Display Name Here",
    // ... other items ...
}
```

**Translation Key Format:** `item.mtlmod.{registry_name}`

**Optional - French Translation:**
Edit `src/main/resources/assets/mtlmod/lang/fr_fr.json`:
```json
{
    "item.mtlmod.your_item": "Nom Français Ici",
    // ...
}
```

### Quick Asset Checklist

For an item named `maple_syrup`:

- [ ] Code: `ModItems.MAPLE_SYRUP = ITEMS.register("maple_syrup", ...)`
- [ ] Texture: `textures/item/maple_syrup.png` (16x16 PNG)
- [ ] Model: `models/item/maple_syrup.json`
- [ ] English name: `"item.mtlmod.maple_syrup": "Maple Syrup"` in `lang/en_us.json`
- [ ] Added to creative tab in `ModItems.java`

---

## Creating Custom Item Behaviors

Sometimes items need special behavior (like the Textbook giving XP). Here's how:

### Example: The Textbook

Located at: `src/main/java/com/mtlmod/item/TextbookItem.java`

```java
public class TextbookItem extends Item {

    public TextbookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide) {  // Only run on server
            player.giveExperiencePoints(50);

            player.displayClientMessage(
                Component.literal("You studied hard! +50 XP"),
                true  // Show above hotbar
            );

            itemStack.shrink(1);  // Remove one from stack
        }

        return InteractionResultHolder.sidedSuccess(itemStack, level.isClientSide());
    }
}
```

### Key Methods to Override

**`use(Level level, Player player, InteractionHand hand)`**
- Triggered when player right-clicks with the item
- Good for: consumables, tools, activatable items

**`inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)`**
- Called every tick while item is in inventory
- Good for: passive effects, durability changes

**`onCraftedBy(ItemStack stack, Level level, Player player)`**
- Triggered when player crafts this item
- Good for: achievements, unlocks

**`finishUsingItem(ItemStack stack, Level level, LivingEntity entity)`**
- Called when eating/using animation completes
- Good for: custom food effects

### Creating a Custom Item

1. Create new class in `src/main/java/com/mtlmod/item/`:
```java
package com.mtlmod.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class YourCustomItem extends Item {

    public YourCustomItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Your custom logic here
        return super.use(level, player, hand);
    }
}
```

2. Register it in `ModItems.java`:
```java
public static final RegistryObject<Item> YOUR_CUSTOM = ITEMS.register("your_custom",
        () -> new YourCustomItem(new Item.Properties()));
```

---

## Quest System

The quest system is defined in `src/main/java/com/mtlmod/quest/QuestManager.java`.

### Quest Structure

```java
registerQuest(new Quest(
    "quest_id",              // Unique identifier (use snake_case)
    "Display Title",         // What players see
    "Quest description",     // Explains what to do
    "storyline_category"     // Groups related quests
)
.setStartDialogue("Text shown when quest begins...")
.setCompleteDialogue("Text shown on completion!")
.setHint("Optional hint if player is stuck")
.addPrerequisite("other_quest_id")     // Must complete this first
.addObjective(...)                     // See below
.addReward(...));                      // See below
```

### Quest Objectives

**Collect Items:**
```java
.addObjective(new Quest.CollectItemObjective(
    "Collect 5 bagels",
    ModItems.BAGEL.get(),
    5
))
```

**Visit Location:**
```java
.addObjective(new Quest.LocationObjective(
    "Visit McGill",
    100, 64, 200,  // x, y, z coordinates
    50             // Radius in blocks
))
```

**Talk to NPC:**
```java
.addObjective(new Quest.InteractObjective(
    "Talk to depanneur owner",
    "npc_depanneur_owner"
))
```

### Quest Rewards

**Give Items:**
```java
.addReward(new Quest.ItemReward(
    "Extra bagels!",
    ModItems.BAGEL.get(),
    10  // Quantity
))
```

**Give XP:**
```java
.addReward(new Quest.XpReward(100))
```

### Adding Your Quest

1. Open `src/main/java/com/mtlmod/quest/QuestManager.java`
2. Find the `loadQuests()` method
3. Add your quest to the appropriate storyline section:

```java
// ========== YOUR STORYLINE ==========
registerQuest(new Quest(
    "mystory_first_quest",
    "My First Quest",
    "This is what you need to do...",
    "mystory"
)
.setStartDialogue("Welcome to my storyline!")
.addObjective(new Quest.CollectItemObjective(
    "Find a bagel",
    ModItems.BAGEL.get(),
    1
))
.addReward(new Quest.XpReward(50)));
```

4. Add translations to `lang/en_us.json`:
```json
"quest.mtlmod.mystory_first_quest": "My First Quest",
"storyline.mtlmod.mystory": "My Storyline"
```

---

## Localization

The mod supports multiple languages via JSON files in `src/main/resources/assets/mtlmod/lang/`.

### Language Files

- `en_us.json` - English (required)
- `fr_fr.json` - French
- `es_es.json` - Spanish
- `de_de.json` - German
- etc. (see [Minecraft language codes](https://minecraft.wiki/w/Language))

### Translation Key Patterns

| Type | Pattern | Example |
|------|---------|---------|
| Item | `item.mtlmod.{item_id}` | `item.mtlmod.poutine` |
| Creative Tab | `itemGroup.mtlmod` | Always this exact string |
| Quest | `quest.mtlmod.{quest_id}` | `quest.mtlmod.frosh_arrival` |
| Storyline | `storyline.mtlmod.{storyline_id}` | `storyline.mtlmod.frosh` |

### Adding a New Language

1. Create new file: `src/main/resources/assets/mtlmod/lang/{language_code}.json`
2. Copy structure from `en_us.json`
3. Translate all values (keep keys the same!)

Example `fr_fr.json`:
```json
{
    "item.mtlmod.poutine": "Poutine",
    "item.mtlmod.bagel": "Bagel Montréalais",
    "item.mtlmod.smoked_meat": "Sandwich à la Viande Fumée",
    "itemGroup.mtlmod": "Mémoires MTL"
}
```

---

## Testing Your Changes

### In-Game Testing

1. Run the mod:
```bash
./gradlew runClient
```

2. Once in game:
   - Open Creative Inventory (E)
   - Find the "MTL Memories" tab
   - Your items should appear there

3. Check the console for errors (look for `[mtlmod]` logs)

### Common Issues

**Item doesn't appear:**
- Check ModItems.java - is it registered?
- Check creative tab - is it added to displayItems?
- Check console for registration errors

**Missing texture (purple/black checkboard):**
- Verify texture exists: `textures/item/{item_name}.png`
- Check model JSON: texture path must match
- Ensure PNG is exactly 16x16 pixels

**Name shows as "item.mtlmod.whatever":**
- Missing from `lang/en_us.json`
- Key format must be: `item.mtlmod.{item_name}`

**Quest not loading:**
- Check QuestManager.java - is registerQuest() called?
- Check for typos in quest ID
- Look for errors in console during startup

---

## File Structure Reference

```
montreal-mod/
├── src/main/
│   ├── java/com/mtlmod/
│   │   ├── MtlMod.java                    # Main entry point
│   │   ├── item/
│   │   │   ├── ModItems.java              # Register items HERE
│   │   │   ├── TextbookItem.java          # Custom item example
│   │   │   └── QuestJournalItem.java      # Another custom item
│   │   ├── quest/
│   │   │   ├── Quest.java                 # Quest data structure
│   │   │   └── QuestManager.java          # Add quests HERE
│   │   └── network/
│   │       └── ModNetwork.java            # Multiplayer sync
│   └── resources/
│       ├── META-INF/
│       │   └── mods.toml                  # Mod metadata (version, etc.)
│       ├── pack.mcmeta                    # Resource pack info
│       └── assets/mtlmod/
│           ├── lang/
│           │   ├── en_us.json             # English names HERE
│           │   └── fr_fr.json             # French names HERE
│           ├── textures/item/
│           │   ├── bagel.png              # Item textures HERE
│           │   └── habs_jersey.png
│           └── models/item/
│               ├── bagel.json             # Item models HERE
│               └── habs_jersey.json
├── build.gradle                           # Build config
└── gradle.properties                      # Mod version
```

---

## Quick Reference: Adding a New Food Item

**Complete example** for adding "Tourtière" (meat pie):

1. **ModItems.java** - Add to FOOD ITEMS section:
```java
public static final RegistryObject<Item> TOURTIERE = ITEMS.register("tourtiere",
        () -> new Item(new Item.Properties()
                .food(new FoodProperties.Builder()
                        .nutrition(8)
                        .saturationMod(0.8f)
                        .meat()
                        .build())
                .rarity(Rarity.UNCOMMON)));
```

2. **ModItems.java** - Add to creative tab:
```java
output.accept(TOURTIERE.get());
```

3. **Create texture:** `textures/item/tourtiere.png` (16x16)

4. **Create model:** `models/item/tourtiere.json`
```json
{
    "parent": "minecraft:item/generated",
    "textures": {
        "layer0": "mtlmod:item/tourtiere"
    }
}
```

5. **Add name:** In `lang/en_us.json`
```json
"item.mtlmod.tourtiere": "Tourtière",
```

Done! Build and test: `./gradlew runClient`

---

## Need Help?

- Check the console for error messages
- Look at existing items for examples
- Minecraft Forge docs: https://docs.minecraftforge.net/
- Minecraft wiki: https://minecraft.wiki/

Remember: The best way to learn is by experimenting! Make a backup, try things, and see what happens.
