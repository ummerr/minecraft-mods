package com.labscraft.agent;

import com.google.gson.Gson;
import com.labscraft.LabsCraft;

import java.io.InputStream;
import java.io.InputStreamReader;

public class AgentConfig {

    private String agent_server_url = "http://localhost:3001";
    private int tick_rate = 20;
    private int timeout_ms = 500;
    private boolean fallback_to_static = true;
    private boolean debug_logging = false;

    public String getServerUrl() {
        return agent_server_url;
    }

    public int getTickRate() {
        return tick_rate;
    }

    public int getTimeoutMs() {
        return timeout_ms;
    }

    public boolean isFallbackToStatic() {
        return fallback_to_static;
    }

    public boolean isDebugLogging() {
        return debug_logging;
    }

    public static AgentConfig load() {
        try (InputStream is = AgentConfig.class.getResourceAsStream("/labscraft-agent.json")) {
            if (is == null) {
                LabsCraft.LOGGER.warn("[AgentConfig] labscraft-agent.json not found, using defaults");
                return new AgentConfig();
            }
            AgentConfig config = new Gson().fromJson(new InputStreamReader(is), AgentConfig.class);
            LabsCraft.LOGGER.info("[AgentConfig] Loaded: url={}, tickRate={}, timeout={}ms",
                    config.agent_server_url, config.tick_rate, config.timeout_ms);
            return config;
        } catch (Exception e) {
            LabsCraft.LOGGER.warn("[AgentConfig] Failed to load config, using defaults: {}", e.getMessage());
            return new AgentConfig();
        }
    }
}
