package com.sportscraft.command;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sportscraft.SportsCraft;
import com.sportscraft.core.golf.GolfHoleDef;
import com.sportscraft.core.physics.BallSpec;
import com.sportscraft.core.physics.Vec3c;
import com.sportscraft.core.swing.ClubSpec;
import com.sportscraft.core.swing.Clubs;
import com.sportscraft.core.swing.LaunchSolver;
import com.sportscraft.course.CoursePlacer;
import com.sportscraft.entity.SportsBallEntity;
import com.sportscraft.game.GolfManager;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * {@code /sports ...} — the mod's command surface, Brigadier per
 * {@code GoogleplexCommands}.
 *
 * <pre>
 * /sports build golf &lt;pos&gt; [&lt;seed&gt;] [force]   op 2; builds and registers a hole
 * /sports info                                what is built and what the clubs do
 * /sports debug ball [&lt;club&gt;]                 op 2; hidden physics probe
 * </pre>
 *
 * <p>The position is an explicit argument rather than the caller's location, so
 * a hole can be built from the dedicated-server console.</p>
 */
public final class SportsCommands {

    private SportsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("sports")
                        .then(CommandManager.literal("info")
                                .executes(SportsCommands::info))
                        .then(CommandManager.literal("build")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("golf")
                                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                                .executes(ctx -> build(ctx, null, false))
                                                .then(CommandManager.literal("force")
                                                        .executes(ctx -> build(ctx, null, true)))
                                                .then(CommandManager.argument("seed", LongArgumentType.longArg())
                                                        .executes(ctx -> build(ctx,
                                                                LongArgumentType.getLong(ctx, "seed"), false))
                                                        .then(CommandManager.literal("force")
                                                                .executes(ctx -> build(ctx,
                                                                        LongArgumentType.getLong(ctx, "seed"), true)))))))
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

    // ------------------------------------------------------------------
    // build
    // ------------------------------------------------------------------

    private static int build(CommandContext<ServerCommandSource> context,
                             Long explicitSeed, boolean force) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos origin = BlockPosArgumentType.getBlockPos(context, "pos");
        long seed = explicitSeed != null ? explicitSeed : origin.asLong();

        if (!force) {
            List<BlockPos> blockEntities = CoursePlacer.findBlockEntities(world, origin, seed);
            if (!blockEntities.isEmpty()) {
                BlockPos first = blockEntities.get(0);
                source.sendError(Text.literal("Refusing to build: found " + blockEntities.size()
                        + " block entit" + (blockEntities.size() == 1 ? "y" : "ies")
                        + " in the target volume (first at " + first.toShortString()
                        + ") — looks like a player build. Append 'force' to overwrite."));
                return 0;
            }
        }

        long startNanos = System.nanoTime();
        CoursePlacer.Result result;
        try {
            result = CoursePlacer.place(world, origin, seed);
        } catch (IllegalStateException e) {
            source.sendError(Text.literal("Hole validation failed: " + e.getMessage()));
            SportsCraft.LOGGER.error("Golf hole validation failed", e);
            return 0;
        }
        long millis = (System.nanoTime() - startNanos) / 1_000_000;

        GolfHoleDef hole = new GolfHoleDef(
                result.tee().getX(), result.tee().getY(), result.tee().getZ(),
                result.cup().getX(), result.cup().getY(), result.cup().getZ(),
                result.par(), result.holeLength(), seed);
        GolfManager.registerHole(source.getServer(), hole);

        source.sendFeedback(() -> Text.literal("Golf hole built.").formatted(Formatting.GREEN)
                .append(Text.literal(" par " + result.par() + ", " + result.holeLength()
                        + " blocks, tee " + result.tee().toShortString()
                        + ", cup " + result.cup().toShortString()
                        + ", seed " + result.seed() + ", " + result.blocksPlaced() + " blocks, "
                        + result.foundationBlocks() + " foundation, "
                        + result.clearedAbove() + " cleared, " + millis + " ms")
                        .formatted(Formatting.GRAY)), true);
        return 1;
    }

    // ------------------------------------------------------------------
    // info / debug
    // ------------------------------------------------------------------

    private static int info(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        source.sendFeedback(() -> Text.literal("SportsCraft — Wii-Sports-style minigames.")
                .formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("  Golf: hold right-click to charge, release to swing. "
                + "You set the aim; the club sets the trajectory.").formatted(Formatting.GRAY), false);
        for (ClubSpec club : Clubs.ALL) {
            source.sendFeedback(() -> Text.literal("  - " + club.displayName() + ": loft "
                            + (int) club.loftDegrees() + "°, speed " + club.maxSpeed())
                    .formatted(Formatting.GRAY), false);
        }

        int holes = GolfManager.holeCount(source.getServer());
        source.sendFeedback(() -> Text.literal("  " + holes + " hole" + (holes == 1 ? "" : "s")
                + " built. Use /sports build golf <pos> to make one.").formatted(Formatting.GRAY), false);
        return 1;
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
        Vec3d spawnPos = player.getPos().add(heading.x(), 0.25, heading.z());
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
}
