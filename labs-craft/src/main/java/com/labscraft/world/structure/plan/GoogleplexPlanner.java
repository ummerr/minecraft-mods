package com.labscraft.world.structure.plan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Deterministically plans the Googleplex office blueprint. The structural
 * skeleton (rooms, walls, doors) is fixed; the seed drives cosmetic variation
 * (accent colors, plant species, desk-lamp placement, carpet runner phase).
 * Pure Java — no Minecraft imports — so the whole layout is unit-testable.
 *
 * <h2>Floor plan (z = 0 is the front / entrance face)</h2>
 * <pre>
 *  z=26 +------------------------------------------+
 *       |                FLOW LAB                   |  consoles + Flow Crafting
 *  z=19 +--------------[glass door]-----------------+  Table + server racks
 *       | MICRO-       |          |                 |
 *       | KITCHEN      | CORRIDOR |   DESK PODS     |  4 seeded-color pods
 *  z=13 +--------------+          |  (open plan,    |
 *       | MEETING ROOM |          |   low wall)     |
 *       | (glass wall) |          |                 |
 *  z=7  +-------------[door]------------------------+
 *       |                 LOBBY                     |  reception + Google dots
 *  z=0  +=============[entrance]====================+
 *        x=0            x=13..17                x=30
 * </pre>
 */
public final class GoogleplexPlanner {

    private static final int MAX_X = GoogleplexBlueprint.SIZE_X - 1; // 30
    private static final int MAX_Z = GoogleplexBlueprint.SIZE_Z - 1; // 26
    private static final int CEILING_Y = 5;
    private static final int PARAPET_Y = 6;

    // Fixed room rectangles (interior cells, inclusive).
    static final Room LOBBY = new Room(RoomType.LOBBY, 1, 1, 29, 6);
    static final Room CORRIDOR = new Room(RoomType.CORRIDOR, 13, 8, 17, 18);
    static final Room MEETING = new Room(RoomType.MEETING_ROOM, 1, 8, 11, 12);
    static final Room KITCHEN = new Room(RoomType.MICRO_KITCHEN, 1, 14, 11, 18);
    static final Room DESK_AREA = new Room(RoomType.DESK_AREA, 19, 8, 29, 18);
    static final Room LAB = new Room(RoomType.LAB, 1, 20, 29, 25);

    private static final BlockSpec[] GOOGLE_CARPETS = {
            BlockSpec.CARPET_BLUE, BlockSpec.CARPET_RED,
            BlockSpec.CARPET_YELLOW, BlockSpec.CARPET_GREEN,
    };

    private GoogleplexPlanner() {
    }

    public static GoogleplexBlueprint plan(long seed) {
        Random random = new Random(seed);
        BlockSpec[][][] g = emptyGrid();

        List<Room> rooms = List.of(LOBBY, CORRIDOR, MEETING, KITCHEN, DESK_AREA, LAB);
        List<Door> doors = buildDoorList();

        buildFloorAndShell(g);
        buildPartitions(g);
        cutDoors(g, doors);
        addCeilingLights(g);

        // Seeded draws happen in a fixed order so output is reproducible.
        furnishLobby(g, random);
        furnishMeeting(g);
        furnishKitchen(g);
        furnishDeskArea(g, random);
        furnishCorridor(g, random);
        furnishLab(g);

        return new GoogleplexBlueprint(g, rooms, doors, seed);
    }

    // ------------------------------------------------------------------
    // Skeleton
    // ------------------------------------------------------------------

    private static BlockSpec[][][] emptyGrid() {
        BlockSpec[][][] g = new BlockSpec[GoogleplexBlueprint.SIZE_X][GoogleplexBlueprint.SIZE_Y][GoogleplexBlueprint.SIZE_Z];
        for (BlockSpec[][] plane : g) {
            for (BlockSpec[] row : plane) {
                Arrays.fill(row, BlockSpec.AIR);
            }
        }
        return g;
    }

    private static List<Door> buildDoorList() {
        List<Door> doors = new ArrayList<>();
        // Front entrance, 3 wide.
        for (int x = 14; x <= 16; x++) {
            doors.add(new Door(x, 0, RoomType.OUTSIDE, RoomType.LOBBY));
        }
        // Lobby -> corridor, 3 wide, in the z=7 wall.
        for (int x = 14; x <= 16; x++) {
            doors.add(new Door(x, 7, RoomType.LOBBY, RoomType.CORRIDOR));
        }
        // Corridor -> meeting room and micro-kitchen, in the x=12 wall.
        doors.add(new Door(12, 10, RoomType.CORRIDOR, RoomType.MEETING_ROOM));
        doors.add(new Door(12, 16, RoomType.CORRIDOR, RoomType.MICRO_KITCHEN));
        // Corridor -> desk pods, two gaps in the x=18 low wall.
        doors.add(new Door(18, 10, RoomType.CORRIDOR, RoomType.DESK_AREA));
        doors.add(new Door(18, 16, RoomType.CORRIDOR, RoomType.DESK_AREA));
        // Corridor -> lab, glass double door, 3 wide, in the z=19 wall.
        for (int x = 14; x <= 16; x++) {
            doors.add(new Door(x, 19, RoomType.CORRIDOR, RoomType.LAB));
        }
        return doors;
    }

    private static void buildFloorAndShell(BlockSpec[][][] g) {
        // Floor slab across the full footprint; lab gets a darker floor.
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                boolean inLab = LAB.contains(x, z);
                g[x][0][z] = inLab ? BlockSpec.FLOOR_LAB : BlockSpec.FLOOR;
            }
        }

        // Perimeter walls, y=1..4.
        for (int y = 1; y <= 4; y++) {
            for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
                g[x][y][0] = BlockSpec.WALL;
                g[x][y][MAX_Z] = BlockSpec.WALL;
            }
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                g[0][y][z] = BlockSpec.WALL;
                g[MAX_X][y][z] = BlockSpec.WALL;
            }
        }

        // Window bands (y=2..3) on all four faces.
        for (int y = 2; y <= 3; y++) {
            for (int x = 3; x <= 11; x++) { // front-west band
                g[x][y][0] = BlockSpec.GLASS;
            }
            for (int x = 19; x <= 27; x++) { // front-east band
                g[x][y][0] = BlockSpec.GLASS;
            }
            for (int x = 3; x <= 27; x += 2) { // back wall, rhythm of panes
                g[x][y][MAX_Z] = BlockSpec.GLASS;
            }
            for (int z = 3; z <= 23; z += 2) { // side walls
                g[0][y][z] = BlockSpec.GLASS;
                g[MAX_X][y][z] = BlockSpec.GLASS;
            }
        }

        // Entrance framing: Google-colored columns flanking the doorway.
        for (int y = 1; y <= 4; y++) {
            g[13][y][0] = BlockSpec.WALL_ACCENT_BLUE;
            g[17][y][0] = BlockSpec.WALL_ACCENT_RED;
        }
        // Accent lintel above the entrance.
        g[14][4][0] = BlockSpec.WALL_ACCENT_YELLOW;
        g[15][4][0] = BlockSpec.WALL_ACCENT_GREEN;
        g[16][4][0] = BlockSpec.WALL_ACCENT_BLUE;

        // Ceiling slab at y=5 across the footprint.
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
                g[x][CEILING_Y][z] = BlockSpec.CEILING;
            }
        }

        // Roof parapet on the perimeter at y=6, with a four-color "logo" run
        // above the entrance.
        for (int x = 0; x < GoogleplexBlueprint.SIZE_X; x++) {
            g[x][PARAPET_Y][0] = BlockSpec.WALL;
            g[x][PARAPET_Y][MAX_Z] = BlockSpec.WALL;
        }
        for (int z = 0; z < GoogleplexBlueprint.SIZE_Z; z++) {
            g[0][PARAPET_Y][z] = BlockSpec.WALL;
            g[MAX_X][PARAPET_Y][z] = BlockSpec.WALL;
        }
        g[13][PARAPET_Y][0] = BlockSpec.WALL_ACCENT_BLUE;
        g[14][PARAPET_Y][0] = BlockSpec.WALL_ACCENT_RED;
        g[16][PARAPET_Y][0] = BlockSpec.WALL_ACCENT_YELLOW;
        g[17][PARAPET_Y][0] = BlockSpec.WALL_ACCENT_GREEN;
    }

    private static void buildPartitions(BlockSpec[][][] g) {
        // Lobby / middle-row wall at z=7: solid base and top, glass band.
        for (int x = 1; x <= 29; x++) {
            g[x][1][7] = BlockSpec.WALL;
            g[x][2][7] = (x >= 2 && x <= 28) ? BlockSpec.GLASS : BlockSpec.WALL;
            g[x][3][7] = (x >= 2 && x <= 28) ? BlockSpec.GLASS : BlockSpec.WALL;
            g[x][4][7] = BlockSpec.WALL;
        }

        // Middle row / lab wall at z=19: same glass-band treatment.
        for (int x = 1; x <= 29; x++) {
            g[x][1][19] = BlockSpec.WALL;
            g[x][2][19] = (x >= 2 && x <= 28) ? BlockSpec.GLASS : BlockSpec.WALL;
            g[x][3][19] = (x >= 2 && x <= 28) ? BlockSpec.GLASS : BlockSpec.WALL;
            g[x][4][19] = BlockSpec.WALL;
        }

        // West partition at x=12 (meeting room side fully glass, kitchen side solid).
        for (int z = 8; z <= 18; z++) {
            boolean meetingSide = z <= 12;
            for (int y = 1; y <= 4; y++) {
                g[12][y][z] = meetingSide ? BlockSpec.GLASS : BlockSpec.WALL;
            }
        }

        // Meeting / kitchen divider at z=13.
        for (int x = 1; x <= 11; x++) {
            for (int y = 1; y <= 4; y++) {
                g[x][y][13] = BlockSpec.WALL;
            }
        }

        // East partition at x=18: open-plan low wall (y=1 only) with planters.
        for (int z = 8; z <= 18; z++) {
            g[18][1][z] = BlockSpec.WALL;
        }
        g[18][2][9] = BlockSpec.PLANT_FERN;
        g[18][2][13] = BlockSpec.PLANT_BAMBOO;
        g[18][2][17] = BlockSpec.PLANT_FERN;
    }

    private static void cutDoors(BlockSpec[][][] g, List<Door> doors) {
        for (Door door : doors) {
            // Entrance and lab doors are tall (y=1..3); interior doors y=1..2.
            boolean tall = door.z() == 0 || door.z() == 19;
            int top = tall ? 3 : 2;
            for (int y = 1; y <= top; y++) {
                g[door.x()][y][door.z()] = BlockSpec.DOOR_AIR;
            }
        }
    }

    private static void addCeilingLights(BlockSpec[][][] g) {
        for (int x = 2; x <= 28; x += 3) {
            for (int z = 2; z <= 24; z += 3) {
                g[x][CEILING_Y][z] = BlockSpec.LIGHT;
            }
        }
    }

    // ------------------------------------------------------------------
    // Furnishing
    // ------------------------------------------------------------------

    private static void furnishLobby(BlockSpec[][][] g, Random random) {
        // Reception counter on the east side, facing the entrance.
        for (int x = 20; x <= 24; x++) {
            g[x][1][4] = BlockSpec.RECEPTION;
        }
        g[22][1][5] = BlockSpec.CHAIR_NORTH; // receptionist faces the door

        // The four Google dots: a row of colored carpet inside the entrance.
        g[9][1][3] = BlockSpec.CARPET_BLUE;
        g[11][1][3] = BlockSpec.CARPET_RED;
        g[13][1][3] = BlockSpec.CARPET_YELLOW;
        g[15][1][3] = BlockSpec.CARPET_GREEN;

        // Potted plants in the corners; species is seeded flavor.
        int[][] plantSpots = {{2, 2}, {28, 2}, {2, 5}, {28, 5}};
        for (int[] spot : plantSpots) {
            g[spot[0]][1][spot[1]] = random.nextBoolean() ? BlockSpec.PLANT_FERN : BlockSpec.PLANT_BAMBOO;
        }
    }

    private static void furnishMeeting(BlockSpec[][][] g) {
        // Long conference table with chairs on both sides and a head chair.
        for (int x = 3; x <= 8; x++) {
            g[x][1][10] = BlockSpec.TABLE;
            g[x][1][9] = BlockSpec.CHAIR_SOUTH;  // north row faces the table
            g[x][1][11] = BlockSpec.CHAIR_NORTH; // south row faces the table
        }
        g[2][1][10] = BlockSpec.CHAIR_EAST;

        // Wall screen for presentations on the west wall.
        for (int z = 9; z <= 11; z++) {
            g[1][2][z] = BlockSpec.SCREEN;
            g[1][3][z] = BlockSpec.SCREEN;
        }
    }

    private static void furnishKitchen(BlockSpec[][][] g) {
        // Counter run along the south edge with snacks and storage.
        for (int x = 2; x <= 8; x++) {
            g[x][1][18] = BlockSpec.COUNTER;
        }
        g[4][2][18] = BlockSpec.CAKE;
        g[9][1][18] = BlockSpec.BARREL;
        g[1][1][14] = BlockSpec.FRIDGE;
        g[1][2][14] = BlockSpec.FRIDGE;

        // Small cafe table with two chairs.
        g[5][1][15] = BlockSpec.TABLE;
        g[6][1][15] = BlockSpec.TABLE;
        g[4][1][15] = BlockSpec.CHAIR_EAST;
        g[7][1][15] = BlockSpec.CHAIR_WEST;
    }

    private static void furnishDeskArea(BlockSpec[][][] g, Random random) {
        // Four 2x2 desk pods; each pod gets a seeded Google color and a
        // seeded desk-lamp position.
        List<BlockSpec> podColors = new ArrayList<>(List.of(GOOGLE_CARPETS));
        Collections.shuffle(podColors, random);

        int[][] podOrigins = {{20, 9}, {25, 9}, {20, 14}, {25, 14}};
        for (int i = 0; i < podOrigins.length; i++) {
            int px = podOrigins[i][0];
            int pz = podOrigins[i][1];
            BlockSpec color = podColors.get(i);

            // 2x2 desk cluster.
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    g[px + dx][1][pz + dz] = BlockSpec.DESK;
                }
            }
            // Desk lamp on one seeded corner of the cluster.
            int lamp = random.nextInt(4);
            g[px + (lamp & 1)][2][pz + (lamp >> 1)] = BlockSpec.DESK_LAMP;

            // Two chairs on opposite sides.
            g[px - 1][1][pz] = BlockSpec.CHAIR_EAST;
            g[px + 2][1][pz + 1] = BlockSpec.CHAIR_WEST;

            // Pod-color carpet at the diagonal corners.
            g[px - 1][1][pz - 1] = color;
            g[px + 2][1][pz - 1] = color;
            g[px - 1][1][pz + 2] = color;
            g[px + 2][1][pz + 2] = color;
        }
    }

    private static void furnishCorridor(BlockSpec[][][] g, Random random) {
        // Colored carpet runner down the corridor spine; phase is seeded.
        int phase = random.nextInt(GOOGLE_CARPETS.length);
        for (int z = 8; z <= 18; z++) {
            g[15][1][z] = GOOGLE_CARPETS[(z + phase) % GOOGLE_CARPETS.length];
        }
        g[13][1][18] = BlockSpec.PLANT_BAMBOO;
        g[17][1][8] = BlockSpec.PLANT_FERN;
    }

    private static void furnishLab(BlockSpec[][][] g) {
        // The three generation consoles along the back of the lab.
        g[11][1][24] = BlockSpec.CONSOLE_FLOW;
        g[15][1][24] = BlockSpec.CONSOLE_NANO_BANANA;
        g[19][1][24] = BlockSpec.CONSOLE_VEO;

        // Flow Crafting Table centered in the lab, ringed by Google dots.
        g[15][1][22] = BlockSpec.FLOW_CRAFTING_TABLE;
        g[13][1][22] = BlockSpec.CARPET_BLUE;
        g[17][1][22] = BlockSpec.CARPET_RED;
        g[15][1][21] = BlockSpec.CARPET_YELLOW;
        g[14][1][23] = BlockSpec.CARPET_GREEN;

        // Server racks along the east and west lab walls.
        for (int z = 21; z <= 25; z += 2) {
            for (int x : new int[]{2, 28}) {
                g[x][1][z] = BlockSpec.SERVER_RACK;
                g[x][2][z] = BlockSpec.SERVER_RACK;
                g[x][3][z] = BlockSpec.SERVER_LIGHT;
            }
        }
    }
}
