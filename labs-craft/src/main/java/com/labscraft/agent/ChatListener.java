package com.labscraft.agent;

import com.labscraft.LabsCraftHooks;
import com.labscraft.entity.JoshWoodwardEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Map;

/**
 * Feeds real game events into the bridge's {@link RecentEventsTracker}:
 * <ul>
 *   <li>server chat within Josh's earshot (24 blocks) → {@code chat_message}</li>
 *   <li>right-clicking Josh → {@code interaction}</li>
 *   <li>breaking labscraft blocks → {@code block_mined}</li>
 *   <li>player death → {@code player_died}</li>
 *   <li>console generation finishing → {@code generation_completed}</li>
 *   <li>console crafted at the Flow table → {@code item_crafted}</li>
 * </ul>
 *
 * <p>Josh's own {@link JoshWoodwardEntity#say} output goes directly through
 * {@code player.sendMessage} and never enters the vanilla chat pipeline, so it
 * cannot feed back in here as a player chat event.</p>
 */
final class ChatListener {

    /** Chat is only relevant when a Josh is within earshot of the speaker. */
    private static final double CHAT_EARSHOT = JoshWoodwardEntity.CHAT_RANGE;

    private ChatListener() {
    }

    static void register(AgentBridge bridge) {
        // Player chat near Josh -> chat_message.
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (ActionExecutor.findNearestJosh(sender, CHAT_EARSHOT) == null) {
                return;
            }
            String text = message.getContent().getString();
            if (!text.isBlank()) {
                bridge.recordEvent(sender.getUuid(), "chat_message", Map.of("text", text));
            }
        });

        // Right-clicking Josh -> interaction.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient()
                    && hand == Hand.MAIN_HAND
                    && entity instanceof JoshWoodwardEntity
                    && player instanceof ServerPlayerEntity serverPlayer) {
                bridge.recordEvent(serverPlayer.getUuid(), "interaction", Map.of());
            }
            return ActionResult.PASS;
        });

        // Breaking labscraft blocks -> block_mined.
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            if (blockId.startsWith("labscraft:")) {
                bridge.recordEvent(serverPlayer.getUuid(), "block_mined", Map.of("block", blockId));
            }
        });

        // Player death -> player_died.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                bridge.recordEvent(player.getUuid(), "player_died", Map.of());
            }
        });

        // Generation finished -> generation_completed (keyed to the starter).
        LabsCraftHooks.GENERATION_COMPLETED.add((world, pos, consoleId, output, startedBy, lifetimeCompleted) -> {
            if (startedBy != null) {
                bridge.recordEvent(startedBy, "generation_completed",
                        Map.of("item", Registries.ITEM.getId(output.getItem()).toString()));
            }
        });

        // Console crafted at the Flow Crafting Table -> item_crafted.
        LabsCraftHooks.CONSOLE_CRAFTED.add((player, consoleId, tablePos) ->
                bridge.recordEvent(player.getUuid(), "item_crafted", Map.of("item", "labscraft:" + consoleId)));
    }
}
