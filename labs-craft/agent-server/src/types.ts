// ── WorldState: sent from the Fabric mod to the agent server each tick ──

export interface Position {
  x: number;
  y: number;
  z: number;
}

export interface PlayerState {
  name: string;
  position: Position;
  health: number;
  hunger: number;
  inventory_summary: string[];
  held_item: string | null;
  is_sneaking: boolean;
  biome: string;
}

export interface JoshState {
  position: Position;
  distance_to_player: number;
  current_activity: "idle" | "walking" | "talking";
  last_spoke_ticks_ago: number;
}

export interface QuestState {
  current_stage: string;
  objectives_completed: string[];
  objectives_remaining: string[];
  time_in_stage_minutes: number;
}

export interface RecentEvent {
  type: string;
  ago_seconds: number;
  [key: string]: unknown; // block, from, text, etc.
}

export interface WorldInfo {
  time_of_day: string;
  weather: string;
  nearby_entities: string[];
  nearby_blocks_of_interest: string[];
}

export interface WorldState {
  timestamp: number;
  player: PlayerState;
  josh: JoshState;
  quest: QuestState;
  world: WorldInfo;
  recent_events: RecentEvent[];
}

// ── Agent Actions: returned from agent server → Fabric mod ──

export type EmoteType = "nod" | "shake_head" | "shrug" | "point";

export interface SayAction {
  type: "SAY";
  text: string;
  delay_ticks: number;
}

export interface WalkToAction {
  type: "WALK_TO";
  position: Position;
  delay_ticks: number;
}

export interface EmoteAction {
  type: "EMOTE";
  emote: EmoteType;
  delay_ticks: number;
}

export interface GiveItemAction {
  type: "GIVE_ITEM";
  item: string;
  quantity: number;
  delay_ticks: number;
}

export interface AdvanceQuestAction {
  type: "ADVANCE_QUEST";
  delay_ticks: number;
}

export interface WaitAction {
  type: "WAIT";
  delay_ticks: number;
}

export type AgentAction =
  | SayAction
  | WalkToAction
  | EmoteAction
  | GiveItemAction
  | AdvanceQuestAction
  | WaitAction;

// ── Response envelope ──

export interface AgentResponse {
  actions: AgentAction[];
  debug?: {
    trigger: string;
    reasoning?: string;
    latency_ms: number;
  };
}
