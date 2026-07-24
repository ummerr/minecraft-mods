/**
 * LabsCraft v2 agent server entrypoint.
 */

import { loadConfig } from "./config";
import { MemoryStore } from "./memory";
import { ProviderChain } from "./providers/chain";
import { ClaudeProvider } from "./providers/claude";
import { GeminiProvider } from "./providers/gemini";
import { OllamaProvider } from "./providers/ollama";
import { StaticFallbackProvider } from "./providers/fallback";
import { createServer } from "./server";
import { TriggerEngine } from "./triggers";

function main(): void {
  const config = loadConfig(process.env.LABSCRAFT_CONFIG);

  const memory = new MemoryStore(config.memory.dbPath);
  const triggers = new TriggerEngine({
    cooldownsMs: config.triggers.cooldownsMs,
    evictAfterMs: config.triggers.evictAfterMs,
  });
  const chain = new ProviderChain([
    new ClaudeProvider(config.providers.claude.apiKey, config.providers.claude.model),
    new GeminiProvider(config.providers.gemini.apiKey, config.providers.gemini.model),
    new OllamaProvider(
      config.providers.ollama.enabled,
      config.providers.ollama.baseUrl,
      config.providers.ollama.model,
    ),
    new StaticFallbackProvider(),
  ]);

  const app = createServer({ memory, triggers, chain });
  const server = app.listen(config.port, () => {
    console.log(`[labscraft] agent server v2 listening on http://localhost:${config.port}`);
    console.log(`[labscraft] primary provider: ${chain.primaryName()}`);
  });

  const shutdown = (signal: string): void => {
    console.log(`[labscraft] received ${signal}, shutting down`);
    server.close(() => {
      memory.close();
      process.exit(0);
    });
    // Hard exit if close hangs (open keep-alive sockets).
    setTimeout(() => process.exit(0), 3000).unref();
  };
  process.on("SIGINT", () => shutdown("SIGINT"));
  process.on("SIGTERM", () => shutdown("SIGTERM"));
}

main();
