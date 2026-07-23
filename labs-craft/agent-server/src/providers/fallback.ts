/**
 * Static fallback provider — no LLM, no network, always available.
 *
 * If no API key is configured at all, this keeps the game fully playable:
 * Josh answers chat by keyword, reacts to interactions, congratulates quest
 * progress, warns about danger, and nudges idle interns — all in character.
 */

import type { Action } from "../protocol";
import type { AgentProvider, Decision, DecisionContext } from "./types";

/** Deterministic-but-varied pick: same input → same line, different inputs rotate. */
function hashPick<T>(options: readonly T[], seed: string): T {
  let h = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  const idx = Math.abs(h) % options.length;
  const picked = options[idx];
  // options arrays are non-empty by construction; satisfy noUncheckedIndexedAccess.
  return picked as T;
}

const STAGE_HINTS: Record<string, readonly string[]> = {
  NOT_STARTED: [
    "Welcome to Labs. You're the new APM intern — I've already put your onboarding on the roadmap.",
    "You must be the new intern. Great. First order of business: we're going to get you shipping on Flow.",
  ],
  FLOW_INTRO: [
    "Flow runs on TPUs, and TPUs come from TPU Ore. It spawns below Y 32. Take a pickaxe; expense the torches.",
    "Step one of the pipeline: mine TPU Ore. Deepslate levels, below Y 32. I'd come with you but I have a hard stop in 30.",
  ],
  LEARNING_PIPELINE: [
    "Craft TPUs — gold nuggets, redstone, an iron ingot. Then feed them into the Flow Crafting Table. Five gets you a Nano Banana Console.",
    "You have ore, now you need throughput. Craft TPUs and bring them to the Flow Crafting Table. Ten TPUs for a Veo Console if you're ambitious.",
  ],
  FIRST_GENERATION: [
    "Your console is idle, which makes our utilization metrics look bad. Run a generation.",
    "Time to dogfood the product. Kick off a generation on your console and let's see the pipeline work end to end.",
  ],
  COMPLETED: [
    "You shipped. I've noted it in your perf packet. Keep generating — the roadmap never sleeps.",
    "Internship objectives: complete. Working as intended. Now we talk headcount for next quarter.",
  ],
};

function stageHint(stage: string, seed: string): string {
  const hints = STAGE_HINTS[stage] ?? [
    "Let's stay focused on the current milestone. Check your objectives.",
  ];
  return hashPick(hints, seed);
}

interface ChatRule {
  pattern: RegExp;
  lines: readonly string[];
}

// Order matters: substantive/topical rules first so "hey josh, where's the TPU
// ore?" gets a TPU answer, not a greeting. Greetings and small talk come last.
const CHAT_RULES: readonly ChatRule[] = [
  {
    pattern: /\b(help|stuck|lost|confused|what (do|should) i do)\b/i,
    lines: [
      "Deep breath. Check your objectives, then execute. That's the whole job, honestly.",
      "Let's unblock you. Look at your current objective — I'll narrate the roadmap if you need it.",
    ],
  },
  {
    pattern: /\b(quest|task|objective|mission|next)\b/i,
    lines: [], // filled dynamically with stage hint
  },
  {
    pattern: /\b(tpu|ore|mine|mining)\b/i,
    lines: [
      "TPU Ore spawns below Y 32 — the deepslate layers are dense with it. Fortune helps. So does not falling in lava.",
      "TPUs are gold nuggets, redstone and an iron ingot. The ore is underground, below Y 32. Standard supply chain.",
    ],
  },
  {
    pattern: /\b(flow|console|generate|generation|banana|veo)\b/i,
    lines: [
      "Flow is our gen-media product. TPUs go into the Flow Crafting Table: five for a Nano Banana Console, ten for a Veo Console.",
      "Consoles run generations. Generations make the metrics go up. The metrics going up is, broadly, the point.",
    ],
  },
  {
    pattern: /\b(hi|hello|hey|yo|hiya|howdy)\b/i,
    lines: [
      "Hey. Josh Woodward, PM on Flow. I've got you down for a 1:1 right now, so this works out.",
      "Morning. Or whatever time it is — I've been in back-to-backs since sunrise.",
      "Hey. Good timing, I just got out of a launch review.",
    ],
  },
  {
    pattern: /\b(thanks|thank you|thx|ty)\b/i,
    lines: [
      "Don't thank me, thank the roadmap.",
      "Noted. I'll take that as positive user feedback.",
    ],
  },
  {
    pattern: /\b(bye|goodbye|see you|later|gtg)\b/i,
    lines: [
      "Go ship something. I have a hard stop in 30 anyway.",
      "Later. I'll be here, aligning stakeholders.",
    ],
  },
  {
    pattern: /\b(who are you|your name|what are you)\b/i,
    lines: [
      "Josh Woodward. PM at Labs, currently owning Flow and, apparently, intern onboarding.",
      "I'm Josh — product manager on Flow. My calendar is a war crime but my roadmap is immaculate.",
    ],
  },
];

const OFF_TOPIC_LINES = [
  "Not in my OKRs. Let's get back to Flow.",
  "I'd love to dig into that, but it's not on the roadmap. Flow is on the roadmap.",
  "Interesting. Let's take that offline — permanently. Back to the pipeline.",
] as const;

const INTERACTION_LINES = [
  "Yes? Make it quick, I'm double-booked.",
  "You have my attention until my next meeting, which is soon.",
  "Status check? Good instinct. Very PM of you.",
] as const;

const CONGRATS_LINES = [
  "Milestone hit. I'll mark it green in the tracker.",
  "Progress. Genuinely. Don't let it go to your head.",
  "That's shipping velocity. Keep it up and I'll mention you in the launch email.",
] as const;

const DANGER_LINES = [
  "Heads up — hostile in your vicinity. HR says I can't fight mobs, liability thing.",
  "There's a threat nearby. I'd classify it as a P0. For you, specifically — I'm invulnerable.",
  "Careful. Losing an intern mid-quarter is a headcount nightmare.",
] as const;

const LOW_HEALTH_LINES = [
  "You look rough. Eat something — I can't expense a respawn.",
  "Your health bar is a launch blocker. Address it.",
] as const;

const PROXIMITY_LINES = [
  "Still here. Still aligned.",
  "Don't mind me, just doing a quick skip-level with myself.",
  "How's the pipeline? Rhetorical — check your objectives.",
  "I'd offer coffee, but supply chain hasn't shipped the machine.",
] as const;

function say(text: string, delay = 5): Action {
  return { type: "SAY", text: text.slice(0, 200), delay_ticks: delay };
}

const LOOK_AT_PLAYER: Action = { type: "LOOK_AT", target: "player", delay_ticks: 0 };

export class StaticFallbackProvider implements AgentProvider {
  readonly name = "static";

  isConfigured(): boolean {
    return true;
  }

  async decide(ctx: DecisionContext): Promise<Decision> {
    const { request, trigger } = ctx;
    const stage = request.quest.current_stage;
    const seed = `${request.player_uuid}:${request.timestamp_ms}`;

    switch (trigger.trigger) {
      case "chat_message": {
        const chat = request.recent_events.find((e) => e.type === "chat_message");
        const text = chat?.text ?? "";
        for (const rule of CHAT_RULES) {
          if (!rule.pattern.test(text)) continue;
          const line =
            rule.lines.length > 0
              ? hashPick(rule.lines, seed + text)
              : stageHint(stage, seed + text);
          return {
            actions: [LOOK_AT_PLAYER, say(line)],
            reasoning: "static: matched chat keyword",
          };
        }
        return {
          actions: [LOOK_AT_PLAYER, say(hashPick(OFF_TOPIC_LINES, seed + text))],
          reasoning: "static: off-topic chat deflection",
        };
      }

      case "interaction":
        return {
          actions: [
            LOOK_AT_PLAYER,
            say(hashPick(INTERACTION_LINES, seed)),
            say(stageHint(stage, seed + stage), 45),
          ],
          reasoning: "static: interaction response with stage hint",
        };

      case "quest_completed":
        return {
          actions: [
            LOOK_AT_PLAYER,
            { type: "EMOTE", emote: hashPick(["clap", "nod"] as const, seed), delay_ticks: 3 },
            say(hashPick(CONGRATS_LINES, seed)),
            say(stageHint(stage, seed + "next"), 50),
          ],
          reasoning: "static: quest progress congratulation",
        };

      case "danger": {
        const lowHealth = request.player.health <= 6;
        const lines = lowHealth ? LOW_HEALTH_LINES : DANGER_LINES;
        return {
          actions: [LOOK_AT_PLAYER, say(hashPick(lines, seed))],
          reasoning: lowHealth ? "static: low health warning" : "static: hostile mob warning",
        };
      }

      case "idle_near_objective": {
        const pending = request.quest.objectives.find((o) => !o.done);
        const nudge =
          pending !== undefined
            ? `Quick nudge: "${pending.description}" is still open (${pending.progress}/${pending.goal}). The sprint ends whether we ship or not.`
            : stageHint(stage, seed);
        return {
          actions: [LOOK_AT_PLAYER, say(nudge.slice(0, 200))],
          reasoning: "static: idle objective nudge",
        };
      }

      case "periodic_proximity":
        return {
          actions: [say(hashPick(PROXIMITY_LINES, seed), 0)],
          reasoning: "static: ambient proximity line",
        };
    }
  }

  /** Naive summarizer so memory still compacts without any LLM configured. */
  async summarize(
    previousSummary: string | null,
    lines: Array<{ role: "player" | "josh"; content: string }>,
    playerName: string,
  ): Promise<string> {
    const parts: string[] = [];
    if (previousSummary !== null) parts.push(previousSummary);
    for (const l of lines) {
      if (l.role === "player") {
        parts.push(`${playerName} said: ${l.content.slice(0, 60)}`);
      }
    }
    return parts.join(" | ").slice(-600);
  }
}
