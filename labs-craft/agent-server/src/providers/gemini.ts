/**
 * Gemini provider — @google/genai with responseSchema-constrained JSON output.
 *
 * Gemini's responseSchema dialect handles unions poorly, so it gets a flat
 * action schema (all fields optional except type/delay_ticks); the shared
 * validator then enforces per-type shape exactly as for every other provider.
 */

import { GoogleGenAI, Type, type Schema } from "@google/genai";
import { EMOTES, GIVE_ITEM_ALLOWLIST, validateActions } from "../actions";
import { buildSummaryPrompt, buildUserPrompt, JOSH_SYSTEM_PROMPT, SUMMARIZER_SYSTEM_PROMPT } from "../prompt";
import type { RawDecision } from "./schema";
import type { AgentProvider, Decision, DecisionContext } from "./types";

const positionSchema: Schema = {
  type: Type.OBJECT,
  properties: {
    x: { type: Type.NUMBER },
    y: { type: Type.NUMBER },
    z: { type: Type.NUMBER },
  },
  required: ["x", "y", "z"],
};

const geminiDecisionSchema: Schema = {
  type: Type.OBJECT,
  properties: {
    reasoning: { type: Type.STRING, description: "One short sentence on why Josh responds this way." },
    actions: {
      type: Type.ARRAY,
      description: "Josh's actions in order. Empty array = do nothing.",
      items: {
        type: Type.OBJECT,
        properties: {
          type: {
            type: Type.STRING,
            enum: [
              "SAY",
              "WALK_TO",
              "LOOK_AT",
              "EMOTE",
              "GIVE_ITEM",
              "ADVANCE_QUEST",
              "COMPLETE_OBJECTIVE",
              "WAIT",
            ],
          },
          text: { type: Type.STRING, description: "SAY only. Max 200 chars." },
          target: { type: Type.STRING, enum: ["player"], description: "WALK_TO/LOOK_AT only." },
          position: { ...positionSchema, description: "WALK_TO/LOOK_AT only (alternative to target)." },
          speed: { type: Type.NUMBER, description: "WALK_TO only. 0.1-1.0." },
          emote: { type: Type.STRING, enum: [...EMOTES], description: "EMOTE only." },
          item: { type: Type.STRING, enum: [...GIVE_ITEM_ALLOWLIST], description: "GIVE_ITEM only." },
          quantity: { type: Type.INTEGER, description: "GIVE_ITEM only. 1-64." },
          objective_id: { type: Type.STRING, description: "COMPLETE_OBJECTIVE only." },
          delay_ticks: { type: Type.INTEGER, description: "Delay in game ticks (20/second)." },
        },
        required: ["type", "delay_ticks"],
      },
    },
  },
  required: ["reasoning", "actions"],
};

export class GeminiProvider implements AgentProvider {
  readonly name = "gemini";
  private readonly apiKey: string;
  private readonly model: string;
  private client: GoogleGenAI | null = null;

  constructor(apiKey: string, model: string) {
    this.apiKey = apiKey;
    this.model = model;
  }

  isConfigured(): boolean {
    return this.apiKey.length > 0;
  }

  private getClient(): GoogleGenAI {
    if (this.client === null) {
      this.client = new GoogleGenAI({ apiKey: this.apiKey });
    }
    return this.client;
  }

  async decide(ctx: DecisionContext): Promise<Decision> {
    const client = this.getClient();
    const response = await client.models.generateContent({
      model: this.model,
      contents: buildUserPrompt(ctx.request, ctx.memory, ctx.trigger),
      config: {
        systemInstruction: JOSH_SYSTEM_PROMPT,
        responseMimeType: "application/json",
        responseSchema: geminiDecisionSchema,
        abortSignal: AbortSignal.timeout(15_000),
      },
    });
    const text = response.text;
    if (text === undefined || text.length === 0) {
      throw new Error("gemini: empty response");
    }
    // Schema-constrained JSON output (not prose scraping): the whole payload
    // is guaranteed by responseSchema to be a single JSON document.
    const raw = JSON.parse(text) as RawDecision;
    return {
      actions: validateActions(raw.actions),
      reasoning: typeof raw.reasoning === "string" ? raw.reasoning : "",
    };
  }

  async summarize(
    previousSummary: string | null,
    lines: Array<{ role: "player" | "josh"; content: string }>,
    playerName: string,
  ): Promise<string> {
    const client = this.getClient();
    const response = await client.models.generateContent({
      model: this.model,
      contents: buildSummaryPrompt(previousSummary, lines, playerName),
      config: {
        systemInstruction: SUMMARIZER_SYSTEM_PROMPT,
        abortSignal: AbortSignal.timeout(15_000),
      },
    });
    const text = (response.text ?? "").trim();
    if (text.length === 0) {
      throw new Error("gemini: empty summary response");
    }
    return text.slice(0, 800);
  }
}
