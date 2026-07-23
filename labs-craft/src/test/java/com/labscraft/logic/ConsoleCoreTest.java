package com.labscraft.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConsoleCoreTest {

    @Test
    void rejectsNonPositiveConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new ConsoleCore(0, 100));
        assertThrows(IllegalArgumentException.class, () -> new ConsoleCore(-1, 100));
        assertThrows(IllegalArgumentException.class, () -> new ConsoleCore(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ConsoleCore(1, -100));
    }

    @Test
    void cannotStartWithoutEnoughTpus() {
        ConsoleCore core = new ConsoleCore(3, 200);
        assertFalse(core.canStart(2, true));
        assertEquals(0, core.start(2, true));
        assertFalse(core.isGenerating());
    }

    @Test
    void cannotStartWithOccupiedOutput() {
        ConsoleCore core = new ConsoleCore(1, 100);
        assertFalse(core.canStart(64, false));
        assertEquals(0, core.start(64, false));
        assertFalse(core.isGenerating());
    }

    @Test
    void startConsumesExactlyTheCostUpFront() {
        ConsoleCore core = new ConsoleCore(3, 200);
        assertEquals(3, core.start(10, true));
        assertTrue(core.isGenerating());
        assertEquals(0, core.getProgress());
    }

    @Test
    void cannotStartWhileGenerating() {
        ConsoleCore core = new ConsoleCore(1, 100);
        assertEquals(1, core.start(10, true));
        assertFalse(core.canStart(10, true));
        assertEquals(0, core.start(10, true));
    }

    @Test
    void completesExactlyAtGenerationTicks() {
        ConsoleCore core = new ConsoleCore(1, 100);
        core.start(1, true);
        for (int i = 0; i < 99; i++) {
            assertFalse(core.tick(), "tick " + i + " should not complete");
        }
        assertTrue(core.tick(), "tick 100 should complete");
        assertFalse(core.isGenerating());
        assertEquals(0, core.getProgress());
        assertEquals(1, core.getCompletedCount());
    }

    @Test
    void tickWhileIdleDoesNothing() {
        ConsoleCore core = new ConsoleCore(1, 100);
        assertFalse(core.tick());
        assertEquals(0, core.getProgress());
        assertEquals(0, core.getCompletedCount());
    }

    @Test
    void completedCountAccumulatesAcrossRuns() {
        ConsoleCore core = new ConsoleCore(1, 5);
        for (int run = 1; run <= 3; run++) {
            assertEquals(1, core.start(1, true));
            for (int i = 0; i < 4; i++) {
                assertFalse(core.tick());
            }
            assertTrue(core.tick());
            assertEquals(run, core.getCompletedCount());
        }
    }

    @Test
    void canStartAgainAfterCompletionOnceOutputCleared() {
        ConsoleCore core = new ConsoleCore(2, 3);
        core.start(2, true);
        core.tick();
        core.tick();
        assertTrue(core.tick());
        // Output now occupied by the artifact:
        assertFalse(core.canStart(5, false));
        // Player takes the artifact:
        assertTrue(core.canStart(5, true));
    }

    @Test
    void restoreRoundTripsPersistedState() {
        ConsoleCore core = new ConsoleCore(1, 100);
        core.restore(42, true, 7);
        assertTrue(core.isGenerating());
        assertEquals(42, core.getProgress());
        assertEquals(7, core.getCompletedCount());

        // Progress resumes and completes at the right total.
        for (int i = 0; i < 57; i++) {
            assertFalse(core.tick());
        }
        assertTrue(core.tick());
        assertEquals(8, core.getCompletedCount());
    }

    @Test
    void restoreClampsBadData() {
        ConsoleCore core = new ConsoleCore(1, 100);
        core.restore(5000, true, -3);
        assertEquals(99, core.getProgress());
        assertEquals(0, core.getCompletedCount());

        ConsoleCore idle = new ConsoleCore(1, 100);
        idle.restore(50, false, 2);
        assertFalse(idle.isGenerating());
        assertEquals(0, idle.getProgress());
        assertEquals(2, idle.getCompletedCount());
    }

    @Test
    void exposesCostAndDuration() {
        ConsoleCore core = new ConsoleCore(3, 200);
        assertEquals(3, core.getTpuCost());
        assertEquals(200, core.getGenerationTicks());
    }
}
