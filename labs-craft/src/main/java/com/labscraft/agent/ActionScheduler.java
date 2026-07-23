package com.labscraft.agent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Schedules validated actions against ONE monotonic server tick counter —
 * the fix for v1 defect #3, where {@code scheduleActions()} took a
 * caller-supplied {@code baseTick} while {@code tick()} incremented an
 * unrelated private counter, so {@code delay_ticks} was never honored
 * reliably.
 *
 * <p>Here the scheduler keeps NO clock of its own. Both {@link #schedule} and
 * {@link #drainDue} take the current tick from the caller, and the bridge
 * feeds both from the same {@code END_SERVER_TICK}-incremented counter. An
 * action with {@code delay_ticks = d} scheduled at tick {@code t} becomes due
 * at exactly {@code t + d}.</p>
 *
 * <p>Pure Java, single-threaded by contract (server thread only).</p>
 */
public final class ActionScheduler {

    /** A validated action waiting for its due tick. */
    public record Scheduled(UUID playerUuid, AgentAction action, long dueTick, long seq) {
    }

    private final PriorityQueue<Scheduled> queue = new PriorityQueue<>(
            Comparator.comparingLong(Scheduled::dueTick).thenComparingLong(Scheduled::seq));
    private long seqCounter;

    /** Queues {@code actions} relative to {@code nowTick} (the single clock). */
    public void schedule(UUID playerUuid, List<AgentAction> actions, long nowTick) {
        for (AgentAction action : actions) {
            queue.add(new Scheduled(playerUuid, action, nowTick + Math.max(0, action.delayTicks()), seqCounter++));
        }
    }

    /**
     * Removes and returns every action due at or before {@code nowTick}, in
     * (dueTick, insertion) order.
     */
    public List<Scheduled> drainDue(long nowTick) {
        List<Scheduled> due = new ArrayList<>();
        while (!queue.isEmpty() && queue.peek().dueTick() <= nowTick) {
            due.add(queue.poll());
        }
        return due;
    }

    public int pending() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }
}
