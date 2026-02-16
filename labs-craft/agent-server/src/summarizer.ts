import Database from "better-sqlite3";
import { LLMProvider } from "./llm/provider";

const SUMMARIZE_PROMPT = `You are summarizing a conversation history between Josh Woodward (a PM at Google Labs) and a Minecraft player in LabsCraft.

Condense this conversation into a brief summary paragraph (3-5 sentences) that captures:
- Key topics discussed
- Quest progress made
- Any important player actions or decisions
- Josh's current relationship/attitude toward the player

Respond with ONLY the summary paragraph, no formatting or labels.`;

export async function summarizeIfNeeded(
  playerUuid: string,
  db: Database.Database,
  llm: LLMProvider,
  threshold: number
): Promise<boolean> {
  // Count unsummarized conversations
  const countResult = db
    .prepare(
      `SELECT COUNT(*) as count FROM conversations
       WHERE player_uuid = ? AND role != 'system'`
    )
    .get(playerUuid) as { count: number };

  if (countResult.count < threshold) {
    return false;
  }

  // Get all conversations except the most recent 5 (keep those intact)
  const oldRows = db
    .prepare(
      `SELECT id, role, content FROM conversations
       WHERE player_uuid = ? AND role != 'system'
       ORDER BY timestamp ASC`
    )
    .all(playerUuid) as { id: number; role: string; content: string }[];

  // Keep the 5 most recent, summarize the rest
  const toSummarize = oldRows.slice(0, -5);
  const keepIds = oldRows.slice(-5).map((r) => r.id);

  if (toSummarize.length < 10) {
    return false; // Not enough to bother summarizing
  }

  const conversationText = toSummarize
    .map((r) => {
      const speaker = r.role === "josh" ? "Josh" : "Player";
      return `${speaker}: ${r.content}`;
    })
    .join("\n");

  try {
    const response = await llm.complete(
      SUMMARIZE_PROMPT,
      [{ role: "user", content: conversationText }],
      300,
      0.3
    );

    const summary = response.content.trim();
    if (!summary) return false;

    // Delete old conversations and insert summary
    const idsToDelete = toSummarize.map((r) => r.id);
    const placeholders = idsToDelete.map(() => "?").join(",");

    db.prepare(
      `DELETE FROM conversations WHERE id IN (${placeholders})`
    ).run(...idsToDelete);

    db.prepare(
      "INSERT INTO conversations (player_uuid, role, content, timestamp) VALUES (?, ?, ?, ?)"
    ).run(
      playerUuid,
      "system",
      `[Previous conversation summary] ${summary}`,
      // Timestamp just before the oldest kept message
      Date.now() - 1
    );

    console.log(
      `[summarizer] Compressed ${idsToDelete.length} messages into summary for ${playerUuid}`
    );
    return true;
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : String(e);
    console.warn(`[summarizer] Failed: ${msg}`);
    return false;
  }
}
