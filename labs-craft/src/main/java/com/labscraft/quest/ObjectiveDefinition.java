package com.labscraft.quest;

import java.util.Set;

/**
 * Immutable template for one objective within a stage. Pure Java.
 *
 * @param id               stable objective id used on the wire and in saves
 * @param description      human-readable text shown to the player and the agent
 * @param goal             number of matching events required (>= 1)
 * @param triggerType      which event type advances this objective
 * @param matchIds         namespaced ids that count; empty set = any id of that type
 * @param prerequisiteIds  ids of objectives in the same stage that must be done
 *                         before this one accepts progress
 * @param agentCompletable whether the LLM's COMPLETE_OBJECTIVE request is
 *                         allowed to complete this objective (true only for
 *                         social objectives; never for mining/crafting)
 */
public record ObjectiveDefinition(
        String id,
        String description,
        int goal,
        QuestEventType triggerType,
        Set<String> matchIds,
        Set<String> prerequisiteIds,
        boolean agentCompletable) {

    public ObjectiveDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("objective id must be non-blank");
        }
        if (goal < 1) {
            throw new IllegalArgumentException("objective goal must be >= 1: " + id);
        }
        matchIds = Set.copyOf(matchIds);
        prerequisiteIds = Set.copyOf(prerequisiteIds);
    }

    /** True when the given event should count toward this objective. */
    public boolean matches(QuestEvent event) {
        if (event.type() != triggerType) {
            return false;
        }
        return matchIds.isEmpty() || matchIds.contains(event.id());
    }
}
