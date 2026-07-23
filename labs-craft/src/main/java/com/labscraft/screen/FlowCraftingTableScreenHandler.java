package com.labscraft.screen;

import com.labscraft.block.entity.FlowCraftingTableBlockEntity;
import com.labscraft.item.ModItems;
import com.labscraft.logic.FlowCraftingLogic;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Flow Crafting Table screen handler: 10 TPU input slots (2x5), one output
 * slot, and two craft buttons via the vanilla button-click mechanism
 * ({@link #CRAFT_NANO_BANANA_BUTTON_ID}, {@link #CRAFT_VEO_BUTTON_ID}).
 */
public class FlowCraftingTableScreenHandler extends ScreenHandler {
    public static final int CRAFT_NANO_BANANA_BUTTON_ID = 0;
    public static final int CRAFT_VEO_BUTTON_ID = 1;

    private static final int BE_SLOTS = FlowCraftingTableBlockEntity.TPU_SLOTS + 1;

    private final Inventory inventory;
    @Nullable
    private final FlowCraftingTableBlockEntity blockEntity;

    /** Client constructor. */
    public FlowCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(BE_SLOTS), null);
    }

    /** Server constructor. */
    public FlowCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory,
            FlowCraftingTableBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity, blockEntity);
    }

    private FlowCraftingTableScreenHandler(int syncId, PlayerInventory playerInventory,
            Inventory inventory, @Nullable FlowCraftingTableBlockEntity blockEntity) {
        super(ModScreenHandlers.FLOW_CRAFTING_TABLE, syncId);
        checkSize(inventory, BE_SLOTS);
        this.inventory = inventory;
        this.blockEntity = blockEntity;
        inventory.onOpen(playerInventory.player);

        // TPU input slots (2 rows of 5)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = row * 5 + col;
                this.addSlot(new Slot(inventory, slotIndex, 26 + col * 18, 22 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return stack.isOf(ModItems.TPU);
                    }
                });
            }
        }

        // Output slot (extract only)
        this.addSlot(new Slot(inventory, FlowCraftingTableBlockEntity.OUTPUT_SLOT, 134, 31) {
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
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (blockEntity == null || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        if (id == CRAFT_NANO_BANANA_BUTTON_ID) {
            blockEntity.craftNanoBanana(serverPlayer);
            return true;
        }
        if (id == CRAFT_VEO_BUTTON_ID) {
            blockEntity.craftVeo(serverPlayer);
            return true;
        }
        return false;
    }

    public int getTpuCount() {
        int count = 0;
        for (int i = 0; i < FlowCraftingTableBlockEntity.TPU_SLOTS; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(ModItems.TPU)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public boolean canCraftNanoBanana() {
        return getTpuCount() >= FlowCraftingLogic.NANO_BANANA_COST
            && inventory.getStack(FlowCraftingTableBlockEntity.OUTPUT_SLOT).isEmpty();
    }

    public boolean canCraftVeo() {
        return getTpuCount() >= FlowCraftingLogic.VEO_COST
            && inventory.getStack(FlowCraftingTableBlockEntity.OUTPUT_SLOT).isEmpty();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < BE_SLOTS) {
                if (!this.insertItem(originalStack, BE_SLOTS, BE_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (originalStack.isOf(ModItems.TPU)) {
                if (!this.insertItem(originalStack, 0, FlowCraftingTableBlockEntity.TPU_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
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
