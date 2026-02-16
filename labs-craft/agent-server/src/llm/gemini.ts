import { GoogleGenAI } from "@google/genai";
import { LLMProvider, LLMMessage, LLMResponse } from "./provider";
import { LLMConfig } from "../config";

export class GeminiProvider implements LLMProvider {
  readonly name = "gemini";
  private client: GoogleGenAI;
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

    this.client = new GoogleGenAI({ apiKey });
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
    // Build contents array from messages (skip system role — passed separately)
    const contents = messages
      .filter((m) => m.role !== "system")
      .map((m) => ({
        role: m.role === "assistant" ? ("model" as const) : ("user" as const),
        parts: [{ text: m.content }],
      }));

    const response = await this.client.models.generateContent({
      model: this.model,
      contents,
      config: {
        systemInstruction: systemPrompt,
        maxOutputTokens: maxTokens ?? this.defaultMaxTokens,
        temperature: temperature ?? this.defaultTemperature,
      },
    });

    const content = response.text ?? "";

    return {
      content,
      usage: {
        input_tokens: response.usageMetadata?.promptTokenCount ?? 0,
        output_tokens: response.usageMetadata?.candidatesTokenCount ?? 0,
      },
    };
  }

  async isAvailable(): Promise<boolean> {
    try {
      const apiKey = process.env["GEMINI_API_KEY"];
      return !!apiKey;
    } catch {
      return false;
    }
  }
}
