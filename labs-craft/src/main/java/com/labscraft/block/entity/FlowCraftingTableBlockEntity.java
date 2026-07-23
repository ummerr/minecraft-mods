package com.labscraft.block.entity;

import com.labscraft.LabsCraftHooks;
import com.labscraft.block.ModBlocks;
import com.labscraft.item.ModItems;
import com.labscraft.logic.FlowCraftingLogic;
import com.labscraft.screen.FlowCraftingTableScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Flow Crafting Table: 10 TPU input slots + 1 output slot.
 * 5 TPU → Nano Banana Console, 10 TPU → Veo Console.
 * Crafting math lives in the pure-Java {@link FlowCraftingLogic}.
 */
public class FlowCraftingTableBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Inventory {
    public static final int TPU_SLOTS = FlowCraftingLogic.INPUT_SLOTS;
    public static final int OUTPUT_SLOT = TPU_SLOTS;
    private static final int SLOT_COUNT = TPU_SLOTS + 1;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);

    public FlowCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_CRAFTING_TABLE, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.labscraft.flow_crafting_table");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new FlowCraftingTableScreenHandler(syncId, playerInventory, this);
    }

    /** Snapshot of TPU counts per input slot, for the pure crafting logic. */
    private int[] tpuSlotCounts() {
        int[] counts = new int[TPU_SLOTS];
        for (int i = 0; i < TPU_SLOTS; i++) {
            ItemStack stack = inventory.get(i);
            counts[i] = stack.isOf(ModItems.TPU) ? stack.getCount() : 0;
        }
        return counts;
    }

    public int countTpus() {
        return FlowCraftingLogic.countTpus(tpuSlotCounts());
    }

    public boolean canCraftNanoBanana() {
        return FlowCraftingLogic.canCraft(tpuSlotCounts(), FlowCraftingLogic.NANO_BANANA_COST,
            inventory.get(OUTPUT_SLOT).isEmpty());
    }

    public boolean canCraftVeo() {
        return FlowCraftingLogic.canCraft(tpuSlotCounts(), FlowCraftingLogic.VEO_COST,
            inventory.get(OUTPUT_SLOT).isEmpty());
    }

    public void craftNanoBanana(@Nullable ServerPlayerEntity player) {
        craft(FlowCraftingLogic.NANO_BANANA_COST, ModBlocks.NANO_BANANA_CONSOLE, "nano_banana_console", player);
    }

    public void craftVeo(@Nullable ServerPlayerEntity player) {
        craft(FlowCraftingLogic.VEO_COST, ModBlocks.VEO_CONSOLE, "veo_console", player);
    }

    private void craft(int cost, Block result, String consoleId, @Nullable ServerPlayerEntity player) {
        if (!inventory.get(OUTPUT_SLOT).isEmpty()) {
            return;
        }
        int[] plan = FlowCraftingLogic.removalPlan(tpuSlotCounts(), cost);
        if (plan == null) {
            return;
        }
        for (int i = 0; i < TPU_SLOTS; i++) {
            if (plan[i] > 0) {
                inventory.get(i).decrement(plan[i]);
            }
        }
        inventory.set(OUTPUT_SLOT, new ItemStack(result));
        markDirty();
        playCraftEffects();
        if (player != null) {
            LabsCraftHooks.fireConsoleCrafted(player, consoleId, pos);
        }
    }

    private void playCraftEffects() {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
        }
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
    public boolean canPlayerUse(PlayerEntity player) {
        // v1 defect #10 fix: real distance check instead of `return true`.
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot != OUTPUT_SLOT && stack.isOf(ModItems.TPU);
    }

    // ---- Persistence ----

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        inventory.clear();
        Inventories.readNbt(nbt, inventory, registryLookup);
    }
}
