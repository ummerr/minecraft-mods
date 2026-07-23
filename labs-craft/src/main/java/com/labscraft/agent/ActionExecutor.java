package com.labscraft.agent;

import com.labscraft.LabsCraft;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.quest.AdvanceResult;
import com.labscraft.quest.ObjectiveCompletionResult;
import com.labscraft.quest.QuestManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Validates and applies actions returned by the agent server.
 *
 * <p><b>Single clock (v1 defect #3 fix):</b> the executor owns no tick counter.
 * {@link AgentBridge} increments ONE monotonic counter in
 * {@code END_SERVER_TICK} and passes the same value to both
 * {@link #submit} (schedule base) and {@link #onServerTick} (drain), so
 * {@code delay_ticks} is honored exactly.</p>
 *
 * <p><b>Untrusted input (protocol rule 2):</b> every action goes through
 * {@link ActionValidator} before scheduling; rejects are dropped individually
 * and logged at debug. <b>Quest authority (rule 3):</b> ADVANCE_QUEST /
 * COMPLETE_OBJECTIVE route through {@link QuestManager} and refusals are
 * honored (logged at debug).</p>
 *
 * <p>All methods run on the server thread.</p>
 */
public final class ActionExecutor {

    /** How far from the player we look for a Josh to act through. */
    public static final double JOSH_SEARCH_RANGE = 64.0;

    private final ActionScheduler scheduler = new ActionScheduler();
    private final AtomicLong executedCount = new AtomicLong();

    /**
     * Validates {@code actions} against {@code josh}'s position and schedules
     * the survivors relative to {@code nowTick}. Returns how many were accepted.
     */
    public int submit(ServerPlayerEntity player, JoshWoodwardEntity josh,
            List<AgentAction> actions, long nowTick) {
        List<AgentAction> accepted = new ArrayList<>(actions.size());
        for (AgentAction action : actions) {
            Optional<String> rejection = ActionValidator.reject(action, josh.getX(), josh.getY(), josh.getZ());
            if (rejection.isPresent()) {
                LabsCraft.LOGGER.debug("[agent] dropped invalid action ({}): {}", rejection.get(), action);
            } else {
                accepted.add(action);
            }
        }
        scheduler.schedule(player.getUuid(), accepted, nowTick);
        return accepted.size();
    }

    /** Executes everything due at {@code nowTick} (same clock as {@link #submit}). */
    public void onServerTick(MinecraftServer server, long nowTick) {
        for (ActionScheduler.Scheduled scheduled : scheduler.drainDue(nowTick)) {
            try {
                execute(server, scheduled);
            } catch (Exception e) {
                LabsCraft.LOGGER.debug("[agent] action execution failed: {}", scheduled.action(), e);
            }
        }
    }

    /** Lifetime count of actions actually executed (smoke-test diagnostic). */
    public long executedCount() {
        return executedCount.get();
    }

    public int pendingCount() {
        return scheduler.pending();
    }

    public void clear() {
        scheduler.clear();
    }

    // ------------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------------

    private void execute(MinecraftServer server, ActionScheduler.Scheduled scheduled) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(scheduled.playerUuid());
        if (player == null) {
            return; // player logged off while the action was queued
        }
        JoshWoodwardEntity josh = findNearestJosh(player, JOSH_SEARCH_RANGE);
        if (josh == null) {
            return; // no Josh left near the player
        }
        AgentAction action = scheduled.action();
        switch (action.type()) {
            case "SAY" -> josh.say(action.text());
            case "WALK_TO" -> {
                if (action.targetPlayer()) {
                    josh.walkTo(player.getX(), player.getY(), player.getZ(), action.speed());
                } else {
                    josh.walkTo(action.x(), action.y(), action.z(), action.speed());
                }
            }
            case "LOOK_AT" -> {
                if (action.targetPlayer()) {
                    josh.lookAtEntity(player);
                } else {
                    josh.lookAtPosition(action.x(), action.y(), action.z());
                }
            }
            case "EMOTE" -> josh.playEmote(action.emote());
            case "GIVE_ITEM" -> giveItem(player, action);
            case "ADVANCE_QUEST" -> {
                AdvanceResult result = QuestManager.requestAdvance(player);
                if (!result.allowed()) {
                    LabsCraft.LOGGER.debug("[agent] ADVANCE_QUEST refused by QuestManager: {}", result.reason());
                }
            }
            case "COMPLETE_OBJECTIVE" -> {
                ObjectiveCompletionResult result =
                        QuestManager.requestCompleteObjective(player, action.objectiveId());
                if (!result.allowed()) {
                    LabsCraft.LOGGER.debug("[agent] COMPLETE_OBJECTIVE '{}' refused by QuestManager: {}",
                            action.objectiveId(), result.reason());
                }
            }
            case "WAIT" -> {
                // Deliberate no-op.
            }
            default -> {
                // Unreachable: validator only passes known types.
            }
        }
        executedCount.incrementAndGet();
    }

    private static void giveItem(ServerPlayerEntity player, AgentAction action) {
        Item item = Registries.ITEM.get(Identifier.of(action.item()));
        if (item == Items.AIR) {
            LabsCraft.LOGGER.debug("[agent] GIVE_ITEM resolved to air, skipping: {}", action.item());
            return;
        }
        ItemStack stack = new ItemStack(item, action.quantity());
        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
    }

    /** Nearest Josh to {@code player} within {@code range} blocks, or null. */
    public static JoshWoodwardEntity findNearestJosh(ServerPlayerEntity player, double range) {
        return player.getServerWorld().getEntitiesByClass(
                        JoshWoodwardEntity.class,
                        player.getBoundingBox().expand(range),
                        entity -> entity.isAlive())
                .stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo))
                .orElse(null);
    }
}
