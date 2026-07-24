/**
 * Trigger system — decides whether a tick warrants waking Josh's brain at all.
 *
 * v1 defect #7 fix: all state lives inside the TriggerEngine instance (no
 * module-global maps), is keyed by player_uuid (never player name), resets when
 * a player's session_id changes, and is evicted after a TTL of inactivity.
 */

import type { TickRequest, TriggerName } from "./protocol";

export interface TriggerResult {
  trigger: TriggerName;
  /** Short human-readable reason, used for prompts + debug output. */
  reason: string;
}

export interface TriggerConfig {
  cooldownsMs: Record<TriggerName, number>;
  /** Evict per-player state after this long without a tick. */
  evictAfterMs: number;
}

export const DEFAULT_TRIGGER_CONFIG: TriggerConfig = {
  cooldownsMs: {
    chat_message: 3_000,
    interaction: 3_000,
    quest_completed: 8_000,
    danger: 20_000,
    idle_near_objective: 120_000,
    periodic_proximity: 60_000,
  },
  evictAfterMs: 600_000, // 10 minutes
};

/** Priority order: first match wins. */
const TRIGGER_PRIORITY: readonly TriggerName[] = [
  "chat_message",
  "interaction",
  "quest_completed",
  "danger",
  "idle_near_objective",
  "periodic_proximity",
];

const HOSTILE_MOBS = new Set([
  "minecraft:zombie",
  "minecraft:skeleton",
  "minecraft:creeper",
  "minecraft:spider",
  "minecraft:cave_spider",
  "minecraft:enderman",
  "minecraft:witch",
  "minecraft:drowned",
  "minecraft:husk",
  "minecraft:stray",
  "minecraft:pillager",
  "minecraft:vindicator",
  "minecraft:phantom",
  "minecraft:slime",
  "minecraft:zombified_piglin",
]);

const CHAT_EARSHOT_BLOCKS = 24; // matches SAY broadcast radius
const DANGER_DISTANCE_BLOCKS = 12;
const LOW_HEALTH_THRESHOLD = 6;
const IDLE_STAGE_SECONDS = 120;
const IDLE_JOSH_DISTANCE = 16;
const PROXIMITY_DISTANCE = 8;
const RECENT_EVENT_WINDOW_S = 5;

interface PlayerTriggerState {
  sessionId: string;
  lastSeenMs: number;
  lastFiredMs: Partial<Record<TriggerName, number>>;
}

/** Parse "minecraft:zombie@8.1" → { id, distance }. */
export function parseEntityRef(ref: string): { id: string; distance: number } | null {
  const at = ref.lastIndexOf("@");
  if (at <= 0) return null;
  const id = ref.slice(0, at);
  const distance = Number(ref.slice(at + 1));
  if (!Number.isFinite(distance)) return null;
  return { id, distance };
}

export class TriggerEngine {
  private readonly config: TriggerConfig;
  private readonly players = new Map<string, PlayerTriggerState>();
  private lastSweepMs = 0;

  constructor(config?: {
    cooldownsMs?: Partial<Record<TriggerName, number>>;
    evictAfterMs?: number;
  }) {
    this.config = {
      cooldownsMs: { ...DEFAULT_TRIGGER_CONFIG.cooldownsMs, ...config?.cooldownsMs },
      evictAfterMs: config?.evictAfterMs ?? DEFAULT_TRIGGER_CONFIG.evictAfterMs,
    };
  }

  /** Number of players currently tracked (for tests / diagnostics). */
  trackedPlayers(): number {
    return this.players.size;
  }

  /** Explicitly drop a player's state (e.g. on disconnect notification). */
  evict(playerUuid: string): void {
    this.players.delete(playerUuid);
  }

  /**
   * Evaluate a tick. Returns the highest-priority trigger that fires (and
   * records its cooldown), or null when Josh should do nothing this tick.
   */
  evaluate(req: TickRequest, nowMs: number = Date.now()): TriggerResult | null {
    this.sweep(nowMs);

    let state = this.players.get(req.player_uuid);
    if (state === undefined || state.sessionId !== req.session_id) {
      // New player or new world load — cooldowns from an old session must not
      // carry over (v1 leaked state across reconnects).
      state = { sessionId: req.session_id, lastSeenMs: nowMs, lastFiredMs: {} };
      this.players.set(req.player_uuid, state);
    }
    state.lastSeenMs = nowMs;

    for (const name of TRIGGER_PRIORITY) {
      const reason = this.check(name, req);
      if (reason === null) continue;
      const lastFired = state.lastFiredMs[name];
      if (lastFired !== undefined && nowMs - lastFired < this.config.cooldownsMs[name]) {
        continue; // on cooldown — try lower-priority triggers
      }
      state.lastFiredMs[name] = nowMs;
      return { trigger: name, reason };
    }
    return null;
  }

  private check(name: TriggerName, req: TickRequest): string | null {
    switch (name) {
      case "chat_message": {
        if (req.josh.distance_to_player > CHAT_EARSHOT_BLOCKS) return null;
        const chat = req.recent_events.find(
          (e) => e.type === "chat_message" && e.seconds_ago <= RECENT_EVENT_WINDOW_S,
        );
        if (chat === undefined) return null;
        return `player said: ${chat.text ?? "(no text)"}`;
      }

      case "interaction": {
        const evt = req.recent_events.find(
          (e) => e.type === "interaction" && e.seconds_ago <= RECENT_EVENT_WINDOW_S,
        );
        if (evt === undefined) return null;
        return "player interacted with Josh";
      }

      case "quest_completed": {
        const evt = req.recent_events.find(
          (e) =>
            (e.type === "stage_changed" || e.type === "objective_completed") &&
            e.seconds_ago <= RECENT_EVENT_WINDOW_S,
        );
        if (evt === undefined) return null;
        if (evt.type === "stage_changed") {
          return `quest stage changed: ${evt.from ?? "?"} -> ${evt.to ?? "?"}`;
        }
        return `objective completed: ${evt.item ?? evt.text ?? "?"}`;
      }

      case "danger": {
        if (req.player.health <= LOW_HEALTH_THRESHOLD) {
          return `player health low (${req.player.health}/${req.player.max_health})`;
        }
        for (const ref of req.world.nearby_entities) {
          const parsed = parseEntityRef(ref);
          if (parsed === null) continue;
          if (HOSTILE_MOBS.has(parsed.id) && parsed.distance <= DANGER_DISTANCE_BLOCKS) {
            return `hostile ${parsed.id} at ${parsed.distance.toFixed(1)} blocks`;
          }
        }
        return null;
      }

      case "idle_near_objective": {
        if (req.josh.distance_to_player > IDLE_JOSH_DISTANCE) return null;
        if (req.quest.seconds_in_stage < IDLE_STAGE_SECONDS) return null;
        const pending = req.quest.objectives.find((o) => !o.done);
        if (pending === undefined) return null;
        return `player idle ${req.quest.seconds_in_stage}s in stage ${req.quest.current_stage}; pending objective: ${pending.description}`;
      }

      case "periodic_proximity": {
        if (req.josh.distance_to_player > PROXIMITY_DISTANCE) return null;
        if (!req.josh.can_see_player) return null;
        return `player nearby (${req.josh.distance_to_player.toFixed(1)} blocks)`;
      }
    }
  }

  /** Drop players not seen within evictAfterMs. Throttled to every 30s. */
  private sweep(nowMs: number): void {
    if (nowMs - this.lastSweepMs < 30_000 && this.lastSweepMs !== 0) return;
    this.lastSweepMs = nowMs;
    for (const [uuid, state] of this.players) {
      if (nowMs - state.lastSeenMs > this.config.evictAfterMs) {
        this.players.delete(uuid);
      }
    }
  }
}
