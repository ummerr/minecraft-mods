package com.labscraft.quest;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips {@link QuestSaveData} — the format-agnostic snapshot the NBT
 * layer stores verbatim. (The NBT wrapper itself is a 1:1 mapping of this
 * record and cannot be unit-tested off the remapped test classpath.)
 */
class PersistenceRoundTripTest {

    private static Objective find(PlayerQuestState state, String id) {
        return state.objectives().stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void saveAndLoadPreservesStageAndProgress() {
        FakeClock clock = new FakeClock(50_000);
        PlayerQuestState original = new PlayerQuestState(clock);
        original.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO
        original.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        original.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        original.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU));
        clock.advanceSeconds(42);

        QuestSaveData data = original.save();
        assertEquals("FLOW_INTRO", data.stageName());
        assertEquals(Map.of("mine_tpu_ore", 2, "craft_tpu", 1), data.objectiveProgress());
        assertEquals(42, data.secondsInStage());

        FakeClock newSessionClock = new FakeClock(999_999_999L);
        PlayerQuestState restored = PlayerQuestState.load(data, newSessionClock);

        assertEquals(QuestStage.FLOW_INTRO, restored.stage());
        assertEquals(2, find(restored, "mine_tpu_ore").progress());
        assertEquals(1, find(restored, "craft_tpu").progress());
        assertEquals(42, restored.secondsInStage(), "time in stage resumes from the saved value");
    }

    @Test
    void restoredStateContinuesProgressingNormally() {
        PlayerQuestState original = new PlayerQuestState(new FakeClock(0));
        original.handleEvent(QuestEvent.talkedToJosh());
        original.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        original.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));

        PlayerQuestState restored = PlayerQuestState.load(original.save(), new FakeClock(0));
        restored.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        restored.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU));
        QuestUpdate last = restored.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU));

        assertTrue(last.stageAdvanced());
        assertEquals(QuestStage.LEARNING_PIPELINE, restored.stage());
    }

    @Test
    void unknownStageNameFallsBackToNotStarted() {
        QuestSaveData data = new QuestSaveData("STAGE_FROM_THE_FUTURE", Map.of(), 10);
        PlayerQuestState restored = PlayerQuestState.load(data, new FakeClock(0));
        assertEquals(QuestStage.NOT_STARTED, restored.stage());
    }

    @Test
    void unknownObjectiveIdsAreIgnoredAndSavedProgressIsClamped() {
        QuestSaveData data = new QuestSaveData("FLOW_INTRO",
                Map.of("objective_deleted_in_v3", 7, "mine_tpu_ore", 999, "craft_tpu", -5), 0);
        PlayerQuestState restored = PlayerQuestState.load(data, new FakeClock(0));

        assertEquals(QuestStage.FLOW_INTRO, restored.stage());
        assertEquals(3, find(restored, "mine_tpu_ore").progress(), "clamped to goal");
        assertEquals(0, find(restored, "craft_tpu").progress(), "clamped to zero");
    }

    @Test
    void terminalStageRoundTrips() {
        FakeClock clock = new FakeClock(0);
        PlayerQuestState original = new PlayerQuestState(clock);
        original.forceSetStage(QuestStage.COMPLETED);

        PlayerQuestState restored = PlayerQuestState.load(original.save(), clock);
        assertEquals(QuestStage.COMPLETED, restored.stage());
        assertTrue(restored.objectives().isEmpty());
    }
}
