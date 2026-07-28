package com.sportscraft.core.physics;

/**
 * Runs {@link BallPhysics} over idealised flat ground, with no world to consult.
 *
 * <p>Two jobs. First, it is how the tuning is pinned down: {@code BallPhysicsTest}
 * asserts per-club carry and total distance against this, so changing gravity or
 * a restitution constant fails a test instead of silently making golf unfun.
 * Second, it is the shape phase 2's NPC needs — a tennis opponent has to ask
 * "where will this ball land?" and that is exactly {@link #simulate}.</p>
 *
 * <p>Being flat-ground-only is a deliberate limit: real play runs through the
 * entity against real blocks. This is the tuning reference, not the game.</p>
 */
public final class BallSimulator {

    /** Safety bound so a pathological spec can never hang a test or the server. */
    public static final int MAX_TICKS = 2000;

    private BallSimulator() {
    }

    /**
     * @param carry         horizontal blocks travelled before the first ground contact
     * @param totalDistance horizontal blocks travelled in total, including run-out
     * @param apexHeight    greatest height above the launch point
     * @param bounces       number of ground contacts that reflected the ball
     * @param ticks         ticks elapsed until rest (or {@link #MAX_TICKS})
     * @param cameToRest    false if the ball was still moving when the tick budget ran out
     */
    public record Flight(double carry, double totalDistance, double apexHeight,
                         int bounces, int ticks, boolean cameToRest) {
    }

    /**
     * Simulates a launch from the origin over flat ground at y=0 until the ball
     * comes to rest.
     *
     * @param launchVelocity initial velocity in blocks/tick
     * @param launchHeight   height above the ground the ball starts at
     */
    public static Flight simulate(Vec3c launchVelocity, double launchHeight,
                                  BallSpec spec, SurfaceType surface) {
        Vec3c pos = new Vec3c(0.0, launchHeight, 0.0);
        Vec3c velocity = launchVelocity;
        final Vec3c start = pos;

        double carry = -1.0;
        double apex = 0.0;
        int bounces = 0;
        int tick = 0;

        int restTicks = 0;
        boolean atRest = false;

        while (tick < MAX_TICKS && !atRest) {
            velocity = BallPhysics.applyFlight(velocity, spec);

            // What Entity.move() would be handed — and what bounce() must reflect,
            // since move() zeroes any component that collides.
            Vec3c preMove = velocity;
            Vec3c next = pos.add(preMove);
            boolean onGround = false;

            if (next.y() <= 0.0) {
                next = next.withY(0.0);
                onGround = true;
                velocity = BallPhysics.bounceVertical(preMove, spec, surface);
                if (velocity.y() > 0.0) {
                    bounces++;
                }
                if (carry < 0.0) {
                    carry = start.horizontalDistanceTo(next);
                }
            }

            if (onGround && velocity.y() == 0.0) {
                velocity = BallPhysics.roll(velocity, spec, surface);
            }

            pos = next;
            apex = Math.max(apex, pos.y() - launchHeight);

            restTicks = BallPhysics.nextRestTicks(restTicks, velocity, spec, onGround);
            atRest = BallPhysics.isAtRest(restTicks, spec);
            tick++;
        }

        if (carry < 0.0) {
            carry = start.horizontalDistanceTo(pos);
        }
        return new Flight(carry, start.horizontalDistanceTo(pos), apex, bounces, tick, atRest);
    }
}
