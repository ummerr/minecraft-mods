package com.labscraft.quest;

import com.labscraft.command.QuestCommands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * The Minecraft-facing facade of the quest system, and the single authority
 * on quest progression (PROTOCOL-V2 rule 3).
 *
 * <p>Thin by design: all state-machine logic lives in the pure
 * {@link PlayerQuestState}; this class only resolves players to state, marks
 * the persistent store dirty on genuine mutation (defect #9 fix), and emits
 * player feedback.
 *
 * <h2>Wiring</h2>
 * Call {@link #register()} once from {@code LabsCraft.onInitialize()}.
 *
 * <h2>Event intake (for content/entity layers)</h2>
 * <ul>
 *   <li>{@link #onBlockMined(ServerPlayerEntity, String)} — pass
 *       {@code Registries.BLOCK.getId(block).toString()}, e.g. {@code "labscraft:tpu_ore"}</li>
 *   <li>{@link #onItemCrafted(ServerPlayerEntity, String)} — pass the crafted
 *       item's namespaced id, e.g. {@code "labscraft:tpu"}</li>
 *   <li>{@link #onGenerationCompleted(ServerPlayerEntity, String)} — pass the
 *       produced item id, e.g. {@code "labscraft:generated_image"}</li>
 *   <li>{@link #onTalkedToJosh(ServerPlayerEntity)}</li>
 * </ul>
 */
public final class QuestManager {

    private QuestManager() {
    }

    /** Registers the /labscraft quest commands. Call once from LabsCraft.onInitialize(). */
    public static void register() {
        QuestCommands.register();
    }

    /** Read-only access to a player's quest state. Does not mark anything dirty. */
    public static PlayerQuestState getState(ServerPlayerEntity player) {
        return store(player).getOrCreate(player.getUuid());
    }

    /** UUID-keyed variant for callers that only have the server + uuid. */
    public static PlayerQuestState getState(MinecraftServer server, UUID playerUuid) {
        return QuestPersistentState.get(server).getOrCreate(playerUuid);
    }

    // ------------------------------------------------------------------
    // Event intake — Minecraft-thin, delegates to the pure layer
    // ------------------------------------------------------------------

    public static void onBlockMined(ServerPlayerEntity player, String blockId) {
        handle(player, QuestEvent.blockMined(blockId));
    }

    public static void onItemCrafted(ServerPlayerEntity player, String itemId) {
        handle(player, QuestEvent.itemCrafted(itemId));
    }

    public static void onGenerationCompleted(ServerPlayerEntity player, String itemId) {
        handle(player, QuestEvent.generationCompleted(itemId));
    }

    public static void onTalkedToJosh(ServerPlayerEntity player) {
        handle(player, QuestEvent.talkedToJosh());
    }

    // ------------------------------------------------------------------
    // Authority: agent bridge entry points
    // ------------------------------------------------------------------

    /**
     * Handles an ADVANCE_QUEST request from the agent. Refused unless the
     * current stage's objectives are genuinely met — the LLM cannot skip the
     * player ahead.
     */
    public static AdvanceResult requestAdvance(ServerPlayerEntity player) {
        QuestPersistentState store = store(player);
        PlayerQuestState state = store.getOrCreate(player.getUuid());
        AdvanceResult result = state.requestAdvance();
        if (result.allowed()) {
            store.markDirty();
            QuestNotifier.notifyStageChange(player, result.stage());
        }
        return result;
    }

    /**
     * Handles a COMPLETE_OBJECTIVE request from the agent. Honored only for
     * agent-completable objectives in the current stage (see
     * {@link PlayerQuestState#requestCompleteObjective}).
     */
    public static ObjectiveCompletionResult requestCompleteObjective(ServerPlayerEntity player, String objectiveId) {
        QuestPersistentState store = store(player);
        PlayerQuestState state = store.getOrCreate(player.getUuid());
        ObjectiveCompletionResult result = state.requestCompleteObjective(objectiveId);
        if (result.allowed() && result.update().anythingChanged()) {
            store.markDirty();
            QuestNotifier.notifyUpdate(player, result.update());
        }
        return result;
    }

    /**
     * The exact {@code quest} JSON block from PROTOCOL-V2.md
     * ({@code current_stage}, {@code objectives[]}, {@code seconds_in_stage})
     * for the agent bridge to embed in /tick payloads.
     */
    public static String toWireJson(ServerPlayerEntity player) {
        return QuestJsonSerializer.toJson(getState(player));
    }

    // ------------------------------------------------------------------
    // Op/testing controls (used by /labscraft quest set|reset)
    // ------------------------------------------------------------------

    public static void forceSetStage(ServerPlayerEntity player, QuestStage stage) {
        QuestPersistentState store = store(player);
        PlayerQuestState state = store.getOrCreate(player.getUuid());
        state.forceSetStage(stage);
        store.markDirty();
        QuestNotifier.notifyStageChange(player, stage);
    }

    public static void reset(ServerPlayerEntity player) {
        forceSetStage(player, QuestStage.NOT_STARTED);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static void handle(ServerPlayerEntity player, QuestEvent event) {
        QuestPersistentState store = store(player);
        PlayerQuestState state = store.getOrCreate(player.getUuid());
        QuestUpdate update = state.handleEvent(event);
        if (update.anythingChanged()) {
            store.markDirty();
            QuestNotifier.notifyUpdate(player, update);
        }
    }

    private static QuestPersistentState store(ServerPlayerEntity player) {
        return QuestPersistentState.get(player.getServer());
    }
}
