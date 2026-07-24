package com.labscraft.quest;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sanity checks on the quest-line content definition. */
class QuestLineTest {

    @Test
    void everyStageHasADefinition() {
        for (QuestStage stage : QuestStage.values()) {
            assertNotNull(QuestLine.definitionFor(stage), "missing definition for " + stage);
            assertEquals(stage, QuestLine.definitionFor(stage).stage());
        }
    }

    @Test
    void everyNonTerminalStageHasObjectives() {
        for (QuestStage stage : QuestStage.values()) {
            if (!stage.isTerminal()) {
                assertFalse(QuestLine.definitionFor(stage).objectives().isEmpty(),
                        stage + " must have at least one objective (defect #4: no fictional stages)");
            }
        }
    }

    @Test
    void terminalStageHasNoObjectives() {
        assertTrue(QuestLine.definitionFor(QuestStage.COMPLETED).objectives().isEmpty());
    }

    @Test
    void objectiveIdsAreGloballyUnique() {
        Set<String> seen = new HashSet<>();
        for (QuestStage stage : QuestStage.values()) {
            for (ObjectiveDefinition objective : QuestLine.definitionFor(stage).objectives()) {
                assertTrue(seen.add(objective.id()), "duplicate objective id: " + objective.id());
            }
        }
    }

    @Test
    void prerequisitesReferenceObjectivesInTheSameStage() {
        for (QuestStage stage : QuestStage.values()) {
            Set<String> stageIds = new HashSet<>();
            QuestLine.definitionFor(stage).objectives().forEach(o -> stageIds.add(o.id()));
            for (ObjectiveDefinition objective : QuestLine.definitionFor(stage).objectives()) {
                for (String prereq : objective.prerequisiteIds()) {
                    assertTrue(stageIds.contains(prereq),
                            objective.id() + " has prerequisite outside its stage: " + prereq);
                }
            }
        }
    }

    @Test
    void onlySocialObjectivesAreAgentCompletable() {
        for (QuestStage stage : QuestStage.values()) {
            for (ObjectiveDefinition objective : QuestLine.definitionFor(stage).objectives()) {
                if (objective.agentCompletable()) {
                    assertEquals(QuestEventType.TALKED_TO_JOSH, objective.triggerType(),
                            objective.id() + ": only talk-to-Josh objectives may be agent-completable");
                }
            }
        }
    }

    @Test
    void stageOrderIsTheInternshipArc() {
        assertEquals(QuestStage.FLOW_INTRO, QuestStage.NOT_STARTED.next());
        assertEquals(QuestStage.LEARNING_PIPELINE, QuestStage.FLOW_INTRO.next());
        assertEquals(QuestStage.FIRST_GENERATION, QuestStage.LEARNING_PIPELINE.next());
        assertEquals(QuestStage.VIDEO_LAUNCH, QuestStage.FIRST_GENERATION.next());
        assertEquals(QuestStage.COMPLETED, QuestStage.VIDEO_LAUNCH.next());
        assertEquals(QuestStage.COMPLETED, QuestStage.COMPLETED.next(), "terminal stage stays terminal");
    }

    @Test
    void protocolExampleObjectiveExists() {
        // PROTOCOL-V2.md's example: FLOW_INTRO carries mine_tpu_ore "Mine 3 TPU Ore" goal 3.
        ObjectiveDefinition mine = QuestLine.definitionFor(QuestStage.FLOW_INTRO).objectives().stream()
                .filter(o -> o.id().equals("mine_tpu_ore")).findFirst().orElseThrow();
        assertEquals("Mine 3 TPU Ore", mine.description());
        assertEquals(3, mine.goal());
    }
}
