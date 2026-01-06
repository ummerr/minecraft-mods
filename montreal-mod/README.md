# MTL Memories 🍁

A Minecraft mod that's a love letter to Montreal, 2004-2008. Built for McGill memories.

## What's in the Box

### Items
- **Poutine** - Heals you and gives strength. The ultimate comfort food.
- **Montreal Bagel** - Quick snack. St-Viateur forever.
- **Smoked Meat** - Schwartz's legendary sandwich
- **Café** - Speed boost for those McLennan all-nighters
- **Steamie** - The Montreal hot dog
- **Orange Cones** - Collectible symbols of eternal construction
- **Metro Ticket / OPUS Card** - Transit items
- **Tam-Tam Drum** - For Sunday afternoons on the mountain
- **Habs Jersey** - Bleu, blanc, rouge
- **McGill Textbook** - Use it to gain XP (and regret your purchases)
- **Gerts Token** - $5 pitcher nights
- **Arcade Fire Vinyl** - Funeral on wax
- **Quest Journal** - Track your Montreal journey

### Quest Storylines
1. **Frosh Week** - Arrival, first poutine, Gerts initiation
2. **Academic Life** - Textbook trauma, McLennan all-nighters
3. **Montreal Life** - Bagel wars, Tam-Tams, construction navigation, Schwartz's
4. **Music Scene** - Witness Arcade Fire before they blow up
5. **Winter Survival** - First real cold, mastering the Metro

## Setup Instructions

### Prerequisites
1. **Java 17** - Download from [Adoptium](https://adoptium.net/)
2. **An IDE** (optional but recommended):
   - [IntelliJ IDEA Community](https://www.jetbrains.com/idea/download/) (recommended)
   - [Eclipse](https://www.eclipse.org/downloads/)
   - Or just use VS Code with Java extensions

### First Time Setup

1. **Open a terminal in this folder**

2. **Run the Gradle wrapper setup:**
   ```bash
   # On Mac/Linux:
   ./gradlew genEclipseRuns  # if using Eclipse
   ./gradlew genIntellijRuns # if using IntelliJ
   
   # On Windows:
   gradlew.bat genEclipseRuns
   gradlew.bat genIntellijRuns
   ```

3. **Import into your IDE:**
   - IntelliJ: File → Open → select this folder
   - Eclipse: File → Import → Gradle → Existing Gradle Project

4. **Run the game:**
   ```bash
   ./gradlew runClient
   ```
   Or use the "runClient" configuration in your IDE.

### Building the Mod

To create a .jar file you can share:
```bash
./gradlew build
```
The mod file will be in `build/libs/`

## Project Structure

```
mtl-mod/
├── build.gradle              # Build configuration
├── src/main/
│   ├── java/com/mtlmod/
│   │   ├── MtlMod.java       # Main mod class
│   │   ├── item/
│   │   │   ├── ModItems.java      # All items registered here
│   │   │   ├── TextbookItem.java  # Custom textbook behavior
│   │   │   └── QuestJournalItem.java
│   │   ├── quest/
│   │   │   ├── Quest.java         # Quest data structure
│   │   │   └── QuestManager.java  # Quest logic + QUEST DEFINITIONS
│   │   └── network/
│   │       └── ModNetwork.java    # Multiplayer sync (placeholder)
│   └── resources/
│       ├── META-INF/mods.toml     # Mod metadata
│       ├── pack.mcmeta
│       └── assets/mtlmod/
│           ├── lang/              # Item names (en + fr!)
│           ├── textures/          # Item/block images (add these!)
│           └── models/            # 3D models
```

## Adding Your Own Quests

Open `QuestManager.java` and add quests in the `loadQuests()` method:

```java
registerQuest(new Quest(
    "quest_id",           // Unique ID
    "Display Name",       // What player sees
    "Description text",   // Quest description
    "storyline"          // Category (frosh, academic, montreal, etc.)
)
.setStartDialogue("Text shown when quest starts")
.setCompleteDialogue("Text shown on completion")
.setHint("Optional hint")
.addPrerequisite("other_quest_id")  // Must complete this first
.addObjective(new Quest.CollectItemObjective(
    "Collect 5 bagels",
    ModItems.BAGEL.get(),
    5
))
.addReward(new Quest.ItemReward("Bagel master", ModItems.BAGEL.get(), 10))
.addReward(new Quest.XpReward(100)));
```

## Adding Custom Items

1. Add the item in `ModItems.java`
2. Add the name in `lang/en_us.json`
3. Add a texture in `textures/item/` (16x16 PNG)
4. Add a model in `models/item/` (or use the default)

## Next Steps / Ideas

- [ ] Add item textures (the fun part!)
- [ ] Create NPCs (professors, dépanneur owners, that Casa del Popolo doorman)
- [ ] Build a Montreal world/map
- [ ] Add custom sounds (metro chimes, tam-tams, hockey crowds)
- [ ] Habs jersey as wearable armor
- [ ] Metro system that teleports between locations
- [ ] More quests (moving day, finding an apartment, language politics)
- [ ] Seasons/weather that affects gameplay

## Memories to Consider Adding

- The 80 bus forever being late
- Dollar oysters at McKibbin's
- The Second Cup on Milton
- Piknic Électronik
- The mountain at night
- Underground city in February
- Dep runs at midnight
- The Milton gates
- Cheap wine from the SAQ
- That one prof everyone loved/feared

---

*"We came for the degree. We stayed for the poutine."*

Made with love by two friends who survived McGill together. 🍁
