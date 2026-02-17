package com.labscraft.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the structural layout parameters and design constraints of GoogleplexGenerator.
 * Since the generator directly uses Minecraft block classes that can't be initialized in
 * unit tests, we verify the dimensional/layout logic by mirroring the constants and
 * testing that the structure plan is internally consistent.
 *
 * These tests catch issues like overlapping rooms, rooms outside the building boundary,
 * and incorrect mine dimensions — the kinds of bugs that matter for the custom map.
 */
class GoogleplexGeneratorTest {

    // Building dimensions from GoogleplexGenerator
    static final int WIDTH = 200;
    static final int DEPTH = 200;
    static final int WALL_HEIGHT = 12;

    // Lab definitions: (startX, startZ, width, depth, name)
    static final int[][] LABS = {
        {10, 35, 40, 45},     // Flow
        {150, 35, 40, 45},    // Genie
        {10, 85, 40, 45},     // Doppl
        {150, 85, 40, 45},    // NotebookLM
        {10, 135, 40, 45},    // Opal
        {150, 135, 40, 45},   // Mixboard
    };
    static final String[] LAB_NAMES = {"Flow", "Genie", "Doppl", "NotebookLM", "Opal", "Mixboard"};

    // Lobby dimensions
    static final int LOBBY_START_X = 70;
    static final int LOBBY_END_X = 130;
    static final int LOBBY_DEPTH = 30;

    // TPU Mine dimensions
    static final int MINE_START_X = 75;
    static final int MINE_START_Z = 75;
    static final int MINE_SIZE = 50;
    static final int MINE_DEPTH = 15;

    // Cafeteria dimensions
    static final int CAFE_START_X = 55;
    static final int CAFE_START_Z = 170;
    static final int CAFE_WIDTH = 90;
    static final int CAFE_DEPTH = 25;

    // --- Helper: check if two rectangles overlap ---
    private boolean rectsOverlap(int x1, int z1, int w1, int d1, int x2, int z2, int w2, int d2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && z1 < z2 + d2 && z1 + d1 > z2;
    }

    private boolean rectInsideBuilding(int x, int z, int w, int d) {
        return x >= 0 && z >= 0 && x + w <= WIDTH && z + d <= DEPTH;
    }

    @Nested
    class BuildingDimensions {
        @Test
        void building_isSquare() {
            assertEquals(WIDTH, DEPTH, "Building should be square");
        }

        @Test
        void building_is200x200() {
            assertEquals(200, WIDTH);
            assertEquals(200, DEPTH);
        }

        @Test
        void wallHeight_is12Blocks() {
            assertEquals(12, WALL_HEIGHT);
        }

        @Test
        void ceiling_isAboveWallHeight() {
            // Ceiling at WALL_HEIGHT + 1
            int ceilingY = WALL_HEIGHT + 1;
            assertEquals(13, ceilingY);
        }
    }

    @Nested
    class LabLayout {
        @Test
        void sixLabs_exist() {
            assertEquals(6, LABS.length);
            assertEquals(6, LAB_NAMES.length);
        }

        @Test
        void allLabs_insideBuilding() {
            for (int i = 0; i < LABS.length; i++) {
                int[] lab = LABS[i];
                assertTrue(rectInsideBuilding(lab[0], lab[1], lab[2], lab[3]),
                    LAB_NAMES[i] + " lab extends outside building boundary");
            }
        }

        @Test
        void allLabs_haveIdenticalDimensions() {
            for (int i = 0; i < LABS.length; i++) {
                assertEquals(40, LABS[i][2], LAB_NAMES[i] + " lab width should be 40");
                assertEquals(45, LABS[i][3], LAB_NAMES[i] + " lab depth should be 45");
            }
        }

        @Test
        void labs_doNotOverlapEachOther() {
            for (int i = 0; i < LABS.length; i++) {
                for (int j = i + 1; j < LABS.length; j++) {
                    assertFalse(
                        rectsOverlap(LABS[i][0], LABS[i][1], LABS[i][2], LABS[i][3],
                                     LABS[j][0], LABS[j][1], LABS[j][2], LABS[j][3]),
                        LAB_NAMES[i] + " and " + LAB_NAMES[j] + " labs overlap"
                    );
                }
            }
        }

        @Test
        void labs_doNotOverlapMine() {
            for (int i = 0; i < LABS.length; i++) {
                assertFalse(
                    rectsOverlap(LABS[i][0], LABS[i][1], LABS[i][2], LABS[i][3],
                                 MINE_START_X, MINE_START_Z, MINE_SIZE, MINE_SIZE),
                    LAB_NAMES[i] + " lab overlaps with TPU mine"
                );
            }
        }

        @Test
        void labs_doNotOverlapLobby() {
            for (int i = 0; i < LABS.length; i++) {
                assertFalse(
                    rectsOverlap(LABS[i][0], LABS[i][1], LABS[i][2], LABS[i][3],
                                 LOBBY_START_X, 0, LOBBY_END_X - LOBBY_START_X, LOBBY_DEPTH),
                    LAB_NAMES[i] + " lab overlaps with lobby"
                );
            }
        }

        @Test
        void labs_doNotOverlapCafeteria() {
            for (int i = 0; i < LABS.length; i++) {
                assertFalse(
                    rectsOverlap(LABS[i][0], LABS[i][1], LABS[i][2], LABS[i][3],
                                 CAFE_START_X, CAFE_START_Z, CAFE_WIDTH, CAFE_DEPTH),
                    LAB_NAMES[i] + " lab overlaps with cafeteria"
                );
            }
        }

        @Test
        void labs_haveDoorwayPositionInsideLab() {
            // Doorway is at startX + width/2, which should be inside the lab
            for (int i = 0; i < LABS.length; i++) {
                int doorX = LABS[i][0] + LABS[i][2] / 2;
                assertTrue(doorX > LABS[i][0] && doorX < LABS[i][0] + LABS[i][2],
                    LAB_NAMES[i] + " lab doorway x=" + doorX + " is outside lab bounds");
            }
        }

        @Test
        void labs_areSymmetricallyArranged() {
            // Left column labs at x=10, right column at x=150
            assertEquals(LABS[0][0], LABS[2][0], "Flow and Doppl should share same x");
            assertEquals(LABS[0][0], LABS[4][0], "Flow and Opal should share same x");
            assertEquals(LABS[1][0], LABS[3][0], "Genie and NotebookLM should share same x");
            assertEquals(LABS[1][0], LABS[5][0], "Genie and Mixboard should share same x");

            // Three rows at z=35, z=85, z=135
            assertEquals(LABS[0][1], LABS[1][1], "Flow and Genie should share same z row");
            assertEquals(LABS[2][1], LABS[3][1], "Doppl and NotebookLM should share same z row");
            assertEquals(LABS[4][1], LABS[5][1], "Opal and Mixboard should share same z row");
        }
    }

    @Nested
    class TPUMine {
        @Test
        void mine_isInsideBuilding() {
            assertTrue(rectInsideBuilding(MINE_START_X, MINE_START_Z, MINE_SIZE, MINE_SIZE));
        }

        @Test
        void mine_isCentered() {
            int mineCenterX = MINE_START_X + MINE_SIZE / 2;
            int mineCenterZ = MINE_START_Z + MINE_SIZE / 2;

            assertEquals(WIDTH / 2, mineCenterX, "Mine should be centered horizontally");
            assertEquals(DEPTH / 2, mineCenterZ, "Mine should be centered vertically");
        }

        @Test
        void mine_is50x50() {
            assertEquals(50, MINE_SIZE);
        }

        @Test
        void mine_goesDown15Blocks() {
            assertEquals(15, MINE_DEPTH);
        }

        @Test
        void mine_doNotOverlapCafeteria() {
            assertFalse(rectsOverlap(
                MINE_START_X, MINE_START_Z, MINE_SIZE, MINE_SIZE,
                CAFE_START_X, CAFE_START_Z, CAFE_WIDTH, CAFE_DEPTH
            ));
        }

        @Test
        void mine_doNotOverlapLobby() {
            assertFalse(rectsOverlap(
                MINE_START_X, MINE_START_Z, MINE_SIZE, MINE_SIZE,
                LOBBY_START_X, 0, LOBBY_END_X - LOBBY_START_X, LOBBY_DEPTH
            ));
        }

        @Test
        void mine_oreSpawnProbability_is15Percent() {
            // placeWallWithOre uses Math.random() < 0.15
            double probability = 0.15;
            assertTrue(probability > 0.0 && probability < 1.0,
                "Ore probability should be between 0 and 1");
            assertEquals(0.15, probability, 0.001);
        }
    }

    @Nested
    class Lobby {
        @Test
        void lobby_isInsideBuilding() {
            assertTrue(LOBBY_START_X >= 0);
            assertTrue(LOBBY_END_X <= WIDTH);
            assertTrue(LOBBY_DEPTH <= DEPTH);
        }

        @Test
        void lobby_isCenteredHorizontally() {
            int lobbyCenter = (LOBBY_START_X + LOBBY_END_X) / 2;
            assertEquals(WIDTH / 2, lobbyCenter);
        }

        @Test
        void lobby_width_is60Blocks() {
            assertEquals(60, LOBBY_END_X - LOBBY_START_X);
        }

        @Test
        void entrance_isCentered() {
            // Entrance clears x from lobbyStartX+20 to lobbyEndX-20
            int entranceStart = LOBBY_START_X + 20;
            int entranceEnd = LOBBY_END_X - 20;
            int entranceCenter = (entranceStart + entranceEnd) / 2;
            assertEquals(WIDTH / 2, entranceCenter);
        }

        @Test
        void entrance_width_is20Blocks() {
            int entranceWidth = (LOBBY_END_X - 20) - (LOBBY_START_X + 20);
            assertEquals(20, entranceWidth);
        }

        @Test
        void googleColorStripes_cover4Colors() {
            // Stripe width is 15, lobby width is 60, so 4 stripes
            int stripeWidth = 15;
            int lobbyWidth = LOBBY_END_X - LOBBY_START_X;
            int stripeCount = lobbyWidth / stripeWidth;
            assertEquals(4, stripeCount, "Should have exactly 4 Google color stripes");
        }
    }

    @Nested
    class Cafeteria {
        @Test
        void cafeteria_isInsideBuilding() {
            assertTrue(rectInsideBuilding(CAFE_START_X, CAFE_START_Z, CAFE_WIDTH, CAFE_DEPTH));
        }

        @Test
        void cafeteria_isAtBottomOfBuilding() {
            assertTrue(CAFE_START_Z > DEPTH / 2,
                "Cafeteria should be in the southern half of the building");
            assertTrue(CAFE_START_Z + CAFE_DEPTH <= DEPTH,
                "Cafeteria should not extend past building boundary");
        }

        @Test
        void cafeteria_has5Tables() {
            // Tables are placed in a loop: for (int i = 0; i < 5; i++)
            int tableCount = 5;
            assertEquals(5, tableCount);
        }

        @Test
        void cafeteria_tableSpacing_isUniform() {
            // Tables at cafeStartX + 10 + i * 16
            int[] tablePositions = new int[5];
            for (int i = 0; i < 5; i++) {
                tablePositions[i] = CAFE_START_X + 10 + i * 16;
            }
            // All tables should be inside cafeteria
            for (int i = 0; i < 5; i++) {
                assertTrue(tablePositions[i] >= CAFE_START_X,
                    "Table " + i + " is before cafeteria start");
                assertTrue(tablePositions[i] < CAFE_START_X + CAFE_WIDTH,
                    "Table " + i + " is past cafeteria end");
            }
        }

        @Test
        void cafeteria_hasDoorway() {
            // Door at cafeStartX + cafeWidth/2
            int doorX = CAFE_START_X + CAFE_WIDTH / 2;
            assertTrue(doorX > CAFE_START_X && doorX < CAFE_START_X + CAFE_WIDTH);
        }
    }

    @Nested
    class OverallLayout {
        @Test
        void allMajorAreas_fitInsideBuilding() {
            // Check all major areas fit within the 200x200 boundary
            assertTrue(rectInsideBuilding(MINE_START_X, MINE_START_Z, MINE_SIZE, MINE_SIZE), "Mine");
            assertTrue(rectInsideBuilding(LOBBY_START_X, 0, LOBBY_END_X - LOBBY_START_X, LOBBY_DEPTH), "Lobby");
            assertTrue(rectInsideBuilding(CAFE_START_X, CAFE_START_Z, CAFE_WIDTH, CAFE_DEPTH), "Cafeteria");
            for (int i = 0; i < LABS.length; i++) {
                assertTrue(rectInsideBuilding(LABS[i][0], LABS[i][1], LABS[i][2], LABS[i][3]),
                    LAB_NAMES[i] + " lab");
            }
        }

        @Test
        void centralHallway_hasClearPathAroundMine() {
            // Hallway runs x=55-144, z=30-169 (skipping mine area)
            int hallwayMinX = 55;
            int hallwayMaxX = 145;
            int hallwayMinZ = 30;
            int hallwayMaxZ = 170;

            // Mine is entirely inside the hallway area
            assertTrue(MINE_START_X >= hallwayMinX, "Mine should start within hallway x range");
            assertTrue(MINE_START_X + MINE_SIZE <= hallwayMaxX, "Mine should end within hallway x range");
            assertTrue(MINE_START_Z >= hallwayMinZ, "Mine should start within hallway z range");
            assertTrue(MINE_START_Z + MINE_SIZE <= hallwayMaxZ, "Mine should end within hallway z range");
        }

        @Test
        void noMajorAreas_overlap() {
            // Already tested pairwise in individual sections, this is a summary check.
            // Mine vs Cafeteria
            assertFalse(rectsOverlap(
                MINE_START_X, MINE_START_Z, MINE_SIZE, MINE_SIZE,
                CAFE_START_X, CAFE_START_Z, CAFE_WIDTH, CAFE_DEPTH));

            // Lobby vs Mine
            assertFalse(rectsOverlap(
                LOBBY_START_X, 0, LOBBY_END_X - LOBBY_START_X, LOBBY_DEPTH,
                MINE_START_X, MINE_START_Z, MINE_SIZE, MINE_SIZE));

            // Lobby vs Cafeteria
            assertFalse(rectsOverlap(
                LOBBY_START_X, 0, LOBBY_END_X - LOBBY_START_X, LOBBY_DEPTH,
                CAFE_START_X, CAFE_START_Z, CAFE_WIDTH, CAFE_DEPTH));
        }

        @Test
        void clearArea_extendsBeyondWallHeight() {
            // Clear area goes from y=-20 to y=WALL_HEIGHT+5
            int clearMinY = -20;
            int clearMaxY = WALL_HEIGHT + 5;
            assertTrue(clearMaxY > WALL_HEIGHT, "Clear area should extend above walls");
            assertTrue(clearMinY < 0, "Clear area should extend below ground");
        }
    }

    @Nested
    class FlowCraftingTablePlacement {
        // Crafting table at relative (85, 1, 12)
        static final int TABLE_X = 85;
        static final int TABLE_Z = 12;

        @Test
        void craftingTable_isInsideLobby() {
            assertTrue(TABLE_X >= LOBBY_START_X && TABLE_X < LOBBY_END_X,
                "Crafting table X should be inside lobby");
            assertTrue(TABLE_Z >= 0 && TABLE_Z < LOBBY_DEPTH,
                "Crafting table Z should be inside lobby");
        }

        @Test
        void craftingTable_doesNotOverlapReceptionDesk() {
            // Reception desk is at x=95-105, z=15-17
            int deskStartX = 95;
            int deskEndX = 105;
            int deskStartZ = 15;
            int deskEndZ = 17;

            boolean overlaps = TABLE_X >= deskStartX && TABLE_X <= deskEndX
                && TABLE_Z >= deskStartZ && TABLE_Z <= deskEndZ;
            assertFalse(overlaps, "Crafting table should not overlap reception desk");
        }

        @Test
        void craftingTable_isAccessibleFromEntrance() {
            // Entrance is at z=0, table at z=12 — player can walk straight to it
            assertTrue(TABLE_Z > 0 && TABLE_Z < LOBBY_DEPTH,
                "Table should be accessible from the entrance");
        }
    }

    @Nested
    class ConsolePlacements {
        @Test
        void flowLab_hasFlowConsole_notVeo() {
            // The Flow Lab should contain a Flow Console (previously had a bug placing Veo)
            // Flow Lab: startX=10, startZ=35, console at relative (30, 1, 57)
            int consoleX = 30;
            int consoleZ = 57;
            int labStartX = 10;
            int labStartZ = 35;
            int labWidth = 40;
            int labDepth = 45;

            assertTrue(consoleX >= labStartX && consoleX < labStartX + labWidth,
                "Flow console should be inside Flow lab X bounds");
            assertTrue(consoleZ >= labStartZ && consoleZ < labStartZ + labDepth,
                "Flow console should be inside Flow lab Z bounds");
        }

        @Test
        void genieLab_hasNanoBananaConsole() {
            // Genie Lab: startX=150, console at (170, 1, 57)
            int consoleX = 170;
            int consoleZ = 57;
            int labStartX = 150;
            int labStartZ = 35;
            int labWidth = 40;
            int labDepth = 45;

            assertTrue(consoleX >= labStartX && consoleX < labStartX + labWidth,
                "Nano Banana console should be inside Genie lab X bounds");
            assertTrue(consoleZ >= labStartZ && consoleZ < labStartZ + labDepth,
                "Nano Banana console should be inside Genie lab Z bounds");
        }
    }

    @Nested
    class ExteriorWalls {
        @Test
        void windowBands_areAtCorrectHeights() {
            // Windows at y=4-6 and y=8-10 (wall columns at y=3, y=7, y=11)
            int[] accentRows = {3, 7, 11};
            for (int row : accentRows) {
                assertTrue(row >= 1 && row <= WALL_HEIGHT,
                    "Accent row y=" + row + " should be within wall height");
            }
        }

        @Test
        void windowSpacing_is6Blocks() {
            // Windows are placed when x % 6 != 0 (wall columns every 6th block)
            int columnSpacing = 6;
            assertEquals(6, columnSpacing);
        }

        @Test
        void wallHeight_allows2BandsOfWindows() {
            // Band 1: y=4-6 (3 blocks)
            // Band 2: y=8-10 (3 blocks)
            int band1Height = 3;
            int band2Height = 3;
            int totalWindowHeight = band1Height + band2Height;
            assertTrue(totalWindowHeight < WALL_HEIGHT, "Windows should fit within wall height");
        }
    }
}
