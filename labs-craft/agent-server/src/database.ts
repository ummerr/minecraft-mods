import Database from "better-sqlite3";
import * as path from "path";
import * as fs from "fs";
import { Config } from "./config";

let db: Database.Database;

export function initDatabase(config: Config): Database.Database {
  const dbPath = path.resolve(__dirname, "..", config.memory.db_path);
  const dbDir = path.dirname(dbPath);

  if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
  }

  db = new Database(dbPath);
  db.pragma("journal_mode = WAL");

  db.exec(`
    CREATE TABLE IF NOT EXISTS conversations (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      player_uuid TEXT NOT NULL,
      role TEXT NOT NULL CHECK(role IN ('player', 'josh', 'system')),
      content TEXT NOT NULL,
      timestamp INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS memories (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      player_uuid TEXT NOT NULL,
      memory TEXT NOT NULL,
      source TEXT,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS quest_state (
      player_uuid TEXT PRIMARY KEY,
      current_stage TEXT NOT NULL,
      completed_objectives TEXT DEFAULT '[]',
      started_at INTEGER,
      last_interaction INTEGER
    );

    CREATE INDEX IF NOT EXISTS idx_conversations_player
      ON conversations(player_uuid, timestamp);

    CREATE INDEX IF NOT EXISTS idx_memories_player
      ON memories(player_uuid, created_at);
  `);

  return db;
}

export function getDatabase(): Database.Database {
  if (!db) {
    throw new Error("Database not initialized. Call initDatabase() first.");
  }
  return db;
}
