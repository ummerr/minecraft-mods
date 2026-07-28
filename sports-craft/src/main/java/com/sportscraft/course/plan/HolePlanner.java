package com.sportscraft.course.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Deterministically plans a golf hole from a seed. Pure Java — no Minecraft
 * imports — so the entire layout is unit-testable, exactly like
 * {@code GoogleplexPlanner}.
 *
 * <p>Seeded draws happen in a fixed order so output is reproducible: length,
 * then the meandering centreline, then fairway width, then the green, then the
 * cup, then hazards. Adding a draw in the middle of that sequence reshuffles
 * every hole, which is why {@code HolePlannerTest} pins determinism.</p>
 *
 * <pre>
 *  z = length-1   . . . R R R R R . . .      R rough
 *                 . . R R G G G R R . .      G green (elliptical)
 *                 . . R R G C G R R . .      C cup
 *                 . . R R G G G R R . .
 *                 . . R F F F F F R . .      F fairway (undulating width)
 *       ~ mid     . . R F F F W W R . .      W water, never spanning the fairway
 *                 . . R F F F F F R . .
 *  z = 0          . . R R T T T R R . .      T 3x3 tee pad
 * </pre>
 */
public final class HolePlanner {

    /** Grid is fixed-width; only the playable corridor inside it varies. */
    public static final int WIDTH = 25;

    public static final int MIN_LENGTH = 40;
    public static final int MAX_LENGTH = 80;

    private static final int TEE_CENTER_Z = 2;
    private static final int TEE_HALF = 1;

    private static final int MIN_FAIRWAY_HALF = 3;  // 7 wide
    private static final int MAX_FAIRWAY_HALF = 5;  // 11 wide

    private static final int ROUGH_BORDER = 3;

    /** How far the centreline may stray from the middle of the grid. */
    private static final int MAX_DRIFT = 4;

    /** Hazards stay this far from the tee and the cup. */
    private static final int TEE_CLEARANCE = 12;
    private static final int GREEN_CLEARANCE = 3;

    private HolePlanner() {
    }

    public static HoleBlueprint plan(long seed) {
        Random random = new Random(seed);

        int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
        int[] centerline = buildCenterline(random, length);
        int[] fairwayHalf = buildFairwayWidths(random, length);

        SurfaceCell[][] grid = filled(length, SurfaceCell.ROUGH);

        int greenZ = length - 7;
        int greenRadiusX = 4 + random.nextInt(3);   // 4..6
        int greenRadiusZ = 4 + random.nextInt(3);
        int greenCenterX = centerline[greenZ];

        carveFairway(grid, centerline, fairwayHalf, length, greenZ);
        carveGreen(grid, greenCenterX, greenZ, greenRadiusX, greenRadiusZ);

        int teeX = centerline[TEE_CENTER_Z];
        carveTee(grid, teeX);

        int[] cup = placeCup(random, grid, greenCenterX, greenZ);
        grid[cup[0]][cup[1]] = SurfaceCell.CUP;

        List<HoleBlueprint.Hazard> hazards = placeHazards(
                random, grid, centerline, fairwayHalf, length, greenCenterX, greenZ,
                greenRadiusX, greenRadiusZ, cup);

        int par = ParCalculator.parFor(distance(teeX, TEE_CENTER_Z, cup[0], cup[1]));
        return new HoleBlueprint(grid, teeX, TEE_CENTER_Z, cup[0], cup[1], par, seed, hazards);
    }

    // ------------------------------------------------------------------
    // Skeleton
    // ------------------------------------------------------------------

    private static SurfaceCell[][] filled(int length, SurfaceCell cell) {
        SurfaceCell[][] grid = new SurfaceCell[WIDTH][length];
        for (SurfaceCell[] column : grid) {
            Arrays.fill(column, cell);
        }
        return grid;
    }

    /**
     * A gentle random walk down the hole. Steps are at most one cell per row so
     * the corridor never kinks hard enough to be unplayable, and the walk is
     * clamped so the rough border always survives at the grid edge.
     */
    private static int[] buildCenterline(Random random, int length) {
        int[] centerline = new int[length];
        int middle = WIDTH / 2;
        int current = middle;
        for (int z = 0; z < length; z++) {
            // Hold the line near the tee so the opening shot is honest.
            if (z > TEE_CENTER_Z + 2 && random.nextInt(3) == 0) {
                current += random.nextBoolean() ? 1 : -1;
                current = Math.max(middle - MAX_DRIFT, Math.min(middle + MAX_DRIFT, current));
            }
            centerline[z] = current;
        }
        return centerline;
    }

    /** Fairway breathes in and out along the hole rather than being a corridor. */
    private static int[] buildFairwayWidths(Random random, int length) {
        int[] widths = new int[length];
        int current = MIN_FAIRWAY_HALF + random.nextInt(MAX_FAIRWAY_HALF - MIN_FAIRWAY_HALF + 1);
        for (int z = 0; z < length; z++) {
            if (random.nextInt(6) == 0) {
                current += random.nextBoolean() ? 1 : -1;
                current = Math.max(MIN_FAIRWAY_HALF, Math.min(MAX_FAIRWAY_HALF, current));
            }
            widths[z] = current;
        }
        return widths;
    }

    private static void carveFairway(SurfaceCell[][] grid, int[] centerline, int[] fairwayHalf,
                                     int length, int greenZ) {
        for (int z = 0; z < length; z++) {
            int half = fairwayHalf[z];
            for (int dx = -half; dx <= half; dx++) {
                int x = centerline[z] + dx;
                if (x >= ROUGH_BORDER && x < WIDTH - ROUGH_BORDER) {
                    grid[x][z] = SurfaceCell.FAIRWAY;
                }
            }
        }
    }

    private static void carveGreen(SurfaceCell[][] grid, int cx, int cz, int rx, int rz) {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < grid[0].length; z++) {
                double nx = (x - cx) / (double) rx;
                double nz = (z - cz) / (double) rz;
                if (nx * nx + nz * nz <= 1.0) {
                    grid[x][z] = SurfaceCell.GREEN;
                }
            }
        }
    }

    private static void carveTee(SurfaceCell[][] grid, int teeX) {
        for (int dx = -TEE_HALF; dx <= TEE_HALF; dx++) {
            for (int dz = -TEE_HALF; dz <= TEE_HALF; dz++) {
                int x = teeX + dx;
                int z = TEE_CENTER_Z + dz;
                if (x >= 0 && x < WIDTH && z >= 0 && z < grid[0].length) {
                    grid[x][z] = SurfaceCell.TEE;
                }
            }
        }
    }

    /**
     * The cup sits inside the green with green on all four sides. Rather than
     * offsetting from the centre and hoping — which puts the cup on the ellipse
     * edge for some seeds — this picks from the cells that actually satisfy the
     * invariant the validator checks.
     */
    private static int[] placeCup(Random random, SurfaceCell[][] grid, int cx, int cz) {
        List<int[]> candidates = new ArrayList<>();
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int z = 1; z < grid[0].length - 1; z++) {
                if (isSurroundedByGreen(grid, x, z)) {
                    candidates.add(new int[]{x, z});
                }
            }
        }
        if (candidates.isEmpty()) {
            return new int[]{cx, cz};
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static boolean isSurroundedByGreen(SurfaceCell[][] grid, int x, int z) {
        return grid[x][z] == SurfaceCell.GREEN
                && grid[x + 1][z] == SurfaceCell.GREEN
                && grid[x - 1][z] == SurfaceCell.GREEN
                && grid[x][z + 1] == SurfaceCell.GREEN
                && grid[x][z - 1] == SurfaceCell.GREEN;
    }

    // ------------------------------------------------------------------
    // Hazards
    // ------------------------------------------------------------------

    /**
     * Zero to two hazards: a bunker beside the green, and water partway down the
     * fairway. Water is deliberately carved as a bite out of one side and never
     * spans the corridor, so a dry route to the green always exists — the
     * validator's BFS would reject the hole otherwise.
     */
    private static List<HoleBlueprint.Hazard> placeHazards(
            Random random, SurfaceCell[][] grid, int[] centerline, int[] fairwayHalf,
            int length, int greenCenterX, int greenZ, int greenRx, int greenRz, int[] cup) {

        List<HoleBlueprint.Hazard> hazards = new ArrayList<>();
        int count = random.nextInt(3); // 0, 1 or 2

        if (count >= 1) {
            // Greenside bunker, just short of the green and off to one side.
            int side = random.nextBoolean() ? 1 : -1;
            int bunkerX = greenCenterX + side * (greenRx + 2);
            int bunkerZ = greenZ - greenRz - 1;
            int radius = 2 + random.nextInt(2);
            if (stamp(grid, bunkerX, bunkerZ, radius, SurfaceCell.SAND, cup, greenCenterX, greenZ)) {
                hazards.add(new HoleBlueprint.Hazard(SurfaceCell.SAND, bunkerX, bunkerZ, radius));
            }
        }

        if (count >= 2) {
            int waterZ = TEE_CLEARANCE + random.nextInt(
                    Math.max(1, greenZ - GREEN_CLEARANCE - TEE_CLEARANCE));
            if (waterZ > TEE_CLEARANCE && waterZ < greenZ - greenRz - GREEN_CLEARANCE) {
                int side = random.nextBoolean() ? 1 : -1;
                int half = fairwayHalf[waterZ];
                // Bite into one edge of the fairway only.
                int waterX = centerline[waterZ] + side * half;
                int radius = 2 + random.nextInt(2);
                if (stamp(grid, waterX, waterZ, radius, SurfaceCell.WATER, cup, greenCenterX, greenZ)) {
                    hazards.add(new HoleBlueprint.Hazard(SurfaceCell.WATER, waterX, waterZ, radius));
                }
            }
        }
        return hazards;
    }

    /**
     * Stamps a round hazard, refusing to touch the tee, the cup or the green.
     * Returns false when the hazard could not be placed cleanly.
     */
    private static boolean stamp(SurfaceCell[][] grid, int cx, int cz, int radius,
                                 SurfaceCell type, int[] cup, int greenCenterX, int greenZ) {
        int length = grid[0].length;
        // Stay inside the rough border: the outer ROUGH_BORDER columns frame the
        // hole and a bunker spilling into them reads as the hole having no edge.
        if (cx - radius < ROUGH_BORDER || cx + radius >= WIDTH - ROUGH_BORDER
                || cz - radius < 0 || cz + radius >= length) {
            return false;
        }
        // Never near the tee.
        if (cz - radius <= TEE_CENTER_Z + TEE_CLEARANCE - GREEN_CLEARANCE) {
            if (cz - radius <= TEE_CENTER_Z + 4) {
                return false;
            }
        }
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if (distance(x, z, cx, cz) > radius) {
                    continue;
                }
                SurfaceCell existing = grid[x][z];
                if (existing == SurfaceCell.TEE || existing == SurfaceCell.GREEN
                        || existing == SurfaceCell.CUP) {
                    return false;
                }
                if (x == cup[0] && z == cup[1]) {
                    return false;
                }
            }
        }
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                if (distance(x, z, cx, cz) <= radius) {
                    grid[x][z] = type;
                }
            }
        }
        return true;
    }

    private static int distance(int x1, int z1, int x2, int z2) {
        int dx = x1 - x2;
        int dz = z1 - z2;
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }
}
