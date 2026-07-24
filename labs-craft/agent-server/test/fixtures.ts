import type { TickRequest } from "../src/protocol";

export function makeTick(overrides: Partial<TickRequest> = {}): TickRequest {
  const base: TickRequest = {
    protocol_version: 2,
    session_id: "session-1",
    player_uuid: "uuid-player-1",
    timestamp_ms: 1_737_000_000_000,
    player: {
      name: "Steve",
      position: { x: 0, y: 64, z: 0 },
      health: 20,
      max_health: 20,
      hunger: 20,
      inventory_summary: ["labscraft:tpu x5"],
      held_item: "labscraft:tpu",
      is_sneaking: false,
      biome: "minecraft:plains",
      dimension: "minecraft:overworld",
    },
    josh: {
      position: { x: 3, y: 64, z: 0 },
      distance_to_player: 3.2,
      current_activity: "idle",
      last_spoke_seconds_ago: 42,
      can_see_player: true,
    },
    quest: {
      current_stage: "FLOW_INTRO",
      objectives: [
        { id: "mine_tpu_ore", description: "Mine 3 TPU Ore", done: false, progress: 1, goal: 3 },
      ],
      seconds_in_stage: 30,
    },
    world: {
      time_of_day: "day",
      weather: "clear",
      nearby_entities: [],
      nearby_blocks_of_interest: [],
    },
    recent_events: [],
  };
  return { ...base, ...overrides };
}
