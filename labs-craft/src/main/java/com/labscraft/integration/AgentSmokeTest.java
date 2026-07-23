package com.labscraft.integration;

import com.labscraft.LabsCraft;
import com.labscraft.agent.AgentBridge;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.entity.ModEntities;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * Headless runtime verification of the G5 agent bridge against a REAL running
 * agent server. Enabled ONLY with {@code LABSCRAFT_AGENT_SMOKETEST=true};
 * completely inert otherwise. (Run the Node agent server first —
 * {@code cd agent-server && npm run dev} — it works with no API keys via its
 * static provider.)
 *
 * <p>Follows the {@link RuntimeSmokeTest} fake-player pattern and proves the
 * full round trip at runtime:</p>
 * <ol>
 *   <li>fake player connects via the real {@code PlayerManager} path</li>
 *   <li>Josh spawns 2 blocks away</li>
 *   <li>the {@code GET /health} probe flips the bridge live</li>
 *   <li>at least one {@code POST /tick} goes out and parses OK</li>
 *   <li>a chat message dispatched through the real
 *       {@code ServerMessageEvents.CHAT_MESSAGE} invoker becomes a
 *       {@code chat_message} recent_event, the server answers with the
 *       {@code chat_message} trigger, and Josh executes the returned SAY
 *       (observed via {@code secondsSinceLastSpoke})</li>
 * </ol>
 */
public final class AgentSmokeTest {

    private static boolean started;
    private static int step;
    private static int delay;
    private static int pollBudget;
    private static boolean failed;

    private static ServerPlayerEntity player;
    private static JoshWoodwardEntity josh;
    private static long actionsBeforeChat;

    private AgentSmokeTest() {
    }

    public static void register() {
        if (!"true".equalsIgnoreCase(System.getenv("LABSCRAFT_AGENT_SMOKETEST"))) {
            return;
        }
        LabsCraft.LOGGER.warn("[AGENT-SMOKETEST] LabsCraft agent-bridge smoke test ENABLED"
                + " (LABSCRAFT_AGENT_SMOKETEST=true)");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            started = true;
            delay = 40; // let the world settle and the startup health probe land
        });
        ServerTickEvents.END_SERVER_TICK.register(AgentSmokeTest::tick);
    }

    private static void tick(MinecraftServer server) {
        if (!started || failed || delay-- > 0) {
            return;
        }
        try {
            runStep(server);
        } catch (Throwable t) {
            failed = true;
            LabsCraft.LOGGER.error("[AGENT-SMOKETEST] FAIL step " + step + " threw", t);
        }
    }

    private static void runStep(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        switch (step) {
            case 0 -> { // Fake player through the real PlayerManager path.
                GameProfile profile = new GameProfile(UUID.randomUUID(), "AgentTester");
                player = new ServerPlayerEntity(server, world, profile, SyncedClientOptions.createDefault());
                server.getPlayerManager().onPlayerConnect(new FakeConnection(), player,
                        new ConnectedClientData(profile, 0, SyncedClientOptions.createDefault(), false));
                pass("fake player connected: " + player.getName().getString());
                next(5);
            }
            case 1 -> { // Josh 2 blocks away (inside chat earshot and poll range).
                josh = ModEntities.JOSH_WOODWARD.create(world, SpawnReason.COMMAND);
                josh.refreshPositionAndAngles(player.getX() + 2.0, player.getY(), player.getZ(), 0.0f, 0.0f);
                world.spawnEntity(josh);
                pass("Josh spawned at " + josh.getBlockPos().toShortString());
                pollBudget = 600; // 30s for the health probe to flip the bridge live
                next(1);
            }
            case 2 -> { // Wait for GET /health to mark the agent path live.
                if (AgentBridge.isAgentLive()) {
                    pass("bridge live: GET /health succeeded, agent path active");
                    pollBudget = 200; // 10s for the first POST /tick round trip
                    next(1);
                } else if (--pollBudget <= 0) {
                    fail("bridge never went live — is the agent server running on its configured port?");
                }
            }
            case 3 -> { // Wait for a real POST /tick to go out and parse OK.
                if (AgentBridge.responsesOkCount() > 0) {
                    pass("first /tick round trip complete (sent=" + AgentBridge.ticksSentCount()
                            + ", ok=" + AgentBridge.responsesOkCount() + ")");
                    next(5);
                } else if (--pollBudget <= 0) {
                    fail("no successful /tick response within budget (sent=" + AgentBridge.ticksSentCount() + ")");
                }
            }
            case 4 -> { // Real chat event near Josh -> chat_message recent_event.
                actionsBeforeChat = AgentBridge.actionsExecutedCount();
                SignedMessage message = SignedMessage.ofUnsigned("hey josh, where do I find TPU ore?");
                ServerMessageEvents.CHAT_MESSAGE.invoker().onChatMessage(
                        message, player, MessageType.params(MessageType.CHAT, player));
                pass("chat message dispatched through ServerMessageEvents.CHAT_MESSAGE near Josh");
                pollBudget = 600; // 30s for poll + response + delayed SAY execution
                next(1);
            }
            case 5 -> { // Wait for the chat_message trigger and Josh's SAY.
                boolean chatTriggered = "chat_message".equals(AgentBridge.lastTriggerSeen());
                boolean actionsRan = AgentBridge.actionsExecutedCount() > actionsBeforeChat;
                // The executor targets the Josh nearest the player — in a
                // reused world that may be a persistent Josh from an earlier
                // run rather than the one this test spawned, so check the
                // same entity the executor picks.
                JoshWoodwardEntity speaker = com.labscraft.agent.ActionExecutor.findNearestJosh(
                        player, com.labscraft.agent.ActionExecutor.JOSH_SEARCH_RANGE);
                long spoke = speaker == null ? -1 : speaker.secondsSinceLastSpoke();
                boolean joshSpoke = spoke >= 0 && spoke <= 10;
                if (chatTriggered && actionsRan && joshSpoke) {
                    pass("round trip proven: trigger=chat_message, actions executed="
                            + AgentBridge.actionsExecutedCount() + ", Josh spoke " + spoke + "s ago");
                    LabsCraft.LOGGER.info(
                            "[AGENT-SMOKETEST] ALL PASS — chat near Josh -> POST /tick -> SAY executed"
                                    + " (ticks sent={}, responses ok={}, actions executed={})",
                            AgentBridge.ticksSentCount(), AgentBridge.responsesOkCount(),
                            AgentBridge.actionsExecutedCount());
                    next(Integer.MAX_VALUE);
                } else if (--pollBudget <= 0) {
                    fail("round trip incomplete: lastTrigger=" + AgentBridge.lastTriggerSeen()
                            + ", actionsExecuted=" + AgentBridge.actionsExecutedCount()
                            + " (before chat " + actionsBeforeChat + ")"
                            + ", joshSpokeSecondsAgo=" + spoke);
                }
            }
            default -> {
            }
        }
    }

    private static void pass(String message) {
        LabsCraft.LOGGER.info("[AGENT-SMOKETEST] PASS: {}", message);
    }

    private static void fail(String message) {
        failed = true;
        LabsCraft.LOGGER.error("[AGENT-SMOKETEST] FAIL: {}", message);
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
