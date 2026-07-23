package com.labscraft.quest;

/**
 * Outcome of an ADVANCE_QUEST request (PROTOCOL-V2 rule 3: the quest engine is
 * the authority; an LLM request is refused unless objectives are genuinely
 * met). Pure Java.
 *
 * @param allowed       whether the stage actually advanced
 * @param previousStage stage before the request
 * @param stage         stage after the request (== previousStage when refused)
 * @param reason        human-readable explanation, mainly for refusals
 */
public record AdvanceResult(boolean allowed, QuestStage previousStage, QuestStage stage, String reason) {

    static AdvanceResult refused(QuestStage current, String reason) {
        return new AdvanceResult(false, current, current, reason);
    }

    static AdvanceResult advanced(QuestStage from, QuestStage to) {
        return new AdvanceResult(true, from, to, "advanced " + from + " -> " + to);
    }
}
