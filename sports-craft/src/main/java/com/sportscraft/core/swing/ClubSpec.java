package com.sportscraft.core.swing;

/**
 * A club's identity and its two numbers that matter: how high it launches the
 * ball and how hard.
 *
 * @param id            registry name, also the NBT/serialised form
 * @param displayName   shown in the actionbar power meter
 * @param loftDegrees   launch elevation above horizontal, independent of look pitch
 * @param maxSpeed      launch speed in blocks/tick at full charge
 */
public record ClubSpec(String id, String displayName, double loftDegrees, double maxSpeed) {

    public ClubSpec {
        if (loftDegrees < 0.0 || loftDegrees >= 90.0) {
            throw new IllegalArgumentException("loft must be in [0, 90): " + loftDegrees);
        }
        if (maxSpeed <= 0.0) {
            throw new IllegalArgumentException("maxSpeed must be positive: " + maxSpeed);
        }
    }

    /** A putter keeps the ball on the deck; used to skip the launch arc entirely. */
    public boolean isPutter() {
        return loftDegrees == 0.0;
    }
}
