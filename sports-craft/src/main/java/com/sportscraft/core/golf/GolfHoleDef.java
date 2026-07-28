package com.sportscraft.core.golf;

/**
 * A placed, playable hole. Pure data — world coordinates as plain ints rather
 * than a {@code BlockPos} — so the round state machine and its tests never need
 * Minecraft on the classpath.
 *
 * @param teeX  tee pad centre, the position a round starts from
 * @param cupY  the cup's floor; hole-out is judged against the rim just above it
 * @param par   strokes the hole is designed to take
 * @param length tee-to-cup distance in blocks
 * @param seed  the seed that generated it, so it can be rebuilt identically
 */
public record GolfHoleDef(int teeX, int teeY, int teeZ,
                          int cupX, int cupY, int cupZ,
                          int par, int length, long seed) {

    /** How close a resting ball must be to the cup, horizontally, to be holed. */
    public static final double HOLE_OUT_RADIUS = 0.5;

    /** A stable identifier derived from the cup position. */
    public String id() {
        return cupX + "_" + cupY + "_" + cupZ;
    }

    /** Horizontal distance from the cup, ignoring height. */
    public double horizontalDistanceToCup(double x, double z) {
        double dx = x - (cupX + 0.5);
        double dz = z - (cupZ + 0.5);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Whether a ball resting at this position counts as holed. The vertical
     * check matters: a ball sitting on the green beside the cup is level with
     * the rim, while a holed ball has dropped into the pit below it.
     */
    public boolean isHoledOut(double x, double y, double z) {
        return horizontalDistanceToCup(x, z) <= HOLE_OUT_RADIUS && y <= cupY + 1.0;
    }
}
