package com.labscraft.world;

import com.labscraft.LabsCraft;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.entity.ModEntities;
import com.labscraft.world.structure.GoogleplexPlacer;
import com.labscraft.world.structure.plan.GoogleplexBlueprint;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Builds the Googleplex once per world, on first server start, entrance
 * aligned with the world spawn — the player walks out of spawn straight into
 * the lobby. Josh Woodward is spawned just inside, and the world spawn is
 * moved into the doorway so respawns land in the office.
 *
 * <p>Subsequent starts are a no-op: the one-shot flag lives in
 * {@link GoogleplexState}, attached to the overworld save. The seed is the
 * origin's {@link BlockPos#asLong()}, matching the default used by
 * {@code /labscraft googleplex generate}, so an op re-running the command at
 * the logged origin rebuilds the identical office.</p>
 */
public final class GoogleplexAutoGenerator {

    /** Blocks between the north wall (entrance face) and the original spawn point. */
    private static final int NORTH_SETBACK = 2;

    private GoogleplexAutoGenerator() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(GoogleplexAutoGenerator::onServerStarted);
    }

    private static void onServerStarted(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            return;
        }

        GoogleplexState state = GoogleplexState.get(overworld);
        if (state.isGenerated()) {
            return;
        }

        BlockPos spawn = overworld.getSpawnPos();
        BlockPos origin = new BlockPos(
                spawn.getX() - GoogleplexBlueprint.ENTRANCE_X,
                spawn.getY(),
                spawn.getZ() - NORTH_SETBACK);

        int blockEntities = GoogleplexPlacer.findBlockEntities(overworld, origin).size();
        if (blockEntities > 0) {
            LabsCraft.LOGGER.warn(
                    "Googleplex volume at {} overlaps {} existing block entit{} (worldgen structure?); building anyway",
                    origin, blockEntities, blockEntities == 1 ? "y" : "ies");
        }

        GoogleplexPlacer.Result result;
        try {
            result = GoogleplexPlacer.place(overworld, origin, origin.asLong());
        } catch (IllegalStateException e) {
            LabsCraft.LOGGER.error("Googleplex auto-generation failed blueprint validation; leaving world untouched", e);
            return;
        }

        BlockPos doorway = origin.add(GoogleplexBlueprint.ENTRANCE_X, 1, 1);
        spawnJosh(overworld, origin.add(GoogleplexBlueprint.ENTRANCE_X, 1, 4));
        overworld.setSpawnPos(doorway, 180.0f);

        state.setGenerated(true);
        LabsCraft.LOGGER.info("Googleplex auto-generated: bounds {} .. {}, seed {}, {} blocks; spawn moved to {}",
                result.min().toShortString(), result.max().toShortString(),
                result.seed(), result.blocksPlaced(), doorway.toShortString());
    }

    private static void spawnJosh(ServerWorld world, BlockPos pos) {
        JoshWoodwardEntity josh = ModEntities.JOSH_WOODWARD.create(world, SpawnReason.COMMAND);
        if (josh == null) {
            LabsCraft.LOGGER.warn("Could not create Josh Woodward for the lobby");
            return;
        }
        josh.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180.0f, 0.0f);
        world.spawnEntity(josh);
    }
}
