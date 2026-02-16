package com.labscraft.world;

import com.labscraft.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class GoogleplexGenerator {
    private final ServerWorld world;
    private final BlockPos origin; // Southwest corner of building at ground level

    // Building dimensions
    private static final int WIDTH = 200;
    private static final int DEPTH = 200;
    private static final int WALL_HEIGHT = 12;
    private static final int FLOOR_Y_OFFSET = 0;

    // Block palettes
    private static final BlockState EXTERIOR_WALL = Blocks.WHITE_CONCRETE.getDefaultState();
    private static final BlockState EXTERIOR_ACCENT = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
    private static final BlockState GLASS = Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState();
    private static final BlockState FLOOR = Blocks.POLISHED_GRANITE.getDefaultState();
    private static final BlockState FLOOR_ACCENT = Blocks.POLISHED_DIORITE.getDefaultState();
    private static final BlockState CEILING = Blocks.SMOOTH_QUARTZ.getDefaultState();
    private static final BlockState INTERIOR_WALL = Blocks.WHITE_CONCRETE.getDefaultState();
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    // Google colors for accents
    private static final BlockState GOOGLE_BLUE = Blocks.BLUE_CONCRETE.getDefaultState();
    private static final BlockState GOOGLE_RED = Blocks.RED_CONCRETE.getDefaultState();
    private static final BlockState GOOGLE_YELLOW = Blocks.YELLOW_CONCRETE.getDefaultState();
    private static final BlockState GOOGLE_GREEN = Blocks.LIME_CONCRETE.getDefaultState();

    // Lab accent colors
    private static final BlockState FLOW_COLOR = Blocks.BLUE_CONCRETE.getDefaultState();
    private static final BlockState GENIE_COLOR = Blocks.MAGENTA_CONCRETE.getDefaultState();
    private static final BlockState DOPPL_COLOR = Blocks.CYAN_CONCRETE.getDefaultState();
    private static final BlockState NOTEBOOK_COLOR = Blocks.ORANGE_CONCRETE.getDefaultState();
    private static final BlockState OPAL_COLOR = Blocks.GREEN_CONCRETE.getDefaultState();
    private static final BlockState MIXBOARD_COLOR = Blocks.PURPLE_CONCRETE.getDefaultState();

    public GoogleplexGenerator(ServerWorld world, BlockPos origin) {
        this.world = world;
        this.origin = origin;
    }

    public void generate() {
        // Clear the area first
        clearArea();

        // Build foundation and floor
        buildFoundation();

        // Build exterior walls
        buildExteriorWalls();

        // Build ceiling
        buildCeiling();

        // Build entrance/lobby
        buildLobby();

        // Build central TPU mine
        buildTPUMine();

        // Build the 6 product labs
        buildFlowLab();
        buildGenieLab();
        buildDopplLab();
        buildNotebookLMLab();
        buildOpalLab();
        buildMixboardLab();

        // Build cafeteria
        buildCafeteria();

        // Add hallways connecting everything
        buildHallways();

        // Add decorations
        addDecorations();

        // Place interactive furniture
        placeFlowCraftingTable();
    }

    private void clearArea() {
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = -20; y < WALL_HEIGHT + 5; y++) {
                    setBlock(x, y, z, AIR);
                }
            }
        }
    }

    private void buildFoundation() {
        // Main floor
        for (int x = 0; x < WIDTH; x++) {
            for (int z = 0; z < DEPTH; z++) {
                // Checkerboard pattern
                if ((x + z) % 8 < 4) {
                    setBlock(x, FLOOR_Y_OFFSET, z, FLOOR);
                } else {
                    setBlock(x, FLOOR_Y_OFFSET, z, FLOOR_ACCENT);
                }
            }
        }
    }

    private void buildExteriorWalls() {
        // North wall (z = 0)
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                if (y == 3 || y == 7 || y == 11) {
                    setBlock(x, y, 0, EXTERIOR_ACCENT);
                } else if (y >= 4 && y <= 6 || y >= 8 && y <= 10) {
                    // Windows
                    if (x % 6 != 0) {
                        setBlock(x, y, 0, GLASS);
                    } else {
                        setBlock(x, y, 0, EXTERIOR_WALL);
                    }
                } else {
                    setBlock(x, y, 0, EXTERIOR_WALL);
                }
            }
        }

        // South wall (z = DEPTH-1)
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                if (y == 3 || y == 7 || y == 11) {
                    setBlock(x, y, DEPTH - 1, EXTERIOR_ACCENT);
                } else if (y >= 4 && y <= 6 || y >= 8 && y <= 10) {
                    if (x % 6 != 0) {
                        setBlock(x, y, DEPTH - 1, GLASS);
                    } else {
                        setBlock(x, y, DEPTH - 1, EXTERIOR_WALL);
                    }
                } else {
                    setBlock(x, y, DEPTH - 1, EXTERIOR_WALL);
                }
            }
        }

        // West wall (x = 0)
        for (int z = 0; z < DEPTH; z++) {
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                if (y == 3 || y == 7 || y == 11) {
                    setBlock(0, y, z, EXTERIOR_ACCENT);
                } else if (y >= 4 && y <= 6 || y >= 8 && y <= 10) {
                    if (z % 6 != 0) {
                        setBlock(0, y, z, GLASS);
                    } else {
                        setBlock(0, y, z, EXTERIOR_WALL);
                    }
                } else {
                    setBlock(0, y, z, EXTERIOR_WALL);
                }
            }
        }

        // East wall (x = WIDTH-1)
        for (int x = WIDTH - 1; x == WIDTH - 1; x++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int y = 1; y <= WALL_HEIGHT; y++) {
                    if (y == 3 || y == 7 || y == 11) {
                        setBlock(x, y, z, EXTERIOR_ACCENT);
                    } else if (y >= 4 && y <= 6 || y >= 8 && y <= 10) {
                        if (z % 6 != 0) {
                            setBlock(x, y, z, GLASS);
                        } else {
                            setBlock(x, y, z, EXTERIOR_WALL);
                        }
                    } else {
                        setBlock(x, y, z, EXTERIOR_WALL);
                    }
                }
            }
        }
    }

    private void buildCeiling() {
        for (int x = 1; x < WIDTH - 1; x++) {
            for (int z = 1; z < DEPTH - 1; z++) {
                setBlock(x, WALL_HEIGHT + 1, z, CEILING);
            }
        }
    }

    private void buildLobby() {
        // Lobby at north side, center (entrance)
        int lobbyStartX = 70;
        int lobbyEndX = 130;
        int lobbyDepth = 30;

        // Clear entrance in north wall
        for (int x = lobbyStartX + 20; x < lobbyEndX - 20; x++) {
            for (int y = 1; y <= 4; y++) {
                setBlock(x, y, 0, AIR);
            }
        }

        // Lobby floor - Google colors stripe
        for (int x = lobbyStartX; x < lobbyEndX; x++) {
            for (int z = 1; z < lobbyDepth; z++) {
                int stripe = (x - lobbyStartX) / 15;
                BlockState color = switch (stripe % 4) {
                    case 0 -> GOOGLE_BLUE;
                    case 1 -> GOOGLE_RED;
                    case 2 -> GOOGLE_YELLOW;
                    default -> GOOGLE_GREEN;
                };
                setBlock(x, FLOOR_Y_OFFSET, z, color);
            }
        }

        // Reception desk
        int deskX = 95;
        int deskZ = 15;
        for (int x = deskX; x < deskX + 10; x++) {
            setBlock(x, 1, deskZ, Blocks.QUARTZ_SLAB.getDefaultState());
            setBlock(x, 1, deskZ + 1, Blocks.QUARTZ_BLOCK.getDefaultState());
            setBlock(x, 1, deskZ + 2, Blocks.QUARTZ_SLAB.getDefaultState());
        }

        // "LABS" sign on back wall using colored wool
        buildLabsSign(85, 6, lobbyDepth - 1);
    }

    private void buildLabsSign(int startX, int startY, int z) {
        // Simple "LABS" text using blocks
        BlockState letter = Blocks.WHITE_WOOL.getDefaultState();

        // L
        for (int y = 0; y < 5; y++) setBlock(startX, startY + y, z, letter);
        for (int x = 0; x < 3; x++) setBlock(startX + x, startY, z, letter);

        // A
        for (int y = 0; y < 5; y++) setBlock(startX + 5, startY + y, z, letter);
        for (int y = 0; y < 5; y++) setBlock(startX + 8, startY + y, z, letter);
        setBlock(startX + 6, startY + 4, z, letter);
        setBlock(startX + 7, startY + 4, z, letter);
        setBlock(startX + 6, startY + 2, z, letter);
        setBlock(startX + 7, startY + 2, z, letter);

        // B
        for (int y = 0; y < 5; y++) setBlock(startX + 11, startY + y, z, letter);
        setBlock(startX + 12, startY + 4, z, letter);
        setBlock(startX + 13, startY + 4, z, letter);
        setBlock(startX + 14, startY + 3, z, letter);
        setBlock(startX + 12, startY + 2, z, letter);
        setBlock(startX + 13, startY + 2, z, letter);
        setBlock(startX + 14, startY + 1, z, letter);
        setBlock(startX + 12, startY, z, letter);
        setBlock(startX + 13, startY, z, letter);

        // S
        setBlock(startX + 17, startY + 4, z, letter);
        setBlock(startX + 18, startY + 4, z, letter);
        setBlock(startX + 19, startY + 4, z, letter);
        setBlock(startX + 17, startY + 3, z, letter);
        setBlock(startX + 17, startY + 2, z, letter);
        setBlock(startX + 18, startY + 2, z, letter);
        setBlock(startX + 19, startY + 2, z, letter);
        setBlock(startX + 19, startY + 1, z, letter);
        setBlock(startX + 17, startY, z, letter);
        setBlock(startX + 18, startY, z, letter);
        setBlock(startX + 19, startY, z, letter);
    }

    private void buildTPUMine() {
        // Central pit, 50x50, going down 15 blocks
        int mineStartX = 75;
        int mineStartZ = 75;
        int mineSize = 50;
        int mineDepth = 15;

        // Glass floor around the pit
        for (int x = mineStartX - 5; x < mineStartX + mineSize + 5; x++) {
            for (int z = mineStartZ - 5; z < mineStartZ + mineSize + 5; z++) {
                if (x < mineStartX || x >= mineStartX + mineSize ||
                    z < mineStartZ || z >= mineStartZ + mineSize) {
                    setBlock(x, FLOOR_Y_OFFSET, z, Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState());
                }
            }
        }

        // Dig the pit
        for (int x = mineStartX; x < mineStartX + mineSize; x++) {
            for (int z = mineStartZ; z < mineStartZ + mineSize; z++) {
                for (int y = FLOOR_Y_OFFSET; y > FLOOR_Y_OFFSET - mineDepth; y--) {
                    if (y == FLOOR_Y_OFFSET - mineDepth + 1) {
                        // Bottom floor
                        setBlock(x, y, z, Blocks.DEEPSLATE.getDefaultState());
                    } else {
                        setBlock(x, y, z, AIR);
                    }
                }
            }
        }

        // Walls of the pit with stone and TPU ore veins
        for (int y = FLOOR_Y_OFFSET; y > FLOOR_Y_OFFSET - mineDepth; y--) {
            for (int x = mineStartX; x < mineStartX + mineSize; x++) {
                placeWallWithOre(x, y, mineStartZ - 1);
                placeWallWithOre(x, y, mineStartZ + mineSize);
            }
            for (int z = mineStartZ; z < mineStartZ + mineSize; z++) {
                placeWallWithOre(mineStartX - 1, y, z);
                placeWallWithOre(mineStartX + mineSize, y, z);
            }
        }

        // Ladders down on one side
        for (int y = FLOOR_Y_OFFSET; y > FLOOR_Y_OFFSET - mineDepth + 1; y--) {
            setBlock(mineStartX + 1, y, mineStartZ, Blocks.LADDER.getDefaultState());
        }

        // Safety railing around the pit
        for (int x = mineStartX - 1; x <= mineStartX + mineSize; x++) {
            setBlock(x, 2, mineStartZ - 1, Blocks.IRON_BARS.getDefaultState());
            setBlock(x, 2, mineStartZ + mineSize, Blocks.IRON_BARS.getDefaultState());
        }
        for (int z = mineStartZ - 1; z <= mineStartZ + mineSize; z++) {
            setBlock(mineStartX - 1, 2, z, Blocks.IRON_BARS.getDefaultState());
            setBlock(mineStartX + mineSize, 2, z, Blocks.IRON_BARS.getDefaultState());
        }

        // TPU Mine sign
        // TODO: Add sign
    }

    private void placeWallWithOre(int x, int y, int z) {
        // Random TPU ore veins
        if (Math.random() < 0.15) {
            if (y < FLOOR_Y_OFFSET - 8) {
                setBlock(x, y, z, ModBlocks.DEEPSLATE_TPU_ORE.getDefaultState());
            } else {
                setBlock(x, y, z, ModBlocks.TPU_ORE.getDefaultState());
            }
        } else {
            if (y < FLOOR_Y_OFFSET - 8) {
                setBlock(x, y, z, Blocks.DEEPSLATE.getDefaultState());
            } else {
                setBlock(x, y, z, Blocks.STONE.getDefaultState());
            }
        }
    }

    private void buildLab(int startX, int startZ, int width, int depth, BlockState accentColor, String name) {
        // Build walls
        for (int x = startX; x < startX + width; x++) {
            for (int y = 1; y <= 8; y++) {
                setBlock(x, y, startZ, INTERIOR_WALL);
                setBlock(x, y, startZ + depth - 1, INTERIOR_WALL);
            }
        }
        for (int z = startZ; z < startZ + depth; z++) {
            for (int y = 1; y <= 8; y++) {
                setBlock(startX, y, z, INTERIOR_WALL);
                setBlock(startX + width - 1, y, z, INTERIOR_WALL);
            }
        }

        // Accent stripe at top
        for (int x = startX; x < startX + width; x++) {
            setBlock(x, 8, startZ, accentColor);
            setBlock(x, 8, startZ + depth - 1, accentColor);
        }
        for (int z = startZ; z < startZ + depth; z++) {
            setBlock(startX, 8, z, accentColor);
            setBlock(startX + width - 1, 8, z, accentColor);
        }

        // Doorway
        for (int y = 1; y <= 3; y++) {
            setBlock(startX + width / 2, y, startZ, AIR);
            setBlock(startX + width / 2 + 1, y, startZ, AIR);
        }

        // Floor with accent color border
        for (int x = startX + 1; x < startX + width - 1; x++) {
            for (int z = startZ + 1; z < startZ + depth - 1; z++) {
                if (x == startX + 1 || x == startX + width - 2 ||
                    z == startZ + 1 || z == startZ + depth - 2) {
                    setBlock(x, FLOOR_Y_OFFSET, z, accentColor);
                }
            }
        }

        // Console in center
        int consoleX = startX + width / 2;
        int consoleZ = startZ + depth / 2;
        setBlock(consoleX, 1, consoleZ, accentColor);

        // Desk/workstations
        for (int i = 0; i < 3; i++) {
            setBlock(startX + 3, 1, startZ + 5 + i * 4, Blocks.SPRUCE_PLANKS.getDefaultState());
            setBlock(startX + width - 4, 1, startZ + 5 + i * 4, Blocks.SPRUCE_PLANKS.getDefaultState());
        }

        // Lighting
        setBlock(startX + width / 2, 7, startZ + depth / 2, Blocks.SEA_LANTERN.getDefaultState());
    }

    private void buildFlowLab() {
        // Top-left area
        buildLab(10, 35, 40, 45, FLOW_COLOR, "Flow");
        // Add Flow Console
        setBlock(30, 1, 57, ModBlocks.FLOW_CONSOLE.getDefaultState());
    }

    private void buildGenieLab() {
        // Top-right area
        buildLab(150, 35, 40, 45, GENIE_COLOR, "Genie");
        setBlock(170, 1, 57, ModBlocks.NANO_BANANA_CONSOLE.getDefaultState());
    }

    private void buildDopplLab() {
        // Middle-left area
        buildLab(10, 85, 40, 45, DOPPL_COLOR, "Doppl");
    }

    private void buildNotebookLMLab() {
        // Middle-right area
        buildLab(150, 85, 40, 45, NOTEBOOK_COLOR, "NotebookLM");
    }

    private void buildOpalLab() {
        // Bottom-left area
        buildLab(10, 135, 40, 45, OPAL_COLOR, "Opal");
    }

    private void buildMixboardLab() {
        // Bottom-right area
        buildLab(150, 135, 40, 45, MIXBOARD_COLOR, "Mixboard");
    }

    private void buildCafeteria() {
        // Bottom center area
        int cafeStartX = 55;
        int cafeStartZ = 170;
        int cafeWidth = 90;
        int cafeDepth = 25;

        // Walls
        for (int x = cafeStartX; x < cafeStartX + cafeWidth; x++) {
            for (int y = 1; y <= 6; y++) {
                setBlock(x, y, cafeStartZ, INTERIOR_WALL);
            }
        }

        // Different floor
        for (int x = cafeStartX; x < cafeStartX + cafeWidth; x++) {
            for (int z = cafeStartZ + 1; z < cafeStartZ + cafeDepth; z++) {
                setBlock(x, FLOOR_Y_OFFSET, z, Blocks.DARK_OAK_PLANKS.getDefaultState());
            }
        }

        // Tables and chairs
        for (int i = 0; i < 5; i++) {
            int tableX = cafeStartX + 10 + i * 16;
            int tableZ = cafeStartZ + 10;

            // Table
            setBlock(tableX, 1, tableZ, Blocks.SPRUCE_FENCE.getDefaultState());
            setBlock(tableX, 2, tableZ, Blocks.SPRUCE_PRESSURE_PLATE.getDefaultState());

            // Chairs (stairs facing table)
            setBlock(tableX - 1, 1, tableZ, Blocks.SPRUCE_STAIRS.getDefaultState());
            setBlock(tableX + 1, 1, tableZ, Blocks.SPRUCE_STAIRS.getDefaultState());
        }

        // Counter/kitchen area
        for (int x = cafeStartX + 5; x < cafeStartX + cafeWidth - 5; x++) {
            setBlock(x, 1, cafeStartZ + 2, Blocks.SMOOTH_STONE_SLAB.getDefaultState());
        }

        // Doorway
        for (int y = 1; y <= 3; y++) {
            setBlock(cafeStartX + cafeWidth / 2, y, cafeStartZ, AIR);
            setBlock(cafeStartX + cafeWidth / 2 + 1, y, cafeStartZ, AIR);
        }

        // Lighting
        for (int i = 0; i < 3; i++) {
            setBlock(cafeStartX + 20 + i * 25, 5, cafeStartZ + 12, Blocks.LANTERN.getDefaultState());
        }
    }

    private void buildHallways() {
        // Main central hallway (north-south)
        for (int z = 30; z < 170; z++) {
            for (int x = 55; x < 145; x++) {
                // Skip TPU mine area
                if (x >= 70 && x <= 130 && z >= 70 && z <= 130) continue;

                // Clear height for hallway
                for (int y = 1; y <= 8; y++) {
                    BlockPos pos = origin.add(x, y, z);
                    if (world.getBlockState(pos).isOf(Blocks.AIR)) continue;
                    if (!world.getBlockState(pos).isOf(Blocks.WHITE_CONCRETE) &&
                        !world.getBlockState(pos).isOf(Blocks.IRON_BARS)) {
                        // Don't clear walls
                    }
                }
            }
        }

        // Hallway lighting
        for (int z = 35; z < 170; z += 10) {
            if (z < 70 || z > 130) {
                setBlock(100, 7, z, Blocks.GLOWSTONE.getDefaultState());
            }
        }
    }

    private void addDecorations() {
        // Plants in lobby
        setBlock(75, 1, 10, Blocks.POTTED_FERN.getDefaultState());
        setBlock(125, 1, 10, Blocks.POTTED_FERN.getDefaultState());
        setBlock(80, 1, 25, Blocks.POTTED_BAMBOO.getDefaultState());
        setBlock(120, 1, 25, Blocks.POTTED_BAMBOO.getDefaultState());

        // Benches in hallways
        for (int z = 40; z < 70; z += 15) {
            setBlock(52, 1, z, Blocks.SPRUCE_STAIRS.getDefaultState());
            setBlock(53, 1, z, Blocks.SPRUCE_STAIRS.getDefaultState());
            setBlock(147, 1, z, Blocks.SPRUCE_STAIRS.getDefaultState());
            setBlock(148, 1, z, Blocks.SPRUCE_STAIRS.getDefaultState());
        }

        // Ceiling lights throughout
        for (int x = 20; x < WIDTH - 20; x += 15) {
            for (int z = 40; z < DEPTH - 40; z += 15) {
                // Skip if in a room or the mine
                if (x >= 70 && x <= 130 && z >= 70 && z <= 130) continue;
                if (x >= 10 && x <= 50 && z >= 35 && z <= 180) continue;
                if (x >= 150 && x <= 190 && z >= 35 && z <= 180) continue;

                setBlock(x, WALL_HEIGHT, z, Blocks.GLOWSTONE.getDefaultState());
            }
        }
    }

    private void placeFlowCraftingTable() {
        // Place in lobby, left of reception desk
        setBlock(85, 1, 12, ModBlocks.FLOW_CRAFTING_TABLE.getDefaultState());
    }

    private void setBlock(int x, int y, int z, BlockState state) {
        BlockPos pos = origin.add(x, y, z);
        world.setBlockState(pos, state);
    }

    // --- Accessors for spawn/NPC placement ---

    public BlockPos getOrigin() {
        return origin;
    }

    /** Returns the recommended world spawn position (inside lobby entrance). */
    public BlockPos getLobbySpawnPos() {
        return origin.add(100, 1, 2);
    }

    /** Returns the recommended position for Josh Woodward NPC (center of lobby). */
    public BlockPos getJoshSpawnPos() {
        return origin.add(100, 1, 10);
    }
}
