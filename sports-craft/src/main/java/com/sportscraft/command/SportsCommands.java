package com.sportscraft.command;

import com.mojang.brigadier.context.CommandContext;
import com.sportscraft.core.physics.BallSpec;
import com.sportscraft.core.physics.Vec3c;
import com.sportscraft.core.swing.ClubSpec;
import com.sportscraft.core.swing.Clubs;
import com.sportscraft.core.swing.LaunchSolver;
import com.sportscraft.entity.SportsBallEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * {@code /sports ...} — the mod's command surface.
 *
 * <p>M1 ships only the hidden debug verb, which is how the ball physics get
 * exercised before clubs exist:</p>
 *
 * <pre>
 * /sports debug ball [&lt;club&gt;]   launch a ball where you are looking
 * /sports info                  what this mod is and what is wired up
 * </pre>
 */
public final class SportsCommands {

    private SportsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("sports")
                        .then(CommandManager.literal("info")
                                .executes(SportsCommands::info))
                        .then(CommandManager.literal("debug")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("ball")
                                        .executes(ctx -> debugBall(ctx, Clubs.DRIVER))
                                        .then(CommandManager.literal("driver")
                                                .executes(ctx -> debugBall(ctx, Clubs.DRIVER)))
                                        .then(CommandManager.literal("iron")
                                                .executes(ctx -> debugBall(ctx, Clubs.IRON)))
                                        .then(CommandManager.literal("putter")
                                                .executes(ctx -> debugBall(ctx, Clubs.PUTTER)))))));
    }

    /** Spawns a ball just in front of the caller and launches it at full power. */
    private static int debugBall(CommandContext<ServerCommandSource> context, ClubSpec club) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("/sports debug ball needs a player to aim with."));
            return 0;
        }

        ServerWorld world = source.getWorld();
        Vec3c heading = LaunchSolver.headingFromYaw(player.getYaw());
        Vec3d spawnPos = player.getPos().add(heading.x(), player.getStandingEyeHeight() * 0.5, heading.z());
        Vec3c velocity = LaunchSolver.solve(player.getYaw(), club, 1.0);

        SportsBallEntity ball = SportsBallEntity.spawn(
                world, spawnPos, velocity, player.getUuid(), BallSpec.GOLF);
        if (ball == null) {
            source.sendError(Text.literal("Could not spawn the ball."));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Launched a ball with the " + club.displayName() + ".")
                .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("SportsCraft — Wii-Sports-style minigames.")
                .formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("  Golf: charge and release to swing; the club sets the "
                + "trajectory, you set the aim.").formatted(Formatting.GRAY), false);
        for (ClubSpec club : Clubs.ALL) {
            source.sendFeedback(() -> Text.literal("  - " + club.displayName() + ": loft "
                    + (int) club.loftDegrees() + "°, speed " + club.maxSpeed())
                    .formatted(Formatting.GRAY), false);
        }
        return 1;
    }
}
