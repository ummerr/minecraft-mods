package com.sportscraft.core.physics;

/**
 * The ball stepper: pure static functions over {@link Vec3c}, with no notion of
 * a world, an entity or a tick loop. {@code SportsBallEntity} owns the
 * collision facts (did we hit the ground? which axis got zeroed? what block is
 * below?) and calls into here for every decision about what the velocity
 * becomes next.
 *
 * <p>Keeping the arithmetic here is what makes the tuning testable: carry
 * distance per club, energy loss per bounce and rest behaviour are all
 * assertions in {@code BallPhysicsTest} rather than things you can only find
 * out by swinging a club in-game.</p>
 *
 * <h2>Order of operations in one airborne tick</h2>
 * <ol>
 *   <li>{@link #applyFlight} — gravity, then drag</li>
 *   <li>the entity moves and reports collisions</li>
 *   <li>{@link #bounceVertical} / {@link #bounceHorizontal} if it hit something</li>
 *   <li>{@link #roll} if it is grounded and no longer bouncing</li>
 *   <li>{@link #nextRestTicks} to advance the rest hysteresis</li>
 * </ol>
 */
public final class BallPhysics {

    private BallPhysics() {
    }

    /**
     * One tick of free flight: gravity first, then drag on the whole vector.
     * Applying drag after gravity (rather than before) is what gives the flight
     * a terminal speed instead of letting gravity accumulate unchecked.
     */
    public static Vec3c applyFlight(Vec3c velocity, BallSpec spec) {
        Vec3c afterGravity = new Vec3c(velocity.x(), velocity.y() - spec.gravity(), velocity.z());
        return afterGravity.multiply(spec.drag());
    }

    /**
     * Reflects the vertical component after a floor/ceiling hit.
     *
     * <p>Must be handed the velocity captured <em>before</em> the move, because
     * {@code Entity.move()} zeroes the component that collided — reading the
     * post-move velocity would always bounce off zero and the ball would die on
     * first contact.</p>
     *
     * <p>Below {@link BallSpec#bounceCutoff} the bounce is suppressed entirely:
     * that is the transition from bouncing to rolling, and without it a ball
     * jitters forever on micro-bounces.</p>
     */
    public static Vec3c bounceVertical(Vec3c preMoveVelocity, BallSpec spec, SurfaceType surface) {
        double restitution = surface.restitution() * spec.restitutionScale();
        double reflected = -preMoveVelocity.y() * restitution;

        if (Math.abs(reflected) < spec.bounceCutoff()) {
            // Not a bounce — this is a ball rolling along the ground, which
            // "collides" with it on every single tick. Charging bounce drag here
            // would compound once per tick and kill a putt in a few blocks, so
            // settle onto the surface and let roll() own the horizontal decay.
            return preMoveVelocity.withY(0.0);
        }
        // A real impact: squashing into the turf costs horizontal speed too.
        return preMoveVelocity.multiplyHorizontal(surface.bounceDrag()).withY(reflected);
    }

    /**
     * Reflects whichever horizontal axes were blocked — a wall, a bunker lip, or
     * (in phase 2) the tennis net. The caller derives the blocked axes by
     * comparing pre-move velocity against post-move velocity.
     */
    public static Vec3c bounceHorizontal(Vec3c preMoveVelocity, boolean xBlocked, boolean zBlocked,
                                         BallSpec spec, SurfaceType surface) {
        double restitution = surface.restitution() * spec.restitutionScale();
        double x = xBlocked ? -preMoveVelocity.x() * restitution : preMoveVelocity.x();
        double z = zBlocked ? -preMoveVelocity.z() * restitution : preMoveVelocity.z();
        return new Vec3c(x, preMoveVelocity.y(), z);
    }

    /**
     * Friction for a grounded, non-bouncing ball. This is where surface choice
     * actually shows up in play: the same putt runs out on the green and dies
     * in a bunker.
     */
    public static Vec3c roll(Vec3c velocity, BallSpec spec, SurfaceType surface) {
        Vec3c slowed = velocity.multiplyHorizontal(surface.rollFriction());
        if (slowed.horizontalLength() < spec.restSpeedThreshold()) {
            return new Vec3c(0.0, slowed.y(), 0.0);
        }
        return slowed;
    }

    /** True while the ball is slow enough to be a candidate for coming to rest. */
    public static boolean isSlow(Vec3c velocity, BallSpec spec) {
        return velocity.length() < spec.restSpeedThreshold();
    }

    /**
     * Advances the consecutive-slow-tick counter. Rest requires
     * {@link BallSpec#restTicksRequired} slow ticks in a row, so a ball at the
     * apex of a bounce — momentarily slow but very much still in play — is not
     * mistaken for a ball that has stopped.
     */
    public static int nextRestTicks(int currentRestTicks, Vec3c velocity, BallSpec spec, boolean onGround) {
        if (!onGround || !isSlow(velocity, spec)) {
            return 0;
        }
        return currentRestTicks + 1;
    }

    public static boolean isAtRest(int restTicks, BallSpec spec) {
        return restTicks >= spec.restTicksRequired();
    }

    /**
     * Whether a ball previously declared at rest should start simulating again
     * (a player nudged it, or the block under it vanished). The wake threshold
     * sits above the rest threshold on purpose — that gap is the hysteresis
     * that stops a settled ball flickering between rest and motion.
     */
    public static boolean shouldWake(Vec3c velocity, BallSpec spec, boolean supported) {
        return !supported || velocity.length() > spec.wakeSpeed();
    }
}
