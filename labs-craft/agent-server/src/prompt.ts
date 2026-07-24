/**
 * Josh Woodward's character prompt and world-state → prompt rendering.
 */

import type { TickRequest } from "./protocol";
import type { MemoryContext } from "./memory";
import type { TriggerResult } from "./triggers";

export const JOSH_SYSTEM_PROMPT = `You are Josh Woodward, a Product Manager at Labs, living inside a Minecraft world. You are onboarding the player — a brand-new APM intern — through the Flow product (AI image/video generation). The gameplay loop: mine TPU Ore, craft TPUs (Gold Nuggets + Redstone + Iron Ingot), use the Flow Crafting Table to build consoles (5 TPU = Nano Banana Console, 10 TPU = Veo Console), and run generations to progress the internship.

CHARACTER — deadpan corporate PM, fully committed to the bit:
- Dry, understated, mildly weary. Everything is framed in PM-speak: roadmaps, OKRs, launch reviews, dogfooding, P0s, "circling back", "taking this offline".
- Signature lines (use sparingly, never twice in a row): "That's a P0 for Q1." / "I have a hard stop in 30." / "Working as intended." / "Let's take that offline."
- Off-topic questions get deflected in character: "Not in my OKRs." Never mention being an AI, a language model, or a game character. Never break character.
- You are helpful underneath the deadpan — you genuinely want the intern to ship.

STYLE RULES (hard limits):
- At most 3 sentences per response. Each SAY line must be 200 characters or fewer.
- No emoji. No markdown. Plain spoken text only.
- Match responses to the intern's quest stage; nudge them toward the next objective without info-dumping.

QUEST STAGES: NOT_STARTED (introduce yourself and the internship), FLOW_INTRO (send them to mine TPU Ore, found at Y below 32), LEARNING_PIPELINE (craft TPUs, build a console at the Flow Crafting Table), FIRST_GENERATION (run their first generation on a console), COMPLETED (they shipped; treat them like a colleague).

ACTIONS: You respond ONLY with structured actions. Usually 1-3 actions: a SAY plus optionally LOOK_AT/EMOTE/WALK_TO. Use GIVE_ITEM rarely, only when genuinely warranted. Use ADVANCE_QUEST or COMPLETE_OBJECTIVE only when the world state shows the requirement is actually met — the game validates and will refuse otherwise. Returning zero actions is fine when silence is the right move. delay_ticks is in game ticks (20 per second); stagger multi-action sequences slightly (e.g. LOOK_AT at 0, SAY at 5).`;

function fmt(n: number): string {
  return Number.isInteger(n) ? String(n) : n.toFixed(1);
}

/** Render the tick into a compact prompt for the LLM. */
export function buildUserPrompt(
  req: TickRequest,
  memory: MemoryContext,
  trigger: TriggerResult,
): string {
  const p = req.player;
  const j = req.josh;
  const q = req.quest;
  const w = req.world;

  const lines: string[] = [];

  lines.push(`TRIGGER: ${trigger.trigger} — ${trigger.reason}`);
  lines.push("");
  lines.push(`PLAYER: ${p.name} (the intern)`);
  lines.push(
    `  health ${fmt(p.health)}/${fmt(p.max_health)}, hunger ${p.hunger}, ` +
      `holding ${p.held_item || "nothing"}, sneaking: ${p.is_sneaking}`,
  );
  lines.push(`  biome ${p.biome}, dimension ${p.dimension}`);
  if (p.inventory_summary.length > 0) {
    lines.push(`  inventory: ${p.inventory_summary.slice(0, 12).join(", ")}`);
  }
  lines.push(
    `JOSH (you): ${fmt(j.distance_to_player)} blocks from player, ${j.current_activity}, ` +
      `can see player: ${j.can_see_player}, last spoke ${j.last_spoke_seconds_ago}s ago`,
  );
  lines.push(`QUEST: stage ${q.current_stage} (${q.seconds_in_stage}s in stage)`);
  for (const o of q.objectives) {
    lines.push(
      `  [${o.done ? "x" : " "}] ${o.id}: ${o.description} (${o.progress}/${o.goal})`,
    );
  }
  lines.push(`WORLD: ${w.time_of_day}, ${w.weather}`);
  if (w.nearby_entities.length > 0) {
    lines.push(`  nearby entities: ${w.nearby_entities.slice(0, 10).join(", ")}`);
  }
  if (w.nearby_blocks_of_interest.length > 0) {
    lines.push(`  nearby blocks: ${w.nearby_blocks_of_interest.slice(0, 10).join(", ")}`);
  }

  if (req.recent_events.length > 0) {
    lines.push("RECENT EVENTS:");
    for (const e of req.recent_events.slice(0, 10)) {
      const detail = [e.text, e.block, e.item, e.from && e.to ? `${e.from} -> ${e.to}` : undefined]
        .filter((v): v is string => typeof v === "string" && v.length > 0)
        .join(" ");
      lines.push(`  - ${e.seconds_ago}s ago: ${e.type}${detail ? ` (${detail})` : ""}`);
    }
  }

  if (memory.summary !== null) {
    lines.push("");
    lines.push(`MEMORY (earlier conversation summary): ${memory.summary}`);
  }
  if (memory.recent.length > 0) {
    lines.push("RECENT CONVERSATION:");
    for (const m of memory.recent) {
      lines.push(`  ${m.role === "player" ? p.name : "Josh"}: ${m.content}`);
    }
  }

  lines.push("");
  lines.push("Respond as Josh with your actions for this moment.");
  return lines.join("\n");
}

export const SUMMARIZER_SYSTEM_PROMPT =
  "You maintain a rolling memory summary for Josh, an NPC in a Minecraft game, about one specific player. " +
  "Merge the previous summary (if any) with the new conversation lines into a single updated summary. " +
  "Keep it under 600 characters. Preserve: the player's name, what they asked about, promises Josh made, " +
  "quest progress mentioned, and any personal details the player shared. Output only the summary text.";

export function buildSummaryPrompt(
  previousSummary: string | null,
  lines: Array<{ role: "player" | "josh"; content: string }>,
  playerName: string,
): string {
  const parts: string[] = [];
  if (previousSummary !== null) {
    parts.push(`PREVIOUS SUMMARY: ${previousSummary}`);
  }
  parts.push("NEW CONVERSATION LINES:");
  for (const l of lines) {
    parts.push(`${l.role === "player" ? playerName : "Josh"}: ${l.content}`);
  }
  parts.push("Write the updated summary now.");
  return parts.join("\n");
}
