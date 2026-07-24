package com.labscraft.world.structure.plan;

import java.util.List;

/**
 * The fully-resolved plan of the Googleplex office: a dense 3D grid of
 * {@link BlockSpec}s plus room and door metadata. Immutable once built.
 * Pure Java — no Minecraft imports — so it is directly unit-testable.
 *
 * <p>Grid convention: {@code y = 0} is the floor slab, {@code y = 1..4} is the
 * interior air space, {@code y = 5} is the ceiling, {@code y = 6} is the roof
 * parapet. The entrance is on the {@code z = 0} face.</p>
 */
public final class GoogleplexBlueprint {
    public static final int SIZE_X = 31;
    public static final int SIZE_Y = 7;
    public static final int SIZE_Z = 27;

    /** Entrance doorway center, on the z = 0 face. */
    public static final int ENTRANCE_X = 15;
    public static final int ENTRANCE_Z = 0;

    private final BlockSpec[][][] grid; // [x][y][z]
    private final List<Room> rooms;
    private final List<Door> doors;
    private final long seed;

    public GoogleplexBlueprint(BlockSpec[][][] grid, List<Room> rooms, List<Door> doors, long seed) {
        if (grid.length != SIZE_X || grid[0].length != SIZE_Y || grid[0][0].length != SIZE_Z) {
            throw new IllegalArgumentException("Grid must be " + SIZE_X + "x" + SIZE_Y + "x" + SIZE_Z);
        }
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    if (grid[x][y][z] == null) {
                        throw new IllegalArgumentException("Null spec at " + x + "," + y + "," + z);
                    }
                }
            }
        }
        this.grid = grid;
        this.rooms = List.copyOf(rooms);
        this.doors = List.copyOf(doors);
        this.seed = seed;
    }

    public BlockSpec get(int x, int y, int z) {
        return grid[x][y][z];
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X && y >= 0 && y < SIZE_Y && z >= 0 && z < SIZE_Z;
    }

    /** Whether a player standing at (x, z) fits: body-height cells y=1 and y=2 both passable. */
    public boolean isWalkable(int x, int z) {
        return grid[x][1][z].passable() && grid[x][2][z].passable();
    }

    public List<Room> rooms() {
        return rooms;
    }

    public List<Door> doors() {
        return doors;
    }

    public long seed() {
        return seed;
    }

    public Room room(RoomType type) {
        for (Room room : rooms) {
            if (room.type() == type) {
                return room;
            }
        }
        throw new IllegalArgumentException("No room of type " + type);
    }

    /** Deep copy of the grid, for building modified blueprints (tests). */
    public BlockSpec[][][] copyGrid() {
        BlockSpec[][][] copy = new BlockSpec[SIZE_X][SIZE_Y][SIZE_Z];
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                System.arraycopy(grid[x][y], 0, copy[x][y], 0, SIZE_Z);
            }
        }
        return copy;
    }

    /** Total number of non-AIR cells (placement work estimate). */
    public int solidCellCount() {
        int count = 0;
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    if (grid[x][y][z] != BlockSpec.AIR) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
