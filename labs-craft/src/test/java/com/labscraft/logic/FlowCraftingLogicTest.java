package com.labscraft.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FlowCraftingLogicTest {

    @Test
    void costsMatchSpec() {
        assertEquals(5, FlowCraftingLogic.NANO_BANANA_COST);
        assertEquals(10, FlowCraftingLogic.VEO_COST);
        assertEquals(10, FlowCraftingLogic.INPUT_SLOTS);
    }

    @Test
    void countsAcrossSlots() {
        assertEquals(0, FlowCraftingLogic.countTpus(new int[10]));
        assertEquals(7, FlowCraftingLogic.countTpus(new int[] { 3, 0, 4, 0, 0, 0, 0, 0, 0, 0 }));
        assertEquals(640, FlowCraftingLogic.countTpus(new int[] { 64, 64, 64, 64, 64, 64, 64, 64, 64, 64 }));
    }

    @Test
    void countIgnoresNegativeCounts() {
        assertEquals(5, FlowCraftingLogic.countTpus(new int[] { -3, 5, 0 }));
    }

    @Test
    void canCraftRequiresEnoughTpusAndFreeOutput() {
        int[] five = { 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
        assertTrue(FlowCraftingLogic.canCraft(five, FlowCraftingLogic.NANO_BANANA_COST, true));
        assertFalse(FlowCraftingLogic.canCraft(five, FlowCraftingLogic.NANO_BANANA_COST, false));
        assertFalse(FlowCraftingLogic.canCraft(five, FlowCraftingLogic.VEO_COST, true));

        int[] ten = { 2, 2, 2, 2, 2, 0, 0, 0, 0, 0 };
        assertTrue(FlowCraftingLogic.canCraft(ten, FlowCraftingLogic.VEO_COST, true));
    }

    @Test
    void removalPlanDrainsFrontToBack() {
        int[] plan = FlowCraftingLogic.removalPlan(new int[] { 2, 2, 2, 2, 2, 0, 0, 0, 0, 0 }, 5);
        assertArrayEquals(new int[] { 2, 2, 1, 0, 0, 0, 0, 0, 0, 0 }, plan);
    }

    @Test
    void removalPlanSpansGaps() {
        int[] plan = FlowCraftingLogic.removalPlan(new int[] { 1, 0, 3, 0, 0, 0, 0, 0, 0, 6 }, 10);
        assertArrayEquals(new int[] { 1, 0, 3, 0, 0, 0, 0, 0, 0, 6 }, plan);
    }

    @Test
    void removalPlanTakesExactlyTheCost() {
        int[] slots = { 64, 64, 0, 0, 0, 0, 0, 0, 0, 0 };
        int[] plan = FlowCraftingLogic.removalPlan(slots, 10);
        assertArrayEquals(new int[] { 10, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, plan);
        assertEquals(10, FlowCraftingLogic.countTpus(plan));
    }

    @Test
    void removalPlanNullWhenInsufficient() {
        assertNull(FlowCraftingLogic.removalPlan(new int[] { 4, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, 5));
        assertNull(FlowCraftingLogic.removalPlan(new int[10], 1));
    }

    @Test
    void removalPlanNullForNonPositiveCost() {
        assertNull(FlowCraftingLogic.removalPlan(new int[] { 5, 5 }, 0));
        assertNull(FlowCraftingLogic.removalPlan(new int[] { 5, 5 }, -2));
    }

    @Test
    void exactSpendLeavesNothingBehind() {
        int[] slots = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
        int[] plan = FlowCraftingLogic.removalPlan(slots, FlowCraftingLogic.VEO_COST);
        assertArrayEquals(slots, plan);
    }
}
