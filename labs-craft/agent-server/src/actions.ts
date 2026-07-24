/**
 * Action validation. LLM output is untrusted input (protocol hard rule #2 —
 * the mod validates too, but the server never emits garbage in the first place).
 *
 * validateActions() takes arbitrary parsed JSON and returns only well-formed
 * actions; anything unknown or out-of-range is dropped, the rest is kept.
 */

import type { Action, EmoteKind, Position } from "./protocol";

export const SAY_MAX_CHARS = 200;
export const MAX_ACTIONS_PER_RESPONSE = 8;

export const EMOTES: readonly EmoteKind[] = [
  "nod",
  "shake_head",
  "shrug",
  "point",
  "facepalm",
  "clap",
];

/** Items Josh is allowed to hand out (protocol: allowlisted ids only). */
export const GIVE_ITEM_ALLOWLIST: readonly string[] = [
  "labscraft:tpu",
  "minecraft:bread",
  "minecraft:cooked_beef",
  "minecraft:torch",
  "minecraft:iron_pickaxe",
  "minecraft:stone_pickaxe",
];

const COORD_LIMIT_XZ = 30_000_000;
const COORD_LIMIT_Y_MIN = -512;
const COORD_LIMIT_Y_MAX = 512;

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === "object" && v !== null && !Array.isArray(v);
}

function asFiniteNumber(v: unknown): number | null {
  return typeof v === "number" && Number.isFinite(v) ? v : null;
}

function parseDelayTicks(v: unknown): number | null {
  // delay_ticks: int >= 0. Tolerate a missing field by defaulting to 0 —
  // an action with no delay is unambiguous. Reject negatives / non-numbers.
  if (v === undefined || v === null) return 0;
  const n = asFiniteNumber(v);
  if (n === null || n < 0) return null;
  return Math.floor(n);
}

function parsePosition(v: unknown): Position | null {
  if (!isRecord(v)) return null;
  const x = asFiniteNumber(v.x);
  const y = asFiniteNumber(v.y);
  const z = asFiniteNumber(v.z);
  if (x === null || y === null || z === null) return null;
  if (Math.abs(x) > COORD_LIMIT_XZ || Math.abs(z) > COORD_LIMIT_XZ) return null;
  if (y < COORD_LIMIT_Y_MIN || y > COORD_LIMIT_Y_MAX) return null;
  return { x, y, z };
}

const NAMESPACED_ID = /^[a-z0-9_.-]+:[a-z0-9_/.-]+$/;

/** Validate a single raw action; returns null when it should be dropped. */
export function validateAction(raw: unknown): Action | null {
  if (!isRecord(raw)) return null;
  const delay = parseDelayTicks(raw.delay_ticks);
  if (delay === null) return null;

  switch (raw.type) {
    case "SAY": {
      if (typeof raw.text !== "string") return null;
      const text = raw.text.trim();
      if (text.length === 0) return null;
      return { type: "SAY", text: text.slice(0, SAY_MAX_CHARS), delay_ticks: delay };
    }

    case "WALK_TO": {
      const speedRaw = asFiniteNumber(raw.speed);
      // speed 0.1–1.0; clamp mild drift, reject nonsense.
      if (speedRaw === null || speedRaw <= 0 || speedRaw > 2) return null;
      const speed = Math.min(1.0, Math.max(0.1, speedRaw));
      if (raw.target === "player") {
        return { type: "WALK_TO", target: "player", speed, delay_ticks: delay };
      }
      const position = parsePosition(raw.position);
      if (position === null) return null;
      return { type: "WALK_TO", position, speed, delay_ticks: delay };
    }

    case "LOOK_AT": {
      if (raw.target === "player") {
        return { type: "LOOK_AT", target: "player", delay_ticks: delay };
      }
      const position = parsePosition(raw.position);
      if (position === null) return null;
      return { type: "LOOK_AT", position, delay_ticks: delay };
    }

    case "EMOTE": {
      if (typeof raw.emote !== "string" || !EMOTES.includes(raw.emote as EmoteKind)) {
        return null;
      }
      return { type: "EMOTE", emote: raw.emote as EmoteKind, delay_ticks: delay };
    }

    case "GIVE_ITEM": {
      if (typeof raw.item !== "string" || !NAMESPACED_ID.test(raw.item)) return null;
      if (!GIVE_ITEM_ALLOWLIST.includes(raw.item)) return null;
      const qty = asFiniteNumber(raw.quantity);
      if (qty === null) return null;
      const quantity = Math.floor(qty);
      if (quantity < 1 || quantity > 64) return null;
      return { type: "GIVE_ITEM", item: raw.item, quantity, delay_ticks: delay };
    }

    case "ADVANCE_QUEST":
      return { type: "ADVANCE_QUEST", delay_ticks: delay };

    case "COMPLETE_OBJECTIVE": {
      if (typeof raw.objective_id !== "string" || raw.objective_id.length === 0) return null;
      if (raw.objective_id.length > 128) return null;
      return { type: "COMPLETE_OBJECTIVE", objective_id: raw.objective_id, delay_ticks: delay };
    }

    case "WAIT":
      return { type: "WAIT", delay_ticks: delay };

    default:
      return null;
  }
}

/**
 * Validate a raw actions payload. Non-arrays yield []; invalid entries are
 * dropped and valid ones kept (protocol: "drop that action, keep the rest").
 */
export function validateActions(raw: unknown): Action[] {
  if (!Array.isArray(raw)) return [];
  const out: Action[] = [];
  for (const entry of raw) {
    if (out.length >= MAX_ACTIONS_PER_RESPONSE) break;
    const action = validateAction(entry);
    if (action !== null) out.push(action);
  }
  return out;
}
