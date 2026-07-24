/**
 * JSON Schema for structured action output, shared by the Claude strict tool
 * and Ollama's structured-output `format` field. This is the spec-defect-#5
 * fix: actions come back as schema-constrained structured output, never parsed
 * out of free prose.
 */

import { EMOTES, GIVE_ITEM_ALLOWLIST } from "../actions";

const positionSchema = {
  type: "object",
  properties: {
    x: { type: "number" },
    y: { type: "number" },
    z: { type: "number" },
  },
  required: ["x", "y", "z"],
  additionalProperties: false,
} as const;

const delayTicks = {
  type: "integer",
  description: "Delay before executing, in game ticks (20 ticks = 1 second). 0 = immediately.",
} as const;

/** anyOf variants — one per action shape. */
export const ACTION_VARIANTS = [
  {
    type: "object",
    description: "Say a line out loud (broadcast to players within 24 blocks). Max 200 chars.",
    properties: {
      type: { type: "string", enum: ["SAY"] },
      text: { type: "string" },
      delay_ticks: delayTicks,
    },
    required: ["type", "text", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Walk to the player using real pathfinding. speed is 0.1-1.0.",
    properties: {
      type: { type: "string", enum: ["WALK_TO"] },
      target: { type: "string", enum: ["player"] },
      speed: { type: "number" },
      delay_ticks: delayTicks,
    },
    required: ["type", "target", "speed", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Walk to a specific position using real pathfinding. speed is 0.1-1.0.",
    properties: {
      type: { type: "string", enum: ["WALK_TO"] },
      position: positionSchema,
      speed: { type: "number" },
      delay_ticks: delayTicks,
    },
    required: ["type", "position", "speed", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Turn to look at the player.",
    properties: {
      type: { type: "string", enum: ["LOOK_AT"] },
      target: { type: "string", enum: ["player"] },
      delay_ticks: delayTicks,
    },
    required: ["type", "target", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Turn to look at a position.",
    properties: {
      type: { type: "string", enum: ["LOOK_AT"] },
      position: positionSchema,
      delay_ticks: delayTicks,
    },
    required: ["type", "position", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Play an emote animation.",
    properties: {
      type: { type: "string", enum: ["EMOTE"] },
      emote: { type: "string", enum: [...EMOTES] },
      delay_ticks: delayTicks,
    },
    required: ["type", "emote", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Give the player an item. Only allowlisted item ids are permitted. quantity 1-64.",
    properties: {
      type: { type: "string", enum: ["GIVE_ITEM"] },
      item: { type: "string", enum: [...GIVE_ITEM_ALLOWLIST] },
      quantity: { type: "integer" },
      delay_ticks: delayTicks,
    },
    required: ["type", "item", "quantity", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description:
      "Request quest stage advancement. The game validates and refuses unless objectives are genuinely met.",
    properties: {
      type: { type: "string", enum: ["ADVANCE_QUEST"] },
      delay_ticks: delayTicks,
    },
    required: ["type", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Mark a quest objective as completed by its id.",
    properties: {
      type: { type: "string", enum: ["COMPLETE_OBJECTIVE"] },
      objective_id: { type: "string" },
      delay_ticks: delayTicks,
    },
    required: ["type", "objective_id", "delay_ticks"],
    additionalProperties: false,
  },
  {
    type: "object",
    description: "Do nothing.",
    properties: {
      type: { type: "string", enum: ["WAIT"] },
      delay_ticks: delayTicks,
    },
    required: ["type", "delay_ticks"],
    additionalProperties: false,
  },
] as const;

/** Full response schema: { reasoning, actions } */
export const DECISION_SCHEMA = {
  type: "object",
  properties: {
    reasoning: {
      type: "string",
      description: "One short sentence on why Josh responds this way.",
    },
    actions: {
      type: "array",
      description: "Josh's actions for this tick, in order. Empty array = do nothing.",
      items: { anyOf: ACTION_VARIANTS },
    },
  },
  required: ["reasoning", "actions"],
  additionalProperties: false,
} as const;

export interface RawDecision {
  reasoning?: unknown;
  actions?: unknown;
}
