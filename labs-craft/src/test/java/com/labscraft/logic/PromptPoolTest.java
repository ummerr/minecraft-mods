package com.labscraft.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PromptPoolTest {

    @Test
    void rejectsEmptyPools() {
        assertThrows(IllegalArgumentException.class, () -> new PromptPool(null));
        assertThrows(IllegalArgumentException.class, () -> new PromptPool(new String[0]));
    }

    @Test
    void builtinPoolsAreNonEmptyAndDistinct() {
        for (PromptPool pool : new PromptPool[] {
                PromptPool.FLOW_SKETCH, PromptPool.GENERATED_IMAGE, PromptPool.GENERATED_VIDEO }) {
            assertTrue(pool.size() >= 8, "pool should have a decent variety");
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < pool.size(); i++) {
                String prompt = pool.pick(i);
                assertNotNull(prompt);
                assertTrue(prompt.length() > 0);
                assertTrue(seen.add(prompt), "duplicate prompt: " + prompt);
            }
        }
    }

    @Test
    void pickIsDeterministic() {
        assertEquals(PromptPool.FLOW_SKETCH.pick(123456789L), PromptPool.FLOW_SKETCH.pick(123456789L));
    }

    @Test
    void pickHandlesNegativeAndExtremeSeeds() {
        PromptPool pool = PromptPool.GENERATED_VIDEO;
        assertNotNull(pool.pick(-1L));
        assertNotNull(pool.pick(Long.MIN_VALUE));
        assertNotNull(pool.pick(Long.MAX_VALUE));
        assertNotNull(pool.pick(0L));
    }

    @Test
    void pickWrapsAroundPoolSize() {
        PromptPool pool = new PromptPool(new String[] { "a", "b", "c" });
        assertEquals("a", pool.pick(0));
        assertEquals("b", pool.pick(1));
        assertEquals("c", pool.pick(2));
        assertEquals("a", pool.pick(3));
        assertEquals("c", pool.pick(-1)); // floorMod, never negative index
    }

    @Test
    void formatSeedIsEightHexDigits() {
        assertEquals("00000000", PromptPool.formatSeed(0));
        assertEquals("ffffffff", PromptPool.formatSeed(-1));
        assertEquals("0000002a", PromptPool.formatSeed(42));
        assertTrue(PromptPool.formatSeed(Long.MAX_VALUE).matches("[0-9a-f]{8}"));
    }
}
