package com.labscraft.block.entity;

import com.labscraft.LabsCraftHooks;
import com.labscraft.item.GeneratedArtifacts;
import com.labscraft.item.ModItems;
import com.labscraft.logic.ConsoleCore;
import com.labscraft.screen.ConsoleScreenHandler;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Shared implementation for the three generation consoles. Two slots: a TPU
 * input and an artifact output. Starting a generation consumes the TPU cost up
 * front; when the progress bar completes, a real flavored artifact item is
 * placed in the output slot (fixes v1 defect #2, where consoles produced
 * nothing).
 */
public abstract class AbstractConsoleBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Inventory {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 2;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ConsoleCore core;
    private final GeneratedArtifacts artifactType;
    private final String consoleId;

    @Nullable
    private UUID startedBy;

    protected final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> core.getProgress();
                case 1 -> core.isGenerating() ? 1 : 0;
                case 2 -> core.getCompletedCount();
                case 3 -> core.getGenerationTicks();
                case 4 -> core.getTpuCost();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // State lives in the ConsoleCore; clients only read.
        }

        @Override
        public int size() {
            return 5;
        }
    };

    protected AbstractConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            String consoleId, GeneratedArtifacts artifactType, int tpuCost, int generationTicks) {
        super(type, pos, state);
        this.consoleId = consoleId;
        this.artifactType = artifactType;
        this.core = new ConsoleCore(tpuCost, generationTicks);
    }

    public String getConsoleId() {
        return consoleId;
    }

    public boolean isGenerating() {
        return core.isGenerating();
    }

    public int getCompletedCount() {
        return core.getCompletedCount();
    }

    public int getTpuCost() {
        return core.getTpuCost();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.labscraft." + consoleId);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            LabsCraftHooks.fireConsoleOpened(serverPlayer, consoleId, pos);
        }
        return new ConsoleScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    /**
     * Called from the screen handler's Generate button. Consumes the TPU cost
     * up front and begins the generation.
     */
    public void tryStartGeneration(ServerPlayerEntity player) {
        ItemStack input = inventory.get(INPUT_SLOT);
        int available = input.isOf(ModItems.TPU) ? input.getCount() : 0;
        int consumed = core.start(available, inventory.get(OUTPUT_SLOT).isEmpty());
        if (consumed <= 0) {
            return;
        }
        input.decrement(consumed);
        startedBy = player.getUuid();
        markDirty();
        sync();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.7f, 1.2f);
        }
        LabsCraftHooks.fireGenerationStarted(player, consoleId, pos);
    }

    public static void tick(World world, BlockPos pos, BlockState state, AbstractConsoleBlockEntity entity) {
        if (world.isClient) {
            return;
        }
        if (entity.core.isGenerating()) {
            if (entity.core.tick()) {
                entity.completeGeneration();
            }
            entity.markDirty();
        }
    }

    private void completeGeneration() {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }
        ItemStack output = artifactType.create(serverWorld.getRandom().nextLong());
        inventory.set(OUTPUT_SLOT, output);
        markDirty();
        sync();

        serverWorld.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.8f, 1.5f);
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 1.0;
        double cz = pos.getZ() + 0.5;
        serverWorld.spawnParticles(ParticleTypes.ENCHANT, cx, cy + 0.5, cz, 15, 0.4, 0.3, 0.4, 0.1);
        serverWorld.spawnParticles(ParticleTypes.END_ROD, cx, cy, cz, 8, 0.3, 0.5, 0.3, 0.02);

        LabsCraftHooks.fireGenerationCompleted(serverWorld, pos, consoleId, output.copy(),
            startedBy, core.getCompletedCount());
    }

    /** Pushes block-entity state to watching clients (for ambient particles etc.). */
    private void sync() {
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    // ---- Inventory ----

    @Override
    public int size() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && stack.isOf(ModItems.TPU);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        // v1 defect #10 fix: real distance check instead of `return true`.
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    // ---- Persistence ----

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
        nbt.putInt("generationProgress", core.getProgress());
        nbt.putBoolean("isGenerating", core.isGenerating());
        nbt.putInt("totalGenerations", core.getCompletedCount());
        if (startedBy != null) {
            nbt.putUuid("startedBy", startedBy);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        inventory.clear();
        Inventories.readNbt(nbt, inventory, registryLookup);
        core.restore(nbt.getInt("generationProgress"), nbt.getBoolean("isGenerating"),
            nbt.getInt("totalGenerations"));
        startedBy = nbt.containsUuid("startedBy") ? nbt.getUuid("startedBy") : null;
    }
}
