package com.labscraft.screen;

import com.labscraft.block.entity.AbstractConsoleBlockEntity;
import com.labscraft.item.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Shared screen handler for the three generation consoles: one TPU input slot,
 * one output slot, a Generate button (vanilla button-click mechanism, id
 * {@link #GENERATE_BUTTON_ID}) and a progress property delegate.
 */
public class ConsoleScreenHandler extends ScreenHandler {
    public static final int GENERATE_BUTTON_ID = 0;

    /** Property delegate indices. */
    public static final int PROP_PROGRESS = 0;
    public static final int PROP_GENERATING = 1;
    public static final int PROP_COMPLETED = 2;
    public static final int PROP_TOTAL_TICKS = 3;
    public static final int PROP_TPU_COST = 4;

    private static final int BE_SLOTS = 2;

    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    @Nullable
    private final AbstractConsoleBlockEntity blockEntity;

    /** Client constructor. */
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(BE_SLOTS), new ArrayPropertyDelegate(5), null);
    }

    /** Server constructor. */
    public ConsoleScreenHandler(int syncId, PlayerInventory playerInventory,
            AbstractConsoleBlockEntity blockEntity, PropertyDelegate propertyDelegate) {
        this(syncId, playerInventory, blockEntity, propertyDelegate, blockEntity);
    }

    private ConsoleScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
            PropertyDelegate propertyDelegate, @Nullable AbstractConsoleBlockEntity blockEntity) {
        super(ModScreenHandlers.CONSOLE, syncId);
        checkSize(inventory, BE_SLOTS);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.blockEntity = blockEntity;
        inventory.onOpen(playerInventory.player);

        // TPU input slot
        this.addSlot(new Slot(inventory, AbstractConsoleBlockEntity.INPUT_SLOT, 35, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(ModItems.TPU);
            }
        });

        // Output slot (extract only)
        this.addSlot(new Slot(inventory, AbstractConsoleBlockEntity.OUTPUT_SLOT, 125, 35) {
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

        this.addProperties(propertyDelegate);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == GENERATE_BUTTON_ID && blockEntity != null && player instanceof ServerPlayerEntity serverPlayer) {
            blockEntity.tryStartGeneration(serverPlayer);
            return true;
        }
        return false;
    }

    public boolean isGenerating() {
        return propertyDelegate.get(PROP_GENERATING) == 1;
    }

    public int getProgress() {
        return propertyDelegate.get(PROP_PROGRESS);
    }

    public int getCompletedCount() {
        return propertyDelegate.get(PROP_COMPLETED);
    }

    public int getTotalTicks() {
        return propertyDelegate.get(PROP_TOTAL_TICKS);
    }

    public int getTpuCost() {
        return propertyDelegate.get(PROP_TPU_COST);
    }

    public float getProgressFraction() {
        int total = getTotalTicks();
        return total == 0 ? 0.0f : (float) getProgress() / (float) total;
    }

    /** True if the input holds enough TPUs and the output slot is free. */
    public boolean canStart() {
        ItemStack input = inventory.getStack(AbstractConsoleBlockEntity.INPUT_SLOT);
        int available = input.isOf(ModItems.TPU) ? input.getCount() : 0;
        return !isGenerating()
            && available >= Math.max(1, getTpuCost())
            && inventory.getStack(AbstractConsoleBlockEntity.OUTPUT_SLOT).isEmpty();
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
                if (!this.insertItem(originalStack, 0, 1, false)) {
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
