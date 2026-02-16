import { WorldState } from "./types";
import { BehaviorConfig } from "./config";

export type TriggerReason =
  | "chat_message"
  | "interaction"
  | "idle_near_objective"
  | "quest_completed"
  | "danger"
  | "periodic_proximity";

export interface TriggerResult {
  shouldRespond: boolean;
  reason: TriggerReason | null;
  detail?: string;
}

// Per-player cooldown tracking
const lastResponseTime: Map<string, number> = new Map();
const lastQuestStage: Map<string, string> = new Map();

export function shouldRespond(
  state: WorldState,
  behaviorConfig: BehaviorConfig
): TriggerResult {
  const playerKey = state.player.name;
  const now = Date.now();
  const cooldownMs = behaviorConfig.proactive_cooldown_seconds * 1000;
  const lastResponse = lastResponseTime.get(playerKey) ?? 0;
  const onCooldown = now - lastResponse < cooldownMs;

  const events = state.recent_events ?? [];

  // 1. Player sent a chat message — always respond (no cooldown)
  const chatEvent = events.find((e) => e.type === "chat_message");
  if (chatEvent) {
    markResponded(playerKey);
    return {
      shouldRespond: true,
      reason: "chat_message",
      detail: typeof chatEvent.text === "string" ? chatEvent.text : undefined,
    };
  }

  // 2. Player right-clicked Josh — always respond (no cooldown)
  const interactionEvent = events.find((e) => e.type === "interaction");
  if (interactionEvent) {
    markResponded(playerKey);
    return { shouldRespond: true, reason: "interaction" };
  }

  // Everything below is proactive — respect cooldown
  if (onCooldown) {
    return { shouldRespond: false, reason: null };
  }

  // 3. Quest stage changed — congratulate/advance
  if (state.quest) {
    const previousStage = lastQuestStage.get(playerKey);
    const currentStage = state.quest.current_stage;

    if (previousStage && previousStage !== currentStage) {
      lastQuestStage.set(playerKey, currentStage);
      markResponded(playerKey);
      return {
        shouldRespond: true,
        reason: "quest_completed",
        detail: `${previousStage} → ${currentStage}`,
      };
    }
    lastQuestStage.set(playerKey, currentStage);
  }

  // 4. Player is in danger — low health + hostile mobs nearby
  if (state.player.health <= behaviorConfig.danger_alert_health_threshold) {
    const hostileMobs = (state.world?.nearby_entities ?? []).some((e) =>
      /zombie|skeleton|creeper|spider|enderman|witch/i.test(e)
    );
    if (
      hostileMobs &&
      state.josh?.distance_to_player !== undefined &&
      state.josh.distance_to_player < 12
    ) {
      markResponded(playerKey);
      return {
        shouldRespond: true,
        reason: "danger",
        detail: `health=${state.player.health}`,
      };
    }
  }

  // 5. Player idle near objective — stuck for too long
  if (
    state.quest?.time_in_stage_minutes >=
    behaviorConfig.idle_nudge_after_seconds / 60
  ) {
    if (
      state.quest.objectives_remaining?.length > 0 &&
      state.josh?.distance_to_player !== undefined &&
      state.josh.distance_to_player < 16
    ) {
      markResponded(playerKey);
      return {
        shouldRespond: true,
        reason: "idle_near_objective",
        detail: `${state.quest.time_in_stage_minutes}min in ${state.quest.current_stage}`,
      };
    }
  }

  // 6. Periodic proximity — player is very close and Josh hasn't spoken in a while
  if (
    state.josh?.distance_to_player !== undefined &&
    state.josh.distance_to_player < 4 &&
    state.josh.last_spoke_ticks_ago > 600 // 30 seconds
  ) {
    markResponded(playerKey);
    return {
      shouldRespond: true,
      reason: "periodic_proximity",
      detail: `distance=${state.josh.distance_to_player}`,
    };
  }

  return { shouldRespond: false, reason: null };
}

function markResponded(playerKey: string): void {
  lastResponseTime.set(playerKey, Date.now());
}

export function resetCooldown(playerKey: string): void {
  lastResponseTime.delete(playerKey);
}
