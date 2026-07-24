/**
 * Ollama provider — local models via the /api/chat endpoint with a JSON-schema
 * `format` constraint (Ollama structured outputs). No API key needed.
 */

import { validateActions } from "../actions";
import { buildSummaryPrompt, buildUserPrompt, JOSH_SYSTEM_PROMPT, SUMMARIZER_SYSTEM_PROMPT } from "../prompt";
import { DECISION_SCHEMA, type RawDecision } from "./schema";
import type { AgentProvider, Decision, DecisionContext } from "./types";

const REQUEST_TIMEOUT_MS = 20_000;

interface OllamaChatResponse {
  message?: { content?: string };
}

export class OllamaProvider implements AgentProvider {
  readonly name = "ollama";
  private readonly enabled: boolean;
  private readonly baseUrl: string;
  private readonly model: string;

  constructor(enabled: boolean, baseUrl: string, model: string) {
    this.enabled = enabled;
    this.baseUrl = baseUrl.replace(/\/+$/, "");
    this.model = model;
  }

  isConfigured(): boolean {
    return this.enabled && this.baseUrl.length > 0 && this.model.length > 0;
  }

  private async chat(system: string, user: string, format?: unknown): Promise<string> {
    const response = await fetch(`${this.baseUrl}/api/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: this.model,
        stream: false,
        messages: [
          { role: "system", content: system },
          { role: "user", content: user },
        ],
        ...(format !== undefined ? { format } : {}),
        options: { temperature: 0.7 },
      }),
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
    if (!response.ok) {
      throw new Error(`ollama: HTTP ${response.status}`);
    }
    const data = (await response.json()) as OllamaChatResponse;
    const content = data.message?.content;
    if (content === undefined || content.length === 0) {
      throw new Error("ollama: empty response");
    }
    return content;
  }

  async decide(ctx: DecisionContext): Promise<Decision> {
    const content = await this.chat(
      JOSH_SYSTEM_PROMPT,
      buildUserPrompt(ctx.request, ctx.memory, ctx.trigger),
      DECISION_SCHEMA,
    );
    // Schema-constrained structured output — the whole message is one JSON doc.
    const raw = JSON.parse(content) as RawDecision;
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
    const text = (
      await this.chat(SUMMARIZER_SYSTEM_PROMPT, buildSummaryPrompt(previousSummary, lines, playerName))
    ).trim();
    if (text.length === 0) {
      throw new Error("ollama: empty summary response");
    }
    return text.slice(0, 800);
  }
}
