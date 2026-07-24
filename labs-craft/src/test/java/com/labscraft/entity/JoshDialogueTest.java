package com.labscraft.entity;

import com.labscraft.quest.PlayerQuestState;
import com.labscraft.quest.QuestEvent;
import com.labscraft.quest.QuestStage;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The static-dialogue fallback must be genuinely good on its own: every stage
 * and objective combination produces a real, in-character, in-budget line.
 */
class JoshDialogueTest {

    private static PlayerQuestState stateAt(QuestStage stage) {
        PlayerQuestState state = new PlayerQuestState(() -> 0L);
        state.forceSetStage(stage);
        return state;
    }

    @Test
    void everyStageAndVariantProducesAValidLine() {
        for (QuestStage stage : QuestStage.values()) {
            PlayerQuestState state = stateAt(stage);
            for (int variant = 0; variant < 12; variant++) {
                String line = JoshDialogue.lineFor(stage, state.objectives(), variant);
                assertNotNull(line, "null line at " + stage + " variant " + variant);
                assertFalse(line.isBlank(), "blank line at " + stage + " variant " + variant);
                assertTrue(line.length() <= JoshDialogue.MAX_LINE_LENGTH,
                        "line over " + JoshDialogue.MAX_LINE_LENGTH + " chars at " + stage
                                + " variant " + variant + ": " + line);
                assertFalse(line.contains("%"), "unformatted placeholder at " + stage + ": " + line);
            }
        }
    }

    @Test
    void variantsRotateThroughDistinctLines() {
        PlayerQuestState state = stateAt(QuestStage.FLOW_INTRO);
        Set<String> lines = new HashSet<>();
        for (int variant = 0; variant < 4; variant++) {
            lines.add(JoshDialogue.lineFor(QuestStage.FLOW_INTRO, state.objectives(), variant));
        }
        assertTrue(lines.size() >= 3, "expected multiple distinct mining lines, got " + lines);
    }

    @Test
    void miningLinesCarryRealProgressCounters() {
        PlayerQuestState state = stateAt(QuestStage.FLOW_INTRO);
        state.handleEvent(QuestEvent.blockMined("labscraft:tpu_ore"));
        state.handleEvent(QuestEvent.blockMined("labscraft:deepslate_tpu_ore"));

        String line = JoshDialogue.lineFor(state.stage(), state.objectives(), 0);
        assertTrue(line.contains("2") && line.contains("3"),
                "expected progress 2/3 to surface in: " + line);
    }

    @Test
    void dialogueTracksTheFirstUnmetObjective() {
        PlayerQuestState state = stateAt(QuestStage.FLOW_INTRO);
        // Finish mining; the craft_tpu objective should now drive the line.
        for (int i = 0; i < 3; i++) {
            state.handleEvent(QuestEvent.blockMined("labscraft:tpu_ore"));
        }
        assertEquals(QuestStage.FLOW_INTRO, state.stage());
        String line = JoshDialogue.lineFor(state.stage(), state.objectives(), 0);
        assertTrue(line.toLowerCase().contains("tpu"), "expected a crafting line, got: " + line);
        assertFalse(line.contains("mine "), "should no longer push mining: " + line);
    }

    @Test
    void completedStageGetsReturnOfferLines() {
        PlayerQuestState state = stateAt(QuestStage.COMPLETED);
        String line = JoshDialogue.lineFor(QuestStage.COMPLETED, state.objectives(), 0);
        assertNotNull(line);
        assertFalse(line.isBlank());
    }

    @Test
    void nullObjectivesFallBackGracefully() {
        String line = JoshDialogue.lineFor(QuestStage.FLOW_INTRO, null, 0);
        assertNotNull(line);
        assertFalse(line.isBlank());
    }

    @Test
    void unknownObjectiveIdsFallBackToGenericLines() {
        // Empty objective list on a non-terminal stage → fallback pool.
        String line = JoshDialogue.lineFor(QuestStage.FLOW_INTRO, List.of(), 1);
        assertNotNull(line);
        assertFalse(line.isBlank());
    }

    @Test
    void negativeVariantsDoNotThrow() {
        PlayerQuestState state = stateAt(QuestStage.VIDEO_LAUNCH);
        String line = JoshDialogue.lineFor(state.stage(), state.objectives(), -7);
        assertNotNull(line);
        assertFalse(line.isBlank());
    }
}
