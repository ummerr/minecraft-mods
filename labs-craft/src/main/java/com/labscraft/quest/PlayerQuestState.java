package com.labscraft.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * The per-player quest state machine. This is the single authority on stage
 * progression: stages advance only when every objective in the current stage
 * is genuinely satisfied (fixes v1 defect #4 — objectives are real, and
 * PROTOCOL-V2 rule 3 — the LLM cannot skip the player ahead).
 *
 * <p>Pure Java, no Minecraft imports; the clock is injected so tests control
 * time. All mutation goes through this class.
 */
public final class PlayerQuestState {

    private final LongSupplier clockMs;
    private QuestStage stage;
    private List<Objective> objectives;
    private long stageEnteredAtMs;

    public PlayerQuestState(LongSupplier clockMs) {
        this.clockMs = clockMs;
        enterStage(QuestStage.NOT_STARTED);
    }

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    public QuestStage stage() {
        return stage;
    }

    /** Live objectives for the current stage (unmodifiable view). */
    public List<Objective> objectives() {
        return List.copyOf(objectives);
    }

    public long secondsInStage() {
        return Math.max(0, (clockMs.getAsLong() - stageEnteredAtMs) / 1000);
    }

    /** True when every objective of the current stage is done (vacuously true for COMPLETED). */
    public boolean allObjectivesDone() {
        return objectives.stream().allMatch(Objective::isDone);
    }

    // ------------------------------------------------------------------
    // Event intake — the only path by which objectives make progress
    // ------------------------------------------------------------------

    /**
     * Feeds one real game event into the machine. Matching, not-yet-done
     * objectives whose prerequisites are met gain one progress point; if that
     * completes the stage, the machine auto-advances to the next stage.
     */
    public QuestUpdate handleEvent(QuestEvent event) {
        QuestStage before = stage;
        List<Objective> progressed = new ArrayList<>();
        List<Objective> completed = new ArrayList<>();

        for (Objective objective : objectives) {
            if (objective.isDone() || !objective.definition().matches(event) || !prerequisitesMet(objective)) {
                continue;
            }
            objective.addProgress(1);
            progressed.add(objective);
            if (objective.isDone()) {
                completed.add(objective);
            }
        }

        advanceWhileComplete();
        return new QuestUpdate(progressed, completed, before, stage);
    }

    // ------------------------------------------------------------------
    // Authority: agent requests (PROTOCOL-V2 rule 3)
    // ------------------------------------------------------------------

    /**
     * ADVANCE_QUEST is a request, not an order. Refused unless the current
     * stage's objectives are all genuinely met. (In normal play the machine
     * auto-advances the moment the last objective completes, so this mostly
     * refuses — that is by design.)
     */
    public AdvanceResult requestAdvance() {
        if (stage.isTerminal()) {
            return AdvanceResult.refused(stage, "already at final stage " + stage.name());
        }
        if (!allObjectivesDone()) {
            List<String> unmet = objectives.stream()
                    .filter(o -> !o.isDone())
                    .map(o -> o.id() + " (" + o.progress() + "/" + o.goal() + ")")
                    .toList();
            return AdvanceResult.refused(stage, "objectives not met: " + String.join(", ", unmet));
        }
        QuestStage from = stage;
        enterStage(stage.next());
        advanceWhileComplete();
        return AdvanceResult.advanced(from, stage);
    }

    /**
     * COMPLETE_OBJECTIVE is likewise only a request. It is honored only for
     * current-stage objectives explicitly flagged agent-completable (social
     * objectives like talking to Josh) whose prerequisites are met. Mining,
     * crafting, and generation objectives can never be completed this way.
     */
    public ObjectiveCompletionResult requestCompleteObjective(String objectiveId) {
        Objective target = objectives.stream()
                .filter(o -> o.id().equals(objectiveId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return ObjectiveCompletionResult.refused(stage,
                    "no objective '" + objectiveId + "' in stage " + stage.name());
        }
        if (target.isDone()) {
            return ObjectiveCompletionResult.refused(stage, "objective '" + objectiveId + "' already done");
        }
        if (!target.definition().agentCompletable()) {
            return ObjectiveCompletionResult.refused(stage,
                    "objective '" + objectiveId + "' is not agent-completable");
        }
        if (!prerequisitesMet(target)) {
            return ObjectiveCompletionResult.refused(stage,
                    "objective '" + objectiveId + "' has unmet prerequisites");
        }
        QuestStage before = stage;
        target.forceComplete();
        advanceWhileComplete();
        QuestUpdate update = new QuestUpdate(List.of(target), List.of(target), before, stage);
        return new ObjectiveCompletionResult(true, "completed " + objectiveId, update);
    }

    // ------------------------------------------------------------------
    // Testing / op-command controls
    // ------------------------------------------------------------------

    /** Jumps to a stage unconditionally, resetting its objectives. Op/testing tool only. */
    public QuestUpdate forceSetStage(QuestStage target) {
        QuestStage before = stage;
        enterStage(target);
        return new QuestUpdate(List.of(), List.of(), before, stage);
    }

    /** Back to the very beginning. Op/testing tool only. */
    public QuestUpdate reset() {
        return forceSetStage(QuestStage.NOT_STARTED);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    public QuestSaveData save() {
        Map<String, Integer> progress = new LinkedHashMap<>();
        for (Objective objective : objectives) {
            progress.put(objective.id(), objective.progress());
        }
        return new QuestSaveData(stage.name(), progress, secondsInStage());
    }

    /**
     * Restores a snapshot. Tolerant of unknown stage names (falls back to
     * NOT_STARTED) and unknown/missing objective ids (ignored / left at 0) so
     * old saves survive quest-line changes. Time in stage resumes from the
     * saved value rather than counting offline wall-clock time.
     */
    public static PlayerQuestState load(QuestSaveData data, LongSupplier clockMs) {
        PlayerQuestState state = new PlayerQuestState(clockMs);
        QuestStage savedStage = QuestStage.fromName(data.stageName());
        state.enterStage(savedStage != null ? savedStage : QuestStage.NOT_STARTED);
        for (Objective objective : state.objectives) {
            Integer saved = data.objectiveProgress().get(objective.id());
            if (saved != null) {
                objective.restoreProgress(saved);
            }
        }
        state.stageEnteredAtMs = clockMs.getAsLong() - Math.max(0, data.secondsInStage()) * 1000L;
        return state;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private boolean prerequisitesMet(Objective objective) {
        for (String prereqId : objective.definition().prerequisiteIds()) {
            boolean done = objectives.stream().anyMatch(o -> o.id().equals(prereqId) && o.isDone());
            if (!done) {
                return false;
            }
        }
        return true;
    }

    private void enterStage(QuestStage target) {
        this.stage = target;
        this.objectives = new ArrayList<>();
        for (ObjectiveDefinition definition : QuestLine.definitionFor(target).objectives()) {
            this.objectives.add(new Objective(definition));
        }
        this.stageEnteredAtMs = clockMs.getAsLong();
    }

    /** Auto-advance while the current stage is fully satisfied (terminal stage never advances). */
    private void advanceWhileComplete() {
        while (!stage.isTerminal() && allObjectivesDone()) {
            enterStage(stage.next());
        }
    }
}
