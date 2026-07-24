package com.labscraft.block;

import com.labscraft.block.entity.AbstractConsoleBlockEntity;
import com.labscraft.block.entity.ModBlockEntities;
import com.labscraft.block.entity.VeoConsoleBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class VeoConsoleBlock extends AbstractConsoleBlock {
    public static final MapCodec<VeoConsoleBlock> CODEC = createCodec(VeoConsoleBlock::new);

    public VeoConsoleBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new VeoConsoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.VEO_CONSOLE, AbstractConsoleBlockEntity::tick);
    }
}
