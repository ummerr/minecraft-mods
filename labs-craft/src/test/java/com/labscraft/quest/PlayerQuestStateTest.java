package com.labscraft.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage transitions and objective progress driven by real game events. */
class PlayerQuestStateTest {

    private FakeClock clock;
    private PlayerQuestState state;

    @BeforeEach
    void setUp() {
        clock = new FakeClock(1_000_000L);
        state = new PlayerQuestState(clock);
    }

    private Objective objective(String id) {
        return state.objectives().stream().filter(o -> o.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void startsAtNotStartedWithMeetJoshObjective() {
        assertEquals(QuestStage.NOT_STARTED, state.stage());
        assertEquals(1, state.objectives().size());
        assertEquals("meet_josh", state.objectives().get(0).id());
        assertFalse(state.allObjectivesDone());
    }

    @Test
    void talkingToJoshCompletesOrientationAndAdvances() {
        QuestUpdate update = state.handleEvent(QuestEvent.talkedToJosh());

        assertTrue(update.anythingChanged());
        assertEquals(1, update.completed().size());
        assertEquals("meet_josh", update.completed().get(0).id());
        assertTrue(update.stageAdvanced());
        assertEquals(QuestStage.NOT_STARTED, update.previousStage());
        assertEquals(QuestStage.FLOW_INTRO, update.stage());
        assertEquals(QuestStage.FLOW_INTRO, state.stage());
    }

    @Test
    void miningTpuOreAdvancesTheCounterButNotTheStageUntilAllObjectivesDone() {
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO

        state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        QuestUpdate second = state.handleEvent(QuestEvent.blockMined(QuestLine.DEEPSLATE_TPU_ORE));

        assertEquals(2, objective("mine_tpu_ore").progress());
        assertFalse(second.stageAdvanced());
        assertTrue(second.completed().isEmpty());

        QuestUpdate third = state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        assertEquals(1, third.completed().size());
        assertEquals("mine_tpu_ore", third.completed().get(0).id());
        // craft_tpu still open, so the stage must not advance.
        assertFalse(third.stageAdvanced());
        assertEquals(QuestStage.FLOW_INTRO, state.stage());
    }

    @Test
    void stageAdvancesOnlyWhenEveryObjectiveIsSatisfied() {
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO
        state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        state.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU));
        assertEquals(QuestStage.FLOW_INTRO, state.stage(), "1 of 2 TPU crafted — must not advance");

        QuestUpdate last = state.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU));
        assertTrue(last.stageAdvanced());
        assertEquals(QuestStage.LEARNING_PIPELINE, state.stage());
    }

    @Test
    void irrelevantEventsChangeNothing() {
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO

        QuestUpdate stone = state.handleEvent(QuestEvent.blockMined("minecraft:stone"));
        QuestUpdate wrongType = state.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU_ORE));

        assertFalse(stone.anythingChanged());
        assertFalse(wrongType.anythingChanged());
        assertEquals(0, objective("mine_tpu_ore").progress());
    }

    @Test
    void progressIsClampedAtGoal() {
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO
        for (int i = 0; i < 10; i++) {
            state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        }
        assertEquals(3, objective("mine_tpu_ore").progress());
    }

    @Test
    void prerequisiteBlocksDemoUntilImageIsGenerated() {
        state.forceSetStage(QuestStage.FIRST_GENERATION);

        // Talking to Josh before generating must NOT complete demo_to_josh.
        QuestUpdate early = state.handleEvent(QuestEvent.talkedToJosh());
        assertFalse(early.anythingChanged());
        assertEquals(0, objective("demo_to_josh").progress());

        state.handleEvent(QuestEvent.generationCompleted(QuestLine.GENERATED_IMAGE));
        assertTrue(objective("generate_image").isDone());

        QuestUpdate demo = state.handleEvent(QuestEvent.talkedToJosh());
        assertTrue(demo.stageAdvanced());
        assertEquals(QuestStage.VIDEO_LAUNCH, state.stage());
    }

    @Test
    void fullPlaythroughReachesCompleted() {
        state.handleEvent(QuestEvent.talkedToJosh());
        for (int i = 0; i < 3; i++) {
            state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        }
        state.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU));
        state.handleEvent(QuestEvent.itemCrafted(QuestLine.TPU));
        state.handleEvent(QuestEvent.itemCrafted(QuestLine.FLOW_CRAFTING_TABLE));
        state.handleEvent(QuestEvent.itemCrafted(QuestLine.NANO_BANANA_CONSOLE));
        state.handleEvent(QuestEvent.generationCompleted(QuestLine.GENERATED_IMAGE));
        state.handleEvent(QuestEvent.talkedToJosh());
        for (int i = 0; i < 5; i++) {
            state.handleEvent(QuestEvent.blockMined(QuestLine.DEEPSLATE_TPU_ORE));
        }
        state.handleEvent(QuestEvent.itemCrafted(QuestLine.VEO_CONSOLE));
        QuestUpdate finale = state.handleEvent(QuestEvent.generationCompleted(QuestLine.GENERATED_VIDEO));

        assertEquals(QuestStage.COMPLETED, state.stage());
        assertTrue(finale.stageAdvanced());
        assertTrue(state.objectives().isEmpty());
        assertTrue(state.allObjectivesDone(), "vacuously true at COMPLETED");
    }

    @Test
    void eventsAfterCompletionAreNoOps() {
        state.forceSetStage(QuestStage.COMPLETED);
        QuestUpdate update = state.handleEvent(QuestEvent.talkedToJosh());
        assertFalse(update.anythingChanged());
        assertEquals(QuestStage.COMPLETED, state.stage());
    }

    @Test
    void secondsInStageTracksTheClockAndResetsOnStageChange() {
        clock.advanceSeconds(130);
        assertEquals(130, state.secondsInStage());

        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO
        assertEquals(0, state.secondsInStage());
        clock.advanceSeconds(7);
        assertEquals(7, state.secondsInStage());
    }

    @Test
    void forceSetStageResetsObjectivesFresh() {
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO
        state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        assertEquals(1, objective("mine_tpu_ore").progress());

        state.forceSetStage(QuestStage.FLOW_INTRO);
        assertEquals(0, objective("mine_tpu_ore").progress());
    }

    @Test
    void resetReturnsToNotStarted() {
        state.handleEvent(QuestEvent.talkedToJosh());
        state.reset();
        assertEquals(QuestStage.NOT_STARTED, state.stage());
        assertEquals(0, objective("meet_josh").progress());
    }
}
