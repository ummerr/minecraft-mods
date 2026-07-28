package com.sportscraft.course.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Pure-Java structural checks over a {@link HoleBlueprint}, used by unit tests
 * and as a pre-placement gate in {@code CoursePlacer}. Returns human-readable
 * problems; an empty list means the hole is sound.
 *
 * <p>The load-bearing check is {@link #checkReachability}: a hole whose green
 * cannot be reached from the tee without crossing water is unplayable, and that
 * is exactly the failure a random hazard placement can introduce.</p>
 */
public final class HoleValidator {

    /** Hazards must not crowd the tee. */
    public static final int TEE_SAFE_RADIUS = 5;

    /** Water must not sit right against the cup. */
    public static final int CUP_WATER_SAFE_RADIUS = 3;

    private HoleValidator() {
    }

    public static List<String> validate(HoleBlueprint bp) {
        List<String> problems = new ArrayList<>();
        checkCupOnGreen(bp, problems);
        checkTeeIntact(bp, problems);
        checkParConsistency(bp, problems);
        checkHazardClearances(bp, problems);
        checkReachability(bp, problems);
        return problems;
    }

    /** The cup must be surrounded by green, or putting makes no sense. */
    private static void checkCupOnGreen(HoleBlueprint bp, List<String> problems) {
        if (bp.get(bp.cupX(), bp.cupZ()) != SurfaceCell.CUP) {
            problems.add("Cup cell at (" + bp.cupX() + "," + bp.cupZ() + ") is not CUP");
            return;
        }
        int[][] neighbors = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] n : neighbors) {
            SurfaceCell around = bp.get(bp.cupX() + n[0], bp.cupZ() + n[1]);
            if (around != SurfaceCell.GREEN) {
                problems.add("Cup is not surrounded by green at ("
                        + (bp.cupX() + n[0]) + "," + (bp.cupZ() + n[1]) + "): " + around);
            }
        }
    }

    /** All nine tee cells must survive hazard and green carving. */
    private static void checkTeeIntact(HoleBlueprint bp, List<String> problems) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                SurfaceCell cell = bp.get(bp.teeX() + dx, bp.teeZ() + dz);
                if (cell != SurfaceCell.TEE) {
                    problems.add("Tee pad broken at (" + (bp.teeX() + dx) + ","
                            + (bp.teeZ() + dz) + "): " + cell);
                }
            }
        }
    }

    private static void checkParConsistency(HoleBlueprint bp, List<String> problems) {
        int expected = ParCalculator.parFor(bp.holeLength());
        if (bp.par() != expected) {
            problems.add("Par " + bp.par() + " does not match a " + bp.holeLength()
                    + "-block hole (expected par " + expected + ")");
        }
        if (bp.par() < 3 || bp.par() > 5) {
            problems.add("Par " + bp.par() + " is outside the 3..5 range");
        }
    }

    private static void checkHazardClearances(HoleBlueprint bp, List<String> problems) {
        for (int x = 0; x < bp.width(); x++) {
            for (int z = 0; z < bp.length(); z++) {
                SurfaceCell cell = bp.get(x, z);
                if (cell != SurfaceCell.SAND && cell != SurfaceCell.WATER) {
                    continue;
                }
                if (distance(x, z, bp.teeX(), bp.teeZ()) < TEE_SAFE_RADIUS) {
                    problems.add(cell + " at (" + x + "," + z + ") is too close to the tee");
                }
                if (cell == SurfaceCell.WATER
                        && distance(x, z, bp.cupX(), bp.cupZ()) < CUP_WATER_SAFE_RADIUS) {
                    problems.add("Water at (" + x + "," + z + ") is too close to the cup");
                }
            }
        }
    }

    /**
     * BFS from the tee over playable cells: the cup must be reachable without
     * crossing water. This is the "you can actually play this hole" invariant.
     */
    private static void checkReachability(HoleBlueprint bp, List<String> problems) {
        if (!bp.isPlayable(bp.teeX(), bp.teeZ())) {
            problems.add("Tee at (" + bp.teeX() + "," + bp.teeZ() + ") is not playable");
            return;
        }

        boolean[][] visited = new boolean[bp.width()][bp.length()];
        Deque<int[]> queue = new ArrayDeque<>();
        visited[bp.teeX()][bp.teeZ()] = true;
        queue.add(new int[]{bp.teeX(), bp.teeZ()});

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int[][] neighbors = {{cell[0] + 1, cell[1]}, {cell[0] - 1, cell[1]},
                    {cell[0], cell[1] + 1}, {cell[0], cell[1] - 1}};
            for (int[] n : neighbors) {
                if (!bp.inBounds(n[0], n[1]) || visited[n[0]][n[1]]) {
                    continue;
                }
                if (bp.isPlayable(n[0], n[1])) {
                    visited[n[0]][n[1]] = true;
                    queue.add(n);
                }
            }
        }

        if (!visited[bp.cupX()][bp.cupZ()]) {
            problems.add("Cup at (" + bp.cupX() + "," + bp.cupZ()
                    + ") is not reachable from the tee without crossing water");
        }

        boolean anyGreenReached = false;
        for (int x = 0; x < bp.width() && !anyGreenReached; x++) {
            for (int z = 0; z < bp.length() && !anyGreenReached; z++) {
                if (bp.get(x, z) == SurfaceCell.GREEN && visited[x][z]) {
                    anyGreenReached = true;
                }
            }
        }
        if (!anyGreenReached) {
            problems.add("No green cell is reachable from the tee");
        }
    }

    private static int distance(int x1, int z1, int x2, int z2) {
        int dx = x1 - x2;
        int dz = z1 - z2;
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }
}
