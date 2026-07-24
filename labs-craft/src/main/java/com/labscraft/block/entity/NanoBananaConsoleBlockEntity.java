package com.labscraft.block.entity;

import com.labscraft.item.GeneratedArtifacts;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/** Nano Banana Console: 1 TPU → a Generated Image, 140 ticks. */
public class NanoBananaConsoleBlockEntity extends AbstractConsoleBlockEntity {
    public static final int TPU_COST = 1;
    public static final int GENERATION_TICKS = 140;

    public NanoBananaConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NANO_BANANA_CONSOLE, pos, state,
            "nano_banana_console", GeneratedArtifacts.GENERATED_IMAGE, TPU_COST, GENERATION_TICKS);
    }
}
