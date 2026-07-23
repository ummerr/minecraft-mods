package com.labscraft.mixin;

import com.labscraft.integration.QuestIntegration;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires the quest layer's item-crafted event when a player takes a result out
 * of a vanilla crafting grid. Needed because Fabric API ships no crafting
 * event, and the quest line requires {@code craft_tpu} / {@code craft_flow_table}
 * which happen at a vanilla crafting table.
 *
 * <p>Yarn 1.21.4 target verified against the decompiled merged jar:
 * {@code public void onTakeItem(PlayerEntity player, ItemStack stack)}.</p>
 */
@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin {

    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void labscraft$afterCraftTaken(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        QuestIntegration.onCraftingResultTaken(player, stack);
    }
}
