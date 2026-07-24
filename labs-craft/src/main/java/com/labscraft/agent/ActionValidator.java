package com.labscraft.agent;

import com.labscraft.entity.JoshEmote;

import java.util.Optional;
import java.util.Set;

/**
 * Validates parsed actions before execution (PROTOCOL-V2 hard rule 2: the LLM
 * is untrusted input). Invalid actions are dropped individually — the rest of
 * the batch still runs. Pure Java; coordinate sanity is checked relative to
 * Josh's position, which the caller supplies.
 */
public final class ActionValidator {

    /** SAY lines longer than this are dropped (protocol limit). */
    public static final int MAX_SAY_CHARS = 200;

    /** Max distance (blocks) from Josh a WALK_TO/LOOK_AT position may be. */
    public static final double MAX_POSITION_DISTANCE = 64.0;

    /** Max delay a single action may request (60 seconds). */
    public static final int MAX_DELAY_TICKS = 1200;

    /** GIVE_ITEM allowlist — namespaced ids Josh may hand out. */
    public static final Set<String> ITEM_ALLOWLIST = Set.of(
            "labscraft:tpu",
            "labscraft:flow_sketch",
            "labscraft:generated_image",
            "labscraft:generated_video",
            "minecraft:bread",
            "minecraft:cooked_beef",
            "minecraft:torch");

    private static final Set<String> KNOWN_TYPES = Set.of(
            "SAY", "WALK_TO", "LOOK_AT", "EMOTE", "GIVE_ITEM",
            "ADVANCE_QUEST", "COMPLETE_OBJECTIVE", "WAIT");

    private ActionValidator() {
    }

    /**
     * Returns a rejection reason, or empty if the action is safe to execute.
     * {@code joshX/Y/Z} anchor the coordinate sanity check.
     */
    public static Optional<String> reject(AgentAction action, double joshX, double joshY, double joshZ) {
        if (action == null || action.type() == null || !KNOWN_TYPES.contains(action.type())) {
            return Optional.of("unknown action type: " + (action == null ? "null" : action.type()));
        }
        if (action.delayTicks() < 0 || action.delayTicks() > MAX_DELAY_TICKS) {
            return Optional.of("delay_ticks out of range: " + action.delayTicks());
        }
        return switch (action.type()) {
            case "SAY" -> rejectSay(action);
            case "WALK_TO" -> rejectWalkTo(action, joshX, joshY, joshZ);
            case "LOOK_AT" -> rejectLookAt(action, joshX, joshY, joshZ);
            case "EMOTE" -> JoshEmote.fromId(action.emote()) == null
                    ? Optional.of("unknown emote: " + action.emote())
                    : Optional.empty();
            case "GIVE_ITEM" -> rejectGiveItem(action);
            case "COMPLETE_OBJECTIVE" -> action.objectiveId() == null || action.objectiveId().isBlank()
                    ? Optional.of("COMPLETE_OBJECTIVE without objective_id")
                    : Optional.empty();
            default -> Optional.empty(); // ADVANCE_QUEST, WAIT need no fields
        };
    }

    private static Optional<String> rejectSay(AgentAction action) {
        if (action.text() == null || action.text().isBlank()) {
            return Optional.of("SAY without text");
        }
        if (action.text().length() > MAX_SAY_CHARS) {
            return Optional.of("SAY text over " + MAX_SAY_CHARS + " chars (" + action.text().length() + ")");
        }
        return Optional.empty();
    }

    private static Optional<String> rejectWalkTo(AgentAction action, double jx, double jy, double jz) {
        double speed = action.speed();
        if (Double.isNaN(speed) || speed < 0.05 || speed > 1.0) {
            return Optional.of("WALK_TO speed out of range: " + speed);
        }
        if (action.targetPlayer()) {
            return Optional.empty();
        }
        return rejectPosition(action, jx, jy, jz, "WALK_TO");
    }

    private static Optional<String> rejectLookAt(AgentAction action, double jx, double jy, double jz) {
        if (action.targetPlayer()) {
            return Optional.empty();
        }
        return rejectPosition(action, jx, jy, jz, "LOOK_AT");
    }

    private static Optional<String> rejectPosition(AgentAction action, double jx, double jy, double jz,
            String what) {
        if (!action.hasPosition()) {
            return Optional.of(what + " with neither target=player nor a position");
        }
        double x = action.x();
        double y = action.y();
        double z = action.z();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return Optional.of(what + " with non-finite coordinates");
        }
        double dx = x - jx;
        double dy = y - jy;
        double dz = z - jz;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > MAX_POSITION_DISTANCE * MAX_POSITION_DISTANCE) {
            return Optional.of(what + " position " + Math.round(Math.sqrt(distSq))
                    + " blocks from Josh (max " + (int) MAX_POSITION_DISTANCE + ")");
        }
        return Optional.empty();
    }

    private static Optional<String> rejectGiveItem(AgentAction action) {
        if (action.item() == null || !ITEM_ALLOWLIST.contains(action.item())) {
            return Optional.of("GIVE_ITEM item not allowlisted: " + action.item());
        }
        if (action.quantity() < 1 || action.quantity() > 64) {
            return Optional.of("GIVE_ITEM quantity out of 1-64: " + action.quantity());
        }
        return Optional.empty();
    }
}
