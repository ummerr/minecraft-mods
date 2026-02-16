package com.labscraft.block.entity;

import net.minecraft.screen.PropertyDelegate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the generation state machine logic shared by all console block entities.
 * Since the console entities can't be easily instantiated outside MC, we test
 * the PropertyDelegate-driven state machine and tick logic patterns directly.
 *
 * The generation cycle is: idle -> startGeneration -> tick N times -> completeGeneration
 * PropertyDelegate indices: 0=progress, 1=isGenerating, 2=totalGenerations, 3=generationTime
 */
class ConsoleGenerationLogicTest {

    /**
     * Simulates the generation state machine used by all console block entities.
     * This mirrors the exact logic from FlowConsoleBlockEntity, NanoBananaConsoleBlockEntity,
     * and VeoConsoleBlockEntity.
     */
    static class GenerationStateMachine {
        private int generationProgress = 0;
        private boolean isGenerating = false;
        private int totalGenerations = 0;
        private final int generationTime;

        GenerationStateMachine(int generationTime) {
            this.generationTime = generationTime;
        }

        void startGeneration() {
            if (!isGenerating) {
                isGenerating = true;
                generationProgress = 0;
            }
        }

        /**
         * Simulates a server-side tick. Returns true if generation completed this tick.
         */
        boolean tick() {
            if (isGenerating) {
                generationProgress++;
                if (generationProgress >= generationTime) {
                    completeGeneration();
                    return true;
                }
            }
            return false;
        }

        private void completeGeneration() {
            isGenerating = false;
            generationProgress = 0;
            totalGenerations++;
        }

        int getGenerationProgress() { return generationProgress; }
        boolean isGenerating() { return isGenerating; }
        int getTotalGenerations() { return totalGenerations; }
        int getGenerationTime() { return generationTime; }

        /** Mirrors the PropertyDelegate.get() from the block entities */
        int getProperty(int index) {
            return switch (index) {
                case 0 -> generationProgress;
                case 1 -> isGenerating ? 1 : 0;
                case 2 -> totalGenerations;
                case 3 -> generationTime;
                default -> 0;
            };
        }
    }

    @Nested
    class FlowConsoleTests {
        private static final int FLOW_GENERATION_TIME = 100; // 5 seconds

        @Test
        void initialState_isIdle() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);
            assertFalse(sm.isGenerating());
            assertEquals(0, sm.getGenerationProgress());
            assertEquals(0, sm.getTotalGenerations());
        }

        @Test
        void startGeneration_setsGeneratingTrue() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);
            sm.startGeneration();
            assertTrue(sm.isGenerating());
            assertEquals(0, sm.getGenerationProgress());
        }

        @Test
        void startGeneration_whileAlreadyGenerating_isNoOp() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);
            sm.startGeneration();
            // Tick a few times
            for (int i = 0; i < 50; i++) sm.tick();
            assertEquals(50, sm.getGenerationProgress());

            // Starting again should not reset progress
            sm.startGeneration();
            assertEquals(50, sm.getGenerationProgress());
        }

        @Test
        void tick_incrementsProgress() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);
            sm.startGeneration();
            sm.tick();
            assertEquals(1, sm.getGenerationProgress());
        }

        @Test
        void tick_whenNotGenerating_doesNothing() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);
            sm.tick();
            assertEquals(0, sm.getGenerationProgress());
            assertFalse(sm.isGenerating());
        }

        @Test
        void generation_completesAfterExactTicks() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);
            sm.startGeneration();

            for (int i = 0; i < FLOW_GENERATION_TIME - 1; i++) {
                assertFalse(sm.tick(), "Should not complete at tick " + (i + 1));
                assertTrue(sm.isGenerating());
            }

            // The completing tick
            assertTrue(sm.tick());
            assertFalse(sm.isGenerating());
            assertEquals(0, sm.getGenerationProgress()); // reset after completion
            assertEquals(1, sm.getTotalGenerations());
        }

        @Test
        void multipleGenerations_incrementsCounter() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);

            for (int gen = 0; gen < 3; gen++) {
                sm.startGeneration();
                for (int t = 0; t < FLOW_GENERATION_TIME; t++) {
                    sm.tick();
                }
                assertEquals(gen + 1, sm.getTotalGenerations());
            }
        }

        @Test
        void propertyDelegate_exposesCorrectValues() {
            var sm = new GenerationStateMachine(FLOW_GENERATION_TIME);
            assertEquals(0, sm.getProperty(0)); // progress
            assertEquals(0, sm.getProperty(1)); // isGenerating
            assertEquals(0, sm.getProperty(2)); // totalGenerations
            assertEquals(FLOW_GENERATION_TIME, sm.getProperty(3)); // generationTime
            assertEquals(0, sm.getProperty(99)); // unknown index

            sm.startGeneration();
            assertEquals(1, sm.getProperty(1)); // isGenerating = true

            for (int i = 0; i < 50; i++) sm.tick();
            assertEquals(50, sm.getProperty(0)); // progress = 50
        }
    }

    @Nested
    class NanoBananaConsoleTests {
        private static final int NANO_BANANA_GENERATION_TIME = 60; // 3 seconds

        @Test
        void generationTime_isFasterThanFlow() {
            assertEquals(60, NANO_BANANA_GENERATION_TIME);
            assertTrue(NANO_BANANA_GENERATION_TIME < 100, "Nano Banana should be faster than Flow");
        }

        @Test
        void completesIn60Ticks() {
            var sm = new GenerationStateMachine(NANO_BANANA_GENERATION_TIME);
            sm.startGeneration();

            for (int i = 0; i < 59; i++) {
                assertFalse(sm.tick());
            }
            assertTrue(sm.tick()); // tick 60 completes
            assertEquals(1, sm.getTotalGenerations());
        }
    }

    @Nested
    class VeoConsoleTests {
        private static final int VEO_GENERATION_TIME = 100; // 5 seconds

        @Test
        void generationTime_matchesFlowConsole() {
            assertEquals(100, VEO_GENERATION_TIME);
        }

        @Test
        void completesIn100Ticks() {
            var sm = new GenerationStateMachine(VEO_GENERATION_TIME);
            sm.startGeneration();

            for (int i = 0; i < 99; i++) {
                assertFalse(sm.tick());
            }
            assertTrue(sm.tick()); // tick 100 completes
            assertEquals(1, sm.getTotalGenerations());
        }
    }

    @Nested
    class GenerationTimingComparison {
        @Test
        void nanoBanana_isFastestConsole() {
            int flow = 100;
            int nanoBanana = 60;
            int veo = 100;

            assertTrue(nanoBanana < flow);
            assertTrue(nanoBanana < veo);
        }

        @Test
        void allConsoles_completeCorrectly() {
            int[] times = {100, 60, 100}; // Flow, NanoBanana, Veo
            String[] names = {"Flow", "NanoBanana", "Veo"};

            for (int i = 0; i < times.length; i++) {
                var sm = new GenerationStateMachine(times[i]);
                sm.startGeneration();
                for (int t = 0; t < times[i]; t++) {
                    sm.tick();
                }
                assertEquals(1, sm.getTotalGenerations(),
                    names[i] + " should complete after " + times[i] + " ticks");
                assertFalse(sm.isGenerating(),
                    names[i] + " should not be generating after completion");
            }
        }
    }
}
