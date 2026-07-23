/**
 * SQLite-backed conversation memory, keyed by player_uuid.
 *
 * Two tables:
 *  - messages: raw player/Josh exchanges
 *  - summaries: one rolling summary per player covering everything up to
 *    last_message_id (produced by the conversation summarizer)
 *
 * getContext() returns the rolling summary plus the unsummarized tail, and is
 * fed back into the LLM prompt so memory actually informs behavior.
 */

import Database from "better-sqlite3";
import * as fs from "fs";
import * as path from "path";

export type MemoryRole = "player" | "josh";

export interface MemoryMessage {
  id: number;
  role: MemoryRole;
  content: string;
  created_at: number;
}

export interface MemoryContext {
  summary: string | null;
  recent: Array<{ role: MemoryRole; content: string }>;
}

const RECENT_LIMIT = 12;
/** When more than this many unsummarized messages accumulate, summarize. */
export const SUMMARIZE_THRESHOLD = 24;
/** How many of the oldest unsummarized messages get folded into the summary. */
export const SUMMARIZE_BATCH = 16;
/** Skip duplicate player lines recorded within this window (tick replays). */
const DEDUPE_WINDOW_MS = 10_000;

export class MemoryStore {
  private readonly db: Database.Database;

  constructor(dbPath: string) {
    if (dbPath !== ":memory:") {
      fs.mkdirSync(path.dirname(path.resolve(dbPath)), { recursive: true });
    }
    this.db = new Database(dbPath);
    this.db.pragma("journal_mode = WAL");
    this.db.exec(`
      CREATE TABLE IF NOT EXISTS messages (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        player_uuid TEXT NOT NULL,
        role TEXT NOT NULL,
        content TEXT NOT NULL,
        created_at INTEGER NOT NULL
      );
      CREATE INDEX IF NOT EXISTS idx_messages_player ON messages(player_uuid, id);
      CREATE TABLE IF NOT EXISTS summaries (
        player_uuid TEXT PRIMARY KEY,
        summary TEXT NOT NULL,
        last_message_id INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      );
    `);
  }

  /** Record a chat line from the player. Dedupes identical recent lines. */
  recordPlayerMessage(playerUuid: string, content: string, nowMs: number = Date.now()): void {
    const last = this.db
      .prepare(
        `SELECT content, created_at FROM messages
         WHERE player_uuid = ? AND role = 'player'
         ORDER BY id DESC LIMIT 1`,
      )
      .get(playerUuid) as { content: string; created_at: number } | undefined;
    if (
      last !== undefined &&
      last.content === content &&
      nowMs - last.created_at < DEDUPE_WINDOW_MS
    ) {
      return;
    }
    this.insert(playerUuid, "player", content, nowMs);
  }

  /** Record something Josh said. */
  recordJoshMessage(playerUuid: string, content: string, nowMs: number = Date.now()): void {
    this.insert(playerUuid, "josh", content, nowMs);
  }

  private insert(playerUuid: string, role: MemoryRole, content: string, nowMs: number): void {
    this.db
      .prepare(
        "INSERT INTO messages (player_uuid, role, content, created_at) VALUES (?, ?, ?, ?)",
      )
      .run(playerUuid, role, content, nowMs);
  }

  /** Summary + unsummarized recent tail for prompt construction. */
  getContext(playerUuid: string): MemoryContext {
    const summaryRow = this.db
      .prepare("SELECT summary, last_message_id FROM summaries WHERE player_uuid = ?")
      .get(playerUuid) as { summary: string; last_message_id: number } | undefined;
    const afterId = summaryRow?.last_message_id ?? 0;
    const rows = this.db
      .prepare(
        `SELECT role, content FROM messages
         WHERE player_uuid = ? AND id > ?
         ORDER BY id DESC LIMIT ?`,
      )
      .all(playerUuid, afterId, RECENT_LIMIT) as Array<{ role: MemoryRole; content: string }>;
    rows.reverse();
    return { summary: summaryRow?.summary ?? null, recent: rows };
  }

  /** True when enough unsummarized history has piled up to warrant a summary pass. */
  needsSummarization(playerUuid: string): boolean {
    return this.unsummarizedCount(playerUuid) > SUMMARIZE_THRESHOLD;
  }

  private unsummarizedCount(playerUuid: string): number {
    const summaryRow = this.db
      .prepare("SELECT last_message_id FROM summaries WHERE player_uuid = ?")
      .get(playerUuid) as { last_message_id: number } | undefined;
    const afterId = summaryRow?.last_message_id ?? 0;
    const row = this.db
      .prepare("SELECT COUNT(*) AS n FROM messages WHERE player_uuid = ? AND id > ?")
      .get(playerUuid, afterId) as { n: number };
    return row.n;
  }

  /** Oldest unsummarized messages (the batch that a summary pass should fold in). */
  getMessagesForSummary(playerUuid: string): MemoryMessage[] {
    const summaryRow = this.db
      .prepare("SELECT summary, last_message_id FROM summaries WHERE player_uuid = ?")
      .get(playerUuid) as { summary: string; last_message_id: number } | undefined;
    const afterId = summaryRow?.last_message_id ?? 0;
    return this.db
      .prepare(
        `SELECT id, role, content, created_at FROM messages
         WHERE player_uuid = ? AND id > ?
         ORDER BY id ASC LIMIT ?`,
      )
      .all(playerUuid, afterId, SUMMARIZE_BATCH) as MemoryMessage[];
  }

  /** Current rolling summary, if any (input to the next summary pass). */
  getSummary(playerUuid: string): string | null {
    const row = this.db
      .prepare("SELECT summary FROM summaries WHERE player_uuid = ?")
      .get(playerUuid) as { summary: string } | undefined;
    return row?.summary ?? null;
  }

  /** Store the new rolling summary covering messages up to and including upToId. */
  applySummary(playerUuid: string, summary: string, upToId: number, nowMs: number = Date.now()): void {
    this.db
      .prepare(
        `INSERT INTO summaries (player_uuid, summary, last_message_id, updated_at)
         VALUES (?, ?, ?, ?)
         ON CONFLICT(player_uuid) DO UPDATE SET
           summary = excluded.summary,
           last_message_id = excluded.last_message_id,
           updated_at = excluded.updated_at`,
      )
      .run(playerUuid, summary, upToId, nowMs);
  }

  close(): void {
    this.db.close();
  }
}
