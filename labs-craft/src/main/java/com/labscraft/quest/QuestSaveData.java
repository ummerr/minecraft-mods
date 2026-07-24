package com.labscraft.quest;

import java.util.Map;

/**
 * Serialization-format-agnostic snapshot of one player's quest state. The
 * Minecraft layer converts this to/from NBT; tests round-trip it directly.
 * Pure Java.
 *
 * @param stageName         {@link QuestStage#name()} (tolerated if unknown on load)
 * @param objectiveProgress objective id -> progress counter for the current stage
 * @param secondsInStage    seconds spent in the current stage at save time
 */
public record QuestSaveData(String stageName, Map<String, Integer> objectiveProgress, long secondsInStage) {

    public QuestSaveData {
        objectiveProgress = Map.copyOf(objectiveProgress);
    }
}
