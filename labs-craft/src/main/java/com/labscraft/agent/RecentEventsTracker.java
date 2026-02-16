package com.labscraft.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class RecentEventsTracker {

    private final List<JsonObject> events = new ArrayList<>();
    private final long createdAt = System.currentTimeMillis();

    public synchronized void addChatMessage(String from, String text) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "chat_message");
        event.addProperty("from", from);
        event.addProperty("text", text);
        event.addProperty("ago_seconds", elapsedSeconds());
        events.add(event);
    }

    public synchronized void addInteraction(String playerName) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "interaction");
        event.addProperty("from", playerName);
        event.addProperty("ago_seconds", elapsedSeconds());
        events.add(event);
    }

    public synchronized void addBlockBroken(String blockName) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "block_broken");
        event.addProperty("block", blockName);
        event.addProperty("ago_seconds", elapsedSeconds());
        events.add(event);
    }

    public synchronized JsonArray drain() {
        JsonArray array = new JsonArray();
        long now = System.currentTimeMillis();
        for (JsonObject event : events) {
            // Recalculate ago_seconds at drain time
            event.addProperty("ago_seconds", (now - createdAt) / 1000);
            array.add(event);
        }
        events.clear();
        return array;
    }

    public synchronized boolean hasEvents() {
        return !events.isEmpty();
    }

    private long elapsedSeconds() {
        return (System.currentTimeMillis() - createdAt) / 1000;
    }
}
