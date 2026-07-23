/**
 * Express app implementing PROTOCOL-V2.md exactly:
 *   POST /tick   — world state in, actions out
 *   GET  /health — availability probe used by the mod on startup
 */

import express, { type Express, type Request, type Response } from "express";
import { PROTOCOL_VERSION, type TickRequest, type TickResponse } from "./protocol";
import type { MemoryStore } from "./memory";
import type { ProviderChain } from "./providers/chain";
import type { TriggerEngine } from "./triggers";

export interface ServerDeps {
  memory: MemoryStore;
  triggers: TriggerEngine;
  chain: ProviderChain;
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null && !Array.isArray(v);
}

/**
 * Light structural validation of the tick payload. The mod is the only
 * intended client, so this guards against malformed input rather than
 * exhaustively re-validating every field.
 */
function parseTickRequest(body: unknown): TickRequest | null {
  if (!isRecord(body)) return null;
  if (body.protocol_version !== PROTOCOL_VERSION) return null;
  if (typeof body.session_id !== "string" || body.session_id.length === 0) return null;
  if (typeof body.player_uuid !== "string" || body.player_uuid.length === 0) return null;
  if (typeof body.timestamp_ms !== "number") return null;
  if (!isRecord(body.player) || !isRecord(body.josh) || !isRecord(body.quest) || !isRecord(body.world)) {
    return null;
  }
  const req = body as unknown as TickRequest;
  if (!Array.isArray(req.recent_events)) {
    (req as { recent_events: unknown[] }).recent_events = [];
  }
  if (!Array.isArray(req.quest.objectives)) {
    (req.quest as { objectives: unknown[] }).objectives = [];
  }
  if (!Array.isArray(req.world.nearby_entities)) {
    (req.world as { nearby_entities: unknown[] }).nearby_entities = [];
  }
  if (!Array.isArray(req.world.nearby_blocks_of_interest)) {
    (req.world as { nearby_blocks_of_interest: unknown[] }).nearby_blocks_of_interest = [];
  }
  if (!Array.isArray(req.player.inventory_summary)) {
    (req.player as { inventory_summary: unknown[] }).inventory_summary = [];
  }
  return req;
}

export function createServer(deps: ServerDeps): Express {
  const { memory, triggers, chain } = deps;
  const app = express();
  app.use(express.json({ limit: "256kb" }));

  app.get("/health", (_req: Request, res: Response) => {
    res.json({
      status: "ok",
      protocol_version: PROTOCOL_VERSION,
      provider: chain.primaryName(),
    });
  });

  app.post("/tick", (req: Request, res: Response) => {
    void handleTick(req, res);
  });

  async function handleTick(req: Request, res: Response): Promise<void> {
    const startedAt = Date.now();
    const tick = parseTickRequest(req.body);
    if (tick === null) {
      res.status(400).json({
        error: "invalid tick payload or unsupported protocol_version (expected 2)",
      });
      return;
    }

    try {
      // Record fresh player chat into memory (deduped in the store, since the
      // same recent_event can appear on consecutive ticks).
      for (const event of tick.recent_events) {
        if (event.type === "chat_message" && typeof event.text === "string" && event.seconds_ago <= 2) {
          memory.recordPlayerMessage(tick.player_uuid, event.text);
        }
      }

      const trigger = triggers.evaluate(tick);
      if (trigger === null) {
        const response: TickResponse = {
          protocol_version: PROTOCOL_VERSION,
          actions: [],
          debug: {
            trigger: "none",
            reasoning: "no trigger fired",
            latency_ms: Date.now() - startedAt,
            provider: "none",
          },
        };
        res.json(response);
        return;
      }

      const memoryContext = memory.getContext(tick.player_uuid);
      const decision = await chain.decide({ request: tick, trigger, memory: memoryContext });

      // Remember what Josh said so memory informs future behavior.
      for (const action of decision.actions) {
        if (action.type === "SAY") {
          memory.recordJoshMessage(tick.player_uuid, action.text);
        }
      }

      // Fire-and-forget summarization to keep prompt context bounded.
      if (memory.needsSummarization(tick.player_uuid)) {
        void summarizeInBackground(tick.player_uuid, tick.player.name);
      }

      const response: TickResponse = {
        protocol_version: PROTOCOL_VERSION,
        actions: decision.actions,
        debug: {
          trigger: trigger.trigger,
          reasoning: decision.reasoning,
          latency_ms: Date.now() - startedAt,
          provider: decision.provider,
        },
      };
      res.json(response);
    } catch (err) {
      console.error("[tick] unexpected error:", err);
      res.status(500).json({ error: "internal error" });
    }
  }

  async function summarizeInBackground(playerUuid: string, playerName: string): Promise<void> {
    try {
      const batch = memory.getMessagesForSummary(playerUuid);
      if (batch.length === 0) return;
      const previous = memory.getSummary(playerUuid);
      const summary = await chain.summarize(
        previous,
        batch.map((m) => ({ role: m.role, content: m.content })),
        playerName,
      );
      if (summary === null) return;
      const lastMessage = batch[batch.length - 1];
      if (lastMessage === undefined) return;
      memory.applySummary(playerUuid, summary, lastMessage.id);
    } catch (err) {
      console.warn("[summarizer] failed:", err instanceof Error ? err.message : String(err));
    }
  }

  return app;
}
