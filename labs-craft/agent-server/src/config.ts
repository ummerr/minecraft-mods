/**
 * Configuration loading. config.json (gitignored, may hold real keys) overrides
 * built-in defaults; environment variables override both.
 */

import * as fs from "fs";
import * as path from "path";
import type { TriggerName } from "./protocol";

export interface ServerConfig {
  port: number;
  providers: {
    claude: { apiKey: string; model: string };
    gemini: { apiKey: string; model: string };
    ollama: { enabled: boolean; baseUrl: string; model: string };
  };
  memory: { dbPath: string };
  triggers: {
    cooldownsMs?: Partial<Record<TriggerName, number>>;
    evictAfterMs?: number;
  };
}

export const DEFAULT_CONFIG: ServerConfig = {
  port: 3001,
  providers: {
    claude: { apiKey: "", model: "claude-opus-4-8" },
    gemini: { apiKey: "", model: "gemini-2.5-flash" },
    ollama: { enabled: false, baseUrl: "http://localhost:11434", model: "llama3.2" },
  },
  memory: { dbPath: "./data/memory.db" },
  triggers: {},
};

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null && !Array.isArray(v);
}

function str(v: unknown, fallback: string): string {
  return typeof v === "string" ? v : fallback;
}

function num(v: unknown, fallback: number): number {
  return typeof v === "number" && Number.isFinite(v) ? v : fallback;
}

function bool(v: unknown, fallback: boolean): boolean {
  return typeof v === "boolean" ? v : fallback;
}

export function loadConfig(configPath?: string): ServerConfig {
  const resolved = path.resolve(configPath ?? "config.json");
  let fileData: Record<string, unknown> = {};
  if (fs.existsSync(resolved)) {
    try {
      const parsed: unknown = JSON.parse(fs.readFileSync(resolved, "utf8"));
      if (isRecord(parsed)) fileData = parsed;
    } catch (err) {
      console.warn(`[config] failed to parse ${resolved}, using defaults: ${String(err)}`);
    }
  }

  const d = DEFAULT_CONFIG;
  const providers = isRecord(fileData.providers) ? fileData.providers : {};
  const claude = isRecord(providers.claude) ? providers.claude : {};
  const gemini = isRecord(providers.gemini) ? providers.gemini : {};
  const ollama = isRecord(providers.ollama) ? providers.ollama : {};
  const memory = isRecord(fileData.memory) ? fileData.memory : {};
  const triggers = isRecord(fileData.triggers) ? fileData.triggers : {};

  const config: ServerConfig = {
    port: num(fileData.port, d.port),
    providers: {
      claude: {
        apiKey: process.env.ANTHROPIC_API_KEY ?? str(claude.apiKey, d.providers.claude.apiKey),
        model: str(claude.model, d.providers.claude.model),
      },
      gemini: {
        apiKey: process.env.GEMINI_API_KEY ?? str(gemini.apiKey, d.providers.gemini.apiKey),
        model: str(gemini.model, d.providers.gemini.model),
      },
      ollama: {
        enabled: bool(ollama.enabled, d.providers.ollama.enabled),
        baseUrl: process.env.OLLAMA_BASE_URL ?? str(ollama.baseUrl, d.providers.ollama.baseUrl),
        model: str(ollama.model, d.providers.ollama.model),
      },
    },
    memory: {
      dbPath: str(memory.dbPath, d.memory.dbPath),
    },
    triggers: {
      cooldownsMs: isRecord(triggers.cooldownsMs)
        ? (triggers.cooldownsMs as Partial<Record<TriggerName, number>>)
        : undefined,
      evictAfterMs:
        typeof triggers.evictAfterMs === "number" ? triggers.evictAfterMs : undefined,
    },
  };
  return config;
}
