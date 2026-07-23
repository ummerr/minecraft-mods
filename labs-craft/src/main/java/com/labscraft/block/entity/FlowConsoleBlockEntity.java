package com.labscraft.block.entity;

import com.labscraft.item.GeneratedArtifacts;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/** Flow Console: 1 TPU → a Flow Sketch, 100 ticks. */
public class FlowConsoleBlockEntity extends AbstractConsoleBlockEntity {
    public static final int TPU_COST = 1;
    public static final int GENERATION_TICKS = 100;

    public FlowConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOW_CONSOLE, pos, state,
            "flow_console", GeneratedArtifacts.FLOW_SKETCH, TPU_COST, GENERATION_TICKS);
    }
}
