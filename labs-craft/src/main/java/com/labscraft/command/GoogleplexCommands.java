package com.labscraft.command;

import com.labscraft.LabsCraft;
import com.labscraft.world.structure.GoogleplexPlacer;
import com.labscraft.world.structure.plan.GoogleplexBlueprint;
import com.labscraft.world.structure.plan.GoogleplexPlanner;
import com.labscraft.world.structure.plan.Room;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * {@code /labscraft googleplex} — summon and inspect the Labs office.
 * Op-only (level 2). Works from the dedicated-server console because the
 * position is an explicit argument, not the caller's location:
 *
 * <pre>
 * /labscraft googleplex generate &lt;x y z&gt; [&lt;seed&gt;] [force]
 * /labscraft googleplex info
 * </pre>
 *
 * <p>The given position is the structure's minimum (north-west, floor-level)
 * corner. The seed defaults to a hash of the position, so re-running at the
 * same spot rebuilds the identical office. Without {@code force} the command
 * refuses to build over any block entity in the target volume — the strongest
 * signal that a player built something there.</p>
 */
public final class GoogleplexCommands {

    private GoogleplexCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("labscraft")
                        .then(CommandManager.literal("googleplex")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("generate")
                                        .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                                .executes(ctx -> generate(ctx, null, false))
                                                .then(CommandManager.literal("force")
                                                        .executes(ctx -> generate(ctx, null, true)))
                                                .then(CommandManager.argument("seed", LongArgumentType.longArg())
                                                        .executes(ctx -> generate(ctx,
                                                                LongArgumentType.getLong(ctx, "seed"), false))
                                                        .then(CommandManager.literal("force")
                                                                .executes(ctx -> generate(ctx,
                                                                        LongArgumentType.getLong(ctx, "seed"), true))))))
                                .then(CommandManager.literal("info")
                                        .executes(GoogleplexCommands::info)))));
    }

    private static int generate(CommandContext<ServerCommandSource> context,
            Long explicitSeed, boolean force) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        BlockPos origin = BlockPosArgumentType.getBlockPos(context, "pos");
        long seed = explicitSeed != null ? explicitSeed : origin.asLong();

        if (!force) {
            List<BlockPos> blockEntities = GoogleplexPlacer.findBlockEntities(world, origin);
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
        GoogleplexPlacer.Result result;
        try {
            result = GoogleplexPlacer.place(world, origin, seed);
        } catch (IllegalStateException e) {
            source.sendError(Text.literal("Blueprint validation failed: " + e.getMessage()));
            LabsCraft.LOGGER.error("Googleplex blueprint validation failed", e);
            return 0;
        }
        long millis = (System.nanoTime() - startNanos) / 1_000_000;

        source.sendFeedback(() -> Text.literal("Googleplex office generated.").formatted(Formatting.GREEN)
                .append(Text.literal(" bounds " + result.min().toShortString() + " .. "
                        + result.max().toShortString() + ", seed " + result.seed() + ", "
                        + result.blocksPlaced() + " blocks placed, "
                        + result.foundationBlocks() + " foundation, "
                        + result.clearedAbove() + " cleared above, "
                        + millis + " ms").formatted(Formatting.GRAY)), true);
        return 1;
    }

    private static int info(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        GoogleplexBlueprint blueprint = GoogleplexPlanner.plan(0L);
        source.sendFeedback(() -> Text.literal("Googleplex office: "
                + GoogleplexBlueprint.SIZE_X + " x " + GoogleplexBlueprint.SIZE_Y + " x "
                + GoogleplexBlueprint.SIZE_Z + " (w/h/d), " + blueprint.solidCellCount()
                + " solid cells, " + blueprint.doors().size() + " door cells. Entrance on the north face.")
                .formatted(Formatting.GOLD), false);
        for (Room room : blueprint.rooms()) {
            source.sendFeedback(() -> Text.literal("  - " + room.type().displayName() + ": "
                    + room.width() + "x" + room.depth() + " at (" + room.minX() + "," + room.minZ() + ")")
                    .formatted(Formatting.GRAY), false);
        }
        return 1;
    }
}
