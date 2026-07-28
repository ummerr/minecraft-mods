package com.sportscraft.game;

import com.sportscraft.SportsCraft;
import com.sportscraft.SportsHooks;
import com.sportscraft.core.golf.GolfHoleDef;
import com.sportscraft.core.golf.GolfRound;
import com.sportscraft.core.golf.StrokeOutcome;
import com.sportscraft.core.physics.BallSpec;
import com.sportscraft.core.physics.Vec3c;
import com.sportscraft.entity.SportsBallEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Facade over golf state, per {@code QuestManager}. Subscribes to
 * {@link SportsHooks}, feeds events into each player's pure {@link GolfRound},
 * and executes whatever {@link StrokeOutcome} comes back.
 *
 * <p>The split matters: {@code GolfRound} decides, this class acts. Rules are
 * unit-tested without Minecraft; entity juggling lives here.</p>
 *
 * <p>Rounds are per-player and concurrent — two people can play different holes
 * at once, and every event is routed by the ball's owner UUID.</p>
 */
public final class GolfManager {

    /** How close to a tee you must be to start a round on that hole. */
    private static final double START_RADIUS = 24.0;

    private static final Map<UUID, GolfRound> ROUNDS = new ConcurrentHashMap<>();

    private GolfManager() {
    }

    public static void register() {
        SportsHooks.STROKE_TAKEN.add((player, clubId, power) -> {
            GolfRound round = ROUNDS.get(player.getUuid());
            if (round != null) {
                round.onStrokeTaken();
            }
        });

        SportsHooks.BALL_AT_REST.add((world, ballId, ownerUuid, pos) ->
                withRound(world, ownerUuid, (player, round) -> {
                    StrokeOutcome outcome = round.onBallAtRest(toVec3c(pos));
                    switch (outcome) {
                        case HOLED_OUT -> completeHole(world, player, round);
                        case READY_FOR_NEXT_STROKE -> SportsNotifier.strokeUpdate(player, round);
                        default -> {
                        }
                    }
                }));

        SportsHooks.BALL_IN_WATER.add((world, ballId, ownerUuid, pos) ->
                withRound(world, ownerUuid, (player, round) ->
                        applyPenalty(world, player, round, "In the water")));

        SportsHooks.BALL_OUT_OF_BOUNDS.add((world, ballId, ownerUuid, pos) ->
                withRound(world, ownerUuid, (player, round) ->
                        applyPenalty(world, player, round, "Out of bounds")));

        SportsCraft.LOGGER.info("GolfManager subscribed to SportsHooks");
    }

    // ------------------------------------------------------------------
    // Round lifecycle
    // ------------------------------------------------------------------

    /** Starts a round on the hole nearest the player, if there is one in range. */
    @Nullable
    public static GolfRound startRound(ServerPlayerEntity player) {
        GolfHoleDef hole = holes(player.getServer()).nearestByTee(player.getPos(), START_RADIUS);
        if (hole == null) {
            return null;
        }
        GolfRound round = new GolfRound(hole);
        ROUNDS.put(player.getUuid(), round);
        removeBalls(player);
        SportsNotifier.roundStarted(player, round);
        return round;
    }

    @Nullable
    public static GolfRound roundOf(ServerPlayerEntity player) {
        return ROUNDS.get(player.getUuid());
    }

    @Nullable
    public static GolfRound quitRound(ServerPlayerEntity player) {
        GolfRound round = ROUNDS.remove(player.getUuid());
        if (round != null) {
            removeBalls(player);
            SportsNotifier.roundQuit(player, round);
        }
        return round;
    }

    /** Drops a fresh ball at the player's feet — the "my ball is stuck" escape. */
    public static boolean dropBall(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        removeBalls(player);
        GolfRound round = ROUNDS.get(player.getUuid());
        Vec3d where = round != null
                ? toVec3d(round.lastRestPosition())
                : player.getPos().add(0, 0.25, 0);
        return SportsBallEntity.spawn(world, where, Vec3c.ZERO, player.getUuid(), BallSpec.GOLF) != null;
    }

    private static void completeHole(ServerWorld world, ServerPlayerEntity player, GolfRound round) {
        SportsNotifier.holedOut(player, round, world);
        SportsHooks.fireHoleCompleted(player,
                new net.minecraft.util.math.BlockPos(
                        round.hole().cupX(), round.hole().cupY(), round.hole().cupZ()),
                round.strokes(), round.par());
        ROUNDS.remove(player.getUuid());
        removeBalls(player);
    }

    private static void applyPenalty(ServerWorld world, ServerPlayerEntity player,
                                     GolfRound round, String reason) {
        if (round.onBallLost() != StrokeOutcome.REPLACE_AT_LAST_REST) {
            return;
        }
        SportsNotifier.penalty(player, round, reason);
        SportsBallEntity.spawn(world, toVec3d(round.lastRestPosition()),
                Vec3c.ZERO, player.getUuid(), BallSpec.GOLF);
    }

    // ------------------------------------------------------------------
    // Holes
    // ------------------------------------------------------------------

    public static void registerHole(MinecraftServer server, GolfHoleDef hole) {
        SportsPersistentState.get(server).registerHole(hole);
    }

    public static int holeCount(MinecraftServer server) {
        return SportsPersistentState.get(server).holes().size();
    }

    public static HoleRegistry holes(MinecraftServer server) {
        return SportsPersistentState.get(server).holes();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface RoundAction {
        void run(ServerPlayerEntity player, GolfRound round);
    }

    /** Routes a ball event to its owner's round, if both still exist. */
    private static void withRound(ServerWorld world, @Nullable UUID ownerUuid, RoundAction action) {
        if (ownerUuid == null) {
            return;
        }
        GolfRound round = ROUNDS.get(ownerUuid);
        if (round == null) {
            return;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (player == null) {
            return;
        }
        action.run(player, round);
    }

    /** Clears this player's balls so a round never inherits a stray one. */
    private static void removeBalls(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Box box = player.getBoundingBox().expand(256.0);
        for (SportsBallEntity ball : world.getEntitiesByClass(SportsBallEntity.class, box,
                b -> player.getUuid().equals(b.getOwnerUuid()))) {
            ball.discard();
        }
    }

    private static Vec3c toVec3c(Vec3d vec) {
        return new Vec3c(vec.x, vec.y, vec.z);
    }

    private static Vec3d toVec3d(Vec3c vec) {
        return new Vec3d(vec.x(), vec.y(), vec.z());
    }
}
