package com.labscraft.block.entity;

import com.labscraft.item.GeneratedArtifacts;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/** Veo Console: 3 TPU → a Generated Video, 200 ticks. */
public class VeoConsoleBlockEntity extends AbstractConsoleBlockEntity {
    public static final int TPU_COST = 3;
    public static final int GENERATION_TICKS = 200;

    public VeoConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VEO_CONSOLE, pos, state,
            "veo_console", GeneratedArtifacts.GENERATED_VIDEO, TPU_COST, GENERATION_TICKS);
    }
}
