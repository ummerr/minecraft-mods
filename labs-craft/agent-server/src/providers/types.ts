import type { Action, TickRequest } from "../protocol";
import type { MemoryContext } from "../memory";
import type { TriggerResult } from "../triggers";

export interface DecisionContext {
  request: TickRequest;
  trigger: TriggerResult;
  memory: MemoryContext;
}

export interface Decision {
  actions: Action[];
  reasoning: string;
}

export interface AgentProvider {
  readonly name: string;
  /** Cheap, local check: is this provider configured at all (key present etc.)? */
  isConfigured(): boolean;
  /** Produce Josh's actions for this tick. May throw; the chain falls through. */
  decide(ctx: DecisionContext): Promise<Decision>;
  /** Optional: summarize conversation lines for the rolling memory summary. */
  summarize?(
    previousSummary: string | null,
    lines: Array<{ role: "player" | "josh"; content: string }>,
    playerName: string,
  ): Promise<string>;
}
