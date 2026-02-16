import Database from "better-sqlite3";
import { LLMMessage } from "./llm/provider";
import { WorldState } from "./types";
import { Config } from "./config";

export function buildContext(
  state: WorldState,
  db: Database.Database,
  config: Config
): LLMMessage[] {
  const messages: LLMMessage[] = [];
  const playerUuid = state.player.name;

  // 1. Load conversation history (last N turns)
  const maxHistory = config.memory.max_conversation_history;
  const rows = db
    .prepare(
      `SELECT role, content FROM conversations
       WHERE player_uuid = ?
       ORDER BY timestamp DESC
       LIMIT ?`
    )
    .all(playerUuid, maxHistory) as { role: string; content: string }[];

  // Reverse so oldest first
  rows.reverse();
  for (const row of rows) {
    messages.push({
      role: row.role === "josh" ? "assistant" : "user",
      content:
        row.role === "josh"
          ? row.content
          : `[Player "${state.player.name}"]: ${row.content}`,
    });
  }

  // 2. Load relevant memories
  const memories = db
    .prepare(
      `SELECT memory FROM memories
       WHERE player_uuid = ?
       ORDER BY created_at DESC
       LIMIT 5`
    )
    .all(playerUuid) as { memory: string }[];

  // 3. Build the current state summary as the latest user message
  const stateSummary = formatWorldState(state, memories);
  messages.push({
    role: "user",
    content: stateSummary,
  });

  return messages;
}

function formatWorldState(
  state: WorldState,
  memories: { memory: string }[]
): string {
  const parts: string[] = [];

  // What triggered this call
  const events = state.recent_events ?? [];
  const chatEvent = events.find((e) => e.type === "chat_message");
  const interactionEvent = events.find((e) => e.type === "interaction");

  if (chatEvent && typeof chatEvent.text === "string") {
    parts.push(
      `[Player "${state.player.name}" says]: ${chatEvent.text}`
    );
  } else if (interactionEvent) {
    parts.push(
      `[Player "${state.player.name}" approached and interacted with you (right-clicked)]`
    );
  } else {
    parts.push(
      `[System: periodic state update — player is nearby]`
    );
  }

  // World state context
  parts.push("");
  parts.push("## Current State");

  // Quest
  if (state.quest) {
    parts.push(`Quest stage: ${state.quest.current_stage}`);
    if (state.quest.objectives_remaining?.length > 0) {
      parts.push(
        `Remaining objectives: ${state.quest.objectives_remaining.join(", ")}`
      );
    }
    if (state.quest.objectives_completed?.length > 0) {
      parts.push(
        `Completed: ${state.quest.objectives_completed.join(", ")}`
      );
    }
    if (state.quest.time_in_stage_minutes > 0) {
      parts.push(
        `Time in current stage: ${state.quest.time_in_stage_minutes} minutes`
      );
    }
  }

  // Player info
  if (state.player) {
    const inv =
      state.player.inventory_summary?.length > 0
        ? state.player.inventory_summary.join(", ")
        : "empty";
    parts.push(`Player inventory: ${inv}`);
    parts.push(
      `Player health: ${state.player.health}/20, hunger: ${state.player.hunger}/20`
    );
    if (state.player.held_item) {
      parts.push(`Holding: ${state.player.held_item}`);
    }
  }

  // Josh state
  if (state.josh) {
    parts.push(
      `Distance to player: ${state.josh.distance_to_player} blocks`
    );
  }

  // World
  if (state.world) {
    parts.push(
      `Time: ${state.world.time_of_day}, Weather: ${state.world.weather}`
    );
    if (state.world.nearby_entities?.length > 0) {
      parts.push(`Nearby mobs: ${state.world.nearby_entities.join(", ")}`);
    }
    if (state.world.nearby_blocks_of_interest?.length > 0) {
      parts.push(
        `Nearby blocks: ${state.world.nearby_blocks_of_interest.join(", ")}`
      );
    }
  }

  // Memories
  if (memories.length > 0) {
    parts.push("");
    parts.push("## What you remember about this player");
    for (const m of memories) {
      parts.push(`- ${m.memory}`);
    }
  }

  return parts.join("\n");
}
