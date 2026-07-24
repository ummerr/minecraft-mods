import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { MemoryStore, SUMMARIZE_THRESHOLD } from "../src/memory";

describe("MemoryStore", () => {
  let store: MemoryStore;

  beforeEach(() => {
    store = new MemoryStore(":memory:");
  });

  afterEach(() => {
    store.close();
  });

  it("records and returns conversation context per player", () => {
    store.recordPlayerMessage("u1", "hi josh", 1000);
    store.recordJoshMessage("u1", "Hey. Josh Woodward.", 2000);
    store.recordPlayerMessage("u2", "unrelated", 3000);

    const ctx = store.getContext("u1");
    expect(ctx.summary).toBeNull();
    expect(ctx.recent).toEqual([
      { role: "player", content: "hi josh" },
      { role: "josh", content: "Hey. Josh Woodward." },
    ]);
  });

  it("dedupes identical player lines recorded within the window (tick replays)", () => {
    store.recordPlayerMessage("u1", "hello", 1000);
    store.recordPlayerMessage("u1", "hello", 1500); // same event seen on next tick
    store.recordPlayerMessage("u1", "hello", 20_000); // far later — genuine repeat
    expect(store.getContext("u1").recent).toHaveLength(2);
  });

  it("signals summarization once enough unsummarized messages pile up", () => {
    for (let i = 0; i < SUMMARIZE_THRESHOLD; i++) {
      store.recordJoshMessage("u1", `line ${i}`, 1000 + i);
    }
    expect(store.needsSummarization("u1")).toBe(false);
    store.recordJoshMessage("u1", "one more", 5000);
    expect(store.needsSummarization("u1")).toBe(true);
  });

  it("applies a rolling summary and excludes summarized messages from the tail", () => {
    for (let i = 0; i < 30; i++) {
      store.recordPlayerMessage("u1", `msg ${i}`, 1000 + i * 20_000);
    }
    const batch = store.getMessagesForSummary("u1");
    expect(batch.length).toBeGreaterThan(0);
    const lastId = batch[batch.length - 1]!.id;

    store.applySummary("u1", "The intern asked about TPUs repeatedly.", lastId, 999_999);

    const ctx = store.getContext("u1");
    expect(ctx.summary).toBe("The intern asked about TPUs repeatedly.");
    // Recent tail must not include anything covered by the summary.
    expect(ctx.recent.every((m) => !batch.some((b) => b.content === m.content))).toBe(true);

    // Next summary batch starts after the previous summary watermark.
    const nextBatch = store.getMessagesForSummary("u1");
    expect(nextBatch.every((m) => m.id > lastId)).toBe(true);
  });
});
