package com.labscraft.block;

import com.labscraft.block.entity.FlowCraftingTableBlockEntity;
import com.labscraft.block.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FlowCraftingTableBlock extends BlockWithEntity {
    public static final MapCodec<FlowCraftingTableBlock> CODEC = createCodec(FlowCraftingTableBlock::new);

    public FlowCraftingTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlowCraftingTableBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FlowCraftingTableBlockEntity craftingEntity) {
                player.openHandledScreen(craftingEntity);
            }
        }
        return ActionResult.SUCCESS;
    }
}
