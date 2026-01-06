package com.mtlmod.quest;

import com.mtlmod.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.*;

/**
 * The Quest Manager - handles all quest logic for the mod.
 * 
 * This is where you'll define all your Montreal missions!
 * Player progress is stored in their persistent data (survives death, logout, etc.)
 */
public class QuestManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // The NBT keys for storing quest data on players
    private static final String QUEST_DATA_KEY = "MtlQuestData";
    private static final String ACTIVE_QUESTS_KEY = "ActiveQuests";
    private static final String COMPLETED_QUESTS_KEY = "CompletedQuests";
    
    // All available quests, keyed by ID
    private final Map<String, Quest> allQuests = new HashMap<>();
    
    // Quests organized by storyline for easy access
    private final Map<String, List<Quest>> questsByStoryline = new HashMap<>();

    /**
     * Load/define all quests.
     * This is called when the game starts up.
     * 
     * ADD YOUR QUESTS HERE!
     */
    public void loadQuests() {
        LOGGER.info("Loading Montreal quests...");
        
        // ==================== STORYLINE: FROSH WEEK ====================
        
        registerQuest(new Quest(
                "frosh_arrival",
                "Welcome to Montreal",
                "You just arrived at McGill. Find your residence and get settled.",
                "frosh"
        ).setStartDialogue("The bus from the airport drops you off on Sherbrooke. The air smells like possibility and bagels.")
         .setHint("Head towards the mountain - that's where campus is.")
         .addObjective(new Quest.CollectItemObjective(
                 "Get a campus map",
                 ModItems.QUEST_JOURNAL.get(), 1))
         .addReward(new Quest.XpReward(50)));

        registerQuest(new Quest(
                "frosh_first_poutine", 
                "Late Night Discovery",
                "It's 2am after the Frosh concert. Someone mentions poutine. Your life is about to change.",
                "frosh"
        ).addPrerequisite("frosh_arrival")
         .setStartDialogue("'Trust me,' your new floormate says, 'you haven't lived until you've had La Banquise at 3am.'")
         .addObjective(new Quest.CollectItemObjective(
                 "Experience your first poutine",
                 ModItems.POUTINE.get(), 1))
         .addReward(new Quest.ItemReward("The knowledge of true comfort food", ModItems.POUTINE.get(), 3))
         .addReward(new Quest.XpReward(75)));

        registerQuest(new Quest(
                "frosh_gerts",
                "Gerts Night",
                "Thursday night. $5 pitchers. A McGill tradition.",
                "frosh"
        ).addPrerequisite("frosh_arrival")
         .setStartDialogue("The basement of the Shatner building holds secrets. And cheap beer.")
         .addObjective(new Quest.CollectItemObjective(
                 "Acquire Gerts tokens",
                 ModItems.GERTS_TOKEN.get(), 3))
         .addReward(new Quest.XpReward(100)));

        // ==================== STORYLINE: ACADEMIC LIFE ====================

        registerQuest(new Quest(
                "academic_textbooks",
                "The Bookstore Heist",
                "Buy your textbooks. Try not to cry at the prices.",
                "academic"
        ).setStartDialogue("The Paragraphe bookstore looms before you. Your wallet trembles.")
         .setHint("Check if previous editions work. They usually do.")
         .addObjective(new Quest.CollectItemObjective(
                 "Acquire overpriced textbooks",
                 ModItems.TEXTBOOK.get(), 3))
         .addReward(new Quest.XpReward(50))
         .setCompleteDialogue("$600 later, you own books you'll open maybe twice."));

        registerQuest(new Quest(
                "academic_allnighter",
                "The McLennan All-Nighter",
                "Finals are here. The library becomes your home.",
                "academic"
        ).addPrerequisite("academic_textbooks")
         .setStartDialogue("The 6th floor of McLennan. Silent. Desperate. Caffeinated.")
         .addObjective(new Quest.CollectItemObjective(
                 "Consume dangerous amounts of coffee",
                 ModItems.CAFE.get(), 5))
         .addObjective(new Quest.CollectItemObjective(
                 "Actually study",
                 ModItems.TEXTBOOK.get(), 1))
         .addReward(new Quest.XpReward(200))
         .setCompleteDialogue("You emerge at dawn, blinking at the sun like a mole person. Was that 16 hours?"));

        // ==================== STORYLINE: MONTREAL LIFE ====================

        registerQuest(new Quest(
                "mtl_bagel_wars",
                "The Great Bagel Debate",
                "St-Viateur or Fairmount? Choose your side. (There is a correct answer.)",
                "montreal"
        ).setStartDialogue("Two bagel shops. One correct opinion. This is important.")
         .addObjective(new Quest.CollectItemObjective(
                 "Sample the evidence",
                 ModItems.BAGEL.get(), 5))
         .addReward(new Quest.ItemReward("Bagel connoisseur status", ModItems.BAGEL.get(), 10))
         .addReward(new Quest.XpReward(75)));

        registerQuest(new Quest(
                "mtl_tam_tams",
                "Sunday at the Tam-Tams",
                "The drum circle on Mont-Royal. A Montreal institution.",
                "montreal"
        ).setStartDialogue("Sunday afternoon. The mountain calls. Bring snacks.")
         .setHint("Follow the sound of drums up the mountain.")
         .addObjective(new Quest.CollectItemObjective(
                 "Join the rhythm",
                 ModItems.TAM_TAM_DRUM.get(), 1))
         .addReward(new Quest.XpReward(100))
         .setCompleteDialogue("The sun sets over the city. Strangers become friends. This is Montreal."));

        registerQuest(new Quest(
                "mtl_construction",
                "Orange Cone Season",
                "Navigate the eternal construction. It's not a bug, it's a feature.",
                "montreal"
        ).setStartDialogue("They say Montreal has two seasons: winter and construction.")
         .addObjective(new Quest.CollectItemObjective(
                 "Document the construction",
                 ModItems.ORANGE_CONE.get(), 20))
         .addReward(new Quest.XpReward(150))
         .setCompleteDialogue("You've accepted that the roads will never be finished. This is peace."));

        registerQuest(new Quest(
                "mtl_smoked_meat",
                "The Schwartz's Experience",
                "Wait in line. It's worth it.",
                "montreal"
        ).setStartDialogue("The line stretches down St-Laurent. You can smell it from here.")
         .setHint("Medium-fat is the correct order. Trust the process.")
         .addObjective(new Quest.CollectItemObjective(
                 "Acquire the legendary sandwich",
                 ModItems.SMOKED_MEAT.get(), 1))
         .addReward(new Quest.ItemReward("Smoked meat appreciation", ModItems.SMOKED_MEAT.get(), 2))
         .addReward(new Quest.XpReward(100)));

        // ==================== STORYLINE: MUSIC SCENE ====================

        registerQuest(new Quest(
                "music_arcade_fire",
                "Funeral",
                "2004. A church basement. History being made.",
                "music"
        ).setStartDialogue("Your friend's friend says there's this band you have to see...")
         .setHint("The Arcade Fire won't stay in small venues for long.")
         .addObjective(new Quest.CollectItemObjective(
                 "Witness greatness",
                 ModItems.ARCADE_FIRE_VINYL.get(), 1))
         .addReward(new Quest.XpReward(250))
         .setCompleteDialogue("Wake up. Hold your mistake up. You were there."));

        // ==================== STORYLINE: WINTER ====================

        registerQuest(new Quest(
                "winter_first",
                "The First Real Winter",
                "You thought you knew cold. You were wrong.",
                "winter"
        ).setStartDialogue("November hits different here. The wind has opinions.")
         .setHint("Layers. More layers than you think.")
         .addObjective(new Quest.CollectItemObjective(
                 "Acquire survival supplies (coffee)",
                 ModItems.CAFE.get(), 3))
         .addReward(new Quest.XpReward(100))
         .setCompleteDialogue("You learn the true meaning of 'wind chill factor.'"));

        registerQuest(new Quest(
                "winter_metro",
                "Underground City",
                "When it's -30, you learn to live underground.",
                "winter"
        ).addPrerequisite("winter_first")
         .setStartDialogue("The Metro isn't just transportation. It's survival.")
         .addObjective(new Quest.CollectItemObjective(
                 "Master the transit system",
                 ModItems.OPUS_CARD.get(), 1))
         .addReward(new Quest.ItemReward("Metro expertise", ModItems.METRO_TICKET.get(), 10))
         .addReward(new Quest.XpReward(100)));

        LOGGER.info("Loaded {} quests across {} storylines", 
                allQuests.size(), questsByStoryline.size());
    }

    /**
     * Register a quest with the manager
     */
    public void registerQuest(Quest quest) {
        allQuests.put(quest.getId(), quest);
        questsByStoryline
                .computeIfAbsent(quest.getStoryline(), k -> new ArrayList<>())
                .add(quest);
    }

    // ==================== PLAYER PROGRESS ====================

    /**
     * Get a player's active quests
     */
    public List<Quest> getActiveQuests(Player player) {
        List<Quest> active = new ArrayList<>();
        Set<String> activeIds = getActiveQuestIds(player);
        for (String id : activeIds) {
            Quest quest = allQuests.get(id);
            if (quest != null) active.add(quest);
        }
        return active;
    }

    /**
     * Get a player's completed quests
     */
    public List<Quest> getCompletedQuests(Player player) {
        List<Quest> completed = new ArrayList<>();
        Set<String> completedIds = getCompletedQuestIds(player);
        for (String id : completedIds) {
            Quest quest = allQuests.get(id);
            if (quest != null) completed.add(quest);
        }
        return completed;
    }

    /**
     * Start a quest for a player
     */
    public boolean startQuest(Player player, String questId) {
        Quest quest = allQuests.get(questId);
        if (quest == null) {
            LOGGER.warn("Tried to start unknown quest: {}", questId);
            return false;
        }
        
        // Check prerequisites
        Set<String> completed = getCompletedQuestIds(player);
        for (String prereq : quest.getPrerequisites()) {
            if (!completed.contains(prereq)) {
                player.sendSystemMessage(Component.literal(
                        "§cYou must complete \"" + allQuests.get(prereq).getDisplayName() + "\" first."));
                return false;
            }
        }
        
        // Add to active quests
        Set<String> active = getActiveQuestIds(player);
        if (active.contains(questId)) {
            return false; // Already active
        }
        if (completed.contains(questId)) {
            return false; // Already completed
        }
        
        active.add(questId);
        saveActiveQuestIds(player, active);
        
        // Show start dialogue
        if (quest.getStartDialogue() != null) {
            player.sendSystemMessage(Component.literal("§6§l[New Quest] §f" + quest.getDisplayName()));
            player.sendSystemMessage(Component.literal("§7§o" + quest.getStartDialogue()));
        }
        
        return true;
    }

    /**
     * Complete a quest for a player
     */
    public boolean completeQuest(Player player, String questId) {
        Quest quest = allQuests.get(questId);
        if (quest == null) return false;
        
        Set<String> active = getActiveQuestIds(player);
        if (!active.contains(questId)) return false; // Not active
        
        if (!quest.isComplete(player)) return false; // Not actually done
        
        // Move from active to completed
        active.remove(questId);
        Set<String> completed = getCompletedQuestIds(player);
        completed.add(questId);
        
        saveActiveQuestIds(player, active);
        saveCompletedQuestIds(player, completed);
        
        // Grant rewards
        quest.grantRewards(player);
        
        // Show completion dialogue
        player.sendSystemMessage(Component.literal("§a§l[Quest Complete] §f" + quest.getDisplayName()));
        if (quest.getCompleteDialogue() != null) {
            player.sendSystemMessage(Component.literal("§7§o" + quest.getCompleteDialogue()));
        }
        
        return true;
    }

    // ==================== NBT STORAGE ====================
    // This saves quest progress to the player's data

    private Set<String> getActiveQuestIds(Player player) {
        CompoundTag data = player.getPersistentData().getCompound(QUEST_DATA_KEY);
        Set<String> ids = new HashSet<>();
        ListTag list = data.getList(ACTIVE_QUESTS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ids.add(list.getString(i));
        }
        return ids;
    }

    private Set<String> getCompletedQuestIds(Player player) {
        CompoundTag data = player.getPersistentData().getCompound(QUEST_DATA_KEY);
        Set<String> ids = new HashSet<>();
        ListTag list = data.getList(COMPLETED_QUESTS_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ids.add(list.getString(i));
        }
        return ids;
    }

    private void saveActiveQuestIds(Player player, Set<String> ids) {
        CompoundTag data = player.getPersistentData().getCompound(QUEST_DATA_KEY);
        ListTag list = new ListTag();
        for (String id : ids) {
            list.add(StringTag.valueOf(id));
        }
        data.put(ACTIVE_QUESTS_KEY, list);
        player.getPersistentData().put(QUEST_DATA_KEY, data);
    }

    private void saveCompletedQuestIds(Player player, Set<String> ids) {
        CompoundTag data = player.getPersistentData().getCompound(QUEST_DATA_KEY);
        ListTag list = new ListTag();
        for (String id : ids) {
            list.add(StringTag.valueOf(id));
        }
        data.put(COMPLETED_QUESTS_KEY, list);
        player.getPersistentData().put(QUEST_DATA_KEY, data);
    }

    // ==================== UTILITIES ====================

    public int getQuestCount() {
        return allQuests.size();
    }

    public Quest getQuest(String id) {
        return allQuests.get(id);
    }

    public List<Quest> getQuestsInStoryline(String storyline) {
        return questsByStoryline.getOrDefault(storyline, Collections.emptyList());
    }

    public Set<String> getStorylines() {
        return questsByStoryline.keySet();
    }
}
