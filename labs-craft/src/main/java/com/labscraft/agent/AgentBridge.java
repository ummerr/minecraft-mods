package com.labscraft.agent;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.labscraft.LabsCraft;
import com.labscraft.entity.JoshWoodwardEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentBridge {

    private static AgentBridge instance;

    private final AgentConfig config;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private final AtomicBoolean serverAvailable = new AtomicBoolean(true);
    private long lastHealthCheck = 0;
    private static final long HEALTH_CHECK_INTERVAL_MS = 10_000; // Re-check every 10s after failure

    // Per-player action executors
    private final ConcurrentMap<String, ActionExecutor> executors = new ConcurrentHashMap<>();

    // Track in-flight requests to avoid stacking
    private final ConcurrentMap<String, Boolean> inFlight = new ConcurrentHashMap<>();

    private AgentBridge(AgentConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();
    }

    public static void init(AgentConfig config) {
        instance = new AgentBridge(config);
        LabsCraft.LOGGER.info("[AgentBridge] Initialized, server: {}", config.getServerUrl());
    }

    public static AgentBridge getInstance() {
        return instance;
    }

    public boolean isAvailable() {
        if (!serverAvailable.get()) {
            // Periodically retry
            if (System.currentTimeMillis() - lastHealthCheck > HEALTH_CHECK_INTERVAL_MS) {
                checkHealth();
            }
        }
        return serverAvailable.get();
    }

    public ActionExecutor getExecutor(ServerPlayerEntity player) {
        return executors.computeIfAbsent(
                player.getUuidAsString(),
                k -> new ActionExecutor()
        );
    }

    public void sendWorldState(ServerPlayerEntity player, JoshWoodwardEntity josh,
                               JsonObject worldState, RecentEventsTracker eventsTracker) {
        String playerKey = player.getUuidAsString();

        // Don't stack requests for the same player
        if (inFlight.getOrDefault(playerKey, false)) {
            return;
        }

        // Attach recent events
        if (eventsTracker != null) {
            worldState.add("recent_events", eventsTracker.drain());
        }

        String body = gson.toJson(worldState);
        String url = config.getServerUrl() + "/api/agent/tick";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();

        inFlight.put(playerKey, true);

        CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(
                request, HttpResponse.BodyHandlers.ofString()
        );

        future.thenAccept(response -> {
            inFlight.put(playerKey, false);

            if (response.statusCode() == 200) {
                serverAvailable.set(true);
                handleResponse(response.body(), player, josh);
            } else {
                LabsCraft.LOGGER.warn("[AgentBridge] Server returned {}: {}",
                        response.statusCode(), response.body());
            }
        }).exceptionally(e -> {
            inFlight.put(playerKey, false);
            serverAvailable.set(false);
            lastHealthCheck = System.currentTimeMillis();

            if (config.isDebugLogging()) {
                LabsCraft.LOGGER.warn("[AgentBridge] Request failed: {}", e.getMessage());
            }
            return null;
        });
    }

    private void handleResponse(String responseBody, ServerPlayerEntity player,
                                JoshWoodwardEntity josh) {
        try {
            JsonObject response = gson.fromJson(responseBody, JsonObject.class);
            JsonArray actions = response.getAsJsonArray("actions");

            if (actions != null && !actions.isEmpty()) {
                ActionExecutor executor = getExecutor(player);
                // Schedule actions on the main server thread
                ServerWorld world = (ServerWorld) player.getWorld();
                world.getServer().execute(() -> {
                    executor.scheduleActions(actions, 0);
                });

                if (config.isDebugLogging()) {
                    JsonObject debug = response.getAsJsonObject("debug");
                    String trigger = debug != null && debug.has("trigger")
                            ? debug.get("trigger").getAsString() : "unknown";
                    LabsCraft.LOGGER.info("[AgentBridge] {} actions from trigger={}",
                            actions.size(), trigger);
                }
            }
        } catch (Exception e) {
            LabsCraft.LOGGER.warn("[AgentBridge] Failed to parse response: {}", e.getMessage());
        }
    }

    private void checkHealth() {
        lastHealthCheck = System.currentTimeMillis();
        String url = config.getServerUrl() + "/health";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofMillis(config.getTimeoutMs()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        serverAvailable.set(true);
                        LabsCraft.LOGGER.info("[AgentBridge] Server reconnected");
                    }
                })
                .exceptionally(e -> null);
    }
}
