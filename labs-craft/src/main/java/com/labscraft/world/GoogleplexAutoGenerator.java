package com.labscraft.world;

import com.labscraft.LabsCraft;
import com.labscraft.entity.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GoogleplexAutoGenerator {

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(GoogleplexAutoGenerator::onServerStarted);
    }

    private static void onServerStarted(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return;

        GoogleplexState state = GoogleplexState.get(overworld);
        if (state.isGenerated()) {
            LabsCraft.LOGGER.info("Googleplex already generated, skipping.");
            return;
        }

        LabsCraft.LOGGER.info("First world load detected - generating Googleplex at spawn...");

        // Center the 200x200 building so the lobby entrance aligns with the world spawn.
        // The lobby entrance is at relative x=90-110 (center of north wall).
        // Offset origin so entrance center (x+100) lands on spawn X, and entrance (z=0) is just north of spawn.
        BlockPos spawnPos = overworld.getSpawnPos();
        BlockPos origin = new BlockPos(spawnPos.getX() - 100, spawnPos.getY(), spawnPos.getZ() - 5);

        GoogleplexGenerator generator = new GoogleplexGenerator(overworld, origin);
        generator.generate();

        // Spawn Josh Woodward NPC in the lobby
        BlockPos joshPos = generator.getJoshSpawnPos();
        spawnJoshWoodward(overworld, joshPos);

        // Set world spawn inside the lobby entrance, facing south (into the building)
        BlockPos lobbySpawn = generator.getLobbySpawnPos();
        overworld.setSpawnPos(lobbySpawn, 180.0f);

        state.setGenerated(true);
        LabsCraft.LOGGER.info("Googleplex generated at origin {} with spawn at {}", origin, lobbySpawn);
    }

    private static void spawnJoshWoodward(ServerWorld world, BlockPos pos) {
        var josh = ModEntities.JOSH_WOODWARD.create(world, SpawnReason.COMMAND);
        if (josh != null) {
            josh.refreshPositionAndAngles(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                180.0f, 0.0f
            );
            world.spawnEntity(josh);
            LabsCraft.LOGGER.info("Josh Woodward spawned at {}", pos);
        }
    }
}
