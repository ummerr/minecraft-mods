package com.labscraft.world.structure.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GoogleplexPlannerTest {

    private static final long[] SAMPLE_SEEDS = {0L, 1L, 42L, -7L, 123456789L, Long.MAX_VALUE, Long.MIN_VALUE};

    // ------------------------------------------------------------------
    // Determinism
    // ------------------------------------------------------------------

    @Test
    void samePlanForSameSeed() {
        GoogleplexBlueprint a = GoogleplexPlanner.plan(42L);
        GoogleplexBlueprint b = GoogleplexPlanner.plan(42L);
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int y = 0; y < GoogleplexBlueprint.SIZE_Y; y++) {
                for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                    assertEquals(a.get(x, y, z), b.get(x, y, z),
                            "Cell mismatch at " + x + "," + y + "," + z);
                }
            }
        }
        assertEquals(a.rooms(), b.rooms());
        assertEquals(a.doors(), b.doors());
    }

    @Test
    void seedVariesCosmeticsSomewhere() {
        // The structural skeleton is fixed but accents are seeded; across a
        // spread of seeds at least two blueprints must differ in some cell.
        GoogleplexBlueprint reference = GoogleplexPlanner.plan(0L);
        boolean anyDifference = false;
        for (long seed = 1; seed <= 16 && !anyDifference; seed++) {
            GoogleplexBlueprint other = GoogleplexPlanner.plan(seed);
            anyDifference = !gridsEqual(reference, other);
        }
        assertTrue(anyDifference, "16 different seeds produced byte-identical blueprints");
    }

    @Test
    void structuralSkeletonIsSeedIndependent() {
        GoogleplexBlueprint a = GoogleplexPlanner.plan(1L);
        GoogleplexBlueprint b = GoogleplexPlanner.plan(999L);
        assertEquals(a.rooms(), b.rooms(), "Rooms must not depend on the seed");
        assertEquals(a.doors(), b.doors(), "Doors must not depend on the seed");
        // Walls and glass are structural: every WALL/GLASS cell in a must be
        // identical in b.
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int y = 0; y < GoogleplexBlueprint.SIZE_Y; y++) {
                for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                    BlockSpec spec = a.get(x, y, z);
                    if (spec == BlockSpec.WALL || spec == BlockSpec.GLASS || spec == BlockSpec.DOOR_AIR) {
                        assertEquals(spec, b.get(x, y, z),
                                "Structural cell changed with seed at " + x + "," + y + "," + z);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Rooms
    // ------------------------------------------------------------------

    @Test
    void hasAllSixRoomTypes() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        assertEquals(6, bp.rooms().size());
        for (RoomType type : new RoomType[]{RoomType.LOBBY, RoomType.CORRIDOR, RoomType.MEETING_ROOM,
                RoomType.MICRO_KITCHEN, RoomType.DESK_AREA, RoomType.LAB}) {
            assertTrue(bp.rooms().stream().anyMatch(r -> r.type() == type), "Missing room " + type);
        }
    }

    @Test
    void roomsAreWithinInteriorBounds() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        for (Room room : bp.rooms()) {
            assertTrue(room.minX() >= 1 && room.minZ() >= 1
                            && room.maxX() <= GoogleplexBlueprint.SIZE_X - 2
                            && room.maxZ() <= GoogleplexBlueprint.SIZE_Z - 2,
                    room.type() + " leaks into or past the exterior wall: " + room);
        }
    }

    @Test
    void roomsDoNotOverlap() {
        List<Room> rooms = GoogleplexPlanner.plan(0L).rooms();
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                assertFalse(rooms.get(i).overlaps(rooms.get(j)),
                        rooms.get(i).type() + " overlaps " + rooms.get(j).type());
            }
        }
    }

    @Test
    void roomsHaveUsableArea() {
        for (Room room : GoogleplexPlanner.plan(0L).rooms()) {
            assertTrue(room.area() >= 15, room.type() + " is implausibly small: " + room.area());
        }
    }

    // ------------------------------------------------------------------
    // Full validation across seeds (bounds, doors, sealing, reachability)
    // ------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 42L, -7L, 123456789L, Long.MAX_VALUE, Long.MIN_VALUE})
    void blueprintValidatesCleanly(long seed) {
        List<String> problems = BlueprintValidator.validate(GoogleplexPlanner.plan(seed));
        assertTrue(problems.isEmpty(), "Seed " + seed + " produced problems: " + problems);
    }

    // ------------------------------------------------------------------
    // Doors and connectivity details
    // ------------------------------------------------------------------

    @Test
    void entranceIsThreeWideAndTall() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        long entranceCells = bp.doors().stream().filter(d -> d.from() == RoomType.OUTSIDE).count();
        assertEquals(3, entranceCells);
        for (int x = 14; x <= 16; x++) {
            for (int y = 1; y <= 3; y++) {
                assertEquals(BlockSpec.DOOR_AIR, bp.get(x, y, 0),
                        "Entrance not open at x=" + x + ", y=" + y);
            }
        }
    }

    @Test
    void everyRoomHasAtLeastOneDoor() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        for (Room room : bp.rooms()) {
            boolean hasDoor = bp.doors().stream()
                    .anyMatch(d -> d.from() == room.type() || d.to() == room.type());
            assertTrue(hasDoor, room.type() + " has no door");
        }
    }

    @Test
    void doorCellsAreWalkable() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        for (Door door : bp.doors()) {
            assertTrue(bp.isWalkable(door.x(), door.z()),
                    "Door at (" + door.x() + "," + door.z() + ") is blocked");
        }
    }

    // ------------------------------------------------------------------
    // Shell integrity
    // ------------------------------------------------------------------

    @Test
    void floorHasNoHoles() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(7L);
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                assertFalse(bp.get(x, 0, z).passable(), "Floor hole at (" + x + "," + z + ")");
            }
        }
    }

    @Test
    void labHasDistinctFloor() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        Room lab = bp.room(RoomType.LAB);
        assertEquals(BlockSpec.FLOOR_LAB, bp.get(lab.centerX(), 0, lab.centerZ()));
        Room lobby = bp.room(RoomType.LOBBY);
        assertEquals(BlockSpec.FLOOR, bp.get(lobby.centerX(), 0, lobby.centerZ()));
    }

    @Test
    void ceilingCoversEveryRoom() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        for (Room room : bp.rooms()) {
            for (int x = room.minX(); x <= room.maxX(); x++) {
                for (int z = room.minZ(); z <= room.maxZ(); z++) {
                    assertNotEquals(BlockSpec.AIR, bp.get(x, 5, z),
                            "Ceiling hole over " + room.type() + " at (" + x + "," + z + ")");
                }
            }
        }
    }

    @Test
    void ceilingHasLights() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        int lights = 0;
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                if (bp.get(x, 5, z) == BlockSpec.LIGHT) {
                    lights++;
                }
            }
        }
        assertTrue(lights >= 20, "Office is too dark: only " + lights + " ceiling lights");
    }

    // ------------------------------------------------------------------
    // Content: the lab is the home of the Flow product
    // ------------------------------------------------------------------

    @Test
    void labContainsExactlyOneOfEachConsoleAndTheFlowTable() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        Room lab = bp.room(RoomType.LAB);
        Map<BlockSpec, Integer> counts = countSpecs(bp);

        for (BlockSpec spec : new BlockSpec[]{BlockSpec.CONSOLE_FLOW, BlockSpec.CONSOLE_NANO_BANANA,
                BlockSpec.CONSOLE_VEO, BlockSpec.FLOW_CRAFTING_TABLE}) {
            assertEquals(1, counts.getOrDefault(spec, 0), "Expected exactly one " + spec);
            int[] pos = findSpec(bp, spec);
            assertTrue(lab.contains(pos[0], pos[2]), spec + " is outside the lab at ("
                    + pos[0] + "," + pos[2] + ")");
        }
    }

    @Test
    void deskAreaHasFourPods() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        Map<BlockSpec, Integer> counts = countSpecs(bp);
        assertEquals(16, counts.getOrDefault(BlockSpec.DESK, 0), "4 pods x 2x2 desks = 16 desk cells");
        assertEquals(4, counts.getOrDefault(BlockSpec.DESK_LAMP, 0), "one lamp per pod");
    }

    @Test
    void usesAllFourGoogleColors() {
        GoogleplexBlueprint bp = GoogleplexPlanner.plan(0L);
        Map<BlockSpec, Integer> counts = countSpecs(bp);
        for (BlockSpec spec : new BlockSpec[]{BlockSpec.CARPET_BLUE, BlockSpec.CARPET_RED,
                BlockSpec.CARPET_YELLOW, BlockSpec.CARPET_GREEN}) {
            assertTrue(counts.getOrDefault(spec, 0) > 0, "Missing Google color " + spec);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean gridsEqual(GoogleplexBlueprint a, GoogleplexBlueprint b) {
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int y = 0; y < GoogleplexBlueprint.SIZE_Y; y++) {
                for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                    if (a.get(x, y, z) != b.get(x, y, z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static Map<BlockSpec, Integer> countSpecs(GoogleplexBlueprint bp) {
        Map<BlockSpec, Integer> counts = new EnumMap<>(BlockSpec.class);
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int y = 0; y < GoogleplexBlueprint.SIZE_Y; y++) {
                for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                    counts.merge(bp.get(x, y, z), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private static int[] findSpec(GoogleplexBlueprint bp, BlockSpec spec) {
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int y = 0; y < GoogleplexBlueprint.SIZE_Y; y++) {
                for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                    if (bp.get(x, y, z) == spec) {
                        return new int[]{x, y, z};
                    }
                }
            }
        }
        throw new AssertionError("Spec not found: " + spec);
    }
}
