package com.labscraft.screen;

import com.labscraft.block.entity.FlowCraftingTableBlockEntity;
import com.labscraft.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FlowCraftingTableScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final FlowCraftingTableBlockEntity blockEntity;

    // Client constructor
    public FlowCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(11));
    }

    // Server constructor with block entity
    public FlowCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory, FlowCraftingTableBlockEntity blockEntity) {
        this(syncId, playerInventory, (Inventory) blockEntity);
        // Store block entity reference for server-side operations
    }

    // Constructor with inventory
    public FlowCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModScreenHandlers.FLOW_CRAFTING_TABLE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 11);
        this.inventory = inventory;
        this.blockEntity = inventory instanceof FlowCraftingTableBlockEntity ? (FlowCraftingTableBlockEntity) inventory : null;
        inventory.onOpen(playerInventory.player);

        // Add TPU input slots (2 rows of 5)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = row * 5 + col;
                this.addSlot(new Slot(inventory, slotIndex, 26 + col * 18, 26 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return stack.isOf(ModItems.TPU);
                    }
                });
            }
        }

        // Add output slot
        this.addSlot(new Slot(inventory, FlowCraftingTableBlockEntity.OUTPUT_SLOT, 134, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public int getTPUCount() {
        int count = 0;
        for (int i = 0; i < FlowCraftingTableBlockEntity.TPU_SLOTS; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isOf(ModItems.TPU)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean canCraftNanoBanana() {
        return getTPUCount() >= FlowCraftingTableBlockEntity.NANO_BANANA_COST
            && inventory.getStack(FlowCraftingTableBlockEntity.OUTPUT_SLOT).isEmpty();
    }

    public boolean canCraftVeo() {
        return getTPUCount() >= FlowCraftingTableBlockEntity.VEO_COST
            && inventory.getStack(FlowCraftingTableBlockEntity.OUTPUT_SLOT).isEmpty();
    }

    public void onCraftNanoBanana() {
        if (blockEntity != null) {
            blockEntity.craftNanoBanana();
        }
    }

    public void onCraftVeo() {
        if (blockEntity != null) {
            blockEntity.craftVeo();
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // If clicking on inventory slots (TPU input or output)
            if (slotIndex < 11) {
                // Move to player inventory
                if (!this.insertItem(originalStack, 11, 47, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // If clicking on player inventory, try to move TPUs to input slots
                if (originalStack.isOf(ModItems.TPU)) {
                    if (!this.insertItem(originalStack, 0, 10, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
