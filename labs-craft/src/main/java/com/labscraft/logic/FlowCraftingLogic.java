package com.labscraft.logic;

/**
 * Pure-Java crafting math for the Flow Crafting Table (10 TPU input slots + 1
 * output). No Minecraft imports so it can be unit tested directly.
 */
public final class FlowCraftingLogic {
    /** TPUs required to craft a Nano Banana Console. */
    public static final int NANO_BANANA_COST = 5;
    /** TPUs required to craft a Veo Console. */
    public static final int VEO_COST = 10;
    /** Number of TPU input slots on the table. */
    public static final int INPUT_SLOTS = 10;

    private FlowCraftingLogic() {
    }

    /** Total TPUs across the input slots. */
    public static int countTpus(int[] slotCounts) {
        int total = 0;
        for (int count : slotCounts) {
            total += Math.max(0, count);
        }
        return total;
    }

    /** True if the given cost is affordable and the output slot is free. */
    public static boolean canCraft(int[] slotCounts, int cost, boolean outputEmpty) {
        return outputEmpty && countTpus(slotCounts) >= cost;
    }

    /**
     * Computes how many TPUs to remove from each slot to pay {@code cost},
     * draining slots front to back. Returns {@code null} if the slots do not
     * hold enough TPUs (in which case nothing should be removed).
     */
    public static int[] removalPlan(int[] slotCounts, int cost) {
        if (cost <= 0 || countTpus(slotCounts) < cost) {
            return null;
        }
        int[] plan = new int[slotCounts.length];
        int remaining = cost;
        for (int i = 0; i < slotCounts.length && remaining > 0; i++) {
            int take = Math.min(Math.max(0, slotCounts[i]), remaining);
            plan[i] = take;
            remaining -= take;
        }
        return plan;
    }
}
