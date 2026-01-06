package com.mtlmod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * The McGill Textbook - $200 new, $180 used, worth about $5 of knowledge
 * 
 * When used (right-clicked), grants the player XP and displays a 
 * sarcastic message about the academic experience.
 */
public class TextbookItem extends Item {
    
    // Messages that appear when you "study" the textbook
    private static final String[] STUDY_MESSAGES = {
            "You pretend to understand macroeconomics.",
            "The words blur together after page 47.",
            "You highlight everything, defeating the purpose.",
            "You gain knowledge! Or at least you hope so.",
            "Caffeine required for continued reading.",
            "You question your choice of major.",
            "The library is too loud. Or too quiet. Either way.",
            "You finally understand that one concept from week 3.",
            "You discover someone drew in the margins. Thanks, previous owner.",
            "You fall asleep on the textbook. Knowledge via osmosis?"
    };

    public TextbookItem(Properties properties) {
        super(properties);
    }

    /**
     * What happens when you right-click with the textbook
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide()) {  // Server-side only (prevents duplication bugs)
            // Grant XP (like a bottle o' enchanting)
            player.giveExperiencePoints(15 + level.random.nextInt(20));
            
            // Play a sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
            
            // Show a random study message
            String message = STUDY_MESSAGES[level.random.nextInt(STUDY_MESSAGES.length)];
            player.displayClientMessage(Component.literal(message), true);
            
            // Small cooldown (can't spam-study)
            player.getCooldowns().addCooldown(this, 100); // 5 seconds
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * Tooltip that appears when hovering over the item
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7\"Introduction to Something You'll Forget\""));
        tooltip.add(Component.literal("§7Right-click to study (gain XP)"));
        tooltip.add(Component.literal("§8Cost: $200 | Buyback value: $12"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
