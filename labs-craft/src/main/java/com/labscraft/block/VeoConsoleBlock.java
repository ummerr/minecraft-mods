package com.labscraft.block;

import com.labscraft.block.entity.VeoConsoleBlockEntity;
import com.labscraft.block.entity.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class VeoConsoleBlock extends BlockWithEntity {
    public static final MapCodec<VeoConsoleBlock> CODEC = createCodec(VeoConsoleBlock::new);

    public VeoConsoleBlock(Settings settings) {
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
        return new VeoConsoleBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof VeoConsoleBlockEntity consoleEntity) {
                player.openHandledScreen(consoleEntity);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof VeoConsoleBlockEntity console && console.isGenerating()) {
            double x = pos.getX() + 0.5 + random.nextDouble() * 0.6 - 0.3;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + random.nextDouble() * 0.6 - 0.3;
            world.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z, 0.0, 0.05, 0.0);
            if (random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.PORTAL, x, y + 0.3, z,
                    random.nextDouble() * 0.2 - 0.1, 0.1, random.nextDouble() * 0.2 - 0.1);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.VEO_CONSOLE, VeoConsoleBlockEntity::tick);
    }
}
