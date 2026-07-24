package com.labscraft.quest;

import java.util.List;

/**
 * What changed as the result of one quest engine call. Used by the
 * Minecraft-facing layer to decide on dirty-marking and player feedback.
 * Pure Java.
 *
 * @param progressed    objectives whose counter advanced (includes the ones
 *                      that completed on this event)
 * @param completed     objectives that reached their goal on this event
 * @param previousStage the stage before the call
 * @param stage         the stage after the call
 */
public record QuestUpdate(
        List<Objective> progressed,
        List<Objective> completed,
        QuestStage previousStage,
        QuestStage stage) {

    public QuestUpdate {
        progressed = List.copyOf(progressed);
        completed = List.copyOf(completed);
    }

    public boolean stageAdvanced() {
        return previousStage != stage;
    }

    public boolean anythingChanged() {
        return !progressed.isEmpty() || stageAdvanced();
    }

    public static QuestUpdate none(QuestStage stage) {
        return new QuestUpdate(List.of(), List.of(), stage, stage);
    }
}
