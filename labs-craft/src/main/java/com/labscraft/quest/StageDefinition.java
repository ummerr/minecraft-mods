package com.labscraft.quest;

import java.util.List;

/** Immutable template for one stage: its identity plus its objectives. Pure Java. */
public record StageDefinition(QuestStage stage, List<ObjectiveDefinition> objectives) {

    public StageDefinition {
        objectives = List.copyOf(objectives);
    }
}
