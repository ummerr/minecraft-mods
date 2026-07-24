package com.labscraft.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentEventsTrackerTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void recordsAndDrainsPerPlayer() {
        RecentEventsTracker tracker = new RecentEventsTracker();
        tracker.record(ALICE, "chat_message", Map.of("text", "hey josh"), 1_000);
        tracker.record(BOB, "interaction", Map.of(), 2_000);

        List<RecentEventsTracker.Event> alice = tracker.drain(ALICE);
        assertEquals(1, alice.size());
        assertEquals("chat_message", alice.get(0).type());
        assertEquals("hey josh", alice.get(0).fields().get("text"));

        // Alice's drain must not touch Bob's buffer.
        assertEquals(1, tracker.bufferedCount(BOB));
    }

    @Test
    void drainClearsTheBuffer() {
        RecentEventsTracker tracker = new RecentEventsTracker();
        tracker.record(ALICE, "block_mined", Map.of("block", "labscraft:tpu_ore"), 0);
        assertEquals(1, tracker.drain(ALICE).size());
        assertTrue(tracker.drain(ALICE).isEmpty());
        assertEquals(0, tracker.bufferedCount(ALICE));
    }

    @Test
    void bufferIsBoundedDroppingOldestFirst() {
        RecentEventsTracker tracker = new RecentEventsTracker();
        for (int i = 0; i < RecentEventsTracker.MAX_EVENTS_PER_PLAYER + 10; i++) {
            tracker.record(ALICE, "chat_message", Map.of("text", "msg" + i), i);
        }
        List<RecentEventsTracker.Event> events = tracker.drain(ALICE);
        assertEquals(RecentEventsTracker.MAX_EVENTS_PER_PLAYER, events.size());
        assertEquals("msg10", events.get(0).fields().get("text"));
    }

    @Test
    void secondsAgoIsComputedFromRecordTime() {
        RecentEventsTracker.Event event = new RecentEventsTracker.Event("chat_message", 10_000, Map.of());
        assertEquals(5, event.secondsAgo(15_500));
        assertEquals(0, event.secondsAgo(9_000)); // clock skew never goes negative
    }

    @Test
    void eventsAreDrainedOldestFirst() {
        RecentEventsTracker tracker = new RecentEventsTracker();
        tracker.record(ALICE, "chat_message", Map.of("text", "first"), 100);
        tracker.record(ALICE, "interaction", Map.of(), 200);
        List<RecentEventsTracker.Event> events = tracker.drain(ALICE);
        assertEquals("chat_message", events.get(0).type());
        assertEquals("interaction", events.get(1).type());
    }

    @Test
    void nullsAreIgnored() {
        RecentEventsTracker tracker = new RecentEventsTracker();
        tracker.record(null, "chat_message", Map.of(), 0);
        tracker.record(ALICE, null, Map.of(), 0);
        tracker.record(ALICE, "chat_message", null, 0);
        assertEquals(1, tracker.bufferedCount(ALICE));
        assertTrue(tracker.drain(ALICE).get(0).fields().isEmpty());
    }

    @Test
    void clearAllEmptiesEverything() {
        RecentEventsTracker tracker = new RecentEventsTracker();
        tracker.record(ALICE, "chat_message", Map.of(), 0);
        tracker.record(BOB, "chat_message", Map.of(), 0);
        tracker.clearAll();
        assertEquals(0, tracker.bufferedCount(ALICE));
        assertEquals(0, tracker.bufferedCount(BOB));
    }
}
