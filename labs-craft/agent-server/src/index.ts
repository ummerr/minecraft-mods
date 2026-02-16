import express from "express";
import cors from "cors";
import { loadConfig, LLMConfig } from "./config";
import { initDatabase } from "./database";
import { WorldState, AgentResponse, AgentAction } from "./types";
import { LLMProvider } from "./llm/provider";
import { ClaudeProvider } from "./llm/claude";
import { GeminiProvider } from "./llm/gemini";
import { OllamaProvider } from "./llm/ollama";
import { JOSH_SYSTEM_PROMPT } from "./prompts/josh";
import { buildContext } from "./context";
import { shouldRespond, TriggerResult } from "./triggers";
import {
  extractMemories,
  getRecentConversationText,
  getConversationCount,
} from "./memory";
import { summarizeIfNeeded } from "./summarizer";

const config = loadConfig();
const app = express();

app.use(cors());
app.use(express.json());

// Initialize SQLite
const db = initDatabase(config);
console.log("[agent-server] SQLite initialized");

// ── Initialize LLM provider with fallback chain ──

let llmProvider: LLMProvider | null = null;

function createProvider(llmConfig: LLMConfig): LLMProvider {
  switch (llmConfig.provider) {
    case "claude":
      return new ClaudeProvider(llmConfig);
    case "gemini":
      return new GeminiProvider(llmConfig);
    default:
      throw new Error(`Unknown LLM provider: ${llmConfig.provider}`);
  }
}

async function initLLM(): Promise<void> {
  const providers = [config.llm, ...(config.llm_alternatives ?? [])];

  for (const llmConfig of providers) {
    try {
      llmProvider = createProvider(llmConfig);
      console.log(
        `[agent-server] LLM: ${llmConfig.provider} (${llmConfig.model})`
      );
      return;
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      console.log(
        `[agent-server] ${llmConfig.provider} unavailable: ${msg}`
      );
    }
  }

  const ollama = new OllamaProvider(config.llm_fallback);
  if (await ollama.isAvailable()) {
    llmProvider = ollama;
    console.log(
      `[agent-server] LLM: Ollama fallback (${config.llm_fallback.model})`
    );
    return;
  }

  console.log(
    "[agent-server] WARNING: No LLM provider available. Using hardcoded responses."
  );
}

// ── Hardcoded fallback responses ──

const FALLBACK_RESPONSES: Record<string, AgentAction[]> = {
  NOT_STARTED: [
    {
      type: "SAY",
      text: "Oh hey, you must be the new APM. Welcome to Labs. Let's get you onboarded — we're behind on OKRs already.",
      delay_ticks: 0,
    },
  ],
  FLOW_INTRO: [
    {
      type: "SAY",
      text: "So Flow is our generative video platform. Very exciting, very P0. Go find the Flow Crafting Table and get familiar with the pipeline.",
      delay_ticks: 0,
    },
    { type: "EMOTE", emote: "point", delay_ticks: 20 },
  ],
  LEARNING_PIPELINE: [
    {
      type: "SAY",
      text: "Great, you found the crafting table. Now head to a console and try generating something. Start simple — we don't want another production incident.",
      delay_ticks: 0,
    },
  ],
  FIRST_GENERATION: [
    {
      type: "SAY",
      text: "Nice, first generation shipped. That's a P0 resolved. Let me get you some TPUs for the next sprint.",
      delay_ticks: 0,
    },
    {
      type: "GIVE_ITEM",
      item: "labscraft:tpu",
      quantity: 5,
      delay_ticks: 20,
    },
    { type: "ADVANCE_QUEST", delay_ticks: 40 },
  ],
  COMPLETED: [
    {
      type: "SAY",
      text: "You've been crushing it. I'll flag this in the next performance review. For now, keep experimenting — there's a lot of untested surface area.",
      delay_ticks: 0,
    },
  ],
};

// ── Parse LLM response into actions ──

function parseLLMResponse(content: string): AgentAction[] {
  let cleaned = content.trim();
  if (cleaned.startsWith("```")) {
    cleaned = cleaned
      .replace(/^```(?:json)?\s*\n?/, "")
      .replace(/\n?```\s*$/, "");
  }

  try {
    const parsed = JSON.parse(cleaned);
    const actions: unknown[] = Array.isArray(parsed) ? parsed : parsed.actions;

    if (!Array.isArray(actions)) {
      if (typeof parsed === "string" || typeof parsed.text === "string") {
        return [
          { type: "SAY", text: parsed.text ?? parsed, delay_ticks: 0 },
        ];
      }
      console.warn("[agent-server] LLM returned non-array actions:", content);
      return [];
    }

    return actions
      .filter(
        (a): a is AgentAction =>
          typeof a === "object" && a !== null && "type" in a
      )
      .map((a) => ({
        ...a,
        delay_ticks:
          (a as unknown as Record<string, number>).delay_ticks ?? 0,
      })) as AgentAction[];
  } catch (e) {
    console.warn("[agent-server] Failed to parse LLM JSON, wrapping as SAY");
    return [{ type: "SAY", text: cleaned.slice(0, 200), delay_ticks: 0 }];
  }
}

// ── Rate limiter ──

const callTimestamps: number[] = [];

function isRateLimited(): boolean {
  const now = Date.now();
  while (callTimestamps.length > 0 && callTimestamps[0] < now - 60_000) {
    callTimestamps.shift();
  }
  return callTimestamps.length >= config.behavior.max_llm_calls_per_minute;
}

function recordCall(): void {
  callTimestamps.push(Date.now());
}

// ── Request validation ──

function validateWorldState(body: unknown): body is WorldState {
  if (!body || typeof body !== "object") return false;
  const ws = body as Record<string, unknown>;
  return (
    ws.player !== undefined &&
    typeof ws.player === "object" &&
    ws.player !== null &&
    typeof (ws.player as Record<string, unknown>).name === "string"
  );
}

// ── Log helpers ──

function logPlayerEvent(state: WorldState): void {
  const playerUuid = state.player.name;
  const events = state.recent_events ?? [];

  const chatEvent = events.find((e) => e.type === "chat_message");
  if (chatEvent && typeof chatEvent.text === "string") {
    db.prepare(
      "INSERT INTO conversations (player_uuid, role, content, timestamp) VALUES (?, ?, ?, ?)"
    ).run(playerUuid, "player", chatEvent.text, Date.now());
  }

  const interactionEvent = events.find((e) => e.type === "interaction");
  if (interactionEvent) {
    db.prepare(
      "INSERT INTO conversations (player_uuid, role, content, timestamp) VALUES (?, ?, ?, ?)"
    ).run(playerUuid, "player", "[Player interacted with Josh]", Date.now());
  }

  if (state.quest) {
    db.prepare(
      `INSERT INTO quest_state (player_uuid, current_stage, completed_objectives, started_at, last_interaction)
       VALUES (?, ?, ?, ?, ?)
       ON CONFLICT(player_uuid) DO UPDATE SET
         current_stage = excluded.current_stage,
         completed_objectives = excluded.completed_objectives,
         last_interaction = excluded.last_interaction`
    ).run(
      playerUuid,
      state.quest.current_stage,
      JSON.stringify(state.quest.objectives_completed ?? []),
      Date.now(),
      Date.now()
    );
  }
}

function logJoshResponse(playerUuid: string, actions: AgentAction[]): void {
  const sayAction = actions.find((a) => a.type === "SAY");
  if (sayAction && sayAction.type === "SAY") {
    db.prepare(
      "INSERT INTO conversations (player_uuid, role, content, timestamp) VALUES (?, ?, ?, ?)"
    ).run(playerUuid, "josh", sayAction.text, Date.now());
  }
}

// ── Background tasks (memory + summarization, non-blocking) ──

function runBackgroundTasks(playerUuid: string): void {
  if (!llmProvider) return;

  // Run asynchronously — don't block the response
  setImmediate(async () => {
    try {
      // Memory extraction: every 5 conversations
      const count = getConversationCount(playerUuid, db);
      if (count > 0 && count % 5 === 0) {
        const recentText = getRecentConversationText(playerUuid, db, 10);
        if (recentText) {
          await extractMemories(playerUuid, recentText, db, llmProvider!);
        }
      }

      // Summarization: when conversation history exceeds threshold
      await summarizeIfNeeded(
        playerUuid,
        db,
        llmProvider!,
        config.memory.summarize_after_turns
      );
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      console.warn(`[background] Error for ${playerUuid}: ${msg}`);
    }
  });
}

// ── Routes ──

app.get("/health", (_req, res) => {
  res.json({
    status: "ok",
    version: "0.2.0",
    llm: llmProvider?.name ?? "none",
  });
});

app.post("/api/agent/tick", async (req, res) => {
  const start = Date.now();

  if (!validateWorldState(req.body)) {
    res.status(400).json({
      error: "Invalid WorldState: must include at least player.name",
    });
    return;
  }

  const state = req.body as WorldState;
  const playerUuid = state.player.name;

  // Log incoming events to SQLite
  logPlayerEvent(state);

  // ── Check if Josh should respond ──
  const trigger: TriggerResult = shouldRespond(state, config.behavior);

  if (!trigger.shouldRespond) {
    // Nothing to do — return empty actions
    res.json({
      actions: [],
      debug: {
        trigger: "none",
        latency_ms: Date.now() - start,
      },
    } as AgentResponse);
    return;
  }

  console.log(
    `[tick] player=${playerUuid} stage=${state.quest?.current_stage ?? "?"} trigger=${trigger.reason}${trigger.detail ? ` (${trigger.detail})` : ""}`
  );

  let actions: AgentAction[];
  let triggerLabel = trigger.reason ?? "unknown";

  // ── Try LLM path ──
  if (llmProvider && !isRateLimited()) {
    try {
      const messages = buildContext(state, db, config);
      recordCall();

      const llmResponse = await llmProvider.complete(
        JOSH_SYSTEM_PROMPT,
        messages,
        config.llm.max_tokens,
        config.llm.temperature
      );

      actions = parseLLMResponse(llmResponse.content);
      triggerLabel = `llm:${trigger.reason}`;

      console.log(
        `[tick] LLM: ${actions.length} actions, ${llmResponse.usage?.input_tokens ?? "?"}in/${llmResponse.usage?.output_tokens ?? "?"}out`
      );

      logJoshResponse(playerUuid, actions);

      // Kick off background memory extraction + summarization
      runBackgroundTasks(playerUuid);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      console.error(`[tick] LLM error, falling back: ${msg}`);
      actions = getFallbackActions(state);
      triggerLabel = "fallback_error";
      logJoshResponse(playerUuid, actions);
    }
  } else {
    // ── Fallback: hardcoded responses ──
    actions = getFallbackActions(state);
    triggerLabel = llmProvider ? "fallback_rate_limit" : "fallback_no_llm";
    logJoshResponse(playerUuid, actions);
  }

  const response: AgentResponse = {
    actions,
    debug: {
      trigger: triggerLabel,
      reasoning: trigger.detail,
      latency_ms: Date.now() - start,
    },
  };

  res.json(response);
});

function getFallbackActions(state: WorldState): AgentAction[] {
  const stage = state.quest?.current_stage ?? "NOT_STARTED";
  return FALLBACK_RESPONSES[stage] ?? FALLBACK_RESPONSES["NOT_STARTED"];
}

// ── Start ──

initLLM().then(() => {
  app.listen(config.port, () => {
    console.log(
      `[agent-server] LabsCraft Agent Server v0.2.0 on port ${config.port}`
    );
    console.log(`[agent-server] LLM: ${llmProvider?.name ?? "none (hardcoded fallback)"}`);
    console.log(`[agent-server] POST /api/agent/tick | GET /health`);
  });
});
