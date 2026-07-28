package com.sportscraft.core.physics;

/**
 * How the ground under the ball behaves. Classified from the real world block
 * below the ball by {@code SurfaceClassifier}, so a ball behaves correctly on
 * player-built terrain too — no course metadata is consulted.
 *
 * @param restitution      fraction of vertical speed kept across a bounce
 * @param rollFriction     fraction of horizontal speed kept per rolling tick
 * @param bounceDrag       fraction of horizontal speed kept across a bounce
 */
public enum SurfaceType {

    /** Moss — the putting green. Very little bounce, long true roll. */
    GREEN(0.30, 0.955, 0.80),

    /** Grass — the fairway. The reference surface the tuning is locked to. */
    FAIRWAY(0.45, 0.940, 0.75),

    /** Coarse dirt — rough. Kills roll fast; punishes a wayward drive. */
    ROUGH(0.28, 0.870, 0.55),

    /** Sand — a bunker. Near-dead stop; the ball plugs. */
    SAND(0.12, 0.620, 0.30),

    /** Anything else the player might be hitting off (stone, planks, ...). */
    GENERIC(0.40, 0.920, 0.70),

    /** Water swallows the ball; the entity fires OUT_OF_BOUNDS instead of bouncing. */
    WATER(0.0, 0.0, 0.0);

    private final double restitution;
    private final double rollFriction;
    private final double bounceDrag;

    SurfaceType(double restitution, double rollFriction, double bounceDrag) {
        this.restitution = restitution;
        this.rollFriction = rollFriction;
        this.bounceDrag = bounceDrag;
    }

    public double restitution() {
        return restitution;
    }

    public double rollFriction() {
        return rollFriction;
    }

    public double bounceDrag() {
        return bounceDrag;
    }

    /** True for surfaces a ball can come to rest and be played from. */
    public boolean playable() {
        return this != WATER;
    }
}
