package com.sportscraft.course.plan;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Sabotage suite, in the spirit of {@code BlueprintValidatorTest}: build a hole
 * that is known-good, break exactly one invariant, and assert the validator
 * notices. A validator that never rejects anything is worse than no validator,
 * because the planner's 50-seed sweep leans on it.
 */
class HoleValidatorTest {

    private static final int LENGTH = 60;
    private static final int TEE_X = 12;
    private static final int TEE_Z = 2;
    private static final int GREEN_X = 12;
    private static final int GREEN_Z = 53;
    private static final int GREEN_R = 5;

    /** A hand-built hole that satisfies every rule. */
    private static SurfaceCell[][] goodGrid() {
        SurfaceCell[][] grid = new SurfaceCell[HolePlanner.WIDTH][LENGTH];
        for (SurfaceCell[] column : grid) {
            Arrays.fill(column, SurfaceCell.ROUGH);
        }
        // Fairway corridor.
        for (int z = 0; z < LENGTH; z++) {
            for (int x = TEE_X - 4; x <= TEE_X + 4; x++) {
                grid[x][z] = SurfaceCell.FAIRWAY;
            }
        }
        // Circular green.
        for (int x = 0; x < HolePlanner.WIDTH; x++) {
            for (int z = 0; z < LENGTH; z++) {
                int dx = x - GREEN_X;
                int dz = z - GREEN_Z;
                if (dx * dx + dz * dz <= GREEN_R * GREEN_R) {
                    grid[x][z] = SurfaceCell.GREEN;
                }
            }
        }
        // Tee pad.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                grid[TEE_X + dx][TEE_Z + dz] = SurfaceCell.TEE;
            }
        }
        grid[GREEN_X][GREEN_Z] = SurfaceCell.CUP;
        return grid;
    }

    private static HoleBlueprint blueprint(SurfaceCell[][] grid) {
        return blueprint(grid, GREEN_X, GREEN_Z);
    }

    private static HoleBlueprint blueprint(SurfaceCell[][] grid, int cupX, int cupZ) {
        int dx = cupX - TEE_X;
        int dz = cupZ - TEE_Z;
        int par = ParCalculator.parFor((int) Math.round(Math.sqrt(dx * dx + dz * dz)));
        return new HoleBlueprint(grid, TEE_X, TEE_Z, cupX, cupZ, par, 0L, List.of());
    }

    @Test
    void theControlHoleIsValid() {
        List<String> problems = HoleValidator.validate(blueprint(goodGrid()));
        assertTrue(problems.isEmpty(), "the hand-built control hole should be clean: " + problems);
    }

    @Test
    void cupNotOnGreenIsRejected() {
        SurfaceCell[][] grid = goodGrid();
        // Move the cup out onto the fairway, well clear of the green.
        grid[GREEN_X][GREEN_Z] = SurfaceCell.GREEN;
        grid[TEE_X][20] = SurfaceCell.CUP;

        assertRejects(blueprint(grid, TEE_X, 20), "surrounded by green");
    }

    @Test
    void cupOnTheGreensEdgeIsRejected() {
        SurfaceCell[][] grid = goodGrid();
        grid[GREEN_X][GREEN_Z] = SurfaceCell.GREEN;
        // The northernmost green cell: its far neighbour is fairway, not green.
        grid[GREEN_X][GREEN_Z - GREEN_R] = SurfaceCell.CUP;

        assertRejects(blueprint(grid, GREEN_X, GREEN_Z - GREEN_R), "surrounded by green");
    }

    @Test
    void aBrokenTeePadIsRejected() {
        SurfaceCell[][] grid = goodGrid();
        grid[TEE_X + 1][TEE_Z] = SurfaceCell.FAIRWAY;

        assertRejects(blueprint(grid), "Tee pad broken");
    }

    @Test
    void aParThatDoesNotMatchTheLengthIsRejected() {
        SurfaceCell[][] grid = goodGrid();
        HoleBlueprint wrongPar = new HoleBlueprint(
                grid, TEE_X, TEE_Z, GREEN_X, GREEN_Z, 3, 0L, List.of());

        assertRejects(wrongPar, "does not match");
    }

    @Test
    void waterWallingOffTheGreenIsRejected() {
        SurfaceCell[][] grid = goodGrid();
        // A moat straight across the hole: no dry route to the green at all.
        for (int x = 0; x < HolePlanner.WIDTH; x++) {
            grid[x][30] = SurfaceCell.WATER;
        }

        assertRejects(blueprint(grid), "not reachable");
    }

    @Test
    void aPartialWaterHazardIsStillPlayable() {
        SurfaceCell[][] grid = goodGrid();
        // Water biting into one side only — the hole must remain valid.
        for (int x = 0; x < TEE_X; x++) {
            grid[x][30] = SurfaceCell.WATER;
        }

        List<String> problems = HoleValidator.validate(blueprint(grid));
        assertTrue(problems.isEmpty(),
                "water that leaves a dry route should be allowed: " + problems);
    }

    @Test
    void aHazardCrowdingTheTeeIsRejected() {
        SurfaceCell[][] grid = goodGrid();
        grid[TEE_X + 2][TEE_Z + 2] = SurfaceCell.SAND;

        assertRejects(blueprint(grid), "too close to the tee");
    }

    @Test
    void waterAgainstTheCupIsRejected() {
        SurfaceCell[][] grid = goodGrid();
        grid[GREEN_X + 2][GREEN_Z] = SurfaceCell.WATER;

        assertRejects(blueprint(grid), "too close to the cup");
    }

    @Test
    void everySabotageIsCaughtIndependently() {
        // Guards against a validator that only ever reports the first problem.
        SurfaceCell[][] grid = goodGrid();
        grid[TEE_X + 1][TEE_Z] = SurfaceCell.FAIRWAY;
        grid[TEE_X + 2][TEE_Z + 2] = SurfaceCell.SAND;

        List<String> problems = HoleValidator.validate(blueprint(grid));
        List<String> kinds = new ArrayList<>();
        for (String problem : problems) {
            if (problem.contains("Tee pad broken")) {
                kinds.add("tee");
            }
            if (problem.contains("too close to the tee")) {
                kinds.add("hazard");
            }
        }
        assertTrue(kinds.contains("tee") && kinds.contains("hazard"),
                "both independent faults should be reported, got: " + problems);
    }

    private static void assertRejects(HoleBlueprint blueprint, String expectedFragment) {
        List<String> problems = HoleValidator.validate(blueprint);
        assertFalse(problems.isEmpty(), "the validator accepted a sabotaged hole");
        assertTrue(problems.stream().anyMatch(p -> p.contains(expectedFragment)),
                "expected a problem mentioning '" + expectedFragment + "', got: " + problems);
    }
}
