package com.labscraft.integration;

import com.labscraft.LabsCraft;
import com.labscraft.LabsCraftHooks;
import com.labscraft.quest.QuestManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Feeds real game events into the quest layer. This is the glue that makes
 * the game playable: without it, mining/crafting/generating would never move
 * an objective.
 *
 * <p>Wired paths:</p>
 * <ul>
 *   <li>{@link LabsCraftHooks#GENERATION_COMPLETED} →
 *       {@link QuestManager#onGenerationCompleted} (resolves the starting
 *       player by UUID; skipped if they logged off)</li>
 *   <li>{@link LabsCraftHooks#CONSOLE_CRAFTED} →
 *       {@link QuestManager#onItemCrafted} with {@code "labscraft:" + consoleId}</li>
 *   <li>{@link PlayerBlockBreakEvents#AFTER} → {@link QuestManager#onBlockMined}
 *       (server side only)</li>
 *   <li>Vanilla crafting-table result taken (via
 *       {@code CraftingResultSlotMixin}) → {@link QuestManager#onItemCrafted} —
 *       Fabric API ships no crafting event, so this path is a mixin</li>
 * </ul>
 */
public final class QuestIntegration {

    private QuestIntegration() {
    }

    /** Call once from {@code LabsCraft.onInitialize()}. */
    public static void register() {
        LabsCraftHooks.GENERATION_COMPLETED.add((world, pos, consoleId, output, startedBy, lifetimeCompleted) -> {
            if (startedBy == null) {
                return;
            }
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(startedBy);
            if (player == null) {
                return; // Player logged off mid-generation.
            }
            QuestManager.onGenerationCompleted(player, Registries.ITEM.getId(output.getItem()).toString());
        });

        LabsCraftHooks.CONSOLE_CRAFTED.add((player, consoleId, tablePos) ->
                QuestManager.onItemCrafted(player, "labscraft:" + consoleId));

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                QuestManager.onBlockMined(serverPlayer, Registries.BLOCK.getId(state.getBlock()).toString());
            }
        });

        LabsCraft.LOGGER.info("Quest integration wired (hooks + block break + crafting mixin)");
    }

    /**
     * Called by {@code CraftingResultSlotMixin} whenever a player takes a
     * result out of a vanilla crafting grid. Fires once per craft operation
     * (shift-clicking calls it once per crafted batch item-take).
     */
    public static void onCraftingResultTaken(PlayerEntity player, ItemStack stack) {
        if (player instanceof ServerPlayerEntity serverPlayer && !stack.isEmpty()) {
            QuestManager.onItemCrafted(serverPlayer, Registries.ITEM.getId(stack.getItem()).toString());
        }
    }
}
