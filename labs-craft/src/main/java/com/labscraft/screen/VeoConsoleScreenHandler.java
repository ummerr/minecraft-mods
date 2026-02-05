package com.labscraft.screen;

import com.labscraft.block.entity.VeoConsoleBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;

public class VeoConsoleScreenHandler extends ScreenHandler {
    private final VeoConsoleBlockEntity blockEntity;
    private final PropertyDelegate propertyDelegate;

    // Client constructor
    public VeoConsoleScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null, new ArrayPropertyDelegate(4));
    }

    // Server constructor
    public VeoConsoleScreenHandler(int syncId, PlayerInventory playerInventory,
                                   VeoConsoleBlockEntity blockEntity, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.VEO_CONSOLE_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.propertyDelegate = propertyDelegate;

        this.addProperties(propertyDelegate);
    }

    public boolean isGenerating() {
        return propertyDelegate.get(1) == 1;
    }

    public int getGenerationProgress() {
        return propertyDelegate.get(0);
    }

    public int getTotalGenerations() {
        return propertyDelegate.get(2);
    }

    public int getGenerationTime() {
        return propertyDelegate.get(3);
    }

    public float getGenerationProgressPercent() {
        int progress = getGenerationProgress();
        int maxProgress = getGenerationTime();
        if (maxProgress == 0) return 0;
        return (float) progress / (float) maxProgress;
    }

    public void onGenerateButtonClicked(PlayerEntity player) {
        if (blockEntity != null && !isGenerating()) {
            blockEntity.startGeneration();
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
