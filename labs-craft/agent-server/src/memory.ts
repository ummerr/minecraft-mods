import Database from "better-sqlite3";
import { LLMProvider } from "./llm/provider";

const MEMORY_EXTRACTION_PROMPT = `You are analyzing a conversation between Josh Woodward (a PM at Google Labs) and a player in LabsCraft.

Based on this conversation, extract 0-3 short facts that Josh should remember about this player for future interactions. Focus on:
- Player preferences (e.g. "prefers exploration over combat")
- Notable achievements (e.g. "generated their first video on day 1")
- Player personality traits (e.g. "asks lots of questions", "very independent")
- Relevant context (e.g. "struggled with finding the crafting table")

Respond with ONLY a JSON array of strings. If there's nothing worth remembering, respond with an empty array [].

Example: ["Player prefers to explore before following instructions", "Completed first generation quickly"]`;

export async function extractMemories(
  playerUuid: string,
  recentConversation: string,
  db: Database.Database,
  llm: LLMProvider
): Promise<void> {
  try {
    const response = await llm.complete(
      MEMORY_EXTRACTION_PROMPT,
      [{ role: "user", content: recentConversation }],
      200,
      0.3 // Low temperature for factual extraction
    );

    let cleaned = response.content.trim();
    if (cleaned.startsWith("```")) {
      cleaned = cleaned
        .replace(/^```(?:json)?\s*\n?/, "")
        .replace(/\n?```\s*$/, "");
    }

    const memories: string[] = JSON.parse(cleaned);

    if (!Array.isArray(memories)) return;

    const now = Date.now();
    const insert = db.prepare(
      "INSERT INTO memories (player_uuid, memory, source, created_at) VALUES (?, ?, ?, ?)"
    );

    for (const memory of memories) {
      if (typeof memory === "string" && memory.length > 0) {
        // Check for duplicate/similar memories
        const existing = db
          .prepare(
            "SELECT id FROM memories WHERE player_uuid = ? AND memory = ?"
          )
          .get(playerUuid, memory);

        if (!existing) {
          insert.run(playerUuid, memory, "conversation", now);
          console.log(`[memory] Stored for ${playerUuid}: "${memory}"`);
        }
      }
    }

    // Prune old memories (keep max 20 per player)
    db.prepare(
      `DELETE FROM memories WHERE player_uuid = ? AND id NOT IN (
        SELECT id FROM memories WHERE player_uuid = ? ORDER BY created_at DESC LIMIT 20
      )`
    ).run(playerUuid, playerUuid);
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    console.warn(`[memory] Extraction failed: ${msg}`);
  }
}

export function getRecentConversationText(
  playerUuid: string,
  db: Database.Database,
  limit: number = 10
): string {
  const rows = db
    .prepare(
      `SELECT role, content FROM conversations
       WHERE player_uuid = ?
       ORDER BY timestamp DESC
       LIMIT ?`
    )
    .all(playerUuid, limit) as { role: string; content: string }[];

  rows.reverse();

  return rows
    .map((r) => {
      const speaker = r.role === "josh" ? "Josh" : "Player";
      return `${speaker}: ${r.content}`;
    })
    .join("\n");
}

export function getConversationCount(
  playerUuid: string,
  db: Database.Database
): number {
  const result = db
    .prepare(
      "SELECT COUNT(*) as count FROM conversations WHERE player_uuid = ?"
    )
    .get(playerUuid) as { count: number };
  return result.count;
}
