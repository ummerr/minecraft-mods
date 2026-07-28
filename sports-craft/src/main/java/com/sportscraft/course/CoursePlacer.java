package com.sportscraft.course;

import com.sportscraft.SportsCraft;
import com.sportscraft.course.plan.HoleBlueprint;
import com.sportscraft.course.plan.HolePlanner;
import com.sportscraft.course.plan.HoleValidator;
import com.sportscraft.course.plan.SurfaceCell;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Thin Minecraft-facing layer: turns a validated {@link HoleBlueprint} into real
 * blocks. All layout maths lives in the pure {@code plan} package; this class
 * only maps {@link SurfaceCell}s to block states and handles the terrain work
 * around them — foundation below, clearance above.
 *
 * <p>The origin is the hole's minimum corner and the playing surface sits at the
 * origin's Y, so a ball at rest is at {@code origin.y + 1}. Blueprint axes map
 * 1:1 to world axes: blueprint z=0 is the tee, so a placed hole plays south.</p>
 */
public final class CoursePlacer {

    /** Headroom kept clear above the surface so drives are not clipped. */
    public static final int CLEARANCE_ABOVE = 16;

    /** Maximum depth of foundation fill below the surface. */
    public static final int MAX_FOUNDATION_DEPTH = 8;

    public record Result(BlockPos min, BlockPos max, BlockPos tee, BlockPos cup,
                         int par, int holeLength, long seed,
                         int blocksPlaced, int foundationBlocks, int clearedAbove) {
    }

    private CoursePlacer() {
    }

    /**
     * Scans the target volume for block entities — chests, furnaces and the like
     * are the strongest signal of a player build. The command layer refuses to
     * place over them without {@code force}.
     */
    public static List<BlockPos> findBlockEntities(ServerWorld world, BlockPos origin, long seed) {
        HoleBlueprint blueprint = HolePlanner.plan(seed);
        List<BlockPos> found = new ArrayList<>();
        BlockPos max = origin.add(blueprint.width() - 1, CLEARANCE_ABOVE, blueprint.length() - 1);
        for (BlockPos pos : BlockPos.iterate(origin, max)) {
            if (world.getBlockEntity(pos) != null) {
                found.add(pos.toImmutable());
            }
        }
        return found;
    }

    /**
     * Plans, validates and places a hole. Throws {@link IllegalStateException}
     * if validation fails, which would mean a planner bug — the pure layer's
     * 50-seed sweep exists to make that impossible.
     */
    public static Result place(ServerWorld world, BlockPos origin, long seed) {
        HoleBlueprint blueprint = HolePlanner.plan(seed);
        List<String> problems = HoleValidator.validate(blueprint);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Hole failed validation: " + problems);
        }

        int clearedAbove = clearAbove(world, origin, blueprint);
        int foundationBlocks = placeFoundation(world, origin, blueprint);
        int blocksPlaced = placeSurface(world, origin, blueprint);
        placeFlag(world, origin, blueprint);

        BlockPos min = origin.toImmutable();
        BlockPos max = origin.add(blueprint.width() - 1, 0, blueprint.length() - 1);
        BlockPos tee = origin.add(blueprint.teeX(), 1, blueprint.teeZ());
        BlockPos cup = origin.add(blueprint.cupX(), 0, blueprint.cupZ());

        Result result = new Result(min, max, tee, cup, blueprint.par(), blueprint.holeLength(),
                seed, blocksPlaced, foundationBlocks, clearedAbove);
        SportsCraft.LOGGER.info("Golf hole placed: bounds {}..{} seed={} par={} length={} blocks={}",
                min.toShortString(), max.toShortString(), seed,
                blueprint.par(), blueprint.holeLength(), blocksPlaced);
        return result;
    }

    /** Deliberately clears anything above the playing surface. */
    private static int clearAbove(ServerWorld world, BlockPos origin, HoleBlueprint bp) {
        int cleared = 0;
        for (int x = 0; x < bp.width(); x++) {
            for (int z = 0; z < bp.length(); z++) {
                for (int y = 1; y <= CLEARANCE_ABOVE; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (!world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                        cleared++;
                    }
                }
            }
        }
        return cleared;
    }

    /** Fills below every column until solid ground, so no hole floats. */
    private static int placeFoundation(ServerWorld world, BlockPos origin, HoleBlueprint bp) {
        int placed = 0;
        BlockState foundation = Blocks.DIRT.getDefaultState();
        for (int x = 0; x < bp.width(); x++) {
            for (int z = 0; z < bp.length(); z++) {
                // The cup needs one extra block of depth for its floor.
                int startDepth = bp.get(x, z) == SurfaceCell.CUP ? 2 : 1;
                for (int dy = startDepth; dy <= MAX_FOUNDATION_DEPTH; dy++) {
                    BlockPos pos = origin.add(x, -dy, z);
                    if (world.getBlockState(pos).isSolidBlock(world, pos)) {
                        break;
                    }
                    world.setBlockState(pos, foundation, Block.NOTIFY_LISTENERS);
                    placed++;
                }
            }
        }
        return placed;
    }

    private static int placeSurface(ServerWorld world, BlockPos origin, HoleBlueprint bp) {
        int placed = 0;
        for (int x = 0; x < bp.width(); x++) {
            for (int z = 0; z < bp.length(); z++) {
                SurfaceCell cell = bp.get(x, z);
                BlockPos pos = origin.add(x, 0, z);

                if (cell == SurfaceCell.CUP) {
                    // A one-block pit: air at the surface, a bright floor below
                    // so the ball is unmistakably in the hole.
                    world.setBlockState(pos.down(), Blocks.YELLOW_CONCRETE.getDefaultState(),
                            Block.NOTIFY_LISTENERS);
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    placed += 2;
                    continue;
                }

                world.setBlockState(pos, toState(cell), Block.NOTIFY_LISTENERS);
                placed++;
            }
        }
        return placed;
    }

    /**
     * The flagstick, planted on a green cell next to the cup rather than in it —
     * a fence in the cup would stop the ball dropping.
     */
    private static void placeFlag(ServerWorld world, BlockPos origin, HoleBlueprint bp) {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            int fx = bp.cupX() + offset[0];
            int fz = bp.cupZ() + offset[1];
            if (bp.get(fx, fz) != SurfaceCell.GREEN) {
                continue;
            }
            BlockPos base = origin.add(fx, 1, fz);
            world.setBlockState(base, Blocks.OAK_FENCE.getDefaultState(), Block.NOTIFY_LISTENERS);
            world.setBlockState(base.up(), Blocks.OAK_FENCE.getDefaultState(), Block.NOTIFY_LISTENERS);
            world.setBlockState(base.up(2), Blocks.RED_BANNER.getDefaultState(), Block.NOTIFY_LISTENERS);
            return;
        }
    }

    /** The palette. {@code SurfaceClassifier} must agree with these choices. */
    private static BlockState toState(SurfaceCell cell) {
        return switch (cell) {
            case TEE -> Blocks.SMOOTH_SANDSTONE.getDefaultState();
            case FAIRWAY -> Blocks.GRASS_BLOCK.getDefaultState();
            case GREEN -> Blocks.MOSS_BLOCK.getDefaultState();
            case ROUGH -> Blocks.COARSE_DIRT.getDefaultState();
            case SAND -> Blocks.SAND.getDefaultState();
            case WATER -> Blocks.WATER.getDefaultState();
            // Handled separately in placeSurface; listed so the switch stays total.
            case CUP -> Blocks.YELLOW_CONCRETE.getDefaultState();
        };
    }
}
