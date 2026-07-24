package com.labscraft.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentConfigTest {

    @Test
    void defaultsAreSane() {
        AgentConfig config = AgentConfig.defaults();
        assertTrue(config.enabled());
        assertEquals("http://localhost:3001", config.serverUrl());
        assertEquals(20, config.pollIntervalTicks());
        assertEquals(2000, config.requestTimeoutMs());
        assertFalse(config.debugLogging());
    }

    @Test
    void missingFileYieldsDefaultsAndWritesTemplate(@TempDir Path dir) {
        Path file = dir.resolve("labscraft-agent.json");
        AgentConfig config = AgentConfig.load(file);
        assertEquals(AgentConfig.defaults(), config);
        assertTrue(Files.isRegularFile(file), "a default config file should be written for discoverability");
        // And the written template must load back to the same defaults.
        assertEquals(AgentConfig.defaults(), AgentConfig.load(file));
    }

    @Test
    void malformedFileYieldsDefaults(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("labscraft-agent.json");
        Files.writeString(file, "{ this is not json ]");
        assertEquals(AgentConfig.defaults(), AgentConfig.load(file));
    }

    @Test
    void fileValuesOverrideDefaults(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("labscraft-agent.json");
        Files.writeString(file, """
                {"enabled": false, "server_url": "http://example.com:9999/",
                 "poll_interval_ticks": 40, "request_timeout_ms": 500,
                 "health_probe_interval_seconds": 60, "debug_logging": true}
                """);
        AgentConfig config = AgentConfig.load(file);
        assertFalse(config.enabled());
        assertEquals("http://example.com:9999", config.serverUrl(), "trailing slash is trimmed");
        assertEquals(40, config.pollIntervalTicks());
        assertEquals(500, config.requestTimeoutMs());
        assertEquals(60, config.healthProbeIntervalSeconds());
        assertTrue(config.debugLogging());
    }

    @Test
    void absurdNumbersAreClamped(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("labscraft-agent.json");
        Files.writeString(file, "{\"poll_interval_ticks\": 0, \"request_timeout_ms\": 999999}");
        AgentConfig config = AgentConfig.load(file);
        assertEquals(5, config.pollIntervalTicks());
        assertEquals(30_000, config.requestTimeoutMs());
    }

    @Test
    void partialFileKeepsDefaultsForMissingKeys(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("labscraft-agent.json");
        Files.writeString(file, "{\"debug_logging\": true}");
        AgentConfig config = AgentConfig.load(file);
        assertTrue(config.debugLogging());
        assertEquals(AgentConfig.defaults().serverUrl(), config.serverUrl());
        assertEquals(AgentConfig.defaults().pollIntervalTicks(), config.pollIntervalTicks());
    }
}
