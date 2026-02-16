package com.labscraft.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class QuestStageTest {

    @Test
    void next_fromNotStarted_returnsFlowIntro() {
        assertEquals(QuestStage.FLOW_INTRO, QuestStage.NOT_STARTED.next());
    }

    @Test
    void next_fromFlowIntro_returnsLearningPipeline() {
        assertEquals(QuestStage.LEARNING_PIPELINE, QuestStage.FLOW_INTRO.next());
    }

    @Test
    void next_fromLearningPipeline_returnsFirstGeneration() {
        assertEquals(QuestStage.FIRST_GENERATION, QuestStage.LEARNING_PIPELINE.next());
    }

    @Test
    void next_fromFirstGeneration_returnsCompleted() {
        assertEquals(QuestStage.COMPLETED, QuestStage.FIRST_GENERATION.next());
    }

    @Test
    void next_fromCompleted_returnsSelf() {
        assertEquals(QuestStage.COMPLETED, QuestStage.COMPLETED.next());
    }

    @Test
    void isAfter_laterStageIsAfterEarlier() {
        assertTrue(QuestStage.COMPLETED.isAfter(QuestStage.NOT_STARTED));
        assertTrue(QuestStage.FLOW_INTRO.isAfter(QuestStage.NOT_STARTED));
        assertTrue(QuestStage.FIRST_GENERATION.isAfter(QuestStage.LEARNING_PIPELINE));
    }

    @Test
    void isAfter_sameStageIsNotAfter() {
        assertFalse(QuestStage.NOT_STARTED.isAfter(QuestStage.NOT_STARTED));
        assertFalse(QuestStage.COMPLETED.isAfter(QuestStage.COMPLETED));
    }

    @Test
    void isAfter_earlierStageIsNotAfterLater() {
        assertFalse(QuestStage.NOT_STARTED.isAfter(QuestStage.FLOW_INTRO));
        assertFalse(QuestStage.LEARNING_PIPELINE.isAfter(QuestStage.COMPLETED));
    }

    @Test
    void isAtLeast_sameStageIsAtLeast() {
        assertTrue(QuestStage.NOT_STARTED.isAtLeast(QuestStage.NOT_STARTED));
        assertTrue(QuestStage.COMPLETED.isAtLeast(QuestStage.COMPLETED));
    }

    @Test
    void isAtLeast_laterStageIsAtLeastEarlier() {
        assertTrue(QuestStage.COMPLETED.isAtLeast(QuestStage.NOT_STARTED));
        assertTrue(QuestStage.FIRST_GENERATION.isAtLeast(QuestStage.FLOW_INTRO));
    }

    @Test
    void isAtLeast_earlierStageIsNotAtLeastLater() {
        assertFalse(QuestStage.NOT_STARTED.isAtLeast(QuestStage.FLOW_INTRO));
        assertFalse(QuestStage.FLOW_INTRO.isAtLeast(QuestStage.COMPLETED));
    }

    @ParameterizedTest
    @EnumSource(QuestStage.class)
    void getDisplayName_neverNull(QuestStage stage) {
        assertNotNull(stage.getDisplayName());
        assertFalse(stage.getDisplayName().isEmpty());
    }

    @Test
    void displayNames_areCorrect() {
        assertEquals("Not Started", QuestStage.NOT_STARTED.getDisplayName());
        assertEquals("Flow Introduction", QuestStage.FLOW_INTRO.getDisplayName());
        assertEquals("Learning the Pipeline", QuestStage.LEARNING_PIPELINE.getDisplayName());
        assertEquals("First Generation", QuestStage.FIRST_GENERATION.getDisplayName());
        assertEquals("Completed", QuestStage.COMPLETED.getDisplayName());
    }

    @Test
    void values_hasExpectedCount() {
        assertEquals(5, QuestStage.values().length);
    }

    @Test
    void ordinals_areSequential() {
        assertEquals(0, QuestStage.NOT_STARTED.ordinal());
        assertEquals(1, QuestStage.FLOW_INTRO.ordinal());
        assertEquals(2, QuestStage.LEARNING_PIPELINE.ordinal());
        assertEquals(3, QuestStage.FIRST_GENERATION.ordinal());
        assertEquals(4, QuestStage.COMPLETED.ordinal());
    }

    @Test
    void next_fullChainProgression() {
        QuestStage stage = QuestStage.NOT_STARTED;
        stage = stage.next();
        assertEquals(QuestStage.FLOW_INTRO, stage);
        stage = stage.next();
        assertEquals(QuestStage.LEARNING_PIPELINE, stage);
        stage = stage.next();
        assertEquals(QuestStage.FIRST_GENERATION, stage);
        stage = stage.next();
        assertEquals(QuestStage.COMPLETED, stage);
        stage = stage.next();
        assertEquals(QuestStage.COMPLETED, stage); // stays at COMPLETED
    }
}
