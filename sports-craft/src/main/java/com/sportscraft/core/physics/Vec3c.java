package com.sportscraft.core.physics;

/**
 * Immutable 3D vector for the pure physics layer.
 *
 * <p>The pure layer must not import {@code net.minecraft.util.math.Vec3d}, so
 * this is the currency every {@code core} function speaks. The Minecraft-facing
 * entity converts at the boundary and nowhere else.</p>
 *
 * <p>Units are Minecraft's: blocks, and blocks per tick.</p>
 */
public record Vec3c(double x, double y, double z) {

    public static final Vec3c ZERO = new Vec3c(0, 0, 0);

    public Vec3c add(Vec3c other) {
        return new Vec3c(x + other.x, y + other.y, z + other.z);
    }

    public Vec3c subtract(Vec3c other) {
        return new Vec3c(x - other.x, y - other.y, z - other.z);
    }

    public Vec3c multiply(double scalar) {
        return new Vec3c(x * scalar, y * scalar, z * scalar);
    }

    /** Scales the x and z components, leaving y untouched. */
    public Vec3c multiplyHorizontal(double scalar) {
        return new Vec3c(x * scalar, y, z * scalar);
    }

    public Vec3c withY(double newY) {
        return new Vec3c(x, newY, z);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    /** Speed in the xz plane — the number that decides rolling and rest. */
    public double horizontalLength() {
        return Math.sqrt(x * x + z * z);
    }

    /** Distance in the xz plane, ignoring height. Used for carry/hole-out checks. */
    public double horizontalDistanceTo(Vec3c other) {
        double dx = x - other.x;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double distanceTo(Vec3c other) {
        return subtract(other).length();
    }
}
