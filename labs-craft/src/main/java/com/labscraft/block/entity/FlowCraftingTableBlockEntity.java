package com.labscraft.block.entity;

import com.labscraft.block.ModBlocks;
import com.labscraft.item.ModItems;
import com.labscraft.screen.FlowCraftingTableScreenHandler;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class FlowCraftingTableBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, Inventory {
    // Inventory slots: 0-9 = TPU input slots (10 slots for up to 10 TPUs), 10 = output slot
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(11, ItemStack.EMPTY);

    public static final int TPU_SLOTS = 10;
    public static final int OUTPUT_SLOT = 10;

    public static final int NANO_BANANA_COST = 5;
    public static final int VEO_COST = 10;

    public FlowCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_CRAFTING_TABLE, pos, state);
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Flow Crafting Table");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new FlowCraftingTableScreenHandler(syncId, playerInventory, this);
    }

    public int countTPUs() {
        int count = 0;
        for (int i = 0; i < TPU_SLOTS; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && stack.isOf(ModItems.TPU)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean canCraftNanoBanana() {
        return countTPUs() >= NANO_BANANA_COST && inventory.get(OUTPUT_SLOT).isEmpty();
    }

    public boolean canCraftVeo() {
        return countTPUs() >= VEO_COST && inventory.get(OUTPUT_SLOT).isEmpty();
    }

    public void craftNanoBanana() {
        if (!canCraftNanoBanana()) return;

        // Remove 5 TPUs
        int toRemove = NANO_BANANA_COST;
        for (int i = 0; i < TPU_SLOTS && toRemove > 0; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && stack.isOf(ModItems.TPU)) {
                int removeFromSlot = Math.min(toRemove, stack.getCount());
                stack.decrement(removeFromSlot);
                toRemove -= removeFromSlot;
            }
        }

        // Add Nano Banana Console to output
        inventory.set(OUTPUT_SLOT, new ItemStack(ModBlocks.NANO_BANANA_CONSOLE));
        markDirty();
        playCraftEffects();
    }

    public void craftVeo() {
        if (!canCraftVeo()) return;

        // Remove 10 TPUs
        int toRemove = VEO_COST;
        for (int i = 0; i < TPU_SLOTS && toRemove > 0; i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty() && stack.isOf(ModItems.TPU)) {
                int removeFromSlot = Math.min(toRemove, stack.getCount());
                stack.decrement(removeFromSlot);
                toRemove -= removeFromSlot;
            }
        }

        // Add Veo Console to output
        inventory.set(OUTPUT_SLOT, new ItemStack(ModBlocks.VEO_CONSOLE));
        markDirty();
        playCraftEffects();
    }

    private void playCraftEffects() {
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_SMITHING_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 1.0;
            double cz = pos.getZ() + 0.5;
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy, cz, 8, 0.3, 0.3, 0.3, 0.0);
        }
    }

    // Inventory implementation
    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) return false;
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
        return true;
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // Only TPUs in input slots, nothing can be placed in output
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        return stack.isOf(ModItems.TPU);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, inventory, registryLookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt, inventory, registryLookup);
    }
}
