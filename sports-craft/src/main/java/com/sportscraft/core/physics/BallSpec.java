package com.sportscraft.core.physics;

/**
 * Per-sport ball tuning. Everything the stepper needs to know about the ball
 * itself; everything it needs to know about the ground lives in
 * {@link SurfaceType}.
 *
 * <p>{@link #TENNIS} is deliberately present and unused in v1 — it is the seam
 * that lets phase 2 reuse {@link BallPhysics} verbatim.</p>
 *
 * @param id                 stable identifier, also the NBT value
 * @param gravity            downward acceleration in blocks/tick²
 * @param drag               fraction of velocity kept per airborne tick
 * @param restitutionScale   global multiplier over {@link SurfaceType#restitution()}
 * @param bounceCutoff       below this vertical speed a bounce becomes a roll
 * @param restSpeedThreshold below this speed the ball is a candidate for rest
 * @param restTicksRequired  consecutive slow ticks before the ball is declared at rest
 * @param wakeSpeed          speed that pulls a resting ball back into simulation
 */
public record BallSpec(String id, double gravity, double drag, double restitutionScale,
                       double bounceCutoff, double restSpeedThreshold,
                       int restTicksRequired, double wakeSpeed) {

    /**
     * Golf ball. gravity 0.04, drag 0.99 and FAIRWAY restitution 0.45 are the
     * plan's starting constants; {@code BallPhysicsTest}'s per-club carry ranges
     * are what keep them honest.
     */
    public static final BallSpec GOLF = new BallSpec(
            "golf", 0.04, 0.99, 1.0, 0.09, 0.020, 5, 0.05);

    /** Phase 2. Lighter, bouncier, more drag — never referenced by v1 code. */
    public static final BallSpec TENNIS = new BallSpec(
            "tennis", 0.05, 0.98, 1.55, 0.07, 0.015, 4, 0.05);

    /** Looks up a spec by its {@link #id}, defaulting to {@link #GOLF}. */
    public static BallSpec byId(String id) {
        return TENNIS.id().equals(id) ? TENNIS : GOLF;
    }
}
