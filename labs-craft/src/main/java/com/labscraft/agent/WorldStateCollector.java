package com.labscraft.agent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.quest.QuestManager;
import com.labscraft.quest.QuestStage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldStateCollector {

    public static JsonObject collect(ServerPlayerEntity player, JoshWoodwardEntity josh) {
        JsonObject state = new JsonObject();
        state.addProperty("timestamp", System.currentTimeMillis() / 1000);

        state.add("player", collectPlayer(player));
        state.add("josh", collectJosh(josh, player));
        state.add("quest", collectQuest(player));
        state.add("world", collectWorld(player));
        state.add("recent_events", new JsonArray()); // Populated by event tracking

        return state;
    }

    private static JsonObject collectPlayer(ServerPlayerEntity player) {
        JsonObject p = new JsonObject();
        p.addProperty("name", player.getName().getString());

        JsonObject pos = new JsonObject();
        pos.addProperty("x", Math.round(player.getX()));
        pos.addProperty("y", Math.round(player.getY()));
        pos.addProperty("z", Math.round(player.getZ()));
        p.add("position", pos);

        p.addProperty("health", Math.round(player.getHealth()));
        p.addProperty("hunger", player.getHungerManager().getFoodLevel());

        // Inventory summary: group items by type with counts
        JsonArray inventory = new JsonArray();
        Map<String, Integer> itemCounts = new HashMap<>();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                itemCounts.merge(itemId, stack.getCount(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            inventory.add(entry.getKey() + " x" + entry.getValue());
        }
        p.add("inventory_summary", inventory);

        ItemStack held = player.getMainHandStack();
        if (!held.isEmpty()) {
            p.addProperty("held_item", Registries.ITEM.getId(held.getItem()).toString());
        } else {
            p.add("held_item", null);
        }

        p.addProperty("is_sneaking", player.isSneaking());

        // Biome
        BlockPos blockPos = player.getBlockPos();
        var biomeEntry = player.getWorld().getBiome(blockPos);
        String biomeName = biomeEntry.getKey()
                .map(key -> key.getValue().getPath())
                .orElse("unknown");
        p.addProperty("biome", biomeName);

        return p;
    }

    private static JsonObject collectJosh(JoshWoodwardEntity josh, ServerPlayerEntity player) {
        JsonObject j = new JsonObject();

        JsonObject pos = new JsonObject();
        pos.addProperty("x", Math.round(josh.getX()));
        pos.addProperty("y", Math.round(josh.getY()));
        pos.addProperty("z", Math.round(josh.getZ()));
        j.add("position", pos);

        j.addProperty("distance_to_player", Math.round(josh.distanceTo(player) * 10.0) / 10.0);
        j.addProperty("current_activity", "idle");
        j.addProperty("last_spoke_ticks_ago", josh.getTicksSinceLastSpoke());

        return j;
    }

    private static JsonObject collectQuest(ServerPlayerEntity player) {
        JsonObject q = new JsonObject();
        ServerWorld world = (ServerWorld) player.getWorld();
        QuestManager questManager = QuestManager.get(world);
        QuestStage stage = questManager.getStage(player);

        q.addProperty("current_stage", stage.name());

        // Build objectives based on stage
        JsonArray completed = new JsonArray();
        JsonArray remaining = new JsonArray();

        switch (stage) {
            case COMPLETED:
                completed.add("onboarded");
                completed.add("found_crafting_table");
                completed.add("first_generation");
                completed.add("reported_back");
                break;
            case FIRST_GENERATION:
                completed.add("onboarded");
                completed.add("found_crafting_table");
                completed.add("first_generation");
                remaining.add("report_back_to_josh");
                break;
            case LEARNING_PIPELINE:
                completed.add("onboarded");
                completed.add("found_crafting_table");
                remaining.add("generate_first_video");
                break;
            case FLOW_INTRO:
                completed.add("onboarded");
                remaining.add("find_crafting_table");
                remaining.add("generate_first_video");
                break;
            case NOT_STARTED:
                remaining.add("talk_to_josh");
                break;
        }

        q.add("objectives_completed", completed);
        q.add("objectives_remaining", remaining);
        q.addProperty("time_in_stage_minutes", 0); // Placeholder - would need stage timing

        return q;
    }

    private static JsonObject collectWorld(ServerPlayerEntity player) {
        JsonObject w = new JsonObject();
        ServerWorld world = (ServerWorld) player.getWorld();

        // Time of day
        long timeOfDay = world.getTimeOfDay() % 24000;
        String time;
        if (timeOfDay < 6000) time = "morning";
        else if (timeOfDay < 12000) time = "afternoon";
        else if (timeOfDay < 18000) time = "evening";
        else time = "night";
        w.addProperty("time_of_day", time);

        // Weather
        String weather;
        if (world.isThundering()) weather = "thunderstorm";
        else if (world.isRaining()) weather = "rain";
        else weather = "clear";
        w.addProperty("weather", weather);

        // Nearby entities (within 16 blocks)
        Box searchBox = player.getBoundingBox().expand(16.0);
        List<Entity> nearbyEntities = world.getOtherEntities(player, searchBox);
        Map<String, Integer> entityCounts = new HashMap<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity && !(entity instanceof JoshWoodwardEntity)) {
                String name = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
                entityCounts.merge(name, 1, Integer::sum);
            }
        }
        JsonArray entities = new JsonArray();
        for (Map.Entry<String, Integer> entry : entityCounts.entrySet()) {
            entities.add(entry.getKey() + " x" + entry.getValue());
        }
        w.add("nearby_entities", entities);

        // Nearby blocks of interest (simplified — checks for labscraft blocks in 8-block radius)
        JsonArray blocks = new JsonArray();
        BlockPos playerPos = player.getBlockPos();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos checkPos = playerPos.add(dx, dy, dz);
                    String blockId = Registries.BLOCK.getId(world.getBlockState(checkPos).getBlock()).toString();
                    if (blockId.startsWith("labscraft:")) {
                        blocks.add(blockId + " at " + checkPos.getX() + "," + checkPos.getY() + "," + checkPos.getZ());
                    }
                }
            }
        }
        w.add("nearby_blocks_of_interest", blocks);

        return w;
    }
}
