import Anthropic from "@anthropic-ai/sdk";
import { LLMProvider, LLMMessage, LLMResponse } from "./provider";
import { LLMConfig } from "../config";

export class ClaudeProvider implements LLMProvider {
  readonly name = "claude";
  private client: Anthropic;
  private model: string;
  private defaultMaxTokens: number;
  private defaultTemperature: number;

  constructor(config: LLMConfig) {
    const apiKey = process.env[config.api_key_env];
    if (!apiKey) {
      throw new Error(
        `Missing API key: set ${config.api_key_env} environment variable`
      );
    }

    this.client = new Anthropic({ apiKey });
    this.model = config.model;
    this.defaultMaxTokens = config.max_tokens;
    this.defaultTemperature = config.temperature;
  }

  async complete(
    systemPrompt: string,
    messages: LLMMessage[],
    maxTokens?: number,
    temperature?: number
  ): Promise<LLMResponse> {
    // Convert messages to Anthropic format (no "system" role in messages array)
    const anthropicMessages = messages
      .filter((m) => m.role !== "system")
      .map((m) => ({
        role: m.role as "user" | "assistant",
        content: m.content,
      }));

    const response = await this.client.messages.create({
      model: this.model,
      max_tokens: maxTokens ?? this.defaultMaxTokens,
      temperature: temperature ?? this.defaultTemperature,
      system: systemPrompt,
      messages: anthropicMessages,
    });

    const textContent = response.content.find((block) => block.type === "text");
    const content = textContent ? textContent.text : "";

    return {
      content,
      usage: {
        input_tokens: response.usage.input_tokens,
        output_tokens: response.usage.output_tokens,
      },
    };
  }

  async isAvailable(): Promise<boolean> {
    try {
      const apiKey = process.env["ANTHROPIC_API_KEY"];
      return !!apiKey;
    } catch {
      return false;
    }
  }
}
