import { LLMProvider, LLMMessage, LLMResponse } from "./provider";
import { LLMFallbackConfig } from "../config";

export class OllamaProvider implements LLMProvider {
  readonly name = "ollama";
  private baseUrl: string;
  private model: string;

  constructor(config: LLMFallbackConfig) {
    this.baseUrl = config.base_url;
    this.model = config.model;
  }

  async complete(
    systemPrompt: string,
    messages: LLMMessage[],
    _maxTokens?: number,
    temperature?: number
  ): Promise<LLMResponse> {
    // Ollama uses /api/chat with OpenAI-compatible message format
    const ollamaMessages = [
      { role: "system", content: systemPrompt },
      ...messages
        .filter((m) => m.role !== "system")
        .map((m) => ({ role: m.role, content: m.content })),
    ];

    const response = await fetch(`${this.baseUrl}/api/chat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: this.model,
        messages: ollamaMessages,
        stream: false,
        options: {
          temperature: temperature ?? 0.7,
        },
      }),
    });

    if (!response.ok) {
      throw new Error(`Ollama returned ${response.status}: ${await response.text()}`);
    }

    const data = (await response.json()) as {
      message?: { content?: string };
      eval_count?: number;
      prompt_eval_count?: number;
    };

    return {
      content: data.message?.content ?? "",
      usage: {
        input_tokens: data.prompt_eval_count ?? 0,
        output_tokens: data.eval_count ?? 0,
      },
    };
  }

  async isAvailable(): Promise<boolean> {
    try {
      const response = await fetch(`${this.baseUrl}/api/tags`, {
        signal: AbortSignal.timeout(2000),
      });
      return response.ok;
    } catch {
      return false;
    }
  }
}
