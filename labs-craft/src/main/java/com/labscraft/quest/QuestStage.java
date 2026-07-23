package com.labscraft.quest;

/**
 * The internship arc. Linear progression; each stage's objectives are defined
 * in {@link QuestLine}. Pure Java — no Minecraft imports (unit-testable).
 */
public enum QuestStage {
    NOT_STARTED("Orientation Day"),
    FLOW_INTRO("Compute Procurement"),
    LEARNING_PIPELINE("Infrastructure Buildout"),
    FIRST_GENERATION("First Launch"),
    VIDEO_LAUNCH("Scale to Video"),
    COMPLETED("Return Offer");

    private final String displayName;

    QuestStage(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED;
    }

    /** The following stage, or this stage itself if terminal. */
    public QuestStage next() {
        return isTerminal() ? this : values()[ordinal() + 1];
    }

    /** Case-insensitive lookup; returns null when the name is unknown. */
    public static QuestStage fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
