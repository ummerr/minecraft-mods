import { describe, expect, it } from "vitest";
import { TriggerEngine, parseEntityRef } from "../src/triggers";
import { makeTick } from "./fixtures";

const T0 = 1_000_000;

describe("parseEntityRef", () => {
  it("parses id@distance", () => {
    expect(parseEntityRef("minecraft:zombie@8.1")).toEqual({ id: "minecraft:zombie", distance: 8.1 });
  });
  it("rejects malformed refs", () => {
    expect(parseEntityRef("minecraft:zombie")).toBeNull();
    expect(parseEntityRef("minecraft:zombie@abc")).toBeNull();
    expect(parseEntityRef("@5")).toBeNull();
  });
});

describe("TriggerEngine", () => {
  it("fires chat_message when the player chats within earshot", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hey josh" }],
    });
    const result = engine.evaluate(tick, T0);
    expect(result?.trigger).toBe("chat_message");
    expect(result?.reason).toContain("hey josh");
  });

  it("does not fire chat_message when Josh is out of earshot", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      josh: { ...makeTick().josh, distance_to_player: 40 },
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hey josh" }],
    });
    expect(engine.evaluate(tick, T0)).toBeNull();
  });

  it("does not fire chat_message for stale chat events", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      josh: { ...makeTick().josh, distance_to_player: 20, can_see_player: false },
      recent_events: [{ type: "chat_message", seconds_ago: 30, text: "old" }],
    });
    expect(engine.evaluate(tick, T0)).toBeNull();
  });

  it("respects per-trigger cooldowns", () => {
    const engine = new TriggerEngine({ cooldownsMs: { chat_message: 3000 } });
    const tick = makeTick({
      josh: { ...makeTick().josh, can_see_player: false }, // suppress proximity
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hi" }],
    });
    expect(engine.evaluate(tick, T0)?.trigger).toBe("chat_message");
    expect(engine.evaluate(tick, T0 + 1000)).toBeNull(); // on cooldown
    expect(engine.evaluate(tick, T0 + 3500)?.trigger).toBe("chat_message"); // cooldown over
  });

  it("keys cooldowns by player_uuid, not name", () => {
    const engine = new TriggerEngine();
    const tickA = makeTick({
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hi" }],
    });
    const tickB = makeTick({
      player_uuid: "uuid-player-2",
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hi" }],
    });
    expect(engine.evaluate(tickA, T0)?.trigger).toBe("chat_message");
    // Same player name, different uuid — must not share cooldown state.
    expect(engine.evaluate(tickB, T0 + 100)?.trigger).toBe("chat_message");
  });

  it("resets state when session_id changes (reconnect/new world)", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      josh: { ...makeTick().josh, can_see_player: false },
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hi" }],
    });
    expect(engine.evaluate(tick, T0)?.trigger).toBe("chat_message");
    const newSession = { ...tick, session_id: "session-2" };
    // Within old cooldown window, but new session — must fire.
    expect(engine.evaluate(newSession, T0 + 500)?.trigger).toBe("chat_message");
  });

  it("evicts players not seen within evictAfterMs", () => {
    const engine = new TriggerEngine({ evictAfterMs: 60_000 });
    engine.evaluate(makeTick(), T0);
    expect(engine.trackedPlayers()).toBe(1);
    // Another player ticks much later; sweep should drop the stale entry.
    engine.evaluate(makeTick({ player_uuid: "uuid-player-2" }), T0 + 120_000);
    expect(engine.trackedPlayers()).toBe(1);
  });

  it("fires interaction on a recent interaction event", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      recent_events: [{ type: "interaction", seconds_ago: 0 }],
    });
    expect(engine.evaluate(tick, T0)?.trigger).toBe("interaction");
  });

  it("fires quest_completed on stage_changed and objective_completed", () => {
    const engine = new TriggerEngine();
    const stage = makeTick({
      recent_events: [{ type: "stage_changed", seconds_ago: 1, from: "FLOW_INTRO", to: "LEARNING_PIPELINE" }],
    });
    expect(engine.evaluate(stage, T0)?.trigger).toBe("quest_completed");

    const objective = makeTick({
      player_uuid: "uuid-player-2",
      recent_events: [{ type: "objective_completed", seconds_ago: 1, item: "mine_tpu_ore" }],
    });
    expect(engine.evaluate(objective, T0)?.trigger).toBe("quest_completed");
  });

  it("fires danger for hostile mobs within range, not passive mobs", () => {
    const engine = new TriggerEngine();
    const hostile = makeTick({
      josh: { ...makeTick().josh, can_see_player: false },
      world: { ...makeTick().world, nearby_entities: ["minecraft:zombie@8.1"] },
    });
    expect(engine.evaluate(hostile, T0)?.trigger).toBe("danger");

    const passive = makeTick({
      player_uuid: "uuid-player-2",
      josh: { ...makeTick().josh, can_see_player: false },
      world: { ...makeTick().world, nearby_entities: ["minecraft:cow@3.0", "minecraft:zombie@30.0"] },
    });
    expect(engine.evaluate(passive, T0)).toBeNull();
  });

  it("fires danger on low player health", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      josh: { ...makeTick().josh, can_see_player: false },
      player: { ...makeTick().player, health: 4 },
    });
    const result = engine.evaluate(tick, T0);
    expect(result?.trigger).toBe("danger");
    expect(result?.reason).toContain("health");
  });

  it("fires idle_near_objective only when idle long with pending objectives near Josh", () => {
    const engine = new TriggerEngine();
    const idle = makeTick({
      josh: { ...makeTick().josh, can_see_player: false, distance_to_player: 10 },
      quest: { ...makeTick().quest, seconds_in_stage: 300 },
    });
    expect(engine.evaluate(idle, T0)?.trigger).toBe("idle_near_objective");

    const notIdle = makeTick({
      player_uuid: "uuid-player-2",
      josh: { ...makeTick().josh, can_see_player: false, distance_to_player: 10 },
      quest: { ...makeTick().quest, seconds_in_stage: 30 },
    });
    expect(engine.evaluate(notIdle, T0)).toBeNull();

    const allDone = makeTick({
      player_uuid: "uuid-player-3",
      josh: { ...makeTick().josh, can_see_player: false, distance_to_player: 10 },
      quest: {
        current_stage: "FLOW_INTRO",
        seconds_in_stage: 300,
        objectives: [{ id: "a", description: "d", done: true, progress: 3, goal: 3 }],
      },
    });
    expect(engine.evaluate(allDone, T0)).toBeNull();
  });

  it("fires periodic_proximity when close and visible", () => {
    const engine = new TriggerEngine();
    expect(engine.evaluate(makeTick(), T0)?.trigger).toBe("periodic_proximity");

    const cannotSee = makeTick({
      player_uuid: "uuid-player-2",
      josh: { ...makeTick().josh, can_see_player: false },
    });
    expect(engine.evaluate(cannotSee, T0)).toBeNull();

    const tooFar = makeTick({
      player_uuid: "uuid-player-3",
      josh: { ...makeTick().josh, distance_to_player: 15 },
    });
    expect(engine.evaluate(tooFar, T0)).toBeNull();
  });

  it("prioritizes chat_message over periodic_proximity", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      recent_events: [{ type: "chat_message", seconds_ago: 0, text: "hello" }],
    });
    expect(engine.evaluate(tick, T0)?.trigger).toBe("chat_message");
  });

  it("falls through to a lower-priority trigger when the higher one is on cooldown", () => {
    const engine = new TriggerEngine();
    const tick = makeTick({
      recent_events: [{ type: "chat_message", seconds_ago: 0, text: "hello" }],
    });
    expect(engine.evaluate(tick, T0)?.trigger).toBe("chat_message");
    // chat on cooldown; proximity is eligible.
    expect(engine.evaluate(tick, T0 + 1000)?.trigger).toBe("periodic_proximity");
  });
});
