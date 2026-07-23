package com.labscraft.quest;

/**
 * A live objective instance for one player: an {@link ObjectiveDefinition}
 * plus a progress counter. Mutations happen only through
 * {@link PlayerQuestState}. Pure Java.
 */
public final class Objective {
    private final ObjectiveDefinition definition;
    private int progress;

    Objective(ObjectiveDefinition definition) {
        this.definition = definition;
        this.progress = 0;
    }

    public ObjectiveDefinition definition() {
        return definition;
    }

    public String id() {
        return definition.id();
    }

    public String description() {
        return definition.description();
    }

    public int progress() {
        return progress;
    }

    public int goal() {
        return definition.goal();
    }

    public boolean isDone() {
        return progress >= definition.goal();
    }

    /** Adds progress, clamped to the goal. Package-private: only the state machine mutates. */
    void addProgress(int amount) {
        progress = Math.min(definition.goal(), progress + Math.max(0, amount));
    }

    /** Restores persisted progress (clamped to [0, goal]). */
    void restoreProgress(int saved) {
        progress = Math.max(0, Math.min(definition.goal(), saved));
    }

    void forceComplete() {
        progress = definition.goal();
    }

    @Override
    public String toString() {
        return id() + " " + progress + "/" + goal() + (isDone() ? " (done)" : "");
    }
}
