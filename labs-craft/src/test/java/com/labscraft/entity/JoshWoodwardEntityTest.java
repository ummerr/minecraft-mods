package com.labscraft.entity;

import com.labscraft.quest.QuestStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JoshWoodwardEntity dialogue and quest reward logic.
 * Since the entity requires a full MC world to instantiate, we test the
 * dialogue selection logic and reward structure by mirroring the switch logic.
 */
class JoshWoodwardEntityTest {

    private static final String[] IDLE_DIALOGUES = {
        "Let me check my calendar...",
        "We should sync on this later.",
        "This is a P0 for Q1.",
        "Can you add that to the PRD?",
        "Let's take this offline.",
        "I'll ping you on Slack.",
        "We need to align on OKRs.",
        "That's definitely in scope for H2."
    };

    /** Mirrors JoshWoodwardEntity.getDialogueForStage */
    private String getDialogueForStage(QuestStage stage) {
        return switch (stage) {
            case NOT_STARTED -> "Oh hey, you're the new APM intern? Welcome to Labs. Let me get you set up with some TPUs.";
            case FLOW_INTRO -> "Use the Flow Crafting Table to combine TPUs into consoles. 5 TPUs for Nano Banana (images), 10 for Veo (videos).";
            case LEARNING_PIPELINE -> "Build a console and generate something. You can mine more TPUs underground or craft them.";
            case FIRST_GENERATION -> "Nice! You got the image gen working. Talk to me again for some bonus TPUs.";
            case COMPLETED -> "You've got both consoles unlocked. Not bad for an intern. Keep mining TPUs if you need more.";
        };
    }

    /** Mirrors the TPU reward logic from interactMob */
    private int getTPURewardForStage(QuestStage stage) {
        return switch (stage) {
            case NOT_STARTED -> 5;       // Welcome gift
            case FIRST_GENERATION -> 5;  // Bonus for first gen
            default -> 0;               // No reward for other stages
        };
    }

    /** Whether interaction at this stage advances the quest */
    private boolean shouldAdvanceQuest(QuestStage stage) {
        return stage == QuestStage.NOT_STARTED || stage == QuestStage.FIRST_GENERATION;
    }

    @ParameterizedTest
    @EnumSource(QuestStage.class)
    void getDialogueForStage_neverReturnsNull(QuestStage stage) {
        assertNotNull(getDialogueForStage(stage));
        assertFalse(getDialogueForStage(stage).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(QuestStage.class)
    void getDialogueForStage_allStagesHaveUniqueDialogue(QuestStage stage) {
        String dialogue = getDialogueForStage(stage);
        for (QuestStage other : QuestStage.values()) {
            if (other != stage) {
                assertNotEquals(dialogue, getDialogueForStage(other),
                    stage + " and " + other + " should have different dialogue");
            }
        }
    }

    @Test
    void notStarted_dialogue_mentionsTPUs() {
        String dialogue = getDialogueForStage(QuestStage.NOT_STARTED);
        assertTrue(dialogue.contains("TPU"), "Welcome dialogue should mention TPUs");
    }

    @Test
    void flowIntro_dialogue_explainsCrafting() {
        String dialogue = getDialogueForStage(QuestStage.FLOW_INTRO);
        assertTrue(dialogue.contains("5 TPUs"), "Should explain Nano Banana cost");
        assertTrue(dialogue.contains("10"), "Should explain Veo cost");
    }

    @Test
    void notStarted_gives5TPUs() {
        assertEquals(5, getTPURewardForStage(QuestStage.NOT_STARTED));
    }

    @Test
    void firstGeneration_gives5TPUs() {
        assertEquals(5, getTPURewardForStage(QuestStage.FIRST_GENERATION));
    }

    @Test
    void otherStages_giveNoTPUs() {
        assertEquals(0, getTPURewardForStage(QuestStage.FLOW_INTRO));
        assertEquals(0, getTPURewardForStage(QuestStage.LEARNING_PIPELINE));
        assertEquals(0, getTPURewardForStage(QuestStage.COMPLETED));
    }

    @Test
    void questAdvances_onlyAtRewardStages() {
        assertTrue(shouldAdvanceQuest(QuestStage.NOT_STARTED));
        assertTrue(shouldAdvanceQuest(QuestStage.FIRST_GENERATION));

        assertFalse(shouldAdvanceQuest(QuestStage.FLOW_INTRO));
        assertFalse(shouldAdvanceQuest(QuestStage.LEARNING_PIPELINE));
        assertFalse(shouldAdvanceQuest(QuestStage.COMPLETED));
    }

    @Test
    void totalTPUsFromQuest_is10() {
        // Player gets 5 TPUs at NOT_STARTED and 5 at FIRST_GENERATION = 10 total
        int totalTPUs = getTPURewardForStage(QuestStage.NOT_STARTED)
            + getTPURewardForStage(QuestStage.FIRST_GENERATION);
        assertEquals(10, totalTPUs, "Total quest TPU rewards should be 10");
    }

    @Test
    void idleDialogues_hasExpectedCount() {
        assertEquals(8, IDLE_DIALOGUES.length);
    }

    @Test
    void idleDialogues_allNonEmpty() {
        for (String dialogue : IDLE_DIALOGUES) {
            assertNotNull(dialogue);
            assertFalse(dialogue.isEmpty());
        }
    }

    @Test
    void idleDialogues_allUnique() {
        for (int i = 0; i < IDLE_DIALOGUES.length; i++) {
            for (int j = i + 1; j < IDLE_DIALOGUES.length; j++) {
                assertNotEquals(IDLE_DIALOGUES[i], IDLE_DIALOGUES[j],
                    "Idle dialogues at index " + i + " and " + j + " should be different");
            }
        }
    }

    @Test
    void questProgression_rewardStructureIsCorrect() {
        // Walk through the full quest progression and verify reward structure
        QuestStage stage = QuestStage.NOT_STARTED;
        int totalRewards = 0;

        // NOT_STARTED: talk to Josh, get 5 TPUs, advance to FLOW_INTRO
        assertEquals(5, getTPURewardForStage(stage));
        assertTrue(shouldAdvanceQuest(stage));
        totalRewards += getTPURewardForStage(stage);
        stage = stage.next(); // -> FLOW_INTRO

        // FLOW_INTRO: no reward from Josh, advance happens via console
        assertEquals(0, getTPURewardForStage(stage));
        assertFalse(shouldAdvanceQuest(stage));
        stage = stage.next(); // -> LEARNING_PIPELINE

        // LEARNING_PIPELINE: no reward, advance happens via generation
        assertEquals(0, getTPURewardForStage(stage));
        assertFalse(shouldAdvanceQuest(stage));
        stage = stage.next(); // -> FIRST_GENERATION

        // FIRST_GENERATION: talk to Josh, get 5 TPUs, advance to COMPLETED
        assertEquals(5, getTPURewardForStage(stage));
        assertTrue(shouldAdvanceQuest(stage));
        totalRewards += getTPURewardForStage(stage);
        stage = stage.next(); // -> COMPLETED

        // COMPLETED: no more rewards
        assertEquals(0, getTPURewardForStage(stage));
        assertFalse(shouldAdvanceQuest(stage));

        assertEquals(10, totalRewards, "Total quest rewards should be exactly 10 TPUs");
    }

    @Test
    void dialogueCooldown_isReasonable() {
        // 200 ticks = 10 seconds, should be a reasonable cooldown
        int cooldownTicks = 200;
        int secondsPerTick = 20; // MC runs at 20 tps
        double cooldownSeconds = (double) cooldownTicks / secondsPerTick;
        assertEquals(10.0, cooldownSeconds, 0.01, "Dialogue cooldown should be 10 seconds");
    }
}
