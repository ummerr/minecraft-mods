package com.labscraft.block;

import com.labscraft.block.entity.AbstractConsoleBlockEntity;
import com.labscraft.block.entity.FlowConsoleBlockEntity;
import com.labscraft.block.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FlowConsoleBlock extends AbstractConsoleBlock {
    public static final MapCodec<FlowConsoleBlock> CODEC = createCodec(FlowConsoleBlock::new);

    public FlowConsoleBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlowConsoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.FLOW_CONSOLE, AbstractConsoleBlockEntity::tick);
    }
}
