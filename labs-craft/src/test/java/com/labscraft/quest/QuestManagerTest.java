package com.labscraft.quest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the quest tracking logic from QuestManager.
 * Since QuestManager extends PersistentState (MC class), we can't instantiate it directly.
 * Instead, we mirror its core logic — a UUID-to-QuestStage map with default NOT_STARTED —
 * and test the behavior contract.
 */
class QuestManagerTest {

    /**
     * Mirrors QuestManager's core logic without MC dependencies.
     */
    static class QuestTracker {
        private final Map<UUID, QuestStage> playerStages = new HashMap<>();

        QuestStage getStage(UUID playerId) {
            return playerStages.getOrDefault(playerId, QuestStage.NOT_STARTED);
        }

        /** Returns true if the stage actually changed. */
        boolean setStage(UUID playerId, QuestStage stage) {
            QuestStage current = getStage(playerId);
            if (stage != current) {
                playerStages.put(playerId, stage);
                return true;
            }
            return false;
        }

        boolean advanceStage(UUID playerId) {
            QuestStage current = getStage(playerId);
            QuestStage next = current.next();
            if (next != current) {
                playerStages.put(playerId, next);
                return true;
            }
            return false;
        }

        boolean isAtStage(UUID playerId, QuestStage stage) {
            return getStage(playerId) == stage;
        }

        boolean hasReachedStage(UUID playerId, QuestStage stage) {
            return getStage(playerId).isAtLeast(stage);
        }

        Map<UUID, QuestStage> getAllStages() {
            return new HashMap<>(playerStages);
        }

        /** Simulates NBT round-trip: serialize to map of strings, deserialize back */
        Map<String, String> serialize() {
            Map<String, String> data = new HashMap<>();
            for (Map.Entry<UUID, QuestStage> entry : playerStages.entrySet()) {
                data.put(entry.getKey().toString(), entry.getValue().name());
            }
            return data;
        }

        static QuestTracker deserialize(Map<String, String> data) {
            QuestTracker tracker = new QuestTracker();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                try {
                    UUID playerId = UUID.fromString(entry.getKey());
                    QuestStage stage = QuestStage.valueOf(entry.getValue());
                    tracker.playerStages.put(playerId, stage);
                } catch (IllegalArgumentException e) {
                    // Skip invalid entries — matches QuestManager.createFromNbt behavior
                }
            }
            return tracker;
        }
    }

    private QuestTracker tracker;
    private UUID player1;
    private UUID player2;

    @BeforeEach
    void setUp() {
        tracker = new QuestTracker();
        player1 = UUID.randomUUID();
        player2 = UUID.randomUUID();
    }

    @Test
    void getStage_newPlayer_returnsNotStarted() {
        assertEquals(QuestStage.NOT_STARTED, tracker.getStage(player1));
    }

    @Test
    void getStage_unknownPlayer_returnsNotStarted() {
        assertEquals(QuestStage.NOT_STARTED, tracker.getStage(UUID.randomUUID()));
    }

    @Test
    void setStage_updatesPlayerStage() {
        tracker.setStage(player1, QuestStage.FLOW_INTRO);
        assertEquals(QuestStage.FLOW_INTRO, tracker.getStage(player1));
    }

    @Test
    void setStage_sameStage_returnsFalse() {
        tracker.setStage(player1, QuestStage.FLOW_INTRO);
        assertFalse(tracker.setStage(player1, QuestStage.FLOW_INTRO));
    }

    @Test
    void setStage_differentStage_returnsTrue() {
        assertTrue(tracker.setStage(player1, QuestStage.FLOW_INTRO));
    }

    @Test
    void advanceStage_progressesFromNotStarted() {
        tracker.advanceStage(player1);
        assertEquals(QuestStage.FLOW_INTRO, tracker.getStage(player1));
    }

    @Test
    void advanceStage_progressesThroughAllStages() {
        tracker.advanceStage(player1); // NOT_STARTED -> FLOW_INTRO
        assertEquals(QuestStage.FLOW_INTRO, tracker.getStage(player1));

        tracker.advanceStage(player1); // FLOW_INTRO -> LEARNING_PIPELINE
        assertEquals(QuestStage.LEARNING_PIPELINE, tracker.getStage(player1));

        tracker.advanceStage(player1); // LEARNING_PIPELINE -> FIRST_GENERATION
        assertEquals(QuestStage.FIRST_GENERATION, tracker.getStage(player1));

        tracker.advanceStage(player1); // FIRST_GENERATION -> COMPLETED
        assertEquals(QuestStage.COMPLETED, tracker.getStage(player1));
    }

    @Test
    void advanceStage_atCompleted_staysCompleted() {
        tracker.setStage(player1, QuestStage.COMPLETED);
        assertFalse(tracker.advanceStage(player1));
        assertEquals(QuestStage.COMPLETED, tracker.getStage(player1));
    }

    @Test
    void advanceStage_atCompleted_returnsFalse() {
        tracker.setStage(player1, QuestStage.COMPLETED);
        assertFalse(tracker.advanceStage(player1));
    }

    @Test
    void isAtStage_returnsTrue_whenAtStage() {
        tracker.setStage(player1, QuestStage.LEARNING_PIPELINE);
        assertTrue(tracker.isAtStage(player1, QuestStage.LEARNING_PIPELINE));
    }

    @Test
    void isAtStage_returnsFalse_whenNotAtStage() {
        tracker.setStage(player1, QuestStage.LEARNING_PIPELINE);
        assertFalse(tracker.isAtStage(player1, QuestStage.FLOW_INTRO));
        assertFalse(tracker.isAtStage(player1, QuestStage.COMPLETED));
    }

    @Test
    void isAtStage_newPlayer_isAtNotStarted() {
        assertTrue(tracker.isAtStage(player1, QuestStage.NOT_STARTED));
    }

    @Test
    void hasReachedStage_returnsTrue_whenAtOrPastStage() {
        tracker.setStage(player1, QuestStage.FIRST_GENERATION);

        assertTrue(tracker.hasReachedStage(player1, QuestStage.NOT_STARTED));
        assertTrue(tracker.hasReachedStage(player1, QuestStage.FLOW_INTRO));
        assertTrue(tracker.hasReachedStage(player1, QuestStage.LEARNING_PIPELINE));
        assertTrue(tracker.hasReachedStage(player1, QuestStage.FIRST_GENERATION));
    }

    @Test
    void hasReachedStage_returnsFalse_whenBeforeStage() {
        tracker.setStage(player1, QuestStage.FLOW_INTRO);

        assertFalse(tracker.hasReachedStage(player1, QuestStage.LEARNING_PIPELINE));
        assertFalse(tracker.hasReachedStage(player1, QuestStage.COMPLETED));
    }

    @Test
    void hasReachedStage_newPlayer_hasReachedNotStarted() {
        assertTrue(tracker.hasReachedStage(player1, QuestStage.NOT_STARTED));
    }

    @Test
    void multiplePlayers_independentProgress() {
        tracker.setStage(player1, QuestStage.COMPLETED);
        tracker.setStage(player2, QuestStage.FLOW_INTRO);

        assertEquals(QuestStage.COMPLETED, tracker.getStage(player1));
        assertEquals(QuestStage.FLOW_INTRO, tracker.getStage(player2));
    }

    @Test
    void multiplePlayers_advanceIndependently() {
        tracker.advanceStage(player1); // -> FLOW_INTRO
        tracker.advanceStage(player1); // -> LEARNING_PIPELINE
        tracker.advanceStage(player2); // -> FLOW_INTRO

        assertEquals(QuestStage.LEARNING_PIPELINE, tracker.getStage(player1));
        assertEquals(QuestStage.FLOW_INTRO, tracker.getStage(player2));
    }

    // === Serialization (NBT round-trip) tests ===

    @Test
    void serialize_and_deserialize_roundTrip() {
        tracker.setStage(player1, QuestStage.FIRST_GENERATION);
        tracker.setStage(player2, QuestStage.COMPLETED);

        Map<String, String> data = tracker.serialize();
        QuestTracker restored = QuestTracker.deserialize(data);

        assertEquals(QuestStage.FIRST_GENERATION, restored.getStage(player1));
        assertEquals(QuestStage.COMPLETED, restored.getStage(player2));
    }

    @Test
    void serialize_emptyTracker_roundTrips() {
        Map<String, String> data = tracker.serialize();
        QuestTracker restored = QuestTracker.deserialize(data);

        assertEquals(QuestStage.NOT_STARTED, restored.getStage(UUID.randomUUID()));
    }

    @Test
    void deserialize_withInvalidStageName_skipsEntry() {
        Map<String, String> data = new HashMap<>();
        data.put(UUID.randomUUID().toString(), "INVALID_STAGE");
        data.put(player1.toString(), "COMPLETED");

        QuestTracker restored = QuestTracker.deserialize(data);

        assertEquals(QuestStage.COMPLETED, restored.getStage(player1));
    }

    @Test
    void deserialize_withInvalidUuid_skipsEntry() {
        Map<String, String> data = new HashMap<>();
        data.put("not-a-uuid", "COMPLETED");

        QuestTracker restored = QuestTracker.deserialize(data);
        assertNotNull(restored);
        // The invalid entry should be skipped, no crash
    }

    @Test
    void serialize_preservesAllNonDefaultStages() {
        // NOT_STARTED is the default and won't be stored (matches QuestManager behavior)
        int stored = 0;
        for (QuestStage stage : QuestStage.values()) {
            UUID uuid = UUID.randomUUID();
            if (tracker.setStage(uuid, stage)) {
                stored++;
            }
        }

        Map<String, String> data = tracker.serialize();
        assertEquals(stored, data.size());

        QuestTracker restored = QuestTracker.deserialize(data);
        assertEquals(stored, restored.getAllStages().size());
    }

    @Test
    void serialize_stageNames_matchEnumNames() {
        tracker.setStage(player1, QuestStage.FIRST_GENERATION);
        Map<String, String> data = tracker.serialize();
        assertEquals("FIRST_GENERATION", data.get(player1.toString()));
    }

    @Test
    void fullQuestProgression_serializesCorrectly() {
        // Progress player through entire quest
        for (int i = 0; i < QuestStage.values().length - 1; i++) {
            tracker.advanceStage(player1);
        }
        assertEquals(QuestStage.COMPLETED, tracker.getStage(player1));

        // Round-trip
        Map<String, String> data = tracker.serialize();
        QuestTracker restored = QuestTracker.deserialize(data);
        assertEquals(QuestStage.COMPLETED, restored.getStage(player1));
    }
}
