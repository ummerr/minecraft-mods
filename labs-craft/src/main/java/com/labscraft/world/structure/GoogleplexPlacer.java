package com.labscraft.world.structure;

import com.labscraft.LabsCraft;
import com.labscraft.block.ModBlocks;
import com.labscraft.world.structure.plan.BlockSpec;
import com.labscraft.world.structure.plan.BlueprintValidator;
import com.labscraft.world.structure.plan.GoogleplexBlueprint;
import com.labscraft.world.structure.plan.GoogleplexPlanner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Thin Minecraft-facing layer: translates a validated
 * {@link GoogleplexBlueprint} into real block placement. All layout math
 * lives in the pure {@code plan} package; this class only maps
 * {@link BlockSpec}s to {@link BlockState}s and handles terrain (foundation
 * below, deliberate clearance above the roof).
 *
 * <p>The origin is the structure's minimum (x, y, z) corner; the floor slab
 * sits at the origin's Y level. Blueprint axes map 1:1 to world axes, so the
 * blueprint's z=0 entrance face is the north face of the placed building.</p>
 */
public final class GoogleplexPlacer {

    /** How far above the roof the footprint is deliberately cleared. */
    public static final int CLEARANCE_ABOVE = 6;
    /** Maximum depth of the foundation fill below the floor. */
    public static final int MAX_FOUNDATION_DEPTH = 12;

    public record Result(BlockPos min, BlockPos max, long seed,
                         int blocksPlaced, int foundationBlocks, int clearedAbove) {
    }

    private GoogleplexPlacer() {
    }

    /**
     * Scans the target volume (structure box + overhead clearance) for block
     * entities — chests, furnaces, consoles and the like are the strongest
     * signal of a player build. The command layer refuses to place over them
     * unless the caller passes {@code force}.
     */
    public static List<BlockPos> findBlockEntities(ServerWorld world, BlockPos origin) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos max = origin.add(GoogleplexBlueprint.SIZE_X - 1,
                GoogleplexBlueprint.SIZE_Y - 1 + CLEARANCE_ABOVE,
                GoogleplexBlueprint.SIZE_Z - 1);
        for (BlockPos pos : BlockPos.iterate(origin, max)) {
            if (world.getBlockEntity(pos) != null) {
                found.add(pos.toImmutable());
            }
        }
        return found;
    }

    /**
     * Plans, validates and places the office. Throws
     * {@link IllegalStateException} if the blueprint fails validation (which
     * would indicate a planner bug — the pure layer is tested against this).
     */
    public static Result place(ServerWorld world, BlockPos origin, long seed) {
        GoogleplexBlueprint blueprint = GoogleplexPlanner.plan(seed);
        List<String> problems = BlueprintValidator.validate(blueprint);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Blueprint failed validation: " + problems);
        }

        int foundationBlocks = placeFoundation(world, origin);
        int clearedAbove = clearAboveRoof(world, origin);
        int blocksPlaced = placeBlueprint(world, origin, blueprint);

        BlockPos max = origin.add(GoogleplexBlueprint.SIZE_X - 1,
                GoogleplexBlueprint.SIZE_Y - 1, GoogleplexBlueprint.SIZE_Z - 1);
        Result result = new Result(origin.toImmutable(), max, seed,
                blocksPlaced, foundationBlocks, clearedAbove);
        LabsCraft.LOGGER.info("Googleplex placed: bounds {}..{} seed={} blocks={} foundation={} cleared={}",
                result.min().toShortString(), result.max().toShortString(),
                seed, blocksPlaced, foundationBlocks, clearedAbove);
        return result;
    }

    /** Fills below every footprint column until solid ground, so nothing floats. */
    private static int placeFoundation(ServerWorld world, BlockPos origin) {
        int placed = 0;
        BlockState foundation = Blocks.STONE_BRICKS.getDefaultState();
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                for (int dy = 1; dy <= MAX_FOUNDATION_DEPTH; dy++) {
                    BlockPos pos = origin.add(x, -dy, z);
                    BlockState existing = world.getBlockState(pos);
                    if (existing.isSolidBlock(world, pos)) {
                        break;
                    }
                    world.setBlockState(pos, foundation, Block.NOTIFY_LISTENERS);
                    placed++;
                }
            }
        }
        return placed;
    }

    /** Deliberately clears terrain intruding above the roof within the footprint. */
    private static int clearAboveRoof(ServerWorld world, BlockPos origin) {
        int cleared = 0;
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                for (int y = GoogleplexBlueprint.SIZE_Y; y < GoogleplexBlueprint.SIZE_Y + CLEARANCE_ABOVE; y++) {
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

    private static int placeBlueprint(ServerWorld world, BlockPos origin, GoogleplexBlueprint blueprint) {
        int placed = 0;
        // y ascending so supports (floor, counters, desks) exist before the
        // blocks that sit on them (carpet, cake, lamps).
        for (int y = 0; y < GoogleplexBlueprint.SIZE_Y; y++) {
            for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
                for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                    BlockState state = toState(blueprint.get(x, y, z));
                    BlockPos pos = origin.add(x, y, z);
                    if (world.getBlockState(pos) != state) {
                        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
                        placed++;
                    }
                }
            }
        }
        return placed;
    }

    /** The palette: abstract blueprint materials to concrete block states. */
    private static BlockState toState(BlockSpec spec) {
        return switch (spec) {
            case AIR, DOOR_AIR -> Blocks.AIR.getDefaultState();

            case FLOOR -> Blocks.SMOOTH_QUARTZ.getDefaultState();
            case FLOOR_LAB -> Blocks.GRAY_CONCRETE.getDefaultState();
            case WALL -> Blocks.WHITE_CONCRETE.getDefaultState();
            case WALL_ACCENT_BLUE -> Blocks.BLUE_CONCRETE.getDefaultState();
            case WALL_ACCENT_RED -> Blocks.RED_CONCRETE.getDefaultState();
            case WALL_ACCENT_YELLOW -> Blocks.YELLOW_CONCRETE.getDefaultState();
            case WALL_ACCENT_GREEN -> Blocks.LIME_CONCRETE.getDefaultState();
            case GLASS -> Blocks.GLASS.getDefaultState();
            case CEILING -> Blocks.WHITE_CONCRETE.getDefaultState();
            case LIGHT -> Blocks.SEA_LANTERN.getDefaultState();

            case CARPET_BLUE -> Blocks.BLUE_CARPET.getDefaultState();
            case CARPET_RED -> Blocks.RED_CARPET.getDefaultState();
            case CARPET_YELLOW -> Blocks.YELLOW_CARPET.getDefaultState();
            case CARPET_GREEN -> Blocks.LIME_CARPET.getDefaultState();

            case DESK -> Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState()
                    .with(Properties.SLAB_TYPE, SlabType.TOP);
            case DESK_LAMP -> Blocks.END_ROD.getDefaultState();
            case TABLE -> Blocks.DARK_OAK_SLAB.getDefaultState()
                    .with(Properties.SLAB_TYPE, SlabType.TOP);
            // A chair's occupant faces the named direction; the stair's high
            // back is on the opposite side.
            case CHAIR_NORTH -> chair(Direction.SOUTH);
            case CHAIR_SOUTH -> chair(Direction.NORTH);
            case CHAIR_EAST -> chair(Direction.WEST);
            case CHAIR_WEST -> chair(Direction.EAST);
            case RECEPTION -> Blocks.QUARTZ_BLOCK.getDefaultState();
            case COUNTER -> Blocks.SMOOTH_STONE.getDefaultState();
            case BARREL -> Blocks.BARREL.getDefaultState();
            case CAKE -> Blocks.CAKE.getDefaultState();
            case FRIDGE -> Blocks.IRON_BLOCK.getDefaultState();
            case PLANT_FERN -> Blocks.POTTED_FERN.getDefaultState();
            case PLANT_BAMBOO -> Blocks.POTTED_BAMBOO.getDefaultState();
            case SCREEN -> Blocks.BLACK_CONCRETE.getDefaultState();

            case SERVER_RACK -> Blocks.IRON_BLOCK.getDefaultState();
            case SERVER_LIGHT -> Blocks.SEA_LANTERN.getDefaultState();
            case CONSOLE_FLOW -> ModBlocks.FLOW_CONSOLE.getDefaultState();
            case CONSOLE_NANO_BANANA -> ModBlocks.NANO_BANANA_CONSOLE.getDefaultState();
            case CONSOLE_VEO -> ModBlocks.VEO_CONSOLE.getDefaultState();
            case FLOW_CRAFTING_TABLE -> ModBlocks.FLOW_CRAFTING_TABLE.getDefaultState();
        };
    }

    private static BlockState chair(Direction stairFacing) {
        return Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, stairFacing);
    }
}
