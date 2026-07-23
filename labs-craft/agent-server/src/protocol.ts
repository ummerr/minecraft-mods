/**
 * Wire types for PROTOCOL-V2.md — the frozen contract shared with the Fabric mod.
 * Field names here must match the protocol document exactly. Do not rename.
 */

export const PROTOCOL_VERSION = 2 as const;

export interface Position {
  x: number;
  y: number;
  z: number;
}

export interface PlayerState {
  name: string;
  position: Position;
  health: number;
  max_health: number;
  hunger: number;
  inventory_summary: string[];
  held_item: string;
  is_sneaking: boolean;
  biome: string;
  dimension: string;
}

export type JoshActivity = "idle" | "walking" | "talking";

export interface JoshState {
  position: Position;
  distance_to_player: number;
  current_activity: JoshActivity;
  last_spoke_seconds_ago: number;
  can_see_player: boolean;
}

export interface QuestObjective {
  id: string;
  description: string;
  done: boolean;
  progress: number;
  goal: number;
}

export interface QuestState {
  current_stage: string;
  objectives: QuestObjective[];
  seconds_in_stage: number;
}

export type TimeOfDay = "day" | "sunset" | "night" | "sunrise";
export type Weather = "clear" | "rain" | "thunder";

export interface WorldInfo {
  time_of_day: TimeOfDay;
  weather: Weather;
  nearby_entities: string[];
  nearby_blocks_of_interest: string[];
}

export type RecentEventType =
  | "chat_message"
  | "interaction"
  | "block_mined"
  | "item_crafted"
  | "objective_completed"
  | "stage_changed"
  | "player_died"
  | "generation_completed";

export interface RecentEvent {
  type: RecentEventType;
  seconds_ago: number;
  text?: string;
  block?: string;
  item?: string;
  from?: string;
  to?: string;
}

export interface TickRequest {
  protocol_version: number;
  session_id: string;
  player_uuid: string;
  timestamp_ms: number;
  player: PlayerState;
  josh: JoshState;
  quest: QuestState;
  world: WorldInfo;
  recent_events: RecentEvent[];
}

// ---------------------------------------------------------------------------
// Actions (server → mod)
// ---------------------------------------------------------------------------

export type EmoteKind = "nod" | "shake_head" | "shrug" | "point" | "facepalm" | "clap";

export interface SayAction {
  type: "SAY";
  text: string;
  delay_ticks: number;
}

export interface WalkToAction {
  type: "WALK_TO";
  target?: "player";
  position?: Position;
  speed: number;
  delay_ticks: number;
}

export interface LookAtAction {
  type: "LOOK_AT";
  target?: "player";
  position?: Position;
  delay_ticks: number;
}

export interface EmoteAction {
  type: "EMOTE";
  emote: EmoteKind;
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

export interface CompleteObjectiveAction {
  type: "COMPLETE_OBJECTIVE";
  objective_id: string;
  delay_ticks: number;
}

export interface WaitAction {
  type: "WAIT";
  delay_ticks: number;
}

export type Action =
  | SayAction
  | WalkToAction
  | LookAtAction
  | EmoteAction
  | GiveItemAction
  | AdvanceQuestAction
  | CompleteObjectiveAction
  | WaitAction;

export type TriggerName =
  | "chat_message"
  | "interaction"
  | "quest_completed"
  | "danger"
  | "idle_near_objective"
  | "periodic_proximity";

export interface TickResponseDebug {
  trigger: string;
  reasoning: string;
  latency_ms: number;
  provider: string;
}

export interface TickResponse {
  protocol_version: typeof PROTOCOL_VERSION;
  actions: Action[];
  debug?: TickResponseDebug;
}
