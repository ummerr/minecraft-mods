package com.sportscraft.integration;

import com.sportscraft.SportsCraft;
import com.sportscraft.core.golf.GolfHoleDef;
import com.sportscraft.core.golf.GolfRound;
import com.sportscraft.core.physics.BallSpec;
import com.sportscraft.core.physics.Vec3c;
import com.sportscraft.core.swing.Clubs;
import com.sportscraft.core.swing.LaunchSolver;
import com.sportscraft.course.CoursePlacer;
import com.sportscraft.entity.SportsBallEntity;
import com.sportscraft.game.GolfManager;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Headless runtime verification of the golf wiring, per labs-craft's
 * {@code RuntimeSmokeTest}. Enabled ONLY when {@code SPORTSCRAFT_SMOKETEST=true}
 * is set on the dedicated server; completely inert otherwise.
 *
 * <p>Drives the REAL paths end to end rather than calling the pure layer, which
 * unit tests already cover:</p>
 * <ol>
 *   <li>builds a real hole with {@code CoursePlacer}</li>
 *   <li>connects a Carpet-style fake player and starts a round</li>
 *   <li>spawns a real ball entity and launches it, letting the entity tick</li>
 *   <li>waits for the ball to come to rest and asserts BALL_AT_REST reached
 *       the round through {@code SportsHooks}</li>
 *   <li>drops a ball in the cup and asserts the round holes out</li>
 * </ol>
 * Every step logs {@code [SMOKETEST] PASS/FAIL} to the server log.
 */
public final class SportsSmokeTest {

    private static boolean started;
    private static int step;
    private static int delay;
    private static int pollBudget;
    private static boolean failed;

    private static ServerPlayerEntity player;
    private static BlockPos origin;
    private static CoursePlacer.Result hole;
    private static SportsBallEntity ball;

    private SportsSmokeTest() {
    }

    public static void register() {
        if (!"true".equalsIgnoreCase(System.getenv("SPORTSCRAFT_SMOKETEST"))) {
            return;
        }
        SportsCraft.LOGGER.warn("[SMOKETEST] SportsCraft runtime smoke test ENABLED");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            started = true;
            delay = 40; // let the world settle
        });
        ServerTickEvents.END_SERVER_TICK.register(SportsSmokeTest::tick);
    }

    private static void tick(MinecraftServer server) {
        if (!started || failed || delay-- > 0) {
            return;
        }
        try {
            runStep(server);
        } catch (Throwable t) {
            failed = true;
            SportsCraft.LOGGER.error("[SMOKETEST] FAIL step " + step + " threw", t);
        }
    }

    private static void runStep(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        switch (step) {
            case 0 -> { // Connect a fake player through the real PlayerManager path.
                GameProfile profile = new GameProfile(UUID.randomUUID(), "GolfTester");
                player = new ServerPlayerEntity(server, world, profile, SyncedClientOptions.createDefault());
                server.getPlayerManager().onPlayerConnect(new FakeConnection(), player,
                        new ConnectedClientData(profile, 0, SyncedClientOptions.createDefault(), false));
                pass("fake player connected at " + player.getBlockPos().toShortString());
                next(5);
            }
            case 1 -> { // Build a real hole through the real placer.
                BlockPos spawn = world.getSpawnPos();
                origin = new BlockPos(spawn.getX() + 128, 80, spawn.getZ() + 128);
                hole = CoursePlacer.place(world, origin, 12345L);
                pass("hole built: par " + hole.par() + ", " + hole.holeLength()
                        + " blocks, tee " + hole.tee().toShortString()
                        + ", cup " + hole.cup().toShortString());

                GolfHoleDef def = new GolfHoleDef(
                        hole.tee().getX(), hole.tee().getY(), hole.tee().getZ(),
                        hole.cup().getX(), hole.cup().getY(), hole.cup().getZ(),
                        hole.par(), hole.holeLength(), 12345L);
                GolfManager.registerHole(server, def);
                if (GolfManager.holeCount(server) < 1) {
                    fail("hole was not registered");
                    return;
                }
                pass("hole registered; registry size " + GolfManager.holeCount(server));
                next(5);
            }
            case 2 -> { // Teleport onto the tee and start a round.
                player.teleport(world, hole.tee().getX() + 0.5, hole.tee().getY(),
                        hole.tee().getZ() + 0.5, java.util.Set.of(), 0.0f, 0.0f, false);
                GolfRound round = GolfManager.startRound(player);
                if (round == null) {
                    fail("no round started — is the tee within range?");
                    return;
                }
                pass("round started on a par " + round.par());
                next(5);
            }
            case 3 -> { // Launch a real ball entity through the real physics path.
                Vec3c velocity = LaunchSolver.solve(player.getYaw(), Clubs.DRIVER, 0.6);
                Vec3d at = new Vec3d(hole.tee().getX() + 0.5, hole.tee().getY() + 0.3,
                        hole.tee().getZ() + 0.5);
                ball = SportsBallEntity.spawn(world, at, velocity, player.getUuid(), BallSpec.GOLF);
                if (ball == null) {
                    fail("ball entity could not be spawned");
                    return;
                }
                com.sportscraft.SportsHooks.fireStrokeTaken(player, Clubs.DRIVER.id(), 0.6);
                pass("ball launched from the tee; waiting for it to come to rest...");
                pollBudget = 600;
                next(1);
            }
            case 4 -> { // The entity must settle and the round must hear about it.
                if (ball.isAtRest()) {
                    GolfRound round = GolfManager.roundOf(player);
                    if (round == null) {
                        fail("round vanished while the ball was moving");
                        return;
                    }
                    pass("ball came to rest at " + ball.getBlockPos().toShortString()
                            + " after a real entity flight; round phase " + round.phase()
                            + ", strokes " + round.strokes());
                    if (round.phase() != GolfRound.Phase.AWAITING_STROKE) {
                        fail("BALL_AT_REST did not reach the round (phase " + round.phase() + ")");
                        return;
                    }
                    pass("BALL_AT_REST propagated through SportsHooks into GolfRound");
                    next(5);
                } else if (--pollBudget <= 0) {
                    fail("ball never came to rest within 600 ticks");
                }
            }
            case 5 -> { // Drop a ball straight into the cup and expect a hole-out.
                ball.discard();
                Vec3d inCup = new Vec3d(hole.cup().getX() + 0.5, hole.cup().getY() + 0.5,
                        hole.cup().getZ() + 0.5);
                ball = SportsBallEntity.spawn(world, inCup, Vec3c.ZERO, player.getUuid(), BallSpec.GOLF);
                com.sportscraft.SportsHooks.fireStrokeTaken(player, Clubs.PUTTER.id(), 0.2);
                pass("ball placed in the cup; waiting for hole-out...");
                pollBudget = 200;
                next(1);
            }
            case 6 -> {
                if (GolfManager.roundOf(player) == null) {
                    pass("HOLE COMPLETED — the round finished and was cleared");
                    if (!failed) {
                        SportsCraft.LOGGER.info(
                                "[SMOKETEST] ALL PASS — golf wiring verified end to end at runtime");
                    }
                    next(Integer.MAX_VALUE);
                } else if (--pollBudget <= 0) {
                    fail("ball in the cup did not hole out within 200 ticks");
                }
            }
            default -> {
            }
        }
    }

    private static void pass(String message) {
        SportsCraft.LOGGER.info("[SMOKETEST] PASS: {}", message);
    }

    private static void fail(String message) {
        failed = true;
        SportsCraft.LOGGER.error("[SMOKETEST] FAIL: {}", message);
    }

    private static void next(int delayTicks) {
        step++;
        delay = delayTicks;
    }

    /** Carpet-style connection that never touches a netty channel. */
    private static final class FakeConnection extends ClientConnection {
        FakeConnection() {
            super(NetworkSide.SERVERBOUND);
        }

        @Override
        public void send(Packet<?> packet) {
        }

        @Override
        public void send(Packet<?> packet, PacketCallbacks callbacks) {
        }

        @Override
        public void send(Packet<?> packet, PacketCallbacks callbacks, boolean flush) {
        }

        @Override
        public void setCompressionThreshold(int threshold, boolean rejectsBadPackets) {
        }

        @Override
        public void disconnect(Text reason) {
        }

        @Override
        public void disconnect(DisconnectionInfo info) {
        }

        @Override
        public <T extends PacketListener> void transitionInbound(NetworkState<T> state, T listener) {
        }

        @Override
        public void transitionOutbound(NetworkState<?> state) {
        }
    }
}
