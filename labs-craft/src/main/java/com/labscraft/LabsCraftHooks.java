package com.labscraft;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Callback surface the game-content layer (G1) exposes for the quest (G2) and
 * agent (G4/G5) layers. Register listeners during mod init; all callbacks fire
 * on the server thread.
 *
 * <p>Console ids passed to callbacks: {@code "flow_console"},
 * {@code "nano_banana_console"}, {@code "veo_console"}.</p>
 *
 * <p>This replaces v1's direct {@code QuestManager} calls inside block entities
 * and gives the quest layer the event it needs to fix v1 defect #1 (the quest
 * never advanced on first generation): listen to {@link #GENERATION_COMPLETED}
 * and inspect the produced stack / the console's lifetime completion count.</p>
 */
public final class LabsCraftHooks {
    private LabsCraftHooks() {
    }

    /** Fired when a player opens a console screen. */
    @FunctionalInterface
    public interface ConsoleOpened {
        void onConsoleOpened(ServerPlayerEntity player, String consoleId, BlockPos pos);
    }

    /** Fired when a player starts a generation (TPUs already consumed). */
    @FunctionalInterface
    public interface GenerationStarted {
        void onGenerationStarted(ServerPlayerEntity player, String consoleId, BlockPos pos);
    }

    /**
     * Fired when a generation finishes and the artifact has been placed in the
     * console's output slot. {@code startedBy} is the UUID of the player who
     * started the generation (may be null if unknown, e.g. legacy saves).
     * {@code lifetimeCompleted} is that console's total completed generations,
     * so "first generation" quest logic is a simple {@code == 1} check.
     */
    @FunctionalInterface
    public interface GenerationCompleted {
        void onGenerationCompleted(ServerWorld world, BlockPos pos, String consoleId,
                ItemStack output, @Nullable UUID startedBy, int lifetimeCompleted);
    }

    /** Fired when a console is crafted at the Flow Crafting Table. */
    @FunctionalInterface
    public interface ConsoleCrafted {
        void onConsoleCrafted(ServerPlayerEntity player, String consoleId, BlockPos tablePos);
    }

    public static final List<ConsoleOpened> CONSOLE_OPENED = new CopyOnWriteArrayList<>();
    public static final List<GenerationStarted> GENERATION_STARTED = new CopyOnWriteArrayList<>();
    public static final List<GenerationCompleted> GENERATION_COMPLETED = new CopyOnWriteArrayList<>();
    public static final List<ConsoleCrafted> CONSOLE_CRAFTED = new CopyOnWriteArrayList<>();

    public static void fireConsoleOpened(ServerPlayerEntity player, String consoleId, BlockPos pos) {
        for (ConsoleOpened listener : CONSOLE_OPENED) {
            listener.onConsoleOpened(player, consoleId, pos);
        }
    }

    public static void fireGenerationStarted(ServerPlayerEntity player, String consoleId, BlockPos pos) {
        for (GenerationStarted listener : GENERATION_STARTED) {
            listener.onGenerationStarted(player, consoleId, pos);
        }
    }

    public static void fireGenerationCompleted(ServerWorld world, BlockPos pos, String consoleId,
            ItemStack output, @Nullable UUID startedBy, int lifetimeCompleted) {
        for (GenerationCompleted listener : GENERATION_COMPLETED) {
            listener.onGenerationCompleted(world, pos, consoleId, output, startedBy, lifetimeCompleted);
        }
    }

    public static void fireConsoleCrafted(ServerPlayerEntity player, String consoleId, BlockPos tablePos) {
        for (ConsoleCrafted listener : CONSOLE_CRAFTED) {
            listener.onConsoleCrafted(player, consoleId, tablePos);
        }
    }
}
