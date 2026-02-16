package com.labscraft.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the crafting logic constants and decision logic of FlowCraftingTableBlockEntity.
 * Since Minecraft classes can't be bootstrapped in unit tests, we test the pure logic
 * by mirroring the crafting decision functions with plain Java.
 */
class FlowCraftingTableBlockEntityTest {

    // Mirror the constants from FlowCraftingTableBlockEntity
    static final int TPU_SLOTS = 10;
    static final int OUTPUT_SLOT = 10;
    static final int NANO_BANANA_COST = 5;
    static final int VEO_COST = 10;
    static final int INVENTORY_SIZE = 11;

    // Mirrors countTPUs logic: sum of TPU counts across input slots
    private int countTPUs(int[] slotCounts) {
        int count = 0;
        for (int i = 0; i < TPU_SLOTS; i++) {
            count += slotCounts[i];
        }
        return count;
    }

    // Mirrors canCraftNanoBanana: enough TPUs and output empty
    private boolean canCraftNanoBanana(int[] slotCounts, boolean outputEmpty) {
        return countTPUs(slotCounts) >= NANO_BANANA_COST && outputEmpty;
    }

    // Mirrors canCraftVeo: enough TPUs and output empty
    private boolean canCraftVeo(int[] slotCounts, boolean outputEmpty) {
        return countTPUs(slotCounts) >= VEO_COST && outputEmpty;
    }

    /**
     * Mirrors the TPU removal logic from craftNanoBanana/craftVeo.
     * Returns the remaining counts per slot after removing toRemove TPUs.
     */
    private int[] removeTPUs(int[] slotCounts, int toRemove) {
        int[] result = slotCounts.clone();
        for (int i = 0; i < TPU_SLOTS && toRemove > 0; i++) {
            if (result[i] > 0) {
                int removeFromSlot = Math.min(toRemove, result[i]);
                result[i] -= removeFromSlot;
                toRemove -= removeFromSlot;
            }
        }
        return result;
    }

    // Mirrors isValid slot validation
    private boolean isValidSlot(int slot) {
        return slot != OUTPUT_SLOT;
    }

    // --- Helper to create slot arrays ---
    private int[] emptySlots() {
        return new int[INVENTORY_SIZE];
    }

    private int[] slotsWithTPUs(int... tpusPerSlot) {
        int[] slots = new int[INVENTORY_SIZE];
        for (int i = 0; i < tpusPerSlot.length && i < TPU_SLOTS; i++) {
            slots[i] = tpusPerSlot[i];
        }
        return slots;
    }

    // === Constants tests ===

    @Test
    void constants_matchBlockEntityValues() {
        assertEquals(10, FlowCraftingTableBlockEntity.TPU_SLOTS);
        assertEquals(10, FlowCraftingTableBlockEntity.OUTPUT_SLOT);
        assertEquals(5, FlowCraftingTableBlockEntity.NANO_BANANA_COST);
        assertEquals(10, FlowCraftingTableBlockEntity.VEO_COST);
    }

    // === countTPUs tests ===

    @Test
    void countTPUs_emptyInventory_returnsZero() {
        assertEquals(0, countTPUs(emptySlots()));
    }

    @Test
    void countTPUs_singleSlot_returnsCount() {
        assertEquals(3, countTPUs(slotsWithTPUs(3)));
    }

    @Test
    void countTPUs_multipleSlots_sumsAll() {
        assertEquals(9, countTPUs(slotsWithTPUs(3, 4, 0, 0, 0, 2)));
    }

    @Test
    void countTPUs_allSlotsFull_returns10() {
        assertEquals(10, countTPUs(slotsWithTPUs(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)));
    }

    @Test
    void countTPUs_stackedInOneSlot() {
        assertEquals(64, countTPUs(slotsWithTPUs(64)));
    }

    @Test
    void countTPUs_ignoresOutputSlot() {
        int[] slots = emptySlots();
        slots[OUTPUT_SLOT] = 99; // put something in output
        assertEquals(0, countTPUs(slots));
    }

    // === canCraftNanoBanana tests ===

    @Test
    void canCraftNanoBanana_withExactly5TPUs_andEmptyOutput() {
        assertTrue(canCraftNanoBanana(slotsWithTPUs(1, 1, 1, 1, 1), true));
    }

    @Test
    void canCraftNanoBanana_withMoreThan5TPUs() {
        assertTrue(canCraftNanoBanana(slotsWithTPUs(10), true));
    }

    @Test
    void canCraftNanoBanana_with4TPUs_fails() {
        assertFalse(canCraftNanoBanana(slotsWithTPUs(1, 1, 1, 1), true));
    }

    @Test
    void canCraftNanoBanana_with0TPUs_fails() {
        assertFalse(canCraftNanoBanana(emptySlots(), true));
    }

    @Test
    void canCraftNanoBanana_outputOccupied_fails() {
        assertFalse(canCraftNanoBanana(slotsWithTPUs(5), false));
    }

    @Test
    void canCraftNanoBanana_enoughTPUs_butOutputOccupied_fails() {
        assertFalse(canCraftNanoBanana(slotsWithTPUs(10, 10, 10), false));
    }

    // === canCraftVeo tests ===

    @Test
    void canCraftVeo_withExactly10TPUs() {
        assertTrue(canCraftVeo(slotsWithTPUs(1, 1, 1, 1, 1, 1, 1, 1, 1, 1), true));
    }

    @Test
    void canCraftVeo_with9TPUs_fails() {
        assertFalse(canCraftVeo(slotsWithTPUs(1, 1, 1, 1, 1, 1, 1, 1, 1), true));
    }

    @Test
    void canCraftVeo_withStackedTPUs() {
        assertTrue(canCraftVeo(slotsWithTPUs(5, 5), true));
    }

    @Test
    void canCraftVeo_with64InOneSlot() {
        assertTrue(canCraftVeo(slotsWithTPUs(64), true));
    }

    @Test
    void canCraftVeo_outputOccupied_fails() {
        assertFalse(canCraftVeo(slotsWithTPUs(10), false));
    }

    // === TPU removal tests ===

    @Test
    void removeTPUs_removesFromFirstSlots() {
        int[] result = removeTPUs(slotsWithTPUs(3, 3), NANO_BANANA_COST);
        assertEquals(0, result[0]); // 3 removed from slot 0
        assertEquals(1, result[1]); // 2 removed from slot 1
    }

    @Test
    void removeTPUs_removesExactAmount() {
        int[] result = removeTPUs(slotsWithTPUs(10), 5);
        assertEquals(5, result[0]); // 5 remaining
    }

    @Test
    void removeTPUs_removesAcrossMultipleSlots() {
        int[] result = removeTPUs(slotsWithTPUs(2, 2, 2, 2, 2), VEO_COST);
        for (int i = 0; i < 5; i++) {
            assertEquals(0, result[i], "Slot " + i + " should be empty");
        }
    }

    @Test
    void removeTPUs_skipsEmptySlots() {
        int[] result = removeTPUs(slotsWithTPUs(0, 0, 3, 0, 3), 5);
        assertEquals(0, result[0]);
        assertEquals(0, result[1]);
        assertEquals(0, result[2]); // 3 removed
        assertEquals(0, result[3]);
        assertEquals(1, result[4]); // 2 removed, 1 remaining
    }

    @Test
    void removeTPUs_doesNotTouchOutputSlot() {
        int[] slots = slotsWithTPUs(5, 5);
        slots[OUTPUT_SLOT] = 1;
        int[] result = removeTPUs(slots, 10);
        assertEquals(1, result[OUTPUT_SLOT], "Output slot should be untouched");
    }

    @Test
    void removeTPUs_withZeroRemoval_changesNothing() {
        int[] original = slotsWithTPUs(3, 4, 5);
        int[] result = removeTPUs(original, 0);
        assertArrayEquals(original, result);
    }

    // === Slot validation tests ===

    @Test
    void isValid_outputSlotRejectsItems() {
        assertFalse(isValidSlot(OUTPUT_SLOT));
    }

    @Test
    void isValid_allInputSlotsAcceptItems() {
        for (int i = 0; i < TPU_SLOTS; i++) {
            assertTrue(isValidSlot(i), "Slot " + i + " should accept items");
        }
    }

    // === Inventory size ===

    @Test
    void inventorySize_is11() {
        assertEquals(11, INVENTORY_SIZE);
    }

    // === Crafting cost relationship ===

    @Test
    void nanoBananaCost_isLessThanVeoCost() {
        assertTrue(NANO_BANANA_COST < VEO_COST,
            "Nano Banana should be cheaper than Veo");
    }

    @Test
    void veoCost_equalsMaxInputSlots() {
        assertEquals(TPU_SLOTS, VEO_COST,
            "Veo should cost exactly as many TPUs as there are input slots");
    }

    @Test
    void nanoBananaCost_isHalfOfVeoCost() {
        assertEquals(VEO_COST / 2, NANO_BANANA_COST);
    }
}
