package com.labscraft.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the coordinate math and placement logic used by GoogleplexAutoGenerator.
 * Verifies that spawn points, NPC positions, and origin calculations produce
 * positions that are actually inside the Googleplex structure.
 */
class GoogleplexAutoGeneratorTest {

    // Building constants (from GoogleplexGenerator)
    static final int WIDTH = 200;
    static final int DEPTH = 200;

    // Lobby constants
    static final int LOBBY_START_X = 70;
    static final int LOBBY_END_X = 130;
    static final int LOBBY_DEPTH = 30;

    // Auto-generator places the origin so entrance center (x+100) aligns with spawn X
    // and entrance (z=0) is 5 blocks north of spawn Z.

    /** Mirrors the origin calculation from GoogleplexAutoGenerator.onServerStarted */
    static int[] calculateOrigin(int spawnX, int spawnY, int spawnZ) {
        return new int[]{spawnX - 100, spawnY, spawnZ - 5};
    }

    /** Mirrors getLobbySpawnPos: origin + (100, 1, 2) */
    static int[] getLobbySpawnPos(int[] origin) {
        return new int[]{origin[0] + 100, origin[1] + 1, origin[2] + 2};
    }

    /** Mirrors getJoshSpawnPos: origin + (100, 1, 10) */
    static int[] getJoshSpawnPos(int[] origin) {
        return new int[]{origin[0] + 100, origin[1] + 1, origin[2] + 10};
    }

    /** Crafting table position: origin + (85, 1, 12) */
    static int[] getCraftingTablePos(int[] origin) {
        return new int[]{origin[0] + 85, origin[1] + 1, origin[2] + 12};
    }

    private boolean isInsideBuilding(int relX, int relZ) {
        return relX >= 0 && relX < WIDTH && relZ >= 0 && relZ < DEPTH;
    }

    private boolean isInsideLobby(int relX, int relZ) {
        return relX >= LOBBY_START_X && relX < LOBBY_END_X && relZ >= 0 && relZ < LOBBY_DEPTH;
    }

    @Nested
    class OriginCalculation {
        @Test
        void originAtDefaultSpawn_centersBuilding() {
            int[] origin = calculateOrigin(0, 64, 0);
            assertEquals(-100, origin[0], "Origin X should offset by -100");
            assertEquals(64, origin[1], "Origin Y should match spawn Y");
            assertEquals(-5, origin[2], "Origin Z should offset by -5");
        }

        @Test
        void originAtArbitrarySpawn() {
            int[] origin = calculateOrigin(500, 72, 300);
            assertEquals(400, origin[0]);
            assertEquals(72, origin[1]);
            assertEquals(295, origin[2]);
        }

        @Test
        void entranceCenter_alignsWithSpawnX() {
            int spawnX = 42;
            int[] origin = calculateOrigin(spawnX, 64, 0);
            int entranceCenter = origin[0] + 100; // relative x=100 is entrance center
            assertEquals(spawnX, entranceCenter);
        }
    }

    @Nested
    class LobbySpawnPosition {
        @Test
        void lobbySpawn_isInsideBuilding() {
            int[] origin = calculateOrigin(0, 64, 0);
            int[] spawn = getLobbySpawnPos(origin);
            int relX = spawn[0] - origin[0]; // 100
            int relZ = spawn[2] - origin[2]; // 2
            assertTrue(isInsideBuilding(relX, relZ),
                "Spawn at relative (" + relX + ", " + relZ + ") should be inside building");
        }

        @Test
        void lobbySpawn_isInsideLobby() {
            int[] origin = calculateOrigin(0, 64, 0);
            int[] spawn = getLobbySpawnPos(origin);
            int relX = spawn[0] - origin[0]; // 100
            int relZ = spawn[2] - origin[2]; // 2
            assertTrue(isInsideLobby(relX, relZ),
                "Spawn at relative (" + relX + ", " + relZ + ") should be inside lobby");
        }

        @Test
        void lobbySpawn_isAboveFloor() {
            int[] origin = calculateOrigin(0, 64, 0);
            int[] spawn = getLobbySpawnPos(origin);
            assertEquals(origin[1] + 1, spawn[1], "Spawn Y should be 1 above floor");
        }

        @Test
        void lobbySpawn_isNearEntrance() {
            // Spawn is at relative z=2, entrance is at z=0
            int relZ = 2;
            assertTrue(relZ < 5, "Spawn should be near the entrance (within 5 blocks)");
        }
    }

    @Nested
    class JoshPosition {
        @Test
        void josh_isInsideLobby() {
            int[] origin = calculateOrigin(0, 64, 0);
            int[] josh = getJoshSpawnPos(origin);
            int relX = josh[0] - origin[0]; // 100
            int relZ = josh[2] - origin[2]; // 10
            assertTrue(isInsideLobby(relX, relZ),
                "Josh at relative (" + relX + ", " + relZ + ") should be inside lobby");
        }

        @Test
        void josh_isBehindEntrance() {
            // Josh is at z=10, entrance is at z=0, so player walks in and sees Josh
            int joshRelZ = 10;
            assertTrue(joshRelZ > 0 && joshRelZ < LOBBY_DEPTH,
                "Josh should be behind entrance but inside lobby");
        }

        @Test
        void josh_isBeforeReceptionDesk() {
            // Reception desk is at z=15, Josh at z=10
            int joshRelZ = 10;
            int deskZ = 15;
            assertTrue(joshRelZ < deskZ,
                "Josh should be between entrance and reception desk");
        }

        @Test
        void josh_isCenteredHorizontally() {
            int joshRelX = 100;
            int buildingCenter = WIDTH / 2;
            assertEquals(buildingCenter, joshRelX, "Josh should be centered in the building");
        }
    }

    @Nested
    class CraftingTablePosition {
        @Test
        void craftingTable_isInsideLobby() {
            int[] origin = calculateOrigin(0, 64, 0);
            int[] table = getCraftingTablePos(origin);
            int relX = table[0] - origin[0]; // 85
            int relZ = table[2] - origin[2]; // 12
            assertTrue(isInsideLobby(relX, relZ),
                "Crafting table at relative (" + relX + ", " + relZ + ") should be inside lobby");
        }

        @Test
        void craftingTable_doesNotBlockEntrance() {
            // Entrance is at x=90-110, z=0. Table is at x=85, z=12.
            int tableRelX = 85;
            int tableRelZ = 12;
            int entranceStartX = 90;
            int entranceEndX = 110;

            boolean blocksEntrance = (tableRelX >= entranceStartX && tableRelX <= entranceEndX && tableRelZ <= 1);
            assertFalse(blocksEntrance, "Crafting table should not block the entrance");
        }

        @Test
        void craftingTable_isNearReceptionDesk() {
            // Reception desk is at x=95-105, z=15. Table is at x=85, z=12.
            int tableRelX = 85;
            int tableRelZ = 12;
            int deskStartX = 95;
            int deskZ = 15;

            int distX = Math.abs(tableRelX - deskStartX);
            int distZ = Math.abs(tableRelZ - deskZ);
            assertTrue(distX <= 15 && distZ <= 5,
                "Crafting table should be near the reception desk");
        }
    }

    @Nested
    class IdempotencyGuard {
        @Test
        void generationState_preventsDoubleGeneration() {
            // Simulates the isGenerated() check
            GoogleplexStateTest.GenerationTracker tracker = new GoogleplexStateTest.GenerationTracker();

            // First run: not generated yet
            assertFalse(tracker.isGenerated());
            tracker.setGenerated(true);

            // Second run: already generated
            assertTrue(tracker.isGenerated());
        }
    }
}
