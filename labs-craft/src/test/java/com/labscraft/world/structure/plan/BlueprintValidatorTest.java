package com.labscraft.world.structure.plan;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Proves the validator actually catches broken blueprints — otherwise the
 * "validates cleanly" tests in {@link GoogleplexPlannerTest} would be
 * vacuous. Each test takes a known-good plan, sabotages one property, and
 * expects a specific complaint.
 */
class BlueprintValidatorTest {

    private static GoogleplexBlueprint sabotaged(java.util.function.Consumer<BlockSpec[][][]> mutation) {
        GoogleplexBlueprint good = GoogleplexPlanner.plan(0L);
        BlockSpec[][][] grid = good.copyGrid();
        mutation.accept(grid);
        return new GoogleplexBlueprint(grid, good.rooms(), good.doors(), good.seed());
    }

    @Test
    void cleanBlueprintHasNoProblems() {
        assertTrue(BlueprintValidator.validate(GoogleplexPlanner.plan(0L)).isEmpty());
    }

    @Test
    void detectsWalledOffRoom() {
        // Brick up the meeting room door at (12, 10).
        GoogleplexBlueprint bad = sabotaged(g -> {
            g[12][1][10] = BlockSpec.WALL;
            g[12][2][10] = BlockSpec.WALL;
        });
        List<String> problems = BlueprintValidator.validate(bad);
        assertTrue(problems.stream().anyMatch(p -> p.contains("MEETING_ROOM") && p.contains("not reachable")),
                "Expected a MEETING_ROOM reachability complaint, got: " + problems);
    }

    @Test
    void detectsBlockedEntrance() {
        GoogleplexBlueprint bad = sabotaged(g -> {
            for (int x = 14; x <= 16; x++) {
                for (int y = 1; y <= 3; y++) {
                    g[x][y][0] = BlockSpec.WALL;
                }
            }
        });
        List<String> problems = BlueprintValidator.validate(bad);
        assertTrue(problems.stream().anyMatch(p -> p.contains("Entrance") || p.contains("not walkable")),
                "Expected an entrance complaint, got: " + problems);
    }

    @Test
    void detectsFloorHole() {
        GoogleplexBlueprint bad = sabotaged(g -> g[5][0][5] = BlockSpec.AIR);
        List<String> problems = BlueprintValidator.validate(bad);
        assertTrue(problems.stream().anyMatch(p -> p.contains("floor") && p.contains("(5,5)")),
                "Expected a floor-hole complaint, got: " + problems);
    }

    @Test
    void detectsCeilingHole() {
        GoogleplexBlueprint bad = sabotaged(g -> g[22][5][11] = BlockSpec.AIR);
        List<String> problems = BlueprintValidator.validate(bad);
        assertTrue(problems.stream().anyMatch(p -> p.contains("ceiling")),
                "Expected a ceiling-hole complaint, got: " + problems);
    }

    @Test
    void detectsUnsealedPerimeter() {
        // Punch an undeclared hole in the back wall.
        GoogleplexBlueprint bad = sabotaged(g -> {
            g[8][1][GoogleplexBlueprint.SIZE_Z - 1] = BlockSpec.AIR;
            g[8][2][GoogleplexBlueprint.SIZE_Z - 1] = BlockSpec.AIR;
        });
        List<String> problems = BlueprintValidator.validate(bad);
        assertTrue(problems.stream().anyMatch(p -> p.contains("Unsealed perimeter")),
                "Expected an unsealed-perimeter complaint, got: " + problems);
    }

    @Test
    void detectsBlockedDoorCell() {
        // Fill just the body-height cell of a door: still "there" in metadata
        // but not walkable.
        GoogleplexBlueprint bad = sabotaged(g -> g[12][2][16] = BlockSpec.GLASS);
        List<String> problems = BlueprintValidator.validate(bad);
        assertTrue(problems.stream().anyMatch(p -> p.contains("Door at (12,16)")),
                "Expected a blocked-door complaint, got: " + problems);
    }

    @Test
    void detectsOverlappingRooms() {
        GoogleplexBlueprint good = GoogleplexPlanner.plan(0L);
        List<Room> rooms = new java.util.ArrayList<>(good.rooms());
        rooms.add(new Room(RoomType.MEETING_ROOM, 1, 1, 29, 25)); // covers everything
        GoogleplexBlueprint bad = new GoogleplexBlueprint(good.copyGrid(), rooms, good.doors(), 0L);
        List<String> problems = BlueprintValidator.validate(bad);
        assertTrue(problems.stream().anyMatch(p -> p.contains("overlap")),
                "Expected an overlap complaint, got: " + problems);
    }
}
