import * as fs from "fs";
import * as path from "path";

export interface LLMConfig {
  provider: string;
  model: string;
  api_key_env: string;
  max_tokens: number;
  temperature: number;
}

export interface LLMFallbackConfig {
  provider: string;
  model: string;
  base_url: string;
}

export interface MemoryConfig {
  db_path: string;
  max_conversation_history: number;
  summarize_after_turns: number;
}

export interface BehaviorConfig {
  max_llm_calls_per_minute: number;
  proactive_cooldown_seconds: number;
  idle_nudge_after_seconds: number;
  danger_alert_health_threshold: number;
}

export interface Config {
  port: number;
  llm: LLMConfig;
  llm_alternatives?: LLMConfig[];
  llm_fallback: LLMFallbackConfig;
  memory: MemoryConfig;
  behavior: BehaviorConfig;
}

export function loadConfig(): Config {
  const configPath = path.resolve(__dirname, "../config.json");
  const raw = fs.readFileSync(configPath, "utf-8");
  return JSON.parse(raw) as Config;
}
