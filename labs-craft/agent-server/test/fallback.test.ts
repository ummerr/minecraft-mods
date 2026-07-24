import { describe, expect, it } from "vitest";
import { validateActions } from "../src/actions";
import { StaticFallbackProvider } from "../src/providers/fallback";
import type { TriggerName } from "../src/protocol";
import type { TriggerResult } from "../src/triggers";
import { makeTick } from "./fixtures";

const provider = new StaticFallbackProvider();

function trigger(name: TriggerName, reason = "test"): TriggerResult {
  return { trigger: name, reason };
}

const EMPTY_MEMORY = { summary: null, recent: [] };

describe("StaticFallbackProvider", () => {
  it("is always configured (playable without any API key)", () => {
    expect(provider.isConfigured()).toBe(true);
  });

  const allTriggers: TriggerName[] = [
    "chat_message",
    "interaction",
    "quest_completed",
    "danger",
    "idle_near_objective",
    "periodic_proximity",
  ];

  for (const name of allTriggers) {
    it(`returns valid, non-empty actions for trigger ${name}`, async () => {
      const tick = makeTick({
        recent_events:
          name === "chat_message"
            ? [{ type: "chat_message", seconds_ago: 1, text: "hello josh" }]
            : [],
      });
      const decision = await provider.decide({
        request: tick,
        trigger: trigger(name),
        memory: EMPTY_MEMORY,
      });
      expect(decision.actions.length).toBeGreaterThan(0);
      expect(decision.reasoning.length).toBeGreaterThan(0);
      // Every action must survive the shared validator untouched.
      expect(validateActions(decision.actions)).toEqual(decision.actions);
      for (const action of decision.actions) {
        if (action.type === "SAY") {
          expect(action.text.length).toBeLessThanOrEqual(200);
        }
      }
    });
  }

  it("answers greetings with a greeting", async () => {
    const tick = makeTick({
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hi there" }],
    });
    const decision = await provider.decide({
      request: tick,
      trigger: trigger("chat_message"),
      memory: EMPTY_MEMORY,
    });
    const say = decision.actions.find((a) => a.type === "SAY");
    expect(say).toBeDefined();
    expect(decision.reasoning).toContain("keyword");
  });

  it("answers quest questions with a stage-appropriate hint", async () => {
    const tick = makeTick({
      quest: { current_stage: "FLOW_INTRO", objectives: [], seconds_in_stage: 10 },
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "what is my next objective?" }],
    });
    const decision = await provider.decide({
      request: tick,
      trigger: trigger("chat_message"),
      memory: EMPTY_MEMORY,
    });
    const say = decision.actions.find((a) => a.type === "SAY");
    expect(say?.type === "SAY" && say.text).toMatch(/TPU|mine|Y 32/i);
  });

  it("deflects off-topic chatter in character", async () => {
    const tick = makeTick({
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "zzz qwxyzzy nonsense" }],
    });
    const decision = await provider.decide({
      request: tick,
      trigger: trigger("chat_message"),
      memory: EMPTY_MEMORY,
    });
    expect(decision.reasoning).toContain("off-topic");
    const say = decision.actions.find((a) => a.type === "SAY");
    expect(say?.type === "SAY" && say.text).toMatch(/OKR|roadmap|offline/i);
  });

  it("nudges toward the first pending objective when idle", async () => {
    const tick = makeTick({
      quest: {
        current_stage: "FLOW_INTRO",
        seconds_in_stage: 400,
        objectives: [
          { id: "mine_tpu_ore", description: "Mine 3 TPU Ore", done: false, progress: 1, goal: 3 },
        ],
      },
    });
    const decision = await provider.decide({
      request: tick,
      trigger: trigger("idle_near_objective"),
      memory: EMPTY_MEMORY,
    });
    const say = decision.actions.find((a) => a.type === "SAY");
    expect(say?.type === "SAY" && say.text).toContain("Mine 3 TPU Ore");
  });

  it("varies lines across players/timestamps but stays deterministic for a given seed", async () => {
    const decide = (uuid: string, ts: number) =>
      provider.decide({
        request: makeTick({ player_uuid: uuid, timestamp_ms: ts }),
        trigger: trigger("periodic_proximity"),
        memory: EMPTY_MEMORY,
      });
    const a1 = await decide("u1", 1000);
    const a2 = await decide("u1", 1000);
    expect(a1).toEqual(a2); // deterministic

    const texts = new Set<string>();
    for (let i = 0; i < 12; i++) {
      const d = await decide("u1", 1000 + i * 7919);
      const say = d.actions.find((a) => a.type === "SAY");
      if (say?.type === "SAY") texts.add(say.text);
    }
    expect(texts.size).toBeGreaterThan(1); // rotates lines
  });
});
