export interface LLMMessage {
  role: "system" | "user" | "assistant";
  content: string;
}

export interface LLMResponse {
  content: string;
  usage?: {
    input_tokens: number;
    output_tokens: number;
  };
}

export interface LLMProvider {
  readonly name: string;

  complete(
    systemPrompt: string,
    messages: LLMMessage[],
    maxTokens?: number,
    temperature?: number
  ): Promise<LLMResponse>;

  isAvailable(): Promise<boolean>;
}
