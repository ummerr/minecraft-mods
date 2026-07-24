/**
 * Provider chain — priority order Claude → Gemini → Ollama → static fallback.
 * First configured provider that succeeds wins; failures log and fall through.
 * The static fallback is always configured and never throws, so decide()
 * always resolves.
 */

import type { AgentProvider, Decision, DecisionContext } from "./types";

export interface ChainDecision extends Decision {
  provider: string;
}

export class ProviderChain {
  private readonly providers: readonly AgentProvider[];

  constructor(providers: readonly AgentProvider[]) {
    if (providers.length === 0) {
      throw new Error("ProviderChain requires at least one provider");
    }
    this.providers = providers;
  }

  /** Name of the highest-priority configured provider (reported by /health). */
  primaryName(): string {
    for (const p of this.providers) {
      if (p.isConfigured()) return p.name;
    }
    return "none";
  }

  async decide(ctx: DecisionContext): Promise<ChainDecision> {
    let lastError: unknown = null;
    for (const provider of this.providers) {
      if (!provider.isConfigured()) continue;
      try {
        const decision = await provider.decide(ctx);
        return { ...decision, provider: provider.name };
      } catch (err) {
        lastError = err;
        console.warn(
          `[provider:${provider.name}] decide failed, falling through: ${err instanceof Error ? err.message : String(err)}`,
        );
      }
    }
    // Unreachable when the static fallback is in the chain, but keep a sane error.
    throw new Error(
      `all providers failed (last: ${lastError instanceof Error ? lastError.message : String(lastError)})`,
    );
  }

  async summarize(
    previousSummary: string | null,
    lines: Array<{ role: "player" | "josh"; content: string }>,
    playerName: string,
  ): Promise<string | null> {
    for (const provider of this.providers) {
      if (!provider.isConfigured() || provider.summarize === undefined) continue;
      try {
        return await provider.summarize(previousSummary, lines, playerName);
      } catch (err) {
        console.warn(
          `[provider:${provider.name}] summarize failed, falling through: ${err instanceof Error ? err.message : String(err)}`,
        );
      }
    }
    return null;
  }
}
