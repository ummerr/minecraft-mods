package com.sportscraft.course.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HolePlannerTest {

    /** The sweep that keeps random hazard placement honest. */
    private static final int SEED_SWEEP = 50;

    @Test
    void planningIsDeterministic() {
        for (long seed : new long[]{0L, 1L, 42L, -7L, 123456789L}) {
            HoleBlueprint first = HolePlanner.plan(seed);
            HoleBlueprint second = HolePlanner.plan(seed);
            assertEquals(first.signature(), second.signature(),
                    "seed " + seed + " must always plan the same hole");
        }
    }

    @Test
    void differentSeedsGiveDifferentHoles() {
        assertNotEquals(HolePlanner.plan(1L).signature(), HolePlanner.plan(2L).signature(),
                "the seed should actually change the layout");
    }

    /**
     * The important one. Every seed must produce a hole the validator accepts —
     * a random bunker or pond that walls the green off is a real bug, and this
     * is what catches it.
     */
    @Test
    void fiftySeedsAllValidateClean() {
        for (long seed = 0; seed < SEED_SWEEP; seed++) {
            HoleBlueprint blueprint = HolePlanner.plan(seed);
            List<String> problems = HoleValidator.validate(blueprint);
            assertTrue(problems.isEmpty(),
                    "seed " + seed + " planned an invalid hole: " + problems);
        }
    }

    @Test
    void holeLengthStaysInRange() {
        for (long seed = 0; seed < SEED_SWEEP; seed++) {
            HoleBlueprint bp = HolePlanner.plan(seed);
            assertTrue(bp.length() >= HolePlanner.MIN_LENGTH && bp.length() <= HolePlanner.MAX_LENGTH,
                    "seed " + seed + " produced a " + bp.length() + "-long grid");
        }
    }

    @Test
    void everyHoleHasATeeACupAndAGreen() {
        for (long seed = 0; seed < SEED_SWEEP; seed++) {
            HoleBlueprint bp = HolePlanner.plan(seed);
            assertEquals(9, bp.countCells(SurfaceCell.TEE), "seed " + seed + " tee pad is not 3x3");
            assertEquals(1, bp.countCells(SurfaceCell.CUP), "seed " + seed + " must have exactly one cup");
            assertTrue(bp.countCells(SurfaceCell.GREEN) > 20,
                    "seed " + seed + " green is suspiciously small");
            assertTrue(bp.countCells(SurfaceCell.FAIRWAY) > 100,
                    "seed " + seed + " has barely any fairway");
        }
    }

    @Test
    void parAlwaysMatchesTheHoleLength() {
        for (long seed = 0; seed < SEED_SWEEP; seed++) {
            HoleBlueprint bp = HolePlanner.plan(seed);
            assertEquals(ParCalculator.parFor(bp.holeLength()), bp.par(), "seed " + seed);
            assertTrue(bp.par() >= 3 && bp.par() <= 5, "seed " + seed + " par " + bp.par());
        }
    }

    @Test
    void everySeedProducesSomeRoughBorder() {
        for (long seed = 0; seed < SEED_SWEEP; seed++) {
            HoleBlueprint bp = HolePlanner.plan(seed);
            for (int z = 0; z < bp.length(); z++) {
                assertEquals(SurfaceCell.ROUGH, bp.get(0, z),
                        "seed " + seed + ": the grid edge should always be rough at z=" + z);
                assertEquals(SurfaceCell.ROUGH, bp.get(bp.width() - 1, z),
                        "seed " + seed + ": the grid edge should always be rough at z=" + z);
            }
        }
    }

    @Test
    void hazardsAreRecordedWhenPlaced() {
        int seedsWithHazards = 0;
        for (long seed = 0; seed < SEED_SWEEP; seed++) {
            HoleBlueprint bp = HolePlanner.plan(seed);
            if (!bp.hazards().isEmpty()) {
                seedsWithHazards++;
                for (HoleBlueprint.Hazard hazard : bp.hazards()) {
                    assertEquals(hazard.type(), bp.get(hazard.centerX(), hazard.centerZ()),
                            "seed " + seed + ": a recorded hazard is not actually in the grid");
                }
            }
        }
        assertTrue(seedsWithHazards > 5,
                "hazards should show up regularly across 50 seeds, saw " + seedsWithHazards);
    }
}
