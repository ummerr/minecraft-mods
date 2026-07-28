package com.sportscraft.course;

import com.sportscraft.core.physics.SurfaceType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Reads the block under the ball and decides how it should behave.
 *
 * <p>Deliberately world-driven rather than course-driven: nothing here consults
 * a registered hole or any course metadata. That means a ball behaves sensibly
 * on terrain the player built themselves, on a hole generated months ago, and
 * on the driving range — and it means the course generator's only job is to
 * place the right blocks, not to maintain a parallel map of what is fairway.</p>
 */
public final class SurfaceClassifier {

    private SurfaceClassifier() {
    }

    /** Classifies the block directly beneath {@code ballPos}. */
    public static SurfaceType below(World world, BlockPos ballPos) {
        return classify(world, ballPos.down());
    }

    /** Classifies a specific block position. */
    public static SurfaceType classify(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        if (!state.getFluidState().isEmpty()) {
            return SurfaceType.WATER;
        }
        return classify(state);
    }

    /**
     * The palette contract, kept in one place so {@code CoursePlacer} and the
     * physics agree on what a green is. Moss is the green, grass the fairway,
     * coarse dirt the rough, and anything sandy is a bunker.
     */
    public static SurfaceType classify(BlockState state) {
        if (state.isOf(Blocks.WATER) || state.isOf(Blocks.BUBBLE_COLUMN) || !state.getFluidState().isEmpty()) {
            return SurfaceType.WATER;
        }
        if (state.isOf(Blocks.MOSS_BLOCK) || state.isOf(Blocks.MOSS_CARPET)) {
            return SurfaceType.GREEN;
        }
        if (state.isOf(Blocks.GRASS_BLOCK) || state.isOf(Blocks.MYCELIUM)) {
            return SurfaceType.FAIRWAY;
        }
        if (state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.ROOTED_DIRT)
                || state.isOf(Blocks.DIRT) || state.isOf(Blocks.PODZOL)) {
            return SurfaceType.ROUGH;
        }
        // Loose sand and gravel only. Sandstone is deliberately NOT a bunker:
        // the tee pad is smooth sandstone, and a tee that played like sand
        // would kill every opening drive.
        if (state.isIn(BlockTags.SAND) || state.isOf(Blocks.GRAVEL)
                || state.isOf(Blocks.SUSPICIOUS_SAND) || state.isOf(Blocks.SUSPICIOUS_GRAVEL)) {
            return SurfaceType.SAND;
        }
        return SurfaceType.GENERIC;
    }
}
