import { describe, expect, it } from "vitest";
import { MAX_ACTIONS_PER_RESPONSE, SAY_MAX_CHARS, validateAction, validateActions } from "../src/actions";

describe("validateAction", () => {
  it("accepts a valid SAY and trims/caps text", () => {
    expect(validateAction({ type: "SAY", text: "  hello  ", delay_ticks: 5 })).toEqual({
      type: "SAY",
      text: "hello",
      delay_ticks: 5,
    });
    const long = "x".repeat(500);
    const result = validateAction({ type: "SAY", text: long, delay_ticks: 0 });
    expect(result?.type).toBe("SAY");
    if (result?.type === "SAY") {
      expect(result.text.length).toBe(SAY_MAX_CHARS);
    }
  });

  it("rejects empty SAY", () => {
    expect(validateAction({ type: "SAY", text: "   ", delay_ticks: 0 })).toBeNull();
    expect(validateAction({ type: "SAY", delay_ticks: 0 })).toBeNull();
  });

  it("defaults missing delay_ticks to 0 and rejects negatives", () => {
    expect(validateAction({ type: "WAIT" })).toEqual({ type: "WAIT", delay_ticks: 0 });
    expect(validateAction({ type: "WAIT", delay_ticks: -1 })).toBeNull();
    expect(validateAction({ type: "WAIT", delay_ticks: "soon" })).toBeNull();
    expect(validateAction({ type: "WAIT", delay_ticks: 2.7 })).toEqual({ type: "WAIT", delay_ticks: 2 });
  });

  it("validates WALK_TO with target or position, clamping speed to 0.1-1.0", () => {
    expect(validateAction({ type: "WALK_TO", target: "player", speed: 0.5, delay_ticks: 0 })).toEqual({
      type: "WALK_TO",
      target: "player",
      speed: 0.5,
      delay_ticks: 0,
    });
    const clamped = validateAction({ type: "WALK_TO", target: "player", speed: 1.5, delay_ticks: 0 });
    expect(clamped?.type === "WALK_TO" && clamped.speed).toBe(1.0);
    expect(validateAction({ type: "WALK_TO", target: "player", speed: 0, delay_ticks: 0 })).toBeNull();
    expect(validateAction({ type: "WALK_TO", target: "player", speed: 99, delay_ticks: 0 })).toBeNull();
    expect(
      validateAction({ type: "WALK_TO", position: { x: 1, y: 64, z: -3 }, speed: 0.8, delay_ticks: 0 }),
    ).toEqual({ type: "WALK_TO", position: { x: 1, y: 64, z: -3 }, speed: 0.8, delay_ticks: 0 });
    // No target and no position → drop
    expect(validateAction({ type: "WALK_TO", speed: 0.5, delay_ticks: 0 })).toBeNull();
  });

  it("rejects absurd coordinates", () => {
    expect(
      validateAction({ type: "WALK_TO", position: { x: 99_000_000, y: 64, z: 0 }, speed: 0.5, delay_ticks: 0 }),
    ).toBeNull();
    expect(
      validateAction({ type: "LOOK_AT", position: { x: 0, y: 9999, z: 0 }, delay_ticks: 0 }),
    ).toBeNull();
    expect(
      validateAction({ type: "LOOK_AT", position: { x: NaN, y: 64, z: 0 }, delay_ticks: 0 }),
    ).toBeNull();
  });

  it("validates EMOTE against the emote enum", () => {
    expect(validateAction({ type: "EMOTE", emote: "nod", delay_ticks: 0 })).toEqual({
      type: "EMOTE",
      emote: "nod",
      delay_ticks: 0,
    });
    expect(validateAction({ type: "EMOTE", emote: "backflip", delay_ticks: 0 })).toBeNull();
  });

  it("enforces the GIVE_ITEM allowlist and quantity range", () => {
    expect(validateAction({ type: "GIVE_ITEM", item: "labscraft:tpu", quantity: 3, delay_ticks: 0 })).toEqual({
      type: "GIVE_ITEM",
      item: "labscraft:tpu",
      quantity: 3,
      delay_ticks: 0,
    });
    expect(
      validateAction({ type: "GIVE_ITEM", item: "minecraft:netherite_sword", quantity: 1, delay_ticks: 0 }),
    ).toBeNull();
    expect(validateAction({ type: "GIVE_ITEM", item: "not-an-id", quantity: 1, delay_ticks: 0 })).toBeNull();
    expect(validateAction({ type: "GIVE_ITEM", item: "labscraft:tpu", quantity: 0, delay_ticks: 0 })).toBeNull();
    expect(validateAction({ type: "GIVE_ITEM", item: "labscraft:tpu", quantity: 65, delay_ticks: 0 })).toBeNull();
  });

  it("validates COMPLETE_OBJECTIVE and ADVANCE_QUEST", () => {
    expect(validateAction({ type: "COMPLETE_OBJECTIVE", objective_id: "mine_tpu_ore", delay_ticks: 0 })).toEqual(
      { type: "COMPLETE_OBJECTIVE", objective_id: "mine_tpu_ore", delay_ticks: 0 },
    );
    expect(validateAction({ type: "COMPLETE_OBJECTIVE", delay_ticks: 0 })).toBeNull();
    expect(validateAction({ type: "ADVANCE_QUEST", delay_ticks: 10 })).toEqual({
      type: "ADVANCE_QUEST",
      delay_ticks: 10,
    });
  });

  it("rejects unknown action types and non-objects", () => {
    expect(validateAction({ type: "SELF_DESTRUCT", delay_ticks: 0 })).toBeNull();
    expect(validateAction("SAY hello")).toBeNull();
    expect(validateAction(null)).toBeNull();
    expect(validateAction(42)).toBeNull();
  });
});

describe("validateActions", () => {
  it("drops invalid entries and keeps the rest (protocol rule: keep the rest)", () => {
    const result = validateActions([
      { type: "SAY", text: "ok", delay_ticks: 0 },
      { type: "TELEPORT", delay_ticks: 0 },
      { type: "EMOTE", emote: "shrug", delay_ticks: 5 },
    ]);
    expect(result).toHaveLength(2);
    expect(result[0]?.type).toBe("SAY");
    expect(result[1]?.type).toBe("EMOTE");
  });

  it("returns [] for non-arrays", () => {
    expect(validateActions(undefined)).toEqual([]);
    expect(validateActions("nope")).toEqual([]);
    expect(validateActions({ actions: [] })).toEqual([]);
  });

  it("caps the number of actions per response", () => {
    const many = Array.from({ length: 20 }, () => ({ type: "WAIT", delay_ticks: 0 }));
    expect(validateActions(many)).toHaveLength(MAX_ACTIONS_PER_RESPONSE);
  });
});
