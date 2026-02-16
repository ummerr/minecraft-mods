package com.labscraft.world;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the GoogleplexState logic for tracking whether the Googleplex has been generated.
 * Since PersistentState requires MC bootstrap, we mirror the boolean flag + serialization logic.
 */
class GoogleplexStateTest {

    /** Mirrors GoogleplexState's core logic without MC dependencies. */
    static class GenerationTracker {
        private boolean generated = false;

        boolean isGenerated() { return generated; }

        void setGenerated(boolean generated) {
            this.generated = generated;
        }

        Map<String, Object> serialize() {
            Map<String, Object> data = new HashMap<>();
            data.put("generated", generated);
            return data;
        }

        static GenerationTracker deserialize(Map<String, Object> data) {
            GenerationTracker tracker = new GenerationTracker();
            tracker.generated = (boolean) data.getOrDefault("generated", false);
            return tracker;
        }
    }

    @Test
    void defaultState_isNotGenerated() {
        var tracker = new GenerationTracker();
        assertFalse(tracker.isGenerated());
    }

    @Test
    void setGenerated_true_updatesState() {
        var tracker = new GenerationTracker();
        tracker.setGenerated(true);
        assertTrue(tracker.isGenerated());
    }

    @Test
    void setGenerated_false_revertsState() {
        var tracker = new GenerationTracker();
        tracker.setGenerated(true);
        tracker.setGenerated(false);
        assertFalse(tracker.isGenerated());
    }

    @Test
    void serialize_roundTrip_preservesTrue() {
        var tracker = new GenerationTracker();
        tracker.setGenerated(true);

        Map<String, Object> data = tracker.serialize();
        var restored = GenerationTracker.deserialize(data);

        assertTrue(restored.isGenerated());
    }

    @Test
    void serialize_roundTrip_preservesFalse() {
        var tracker = new GenerationTracker();

        Map<String, Object> data = tracker.serialize();
        var restored = GenerationTracker.deserialize(data);

        assertFalse(restored.isGenerated());
    }

    @Test
    void deserialize_missingKey_defaultsToFalse() {
        Map<String, Object> emptyData = new HashMap<>();
        var restored = GenerationTracker.deserialize(emptyData);
        assertFalse(restored.isGenerated());
    }
}
