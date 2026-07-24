import { afterAll, beforeAll, describe, expect, it } from "vitest";
import type { Server } from "http";
import { MemoryStore } from "../src/memory";
import { ProviderChain } from "../src/providers/chain";
import { StaticFallbackProvider } from "../src/providers/fallback";
import { createServer } from "../src/server";
import { TriggerEngine } from "../src/triggers";
import { makeTick } from "./fixtures";

describe("HTTP server (protocol v2)", () => {
  let server: Server;
  let baseUrl: string;
  let memory: MemoryStore;

  beforeAll(async () => {
    memory = new MemoryStore(":memory:");
    const app = createServer({
      memory,
      triggers: new TriggerEngine(),
      chain: new ProviderChain([new StaticFallbackProvider()]),
    });
    await new Promise<void>((resolve) => {
      server = app.listen(0, resolve);
    });
    const address = server.address();
    if (address === null || typeof address === "string") throw new Error("no port");
    baseUrl = `http://127.0.0.1:${address.port}`;
  });

  afterAll(async () => {
    await new Promise<void>((resolve, reject) =>
      server.close((err) => (err ? reject(err) : resolve())),
    );
    memory.close();
  });

  it("GET /health returns the protocol handshake", async () => {
    const res = await fetch(`${baseUrl}/health`);
    expect(res.status).toBe(200);
    expect(await res.json()).toEqual({
      status: "ok",
      protocol_version: 2,
      provider: "static",
    });
  });

  it("POST /tick responds to a chat message with valid actions and debug info", async () => {
    const tick = makeTick({
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hey josh, what now?" }],
    });
    const res = await fetch(`${baseUrl}/tick`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(tick),
    });
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      protocol_version: number;
      actions: Array<{ type: string }>;
      debug: { trigger: string; provider: string; latency_ms: number };
    };
    expect(body.protocol_version).toBe(2);
    expect(body.actions.length).toBeGreaterThan(0);
    expect(body.debug.trigger).toBe("chat_message");
    expect(body.debug.provider).toBe("static");
    expect(body.debug.latency_ms).toBeGreaterThanOrEqual(0);
  });

  it("POST /tick returns empty actions when no trigger fires", async () => {
    const tick = makeTick({
      player_uuid: "uuid-quiet-player",
      josh: { ...makeTick().josh, distance_to_player: 50, can_see_player: false },
    });
    const res = await fetch(`${baseUrl}/tick`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(tick),
    });
    expect(res.status).toBe(200);
    const body = (await res.json()) as { actions: unknown[]; debug: { trigger: string } };
    expect(body.actions).toEqual([]);
    expect(body.debug.trigger).toBe("none");
  });

  it("POST /tick rejects wrong protocol_version with 400", async () => {
    const tick = { ...makeTick(), protocol_version: 1 };
    const res = await fetch(`${baseUrl}/tick`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(tick),
    });
    expect(res.status).toBe(400);
  });

  it("POST /tick rejects malformed payloads with 400", async () => {
    const res = await fetch(`${baseUrl}/tick`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ protocol_version: 2 }),
    });
    expect(res.status).toBe(400);
  });

  it("records the exchange into memory (memory feeds future prompts)", async () => {
    const uuid = "uuid-memory-check";
    const tick = makeTick({
      player_uuid: uuid,
      recent_events: [{ type: "chat_message", seconds_ago: 1, text: "hello josh" }],
    });
    const res = await fetch(`${baseUrl}/tick`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(tick),
    });
    expect(res.status).toBe(200);
    const ctx = memory.getContext(uuid);
    expect(ctx.recent.some((m) => m.role === "player" && m.content === "hello josh")).toBe(true);
    expect(ctx.recent.some((m) => m.role === "josh")).toBe(true);
  });
});
