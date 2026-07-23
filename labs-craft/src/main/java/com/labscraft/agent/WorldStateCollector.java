package com.labscraft.agent;

import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.quest.QuestManager;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the exact PROTOCOL-V2 {@code POST /tick} payload: player block, josh
 * block (incl. {@code can_see_player} via the entity visibility raycast),
 * quest block (delegated verbatim to {@link QuestManager#toWireJson}), world
 * block ({@code @distance}-suffixed nearby entities/blocks), and
 * {@code recent_events} from the {@link RecentEventsTracker}.
 *
 * <p>Runs on the server thread; the resulting String is handed to the async
 * HTTP layer, so nothing here ever blocks on I/O.</p>
 */
public final class WorldStateCollector {

    /** Radius for nearby_entities. */
    private static final double ENTITY_RADIUS = 16.0;

    /** Radius (cube) for nearby_blocks_of_interest scanning. */
    private static final int BLOCK_RADIUS = 8;

    private static final int MAX_NEARBY_ENTITIES = 12;
    private static final int MAX_INVENTORY_LINES = 12;

    private WorldStateCollector() {
    }

    public static String buildPayload(ServerPlayerEntity player, JoshWoodwardEntity josh,
            String sessionId, List<RecentEventsTracker.Event> events, long nowMs) {
        ServerWorld world = player.getServerWorld();
        StringBuilder sb = new StringBuilder(1600);
        sb.append("{\"protocol_version\":2")
                .append(",\"session_id\":\"").append(JsonLite.escape(sessionId)).append('"')
                .append(",\"player_uuid\":\"").append(player.getUuid()).append('"')
                .append(",\"timestamp_ms\":").append(nowMs);

        appendPlayer(sb, player, world);
        appendJosh(sb, player, josh);
        sb.append(",\"quest\":").append(QuestManager.toWireJson(player));
        appendWorld(sb, player, josh, world);
        appendRecentEvents(sb, events, nowMs);

        return sb.append('}').toString();
    }

    // ------------------------------------------------------------------
    // player
    // ------------------------------------------------------------------

    private static void appendPlayer(StringBuilder sb, ServerPlayerEntity player, ServerWorld world) {
        sb.append(",\"player\":{\"name\":\"").append(JsonLite.escape(player.getName().getString())).append('"')
                .append(",\"position\":").append(position(player.getX(), player.getY(), player.getZ()))
                .append(",\"health\":").append(oneDecimal(player.getHealth()))
                .append(",\"max_health\":").append(oneDecimal(player.getMaxHealth()))
                .append(",\"hunger\":").append(player.getHungerManager().getFoodLevel())
                .append(",\"inventory_summary\":").append(inventorySummary(player))
                .append(",\"held_item\":\"")
                .append(JsonLite.escape(itemId(player.getMainHandStack()))).append('"')
                .append(",\"is_sneaking\":").append(player.isSneaking())
                .append(",\"biome\":\"").append(JsonLite.escape(biomeId(player, world))).append('"')
                .append(",\"dimension\":\"")
                .append(JsonLite.escape(world.getRegistryKey().getValue().toString())).append("\"}");
    }

    private static String inventorySummary(ServerPlayerEntity player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                counts.merge(itemId(stack), stack.getCount(), Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> top = new ArrayList<>(counts.entrySet());
        top.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        StringBuilder sb = new StringBuilder("[");
        int emitted = 0;
        for (Map.Entry<String, Integer> entry : top) {
            if (emitted >= MAX_INVENTORY_LINES) {
                break;
            }
            if (emitted++ > 0) {
                sb.append(',');
            }
            sb.append('"').append(JsonLite.escape(entry.getKey() + " x" + entry.getValue())).append('"');
        }
        return sb.append(']').toString();
    }

    private static String itemId(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString();
    }

    private static String biomeId(ServerPlayerEntity player, ServerWorld world) {
        return world.getBiome(player.getBlockPos()).getKey()
                .map(key -> key.getValue().toString())
                .orElse("minecraft:plains");
    }

    // ------------------------------------------------------------------
    // josh
    // ------------------------------------------------------------------

    private static void appendJosh(StringBuilder sb, ServerPlayerEntity player, JoshWoodwardEntity josh) {
        sb.append(",\"josh\":{\"position\":").append(position(josh.getX(), josh.getY(), josh.getZ()))
                .append(",\"distance_to_player\":").append(oneDecimal(josh.distanceTo(player)))
                .append(",\"current_activity\":\"").append(josh.getCurrentActivity()).append('"')
                .append(",\"last_spoke_seconds_ago\":").append(josh.secondsSinceLastSpoke())
                .append(",\"can_see_player\":").append(josh.canSee(player))
                .append('}');
    }

    // ------------------------------------------------------------------
    // world
    // ------------------------------------------------------------------

    private static void appendWorld(StringBuilder sb, ServerPlayerEntity player,
            JoshWoodwardEntity josh, ServerWorld world) {
        sb.append(",\"world\":{\"time_of_day\":\"").append(timeOfDay(world)).append('"')
                .append(",\"weather\":\"").append(weather(world)).append('"')
                .append(",\"nearby_entities\":").append(nearbyEntities(player, josh, world))
                .append(",\"nearby_blocks_of_interest\":").append(nearbyBlocksOfInterest(player, world))
                .append('}');
    }

    static String timeOfDayName(long timeOfDay) {
        long t = timeOfDay % 24000L;
        if (t < 0) {
            t += 24000L;
        }
        if (t >= 23000L) {
            return "sunrise";
        }
        if (t < 12000L) {
            return "day";
        }
        if (t < 13500L) {
            return "sunset";
        }
        return "night";
    }

    private static String timeOfDay(ServerWorld world) {
        return timeOfDayName(world.getTimeOfDay());
    }

    private static String weather(ServerWorld world) {
        if (world.isThundering()) {
            return "thunder";
        }
        return world.isRaining() ? "rain" : "clear";
    }

    private static String nearbyEntities(ServerPlayerEntity player, JoshWoodwardEntity josh, ServerWorld world) {
        Box box = player.getBoundingBox().expand(ENTITY_RADIUS);
        List<Entity> entities = world.getOtherEntities(player, box,
                e -> e != josh && e.isAlive() && !(e instanceof JoshWoodwardEntity));
        entities.sort(Comparator.comparingDouble(player::distanceTo));
        StringBuilder sb = new StringBuilder("[");
        int emitted = 0;
        for (Entity entity : entities) {
            if (emitted >= MAX_NEARBY_ENTITIES) {
                break;
            }
            double distance = player.distanceTo(entity);
            if (distance > ENTITY_RADIUS) {
                continue;
            }
            if (emitted++ > 0) {
                sb.append(',');
            }
            sb.append('"')
                    .append(JsonLite.escape(Registries.ENTITY_TYPE.getId(entity.getType()).toString()))
                    .append('@').append(oneDecimal(distance)).append('"');
        }
        return sb.append(']').toString();
    }

    /**
     * Scans a cube around the player for labscraft blocks of interest
     * (ores, consoles, the crafting table) and reports the nearest instance of
     * each id as {@code "id@distance"}.
     */
    private static String nearbyBlocksOfInterest(ServerPlayerEntity player, ServerWorld world) {
        BlockPos center = player.getBlockPos();
        Map<String, Double> nearestById = new LinkedHashMap<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -BLOCK_RADIUS; dx <= BLOCK_RADIUS; dx++) {
            for (int dy = -BLOCK_RADIUS; dy <= BLOCK_RADIUS; dy++) {
                for (int dz = -BLOCK_RADIUS; dz <= BLOCK_RADIUS; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    Block block = world.getBlockState(pos).getBlock();
                    String id = Registries.BLOCK.getId(block).toString();
                    if (!id.startsWith("labscraft:")) {
                        continue;
                    }
                    double distance = Math.sqrt(center.getSquaredDistance(pos));
                    nearestById.merge(id, distance, Math::min);
                }
            }
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, Double> entry : nearestById.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(JsonLite.escape(entry.getKey()))
                    .append('@').append(oneDecimal(entry.getValue())).append('"');
        }
        return sb.append(']').toString();
    }

    // ------------------------------------------------------------------
    // recent_events
    // ------------------------------------------------------------------

    private static void appendRecentEvents(StringBuilder sb, List<RecentEventsTracker.Event> events, long nowMs) {
        sb.append(",\"recent_events\":[");
        boolean first = true;
        for (RecentEventsTracker.Event event : events) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"type\":\"").append(JsonLite.escape(event.type()))
                    .append("\",\"seconds_ago\":").append(event.secondsAgo(nowMs));
            for (Map.Entry<String, String> field : event.fields().entrySet()) {
                sb.append(",\"").append(JsonLite.escape(field.getKey()))
                        .append("\":\"").append(JsonLite.escape(field.getValue())).append('"');
            }
            sb.append('}');
        }
        sb.append(']');
    }

    // ------------------------------------------------------------------
    // formatting helpers
    // ------------------------------------------------------------------

    private static String position(double x, double y, double z) {
        return "{\"x\":" + twoDecimals(x) + ",\"y\":" + twoDecimals(y) + ",\"z\":" + twoDecimals(z) + "}";
    }

    private static String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
