package com.labscraft.command;

import com.labscraft.LabsCraft;
import com.labscraft.world.GoogleplexGenerator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

        source.sendFeedback(() -> Text.literal("Googleplex complete! Entrance is at " + playerPos), true);

        return 1;
    }
}
