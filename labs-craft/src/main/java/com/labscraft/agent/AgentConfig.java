package com.labscraft.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Agent-bridge configuration, loaded from {@code config/labscraft-agent.json}.
 * A missing or malformed file never breaks anything — defaults apply and (for
 * a missing file) a commented-by-example default file is written so users can
 * discover the knobs. Pure Java.
 */
public record AgentConfig(
        boolean enabled,
        String serverUrl,
        int pollIntervalTicks,
        int requestTimeoutMs,
        int healthProbeIntervalSeconds,
        boolean debugLogging) {

    public static final String FILE_NAME = "labscraft-agent.json";

    public static AgentConfig defaults() {
        return new AgentConfig(true, "http://localhost:3001", 20, 2000, 15, false);
    }

    /**
     * Loads config from {@code file}. Missing file → defaults (and a default
     * file is written, best-effort). Malformed file → defaults. Out-of-range
     * numbers are clamped to sane bounds.
     */
    public static AgentConfig load(Path file) {
        AgentConfig d = defaults();
        if (file == null) {
            return d;
        }
        if (!Files.isRegularFile(file)) {
            writeDefaultFile(file, d);
            return d;
        }
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, Object> root = JsonLite.asObject(JsonLite.parse(text));
            if (root == null) {
                return d;
            }
            return new AgentConfig(
                    JsonLite.bool(root, "enabled", d.enabled()),
                    normalizeUrl(JsonLite.str(root, "server_url", d.serverUrl())),
                    clamp((int) JsonLite.num(root, "poll_interval_ticks", d.pollIntervalTicks()), 5, 200),
                    clamp((int) JsonLite.num(root, "request_timeout_ms", d.requestTimeoutMs()), 250, 30_000),
                    clamp((int) JsonLite.num(root, "health_probe_interval_seconds",
                            d.healthProbeIntervalSeconds()), 5, 600),
                    JsonLite.bool(root, "debug_logging", d.debugLogging()));
        } catch (IOException | RuntimeException e) {
            return d;
        }
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return defaults().serverUrl();
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void writeDefaultFile(Path file, AgentConfig d) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            String json = "{\n"
                    + "  \"enabled\": " + d.enabled() + ",\n"
                    + "  \"server_url\": \"" + d.serverUrl() + "\",\n"
                    + "  \"poll_interval_ticks\": " + d.pollIntervalTicks() + ",\n"
                    + "  \"request_timeout_ms\": " + d.requestTimeoutMs() + ",\n"
                    + "  \"health_probe_interval_seconds\": " + d.healthProbeIntervalSeconds() + ",\n"
                    + "  \"debug_logging\": " + d.debugLogging() + "\n"
                    + "}\n";
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Best-effort only; defaults still apply.
        }
    }
}
