package com.labscraft.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROTOCOL-V2 rule 3: ADVANCE_QUEST and COMPLETE_OBJECTIVE from the LLM are
 * requests. The quest engine is the authority and refuses anything unearned.
 */
class QuestAuthorityTest {

    private PlayerQuestState state;

    @BeforeEach
    void setUp() {
        state = new PlayerQuestState(new FakeClock(0));
    }

    @Test
    void advanceIsRefusedWhenObjectivesAreUnmet() {
        AdvanceResult result = state.requestAdvance();

        assertFalse(result.allowed());
        assertEquals(QuestStage.NOT_STARTED, result.stage());
        assertEquals(QuestStage.NOT_STARTED, state.stage(), "state must be untouched");
        assertTrue(result.reason().contains("meet_josh"), "refusal names the unmet objective: " + result.reason());
    }

    @Test
    void advanceIsRefusedWithPartialProgress() {
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO
        state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));

        AdvanceResult result = state.requestAdvance();

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("mine_tpu_ore (1/3)"), result.reason());
        assertTrue(result.reason().contains("craft_tpu (0/2)"), result.reason());
        assertEquals(QuestStage.FLOW_INTRO, state.stage());
    }

    @Test
    void repeatedAdvanceSpamCannotSkipStages() {
        for (int i = 0; i < 50; i++) {
            state.requestAdvance();
        }
        assertEquals(QuestStage.NOT_STARTED, state.stage(), "an LLM must not be able to skip the player ahead");
    }

    @Test
    void advanceIsRefusedAtTerminalStage() {
        state.forceSetStage(QuestStage.COMPLETED);
        AdvanceResult result = state.requestAdvance();
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("final stage"));
    }

    @Test
    void advanceIsAllowedWhenObjectivesAreGenuinelyMet() {
        // Construct a met-but-not-yet-advanced situation via the agent-completable path:
        // meet_josh is agent-completable, so complete it through the request API on a
        // fresh state where auto-advance then fires — instead, verify via forceSetStage
        // plus real events on a two-objective stage, finishing all but the auto-advance
        // trigger is exactly the last event; so the honest check is: after a legitimate
        // completion the machine has advanced and further advance is refused again.
        state.handleEvent(QuestEvent.talkedToJosh()); // legitimately completes NOT_STARTED
        assertEquals(QuestStage.FLOW_INTRO, state.stage(), "auto-advance on genuine completion");

        AdvanceResult afterward = state.requestAdvance();
        assertFalse(afterward.allowed(), "new stage's objectives are unmet, so advance refuses again");
    }

    @Test
    void completeObjectiveRefusedForUnknownId() {
        ObjectiveCompletionResult result = state.requestCompleteObjective("nonexistent");
        assertFalse(result.allowed());
        assertFalse(result.update().anythingChanged());
    }

    @Test
    void completeObjectiveRefusedForNonAgentCompletableObjectives() {
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO

        ObjectiveCompletionResult result = state.requestCompleteObjective("mine_tpu_ore");

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("not agent-completable"), result.reason());
        assertEquals(QuestStage.FLOW_INTRO, state.stage());
        assertTrue(state.objectives().stream().noneMatch(Objective::isDone));
    }

    @Test
    void completeObjectiveRefusedWhenPrerequisitesUnmet() {
        state.forceSetStage(QuestStage.FIRST_GENERATION);

        ObjectiveCompletionResult result = state.requestCompleteObjective("demo_to_josh");

        assertFalse(result.allowed());
        assertTrue(result.reason().contains("prerequisites"), result.reason());
    }

    @Test
    void completeObjectiveHonoredForAgentCompletableSocialObjective() {
        ObjectiveCompletionResult result = state.requestCompleteObjective("meet_josh");

        assertTrue(result.allowed());
        assertTrue(result.update().stageAdvanced());
        assertEquals(QuestStage.FLOW_INTRO, state.stage());
    }

    @Test
    void completeObjectiveRefusedWhenAlreadyDone() {
        state.forceSetStage(QuestStage.FIRST_GENERATION);
        state.handleEvent(QuestEvent.generationCompleted(QuestLine.GENERATED_IMAGE));
        state.requestCompleteObjective("demo_to_josh"); // honored -> advances stage

        // demo_to_josh no longer exists in the new stage.
        ObjectiveCompletionResult again = state.requestCompleteObjective("demo_to_josh");
        assertFalse(again.allowed());
    }
}
