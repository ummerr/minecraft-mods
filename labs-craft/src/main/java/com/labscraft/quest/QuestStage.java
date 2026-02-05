package com.labscraft.quest;

public enum QuestStage {
    NOT_STARTED("Not Started"),
    FLOW_INTRO("Flow Introduction"),
    LEARNING_PIPELINE("Learning the Pipeline"),
    FIRST_GENERATION("First Generation"),
    COMPLETED("Completed");

    private final String displayName;

    QuestStage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public QuestStage next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return this;
        }
        return values()[nextOrdinal];
    }

    public boolean isAfter(QuestStage other) {
        return this.ordinal() > other.ordinal();
    }

    public boolean isAtLeast(QuestStage other) {
        return this.ordinal() >= other.ordinal();
    }
}
