package com.mtlmod.item;

import com.mtlmod.MtlMod;
import com.mtlmod.quest.Quest;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Quest Journal - Your guide to the Montreal experience
 * 
 * Right-click to see your active quests and progress.
 * For now, this prints quest info to chat. Later we'll add a proper GUI.
 */
public class QuestJournalItem extends Item {

    public QuestJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide() && MtlMod.questManager != null) {
            // Header
            player.sendSystemMessage(Component.literal("§6§l═══ MTL MEMORIES ═══"));
            player.sendSystemMessage(Component.literal("§7Montreal 2004-2008"));
            player.sendSystemMessage(Component.literal(""));
            
            // Get player's quest progress
            List<Quest> activeQuests = MtlMod.questManager.getActiveQuests(player);
            List<Quest> completedQuests = MtlMod.questManager.getCompletedQuests(player);
            
            if (activeQuests.isEmpty() && completedQuests.isEmpty()) {
                player.sendSystemMessage(Component.literal("§7Your adventure begins..."));
                player.sendSystemMessage(Component.literal("§7Talk to NPCs to discover quests."));
            } else {
                // Show active quests
                if (!activeQuests.isEmpty()) {
                    player.sendSystemMessage(Component.literal("§e§lActive Quests:"));
                    for (Quest quest : activeQuests) {
                        player.sendSystemMessage(Component.literal("§f • " + quest.getDisplayName()));
                        player.sendSystemMessage(Component.literal("§7   " + quest.getDescription()));
                        player.sendSystemMessage(Component.literal("§a   Progress: " + quest.getProgressString(player)));
                    }
                    player.sendSystemMessage(Component.literal(""));
                }
                
                // Show completed quests count
                if (!completedQuests.isEmpty()) {
                    player.sendSystemMessage(Component.literal(
                            "§a§lCompleted: §f" + completedQuests.size() + " memories collected"));
                }
            }
            
            player.sendSystemMessage(Component.literal("§6═══════════════════"));
            
            // Cooldown to prevent spam
            player.getCooldowns().addCooldown(this, 20); // 1 second
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Your memories of Montreal"));
        tooltip.add(Component.literal("§7Right-click to view your journey"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
