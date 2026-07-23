/**
 * Claude provider — @anthropic-ai/sdk with a forced strict tool call.
 * Actions arrive as validated tool input (structured output), never free text.
 */

import Anthropic from "@anthropic-ai/sdk";
import { validateActions } from "../actions";
import { buildSummaryPrompt, buildUserPrompt, JOSH_SYSTEM_PROMPT, SUMMARIZER_SYSTEM_PROMPT } from "../prompt";
import { DECISION_SCHEMA, type RawDecision } from "./schema";
import type { AgentProvider, Decision, DecisionContext } from "./types";

const REQUEST_TIMEOUT_MS = 15_000;

export class ClaudeProvider implements AgentProvider {
  readonly name = "claude";
  private readonly apiKey: string;
  private readonly model: string;
  private client: Anthropic | null = null;

  constructor(apiKey: string, model: string) {
    this.apiKey = apiKey;
    this.model = model;
  }

  isConfigured(): boolean {
    return this.apiKey.length > 0;
  }

  private getClient(): Anthropic {
    if (this.client === null) {
      this.client = new Anthropic({ apiKey: this.apiKey, timeout: REQUEST_TIMEOUT_MS, maxRetries: 1 });
    }
    return this.client;
  }

  async decide(ctx: DecisionContext): Promise<Decision> {
    const client = this.getClient();
    const response = await client.messages.create({
      model: this.model,
      max_tokens: 1024,
      system: JOSH_SYSTEM_PROMPT,
      messages: [
        { role: "user", content: buildUserPrompt(ctx.request, ctx.memory, ctx.trigger) },
      ],
      tools: [
        {
          name: "emit_actions",
          description:
            "Emit Josh's actions for this game tick. Always call this tool; an empty actions array means Josh does nothing.",
          strict: true,
          input_schema: DECISION_SCHEMA as unknown as Anthropic.Tool.InputSchema,
        },
      ],
      tool_choice: { type: "tool", name: "emit_actions" },
    });

    const toolUse = response.content.find(
      (block): block is Anthropic.ToolUseBlock => block.type === "tool_use",
    );
    if (toolUse === undefined) {
      throw new Error(`claude: no tool_use block in response (stop_reason=${response.stop_reason})`);
    }
    const raw = toolUse.input as RawDecision;
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
    const response = await client.messages.create({
      model: this.model,
      max_tokens: 512,
      system: SUMMARIZER_SYSTEM_PROMPT,
      messages: [{ role: "user", content: buildSummaryPrompt(previousSummary, lines, playerName) }],
    });
    const text = response.content
      .filter((block): block is Anthropic.TextBlock => block.type === "text")
      .map((block) => block.text)
      .join(" ")
      .trim();
    if (text.length === 0) {
      throw new Error("claude: empty summary response");
    }
    return text.slice(0, 800);
  }
}
