package com.labscraft.agent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player buffer of PROTOCOL-V2 {@code recent_events} accumulated between
 * polls. Bounded (oldest dropped first), keyed by player UUID (never name),
 * cleared per send via {@link #drain}. Pure Java; thread-safe because chat
 * events and the poll loop may race in edge cases.
 */
public final class RecentEventsTracker {

    /** Max buffered events per player between polls. */
    public static final int MAX_EVENTS_PER_PLAYER = 32;

    /** Max tracked players (safety valve; server player counts are far lower). */
    public static final int MAX_PLAYERS = 128;

    /**
     * One buffered event. {@code fields} holds the type-specific extras
     * ({@code text}, {@code block}, {@code item}, {@code from}, {@code to}).
     */
    public record Event(String type, long atMs, Map<String, String> fields) {

        public long secondsAgo(long nowMs) {
            return Math.max(0, (nowMs - atMs) / 1000);
        }
    }

    private final Map<UUID, ArrayDeque<Event>> byPlayer = new HashMap<>();

    public synchronized void record(UUID playerUuid, String type, Map<String, String> fields, long nowMs) {
        if (playerUuid == null || type == null) {
            return;
        }
        if (!byPlayer.containsKey(playerUuid) && byPlayer.size() >= MAX_PLAYERS) {
            return;
        }
        ArrayDeque<Event> buffer = byPlayer.computeIfAbsent(playerUuid, k -> new ArrayDeque<>());
        while (buffer.size() >= MAX_EVENTS_PER_PLAYER) {
            buffer.pollFirst();
        }
        buffer.addLast(new Event(type, nowMs, fields == null ? Map.of() : Map.copyOf(fields)));
    }

    /** Returns and clears the player's buffered events (oldest first). */
    public synchronized List<Event> drain(UUID playerUuid) {
        ArrayDeque<Event> buffer = byPlayer.remove(playerUuid);
        return buffer == null ? List.of() : new ArrayList<>(buffer);
    }

    public synchronized int bufferedCount(UUID playerUuid) {
        ArrayDeque<Event> buffer = byPlayer.get(playerUuid);
        return buffer == null ? 0 : buffer.size();
    }

    public synchronized void clear(UUID playerUuid) {
        byPlayer.remove(playerUuid);
    }

    public synchronized void clearAll() {
        byPlayer.clear();
    }
}
