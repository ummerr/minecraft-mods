package com.labscraft.agent;

import com.labscraft.LabsCraft;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.quest.Objective;
import com.labscraft.quest.PlayerQuestState;
import com.labscraft.quest.QuestManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The mod ↔ agent-server bridge (G5). Polls {@code POST /tick} at most once
 * per {@link AgentConfig#pollIntervalTicks()} per player — never every game
 * tick, never blocking the game thread (all HTTP is
 * {@link HttpClient#sendAsync}; results hop back via {@code server.execute}).
 *
 * <h2>Graceful degradation (PROTOCOL-V2 hard rule 1)</h2>
 * A {@code GET /health} probe on server start decides whether the agent path
 * is live; while offline it re-probes every
 * {@link AgentConfig#healthProbeIntervalSeconds()} so starting the Node server
 * mid-session works. Connection refused, timeout, malformed JSON, non-200, or
 * {@code protocol_version != 2} flip the bridge offline and Josh falls back to
 * G3's static dialogue. State transitions are logged exactly once — repeat
 * failures log at debug only.
 *
 * <h2>Single clock (v1 defect #3 fix)</h2>
 * One monotonic counter, incremented only in {@code END_SERVER_TICK}, is the
 * sole time base for both scheduling and draining delayed actions.
 */
public final class AgentBridge {

    private static volatile AgentBridge instance;

    private final AgentConfig config;
    private final HttpClient http;
    private final RecentEventsTracker tracker = new RecentEventsTracker();
    private final ActionExecutor executor = new ActionExecutor();

    /** THE single monotonic tick counter. Server thread only. */
    private long tickCounter;

    private volatile boolean live;
    private volatile boolean offlineAnnounced;
    private volatile String sessionId = UUID.randomUUID().toString();

    // Server-thread state.
    private final Map<UUID, Long> lastPollTick = new HashMap<>();
    private final Map<UUID, QuestSnapshot> questSnapshots = new HashMap<>();
    private long lastProbeTick = Long.MIN_VALUE;

    // Cross-thread state.
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();
    private volatile boolean probeInFlight;

    // Diagnostics (read by the agent smoke test).
    private final AtomicLong ticksSent = new AtomicLong();
    private final AtomicLong responsesOk = new AtomicLong();
    private volatile String lastTrigger = "";

    private record QuestSnapshot(String stage, Set<String> doneObjectives) {
    }

    private AgentBridge(AgentConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(config.requestTimeoutMs(), 2000)))
                .build();
    }

    /** Call once from {@code LabsCraft.onInitialize()}. */
    public static void register() {
        AgentConfig config = AgentConfig.load(
                FabricLoader.getInstance().getConfigDir().resolve(AgentConfig.FILE_NAME));
        if (!config.enabled()) {
            LabsCraft.LOGGER.info("[agent] Agent bridge disabled by config — Josh uses static dialogue only");
            return;
        }
        AgentBridge bridge = new AgentBridge(config);
        instance = bridge;
        ChatListener.register(bridge);
        JoshWoodwardEntity.setAgentDrivenCheck(AgentBridge::isAgentLive);

        ServerLifecycleEvents.SERVER_STARTED.register(bridge::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> bridge.onServerStopping());
        ServerTickEvents.END_SERVER_TICK.register(bridge::onEndServerTick);

        LabsCraft.LOGGER.info("[agent] Agent bridge registered (server: {}, poll every {} ticks)",
                config.serverUrl(), config.pollIntervalTicks());
    }

    // ------------------------------------------------------------------
    // Static accessors
    // ------------------------------------------------------------------

    /** True when the agent server is reachable and speaking protocol v2. */
    public static boolean isAgentLive() {
        AgentBridge bridge = instance;
        return bridge != null && bridge.live;
    }

    /** Entry point for {@code QuestIntegration}'s crafting mixin path. */
    public static void notifyItemCrafted(ServerPlayerEntity player, String itemId) {
        AgentBridge bridge = instance;
        if (bridge != null) {
            bridge.recordEvent(player.getUuid(), "item_crafted", Map.of("item", itemId));
        }
    }

    // Smoke-test diagnostics.
    public static long ticksSentCount() {
        AgentBridge bridge = instance;
        return bridge == null ? 0 : bridge.ticksSent.get();
    }

    public static long responsesOkCount() {
        AgentBridge bridge = instance;
        return bridge == null ? 0 : bridge.responsesOk.get();
    }

    public static long actionsExecutedCount() {
        AgentBridge bridge = instance;
        return bridge == null ? 0 : bridge.executor.executedCount();
    }

    public static String lastTriggerSeen() {
        AgentBridge bridge = instance;
        return bridge == null ? "" : bridge.lastTrigger;
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    private void onServerStarted(MinecraftServer server) {
        sessionId = UUID.randomUUID().toString();
        live = false;
        offlineAnnounced = false;
        lastProbeTick = Long.MIN_VALUE;
        lastPollTick.clear();
        questSnapshots.clear();
        tracker.clearAll();
        executor.clear();
        inFlight.clear();
        probeHealth();
    }

    private void onServerStopping() {
        live = false;
        tracker.clearAll();
        executor.clear();
        inFlight.clear();
    }

    private void onEndServerTick(MinecraftServer server) {
        tickCounter++;
        executor.onServerTick(server, tickCounter);

        if (!live) {
            long probeInterval = config.healthProbeIntervalSeconds() * 20L;
            if (!probeInFlight
                    && (lastProbeTick == Long.MIN_VALUE || tickCounter - lastProbeTick >= probeInterval)) {
                lastProbeTick = tickCounter;
                probeHealth();
            }
            return;
        }
        pollPlayers(server);
    }

    // ------------------------------------------------------------------
    // Event intake (from ChatListener / QuestIntegration)
    // ------------------------------------------------------------------

    void recordEvent(UUID playerUuid, String type, Map<String, String> fields) {
        tracker.record(playerUuid, type, fields, System.currentTimeMillis());
    }

    // ------------------------------------------------------------------
    // Health probe
    // ------------------------------------------------------------------

    private void probeHealth() {
        probeInFlight = true;
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(config.serverUrl() + "/health"))
                    .timeout(Duration.ofMillis(config.requestTimeoutMs()))
                    .GET()
                    .build();
        } catch (RuntimeException e) {
            probeInFlight = false;
            goOffline("bad server_url: " + e.getMessage());
            return;
        }
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    probeInFlight = false;
                    if (error != null || response.statusCode() != 200) {
                        goOffline(error != null ? rootMessage(error) : "health HTTP " + response.statusCode());
                        return;
                    }
                    try {
                        Map<String, Object> body = JsonLite.asObject(JsonLite.parse(response.body()));
                        if (body != null
                                && "ok".equals(JsonLite.str(body, "status", ""))
                                && (int) JsonLite.num(body, "protocol_version", -1) == ActionParser.PROTOCOL_VERSION) {
                            goOnline(JsonLite.str(body, "provider", "?"));
                        } else {
                            goOffline("health response not protocol v2");
                        }
                    } catch (RuntimeException e) {
                        goOffline("malformed health response");
                    }
                });
    }

    // ------------------------------------------------------------------
    // Polling
    // ------------------------------------------------------------------

    private void pollPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            Long last = lastPollTick.get(playerId);
            if (last != null && tickCounter - last < config.pollIntervalTicks()) {
                continue;
            }
            if (inFlight.contains(playerId)) {
                continue;
            }
            JoshWoodwardEntity josh = ActionExecutor.findNearestJosh(player, ActionExecutor.JOSH_SEARCH_RANGE);
            if (josh == null) {
                continue; // no Josh around — nothing to drive
            }
            lastPollTick.put(playerId, tickCounter);
            recordQuestDiff(player);
            String payload = WorldStateCollector.buildPayload(
                    player, josh, sessionId, tracker.drain(playerId), System.currentTimeMillis());
            sendTick(server, playerId, payload);
        }
    }

    /**
     * Synthesizes {@code stage_changed} / {@code objective_completed} events by
     * diffing the quest state against the last poll's snapshot.
     */
    private void recordQuestDiff(ServerPlayerEntity player) {
        PlayerQuestState state = QuestManager.getState(player);
        String stage = state.stage().name();
        Set<String> done = new HashSet<>();
        for (Objective objective : state.objectives()) {
            if (objective.isDone()) {
                done.add(objective.id());
            }
        }
        QuestSnapshot previous = questSnapshots.get(player.getUuid());
        if (previous != null) {
            if (!previous.stage().equals(stage)) {
                recordEvent(player.getUuid(), "stage_changed",
                        Map.of("from", previous.stage(), "to", stage));
            }
            for (String id : done) {
                if (!previous.doneObjectives().contains(id)) {
                    recordEvent(player.getUuid(), "objective_completed", Map.of("text", id));
                }
            }
        }
        questSnapshots.put(player.getUuid(), new QuestSnapshot(stage, done));
    }

    private void sendTick(MinecraftServer server, UUID playerId, String payload) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.serverUrl() + "/tick"))
                .timeout(Duration.ofMillis(config.requestTimeoutMs()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        inFlight.add(playerId);
        ticksSent.incrementAndGet();
        if (config.debugLogging()) {
            LabsCraft.LOGGER.info("[agent] POST /tick for {} ({} bytes)", playerId, payload.length());
        }
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> {
                    inFlight.remove(playerId);
                    if (error != null) {
                        goOffline(rootMessage(error));
                        return;
                    }
                    if (response.statusCode() != 200) {
                        goOffline("tick HTTP " + response.statusCode());
                        return;
                    }
                    ActionParser.Result result = ActionParser.parse(response.body());
                    // Hop back to the game thread before touching world state.
                    server.execute(() -> handleTickResponse(server, playerId, result));
                });
    }

    private void handleTickResponse(MinecraftServer server, UUID playerId, ActionParser.Result result) {
        if (!result.protocolOk()) {
            goOffline("malformed tick response or protocol_version != 2");
            return;
        }
        responsesOk.incrementAndGet();
        if (!result.trigger().isEmpty() && !"none".equals(result.trigger())) {
            lastTrigger = result.trigger();
        }
        goOnline(result.provider());
        if (result.actions().isEmpty()) {
            return; // the normal, common case
        }
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
        if (player == null) {
            return; // logged off mid-flight
        }
        JoshWoodwardEntity josh = ActionExecutor.findNearestJosh(player, ActionExecutor.JOSH_SEARCH_RANGE);
        if (josh == null) {
            return;
        }
        int accepted = executor.submit(player, josh, result.actions(), tickCounter);
        if (config.debugLogging()) {
            LabsCraft.LOGGER.info("[agent] response trigger={} provider={} actions={} accepted={}",
                    result.trigger(), result.provider(), result.actions().size(), accepted);
        }
    }

    // ------------------------------------------------------------------
    // Online/offline transitions (log once, never spam)
    // ------------------------------------------------------------------

    private void goOnline(String provider) {
        if (!live) {
            live = true;
            offlineAnnounced = false;
            LabsCraft.LOGGER.info("[agent] Agent server online at {} (provider: {}) — Josh is now agent-driven",
                    config.serverUrl(), provider == null || provider.isEmpty() ? "?" : provider);
        }
    }

    private void goOffline(String reason) {
        if (live) {
            live = false;
            LabsCraft.LOGGER.info("[agent] Agent server lost ({}) — Josh falls back to static dialogue",
                    reason);
        } else if (!offlineAnnounced) {
            offlineAnnounced = true;
            LabsCraft.LOGGER.info(
                    "[agent] Agent server not reachable at {} ({}) — Josh uses static dialogue; probing every {}s",
                    config.serverUrl(), reason, config.healthProbeIntervalSeconds());
        } else {
            LabsCraft.LOGGER.debug("[agent] agent server still unreachable: {}", reason);
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
