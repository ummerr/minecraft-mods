package com.labscraft.integration;

import com.labscraft.LabsCraft;
import com.labscraft.LabsCraftHooks;
import com.labscraft.block.ModBlocks;
import com.labscraft.block.entity.AbstractConsoleBlockEntity;
import com.labscraft.entity.JoshWoodwardEntity;
import com.labscraft.entity.ModEntities;
import com.labscraft.item.ModItems;
import com.labscraft.quest.Objective;
import com.labscraft.quest.PlayerQuestState;
import com.labscraft.quest.QuestManager;
import com.labscraft.quest.QuestStage;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.UUID;

/**
 * Headless runtime verification of the G3 integration wiring. Enabled ONLY
 * when the environment variable {@code LABSCRAFT_SMOKETEST=true} is set on the
 * dedicated server; completely inert otherwise.
 *
 * <p>Connects a Carpet-style fake player through the real
 * {@code PlayerManager.onPlayerConnect} path, then drives the REAL event
 * paths end to end:</p>
 * <ol>
 *   <li>right-clicks Josh (real {@code Entity.interact}) → meet_josh</li>
 *   <li>breaks real placed TPU ore via {@code ServerPlayerInteractionManager.tryBreakBlock}
 *       → PlayerBlockBreakEvents.AFTER → mine_tpu_ore</li>
 *   <li>takes results out of a real {@code CraftingResultSlot} → mixin → craft_tpu,
 *       craft_flow_table</li>
 *   <li>fires CONSOLE_CRAFTED → craft_nano_banana_console</li>
 *   <li>places a real Nano Banana Console block entity, inserts TPU, starts a
 *       generation, and waits for the 100-tick ticker to fire
 *       GENERATION_COMPLETED → generate_image</li>
 *   <li>talks to Josh again → demo_to_josh → VIDEO_LAUNCH</li>
 * </ol>
 * Every step logs [SMOKETEST] PASS/FAIL lines to the server log.
 */
public final class RuntimeSmokeTest {

    private static boolean started;
    private static int step;
    private static int delay;
    private static int pollBudget;
    private static boolean failed;

    private static ServerPlayerEntity player;
    private static JoshWoodwardEntity josh;
    private static BlockPos consolePos;

    private RuntimeSmokeTest() {
    }

    public static void register() {
        if (!"true".equalsIgnoreCase(System.getenv("LABSCRAFT_SMOKETEST"))) {
            return;
        }
        LabsCraft.LOGGER.warn("[SMOKETEST] LabsCraft runtime smoke test ENABLED (LABSCRAFT_SMOKETEST=true)");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            started = true;
            delay = 40; // let the world settle
        });
        ServerTickEvents.END_SERVER_TICK.register(RuntimeSmokeTest::tick);
    }

    private static void tick(MinecraftServer server) {
        if (!started || failed || delay-- > 0) {
            return;
        }
        try {
            runStep(server);
        } catch (Throwable t) {
            failed = true;
            LabsCraft.LOGGER.error("[SMOKETEST] FAIL step " + step + " threw", t);
        }
    }

    private static void runStep(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        switch (step) {
            case 0 -> { // Connect fake player through the real PlayerManager path.
                GameProfile profile = new GameProfile(UUID.randomUUID(), "SmokeTester");
                player = new ServerPlayerEntity(server, world, profile, SyncedClientOptions.createDefault());
                server.getPlayerManager().onPlayerConnect(new FakeConnection(), player,
                        new ConnectedClientData(profile, 0, SyncedClientOptions.createDefault(), false));
                pass("fake player connected: " + player.getName().getString()
                        + " at " + player.getBlockPos().toShortString());
                LabsCraft.LOGGER.info("[SMOKETEST] initial quest wire JSON: {}", QuestManager.toWireJson(player));
                expectStage("initial stage", QuestStage.NOT_STARTED);
                next(5);
            }
            case 1 -> { // Spawn Josh and talk to him: TALKED_TO_JOSH -> meet_josh -> FLOW_INTRO.
                josh = ModEntities.JOSH_WOODWARD.create(world, SpawnReason.COMMAND);
                josh.refreshPositionAndAngles(player.getX() + 1.5, player.getY(), player.getZ(), 0.0f, 0.0f);
                world.spawnEntity(josh);
                pass("Josh spawned at " + josh.getBlockPos().toShortString());
                josh.interact(player, Hand.MAIN_HAND);
                expectStage("after meeting Josh (real interact -> onTalkedToJosh)", QuestStage.FLOW_INTRO);
                next(5);
            }
            case 2 -> { // Real block breaks -> PlayerBlockBreakEvents.AFTER -> mine_tpu_ore.
                BlockPos base = world.getSpawnPos().add(64, 0, 64);
                for (int i = 0; i < 3; i++) {
                    BlockPos orePos = new BlockPos(base.getX() + i,
                            world.getTopY(Heightmap.Type.MOTION_BLOCKING, base.getX() + i, base.getZ()) + 1,
                            base.getZ());
                    world.setBlockState(orePos, ModBlocks.TPU_ORE.getDefaultState());
                    player.interactionManager.tryBreakBlock(orePos);
                    int progress = objectiveProgress("mine_tpu_ore");
                    LabsCraft.LOGGER.info("[SMOKETEST] mined TPU ore #{} -> mine_tpu_ore progress {}/3", i + 1, progress);
                    if (progress != i + 1) {
                        fail("mine_tpu_ore progress expected " + (i + 1) + " but was " + progress);
                        return;
                    }
                }
                pass("block-mined path: mine_tpu_ore incremented 1->2->3 via real tryBreakBlock");
                next(5);
            }
            case 3 -> { // Real CraftingResultSlot.onTakeItem (mixin) -> craft_tpu x2 -> LEARNING_PIPELINE.
                takeFromCraftingResultSlot(ModItems.TPU);
                int after1 = objectiveProgress("craft_tpu");
                if (after1 != 1) {
                    fail("craft_tpu progress expected 1 after first take but was " + after1);
                    return;
                }
                takeFromCraftingResultSlot(ModItems.TPU);
                pass("crafting mixin path: craft_tpu progress incremented 1 then 2 (done)");
                expectStage("after crafting 2 TPU via CraftingResultSlot mixin", QuestStage.LEARNING_PIPELINE);
                next(5);
            }
            case 4 -> { // Flow table via mixin; nano banana console via CONSOLE_CRAFTED hook.
                takeFromCraftingResultSlot(ModBlocks.FLOW_CRAFTING_TABLE.asItem());
                if (!objectiveDone("craft_flow_table")) {
                    fail("craft_flow_table not completed by mixin path");
                    return;
                }
                pass("crafting mixin path: craft_flow_table done");
                LabsCraftHooks.fireConsoleCrafted(player, "nano_banana_console", player.getBlockPos());
                expectStage("after CONSOLE_CRAFTED hook", QuestStage.FIRST_GENERATION);
                next(5);
            }
            case 5 -> { // Real console block entity generation (100-tick ticker).
                BlockPos spawn = world.getSpawnPos();
                consolePos = new BlockPos(spawn.getX() + 2,
                        world.getTopY(Heightmap.Type.MOTION_BLOCKING, spawn.getX() + 2, spawn.getZ() + 2) + 1,
                        spawn.getZ() + 2);
                world.setBlockState(consolePos, ModBlocks.NANO_BANANA_CONSOLE.getDefaultState());
                BlockEntity be = world.getBlockEntity(consolePos);
                if (!(be instanceof AbstractConsoleBlockEntity console)) {
                    fail("no console block entity at " + consolePos.toShortString());
                    return;
                }
                console.setStack(AbstractConsoleBlockEntity.INPUT_SLOT, new ItemStack(ModItems.TPU, 5));
                console.tryStartGeneration(player);
                if (!console.isGenerating()) {
                    fail("console did not start generating");
                    return;
                }
                pass("real Nano Banana Console placed at " + consolePos.toShortString()
                        + ", generation started, waiting for 100-tick ticker...");
                pollBudget = 600;
                next(1);
            }
            case 6 -> { // Poll for GENERATION_COMPLETED -> generate_image.
                if (objectiveDone("generate_image")) {
                    pass("generation path: GENERATION_COMPLETED fired by real console ticker -> generate_image done");
                    next(5);
                } else if (--pollBudget <= 0) {
                    fail("generate_image not completed within 600 ticks (is the spawn chunk ticking?)");
                }
            }
            case 7 -> { // Demo to Josh -> VIDEO_LAUNCH; emote + accessor sanity.
                josh.interact(player, Hand.MAIN_HAND);
                expectStage("after demoing to Josh", QuestStage.VIDEO_LAUNCH);
                boolean emoteOk = josh.playEmote("shake_head");
                boolean emoteRejected = !josh.playEmote("dab");
                LabsCraft.LOGGER.info("[SMOKETEST] emote accepted={} unknown-rejected={} activity={} lastSpoke={}s",
                        emoteOk, emoteRejected, josh.getCurrentActivity(), josh.secondsSinceLastSpoke());
                if (!emoteOk || !emoteRejected) {
                    fail("emote API misbehaved");
                    return;
                }
                LabsCraft.LOGGER.info("[SMOKETEST] final quest wire JSON: {}", QuestManager.toWireJson(player));
                if (!failed) {
                    LabsCraft.LOGGER.info("[SMOKETEST] ALL PASS — quest wiring verified end to end at runtime");
                }
                next(Integer.MAX_VALUE); // done
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Takes one result item out of a real CraftingResultSlot so the mixin fires. */
    private static void takeFromCraftingResultSlot(Item item) {
        CraftingInventory input = new CraftingInventory(player.playerScreenHandler, 2, 2);
        CraftingResultInventory result = new CraftingResultInventory();
        CraftingResultSlot slot = new CraftingResultSlot(player, input, result, 0, 0, 0);
        result.setStack(0, new ItemStack(item, 1));
        slot.onTakeItem(player, new ItemStack(item, 1));
    }

    private static PlayerQuestState state() {
        return QuestManager.getState(player);
    }

    private static int objectiveProgress(String id) {
        return state().objectives().stream()
                .filter(o -> o.id().equals(id)).mapToInt(Objective::progress).findFirst().orElse(-1);
    }

    private static boolean objectiveDone(String id) {
        return state().objectives().stream().filter(o -> o.id().equals(id)).anyMatch(Objective::isDone);
    }

    private static void expectStage(String what, QuestStage expected) {
        QuestStage actual = state().stage();
        if (actual == expected) {
            pass(what + ": stage=" + actual);
        } else {
            fail(what + ": expected stage " + expected + " but was " + actual);
        }
    }

    private static void pass(String message) {
        LabsCraft.LOGGER.info("[SMOKETEST] PASS: {}", message);
    }

    private static void fail(String message) {
        failed = true;
        LabsCraft.LOGGER.error("[SMOKETEST] FAIL: {}", message);
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
