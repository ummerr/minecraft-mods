package com.sportscraft;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * The event surface between the ball (which knows physics) and the game rules
 * (which know golf). Register listeners during mod init; every callback fires
 * on the server thread.
 *
 * <p>This split is the whole reason tennis is cheap later: the ball fires
 * "I came to rest" and "I went in the water" without any idea that a scorecard
 * exists, and a second sport subscribes to exactly the same events. It also
 * keeps {@code SportsBallEntity} free of round bookkeeping — the entity never
 * calls into {@code GolfManager}, which is the mistake LabsCraft v1 made when
 * its block entities called {@code QuestManager} directly.</p>
 */
public final class SportsHooks {

    private SportsHooks() {
    }

    /** A ball has stopped moving and is playable where it lies. */
    @FunctionalInterface
    public interface BallAtRest {
        void onBallAtRest(ServerWorld world, UUID ballId, @Nullable UUID ownerUuid, Vec3d pos);
    }

    /** A ball entered water — a penalty stroke in golf. */
    @FunctionalInterface
    public interface BallInWater {
        void onBallInWater(ServerWorld world, UUID ballId, @Nullable UUID ownerUuid, Vec3d pos);
    }

    /** A ball fell out of the world, or otherwise left play. */
    @FunctionalInterface
    public interface BallOutOfBounds {
        void onBallOutOfBounds(ServerWorld world, UUID ballId, @Nullable UUID ownerUuid, Vec3d pos);
    }

    /** A player swung a club and connected. Fired before the ball moves. */
    @FunctionalInterface
    public interface StrokeTaken {
        void onStrokeTaken(ServerPlayerEntity player, String clubId, double power);
    }

    /** A ball finished in the cup. {@code strokes} includes penalties. */
    @FunctionalInterface
    public interface HoleCompleted {
        void onHoleCompleted(ServerPlayerEntity player, BlockPos cup, int strokes, int par);
    }

    public static final List<BallAtRest> BALL_AT_REST = new CopyOnWriteArrayList<>();
    public static final List<BallInWater> BALL_IN_WATER = new CopyOnWriteArrayList<>();
    public static final List<BallOutOfBounds> BALL_OUT_OF_BOUNDS = new CopyOnWriteArrayList<>();
    public static final List<StrokeTaken> STROKE_TAKEN = new CopyOnWriteArrayList<>();
    public static final List<HoleCompleted> HOLE_COMPLETED = new CopyOnWriteArrayList<>();

    public static void fireBallAtRest(ServerWorld world, UUID ballId, @Nullable UUID ownerUuid, Vec3d pos) {
        for (BallAtRest listener : BALL_AT_REST) {
            listener.onBallAtRest(world, ballId, ownerUuid, pos);
        }
    }

    public static void fireBallInWater(ServerWorld world, UUID ballId, @Nullable UUID ownerUuid, Vec3d pos) {
        for (BallInWater listener : BALL_IN_WATER) {
            listener.onBallInWater(world, ballId, ownerUuid, pos);
        }
    }

    public static void fireBallOutOfBounds(ServerWorld world, UUID ballId, @Nullable UUID ownerUuid, Vec3d pos) {
        for (BallOutOfBounds listener : BALL_OUT_OF_BOUNDS) {
            listener.onBallOutOfBounds(world, ballId, ownerUuid, pos);
        }
    }

    public static void fireStrokeTaken(ServerPlayerEntity player, String clubId, double power) {
        for (StrokeTaken listener : STROKE_TAKEN) {
            listener.onStrokeTaken(player, clubId, power);
        }
    }

    public static void fireHoleCompleted(ServerPlayerEntity player, BlockPos cup, int strokes, int par) {
        for (HoleCompleted listener : HOLE_COMPLETED) {
            listener.onHoleCompleted(player, cup, strokes, par);
        }
    }
}
