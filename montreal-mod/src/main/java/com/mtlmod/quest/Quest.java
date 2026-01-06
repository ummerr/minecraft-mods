package com.mtlmod.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single quest/mission in the Montreal experience.
 * 
 * Quests have:
 * - An ID (unique identifier)
 * - Display name and description (what the player sees)
 * - Objectives (things to do)
 * - Rewards (what you get for completing it)
 * - Prerequisites (other quests that must be done first)
 */
public class Quest {
    
    private final String id;
    private final String displayName;
    private final String description;
    private final String storyline;  // Which story arc this belongs to
    
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final List<String> prerequisites;  // Quest IDs that must be completed first
    
    // Optional flavor
    private String startDialogue;
    private String completeDialogue;
    private String hint;

    public Quest(String id, String displayName, String description, String storyline) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.storyline = storyline;
        this.objectives = new ArrayList<>();
        this.rewards = new ArrayList<>();
        this.prerequisites = new ArrayList<>();
    }

    // ==================== BUILDER METHODS ====================
    // These let you chain calls like: quest.addObjective(...).addReward(...).setHint(...)

    public Quest addObjective(QuestObjective objective) {
        objectives.add(objective);
        return this;
    }

    public Quest addReward(QuestReward reward) {
        rewards.add(reward);
        return this;
    }

    public Quest addPrerequisite(String questId) {
        prerequisites.add(questId);
        return this;
    }

    public Quest setStartDialogue(String dialogue) {
        this.startDialogue = dialogue;
        return this;
    }

    public Quest setCompleteDialogue(String dialogue) {
        this.completeDialogue = dialogue;
        return this;
    }

    public Quest setHint(String hint) {
        this.hint = hint;
        return this;
    }

    // ==================== PROGRESS CHECKING ====================

    /**
     * Check if all objectives are complete for a player
     */
    public boolean isComplete(Player player) {
        for (QuestObjective objective : objectives) {
            if (!objective.isComplete(player)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get a human-readable progress string
     */
    public String getProgressString(Player player) {
        int completed = 0;
        for (QuestObjective obj : objectives) {
            if (obj.isComplete(player)) completed++;
        }
        return completed + "/" + objectives.size() + " objectives";
    }

    /**
     * Grant all rewards to the player
     */
    public void grantRewards(Player player) {
        for (QuestReward reward : rewards) {
            reward.grant(player);
        }
    }

    // ==================== GETTERS ====================

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getStoryline() { return storyline; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public List<QuestReward> getRewards() { return rewards; }
    public List<String> getPrerequisites() { return prerequisites; }
    public String getStartDialogue() { return startDialogue; }
    public String getCompleteDialogue() { return completeDialogue; }
    public String getHint() { return hint; }

    // ==================== INNER CLASSES ====================

    /**
     * Represents something the player must do
     */
    public static abstract class QuestObjective {
        protected String description;
        
        public QuestObjective(String description) {
            this.description = description;
        }
        
        public abstract boolean isComplete(Player player);
        public abstract String getProgressString(Player player);
        public String getDescription() { return description; }
    }

    /**
     * Objective: Collect X of an item
     */
    public static class CollectItemObjective extends QuestObjective {
        private final Item item;
        private final int count;
        
        public CollectItemObjective(String description, Item item, int count) {
            super(description);
            this.item = item;
            this.count = count;
        }
        
        @Override
        public boolean isComplete(Player player) {
            return countItems(player) >= count;
        }
        
        @Override
        public String getProgressString(Player player) {
            return countItems(player) + "/" + count;
        }
        
        private int countItems(Player player) {
            int total = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() == item) {
                    total += stack.getCount();
                }
            }
            return total;
        }
    }

    /**
     * Objective: Visit a location (be within X blocks of coordinates)
     */
    public static class VisitLocationObjective extends QuestObjective {
        private final double x, y, z;
        private final double radius;
        private final String locationName;
        
        public VisitLocationObjective(String description, String locationName, 
                                       double x, double y, double z, double radius) {
            super(description);
            this.locationName = locationName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }
        
        @Override
        public boolean isComplete(Player player) {
            // This needs to be tracked separately since it's a "did you ever visit" check
            // For now, check if player is currently there
            double dist = player.distanceToSqr(x, y, z);
            return dist <= (radius * radius);
        }
        
        @Override
        public String getProgressString(Player player) {
            return isComplete(player) ? "✓ Visited" : "Not yet visited";
        }
    }

    /**
     * Represents a reward for completing a quest
     */
    public static abstract class QuestReward {
        protected String description;
        
        public QuestReward(String description) {
            this.description = description;
        }
        
        public abstract void grant(Player player);
        public String getDescription() { return description; }
    }

    /**
     * Reward: Give the player an item
     */
    public static class ItemReward extends QuestReward {
        private final Item item;
        private final int count;
        
        public ItemReward(String description, Item item, int count) {
            super(description);
            this.item = item;
            this.count = count;
        }
        
        @Override
        public void grant(Player player) {
            ItemStack stack = new ItemStack(item, count);
            if (!player.addItem(stack)) {
                // Inventory full - drop at player's feet
                player.drop(stack, false);
            }
        }
    }

    /**
     * Reward: Give the player XP
     */
    public static class XpReward extends QuestReward {
        private final int xp;
        
        public XpReward(int xp) {
            super(xp + " XP");
            this.xp = xp;
        }
        
        @Override
        public void grant(Player player) {
            player.giveExperiencePoints(xp);
        }
    }
}
