package com.labscraft.block;

import com.labscraft.block.entity.AbstractConsoleBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Shared behavior for the three generation console blocks. */
public abstract class AbstractConsoleBlock extends BlockWithEntity {
    protected AbstractConsoleBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AbstractConsoleBlockEntity console) {
                player.openHandledScreen(console);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof AbstractConsoleBlockEntity console) {
                ItemScatterer.spawn(world, pos, console);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof AbstractConsoleBlockEntity console && console.isGenerating()) {
            double x = pos.getX() + 0.5 + random.nextDouble() * 0.6 - 0.3;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + random.nextDouble() * 0.6 - 0.3;
            world.addParticle(ParticleTypes.ENCHANT, x, y, z, 0.0, 0.2, 0.0);
            if (random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.END_ROD, x, y + 0.2, z, 0.0, 0.05, 0.0);
            }
        }
    }
}
