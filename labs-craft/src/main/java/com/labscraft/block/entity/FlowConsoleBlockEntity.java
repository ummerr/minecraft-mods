package com.labscraft.block.entity;

import com.labscraft.quest.QuestManager;
import com.labscraft.quest.QuestStage;
import com.labscraft.screen.FlowConsoleScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FlowConsoleBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private static final int GENERATION_TIME = 100; // 5 seconds (100 ticks)

    private int generationProgress = 0;
    private boolean isGenerating = false;
    private int totalGenerations = 0;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> generationProgress;
                case 1 -> isGenerating ? 1 : 0;
                case 2 -> totalGenerations;
                case 3 -> GENERATION_TIME;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> generationProgress = value;
                case 1 -> isGenerating = value == 1;
                case 2 -> totalGenerations = value;
            }
        }

        @Override
        public int size() {
            return 4;
        }
    };

    public FlowConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_CONSOLE, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Flow Console");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        // Advance quest when player opens the console
        if (player instanceof ServerPlayerEntity serverPlayer && world instanceof ServerWorld serverWorld) {
            QuestManager questManager = QuestManager.get(serverWorld);
            if (questManager.isAtStage(serverPlayer, QuestStage.FLOW_INTRO)) {
                questManager.advanceStage(serverPlayer);
            }
        }
        return new FlowConsoleScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    public void startGeneration() {
        if (!isGenerating) {
            isGenerating = true;
            generationProgress = 0;
            markDirty();
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, FlowConsoleBlockEntity entity) {
        if (world.isClient) return;

        if (entity.isGenerating) {
            entity.generationProgress++;

            if (entity.generationProgress >= GENERATION_TIME) {
                entity.completeGeneration();
            }
            entity.markDirty();
        }
    }

    private void completeGeneration() {
        isGenerating = false;
        generationProgress = 0;
        totalGenerations++;
        markDirty();

        // Advance quest on first generation
        if (world instanceof ServerWorld serverWorld && totalGenerations == 1) {
            // We'll notify players viewing this screen through the screen handler
        }
    }

    public boolean isGenerating() {
        return isGenerating;
    }

    public int getGenerationProgress() {
        return generationProgress;
    }

    public int getTotalGenerations() {
        return totalGenerations;
    }

    public int getGenerationTime() {
        return GENERATION_TIME;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("generationProgress", generationProgress);
        nbt.putBoolean("isGenerating", isGenerating);
        nbt.putInt("totalGenerations", totalGenerations);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        generationProgress = nbt.getInt("generationProgress");
        isGenerating = nbt.getBoolean("isGenerating");
        totalGenerations = nbt.getInt("totalGenerations");
    }
}
