package com.labscraft.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the v1 defect #3 fix: ONE monotonic tick counter is the single
 * source of truth. The scheduler holds no clock of its own — an action with
 * delay d scheduled at tick t is due at exactly t + d on the same clock.
 */
class ActionSchedulerTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void delayIsHonoredExactlyAgainstTheSchedulingTick() {
        ActionScheduler scheduler = new ActionScheduler();
        scheduler.schedule(PLAYER, List.of(AgentAction.say("hi", 5)), 100);

        assertTrue(scheduler.drainDue(104).isEmpty(), "must not fire before tick 105");
        List<ActionScheduler.Scheduled> due = scheduler.drainDue(105);
        assertEquals(1, due.size());
        assertEquals(105, due.get(0).dueTick());
        assertTrue(scheduler.drainDue(1000).isEmpty(), "drained actions must not fire twice");
    }

    @Test
    void zeroDelayFiresOnTheSameTick() {
        ActionScheduler scheduler = new ActionScheduler();
        scheduler.schedule(PLAYER, List.of(AgentAction.say("now", 0)), 42);
        assertEquals(1, scheduler.drainDue(42).size());
    }

    @Test
    void regressionDualClockBug() {
        // v1: scheduleActions() took a caller baseTick while tick() incremented
        // a private counter starting at 0 — so a delay of 5 scheduled at
        // "baseTick 1000" fired ~1000 ticks late (or never drained relative to
        // the right base). Here both operations share the caller's clock, so
        // scheduling at a large tick value works with no internal counter to
        // drift against.
        ActionScheduler scheduler = new ActionScheduler();
        scheduler.schedule(PLAYER, List.of(AgentAction.say("a", 3)), 1_000_000);
        assertTrue(scheduler.drainDue(1_000_002).isEmpty());
        assertEquals(1, scheduler.drainDue(1_000_003).size());
    }

    @Test
    void multipleActionsDrainInDueOrderThenInsertionOrder() {
        ActionScheduler scheduler = new ActionScheduler();
        scheduler.schedule(PLAYER, List.of(
                AgentAction.say("second", 10),
                AgentAction.say("first", 0),
                AgentAction.say("also-first", 0)), 50);

        List<ActionScheduler.Scheduled> immediate = scheduler.drainDue(50);
        assertEquals(2, immediate.size());
        assertEquals("first", immediate.get(0).action().text());
        assertEquals("also-first", immediate.get(1).action().text());
        assertEquals(1, scheduler.pending());
        assertEquals("second", scheduler.drainDue(60).get(0).action().text());
    }

    @Test
    void negativeDelayIsClampedToNow() {
        // The validator rejects negative delays; the scheduler still clamps
        // defensively so a bug upstream cannot schedule into the past.
        ActionScheduler scheduler = new ActionScheduler();
        scheduler.schedule(PLAYER, List.of(AgentAction.say("x", -7)), 100);
        assertEquals(1, scheduler.drainDue(100).size());
    }

    @Test
    void clearDropsEverything() {
        ActionScheduler scheduler = new ActionScheduler();
        scheduler.schedule(PLAYER, List.of(AgentAction.say("x", 5)), 1);
        scheduler.clear();
        assertEquals(0, scheduler.pending());
        assertTrue(scheduler.drainDue(100).isEmpty());
    }
}
