package com.labscraft.world.structure.plan;

/**
 * An axis-aligned interior room rectangle in plan space (inclusive bounds).
 * Coordinates are blueprint-local: x grows east, z grows toward the back of
 * the building (the entrance is at z = 0).
 */
public record Room(RoomType type, int minX, int minZ, int maxX, int maxZ) {

    public Room {
        if (minX > maxX || minZ > maxZ) {
            throw new IllegalArgumentException("Degenerate room rect for " + type
                    + ": (" + minX + "," + minZ + ")-(" + maxX + "," + maxZ + ")");
        }
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(Room other) {
        return minX <= other.maxX && maxX >= other.minX
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int depth() {
        return maxZ - minZ + 1;
    }

    public int centerX() {
        return (minX + maxX) / 2;
    }

    public int centerZ() {
        return (minZ + maxZ) / 2;
    }

    public int area() {
        return width() * depth();
    }
}
