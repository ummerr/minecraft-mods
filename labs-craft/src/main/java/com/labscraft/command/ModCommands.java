package com.labscraft.command;

import com.labscraft.LabsCraft;
import com.labscraft.entity.ModEntities;
import com.labscraft.world.GoogleplexGenerator;
import com.labscraft.world.GoogleplexState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class ModCommands {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerBuildCommand(dispatcher);
        });
        LabsCraft.LOGGER.info("Registering commands for " + LabsCraft.MOD_ID);
    }

    private static void registerBuildCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("labscraft")
            .then(CommandManager.literal("build")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ModCommands::executeBuild)
            )
        );
    }

    private static int executeBuild(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());

        source.sendFeedback(() -> Text.literal("Building Googleplex at " + playerPos + "..."), true);

        // Build the structure
        GoogleplexGenerator generator = new GoogleplexGenerator(world, playerPos);
        generator.generate();

        // Spawn Josh Woodward in the lobby
        BlockPos joshPos = generator.getJoshSpawnPos();
        var josh = ModEntities.JOSH_WOODWARD.create(world, SpawnReason.COMMAND);
        if (josh != null) {
            josh.refreshPositionAndAngles(
                joshPos.getX() + 0.5, joshPos.getY(), joshPos.getZ() + 0.5,
                180.0f, 0.0f
            );
            world.spawnEntity(josh);
        }

        // Mark as generated so auto-generator won't duplicate
        GoogleplexState state = GoogleplexState.get(world);
        state.setGenerated(true);

        source.sendFeedback(() -> Text.literal("Googleplex complete! Josh is in the lobby."), true);

        return 1;
    }
}
