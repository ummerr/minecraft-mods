package com.labscraft.quest;

/**
 * Outcome of a COMPLETE_OBJECTIVE request from the agent. Only objectives
 * flagged {@link ObjectiveDefinition#agentCompletable()} may be completed this
 * way; everything else is refused — the LLM is untrusted input. Pure Java.
 *
 * @param allowed whether the objective was completed
 * @param reason  human-readable explanation, mainly for refusals
 * @param update  resulting state change ({@link QuestUpdate#none} when refused)
 */
public record ObjectiveCompletionResult(boolean allowed, String reason, QuestUpdate update) {

    static ObjectiveCompletionResult refused(QuestStage stage, String reason) {
        return new ObjectiveCompletionResult(false, reason, QuestUpdate.none(stage));
    }
}
