package com.labscraft.logic;

/**
 * Pure-Java state machine for a generation console. No Minecraft imports so it
 * can be unit tested directly.
 *
 * <p>Lifecycle: {@link #start(int, boolean)} consumes the TPU cost up front and
 * begins a timed generation; {@link #tick()} advances it and returns {@code true}
 * exactly once, on the tick the generation completes. The caller (the block
 * entity) is responsible for actually decrementing input items by the amount
 * {@code start} returns and for placing the produced artifact in the output slot
 * when {@code tick} reports completion. This fixes v1 defect #2, where consoles
 * ran a progress bar but produced nothing.</p>
 */
public final class ConsoleCore {
    private final int tpuCost;
    private final int generationTicks;

    private int progress;
    private boolean generating;
    private int completedCount;

    public ConsoleCore(int tpuCost, int generationTicks) {
        if (tpuCost <= 0) {
            throw new IllegalArgumentException("tpuCost must be positive, got " + tpuCost);
        }
        if (generationTicks <= 0) {
            throw new IllegalArgumentException("generationTicks must be positive, got " + generationTicks);
        }
        this.tpuCost = tpuCost;
        this.generationTicks = generationTicks;
    }

    /** True if a generation could start right now. */
    public boolean canStart(int availableTpus, boolean outputEmpty) {
        return !generating && outputEmpty && availableTpus >= tpuCost;
    }

    /**
     * Attempts to start a generation. Returns the number of TPUs the caller must
     * consume from the input (always the full cost), or 0 if the generation
     * could not start.
     */
    public int start(int availableTpus, boolean outputEmpty) {
        if (!canStart(availableTpus, outputEmpty)) {
            return 0;
        }
        generating = true;
        progress = 0;
        return tpuCost;
    }

    /**
     * Advances the generation by one tick. Returns {@code true} exactly on the
     * completing tick; the caller must then produce the output artifact.
     */
    public boolean tick() {
        if (!generating) {
            return false;
        }
        progress++;
        if (progress >= generationTicks) {
            generating = false;
            progress = 0;
            completedCount++;
            return true;
        }
        return false;
    }

    public boolean isGenerating() {
        return generating;
    }

    public int getProgress() {
        return progress;
    }

    public int getGenerationTicks() {
        return generationTicks;
    }

    public int getTpuCost() {
        return tpuCost;
    }

    /** Number of generations this console has completed over its lifetime. */
    public int getCompletedCount() {
        return completedCount;
    }

    /** Restores persisted state (e.g. from NBT). Values are clamped to sane ranges. */
    public void restore(int progress, boolean generating, int completedCount) {
        this.generating = generating;
        this.progress = generating ? Math.max(0, Math.min(progress, generationTicks - 1)) : 0;
        this.completedCount = Math.max(0, completedCount);
    }
}
