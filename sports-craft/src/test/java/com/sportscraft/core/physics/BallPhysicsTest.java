package com.sportscraft.core.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportscraft.core.swing.ClubSpec;
import com.sportscraft.core.swing.Clubs;
import com.sportscraft.core.swing.LaunchSolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The tuning lock. These ranges are deliberately wider than the measured values
 * but far tighter than "any number at all": nudging gravity, drag or a surface
 * constant enough to change how golf actually plays will fail a test here rather
 * than only showing up as a bad round in-game.
 */
class BallPhysicsTest {

    /** Balls rest on the ground, so shots launch from just above it. */
    private static final double GROUND_LAUNCH = 0.2;

    private static BallSimulator.Flight fullSwing(ClubSpec club, SurfaceType surface) {
        return BallSimulator.simulate(
                LaunchSolver.solve(0.0, club, 1.0), GROUND_LAUNCH, BallSpec.GOLF, surface);
    }

    @Nested
    @DisplayName("flight")
    class Flight {

        @Test
        void gravityPullsTheBallDown() {
            Vec3c after = BallPhysics.applyFlight(Vec3c.ZERO, BallSpec.GOLF);
            assertTrue(after.y() < 0.0, "a ball with no velocity should start falling");
        }

        @Test
        void dragBleedsOffHorizontalSpeed() {
            Vec3c launched = new Vec3c(1.0, 0.0, 0.0);
            Vec3c after = BallPhysics.applyFlight(launched, BallSpec.GOLF);
            assertTrue(after.x() < 1.0, "drag should reduce horizontal speed");
            assertTrue(after.x() > 0.9, "drag should be gentle, not a wall: " + after.x());
        }

        @Test
        void aBallThrownUpComesBackDown() {
            Vec3c velocity = new Vec3c(0.0, 1.0, 0.0);
            for (int tick = 0; tick < 60 && velocity.y() > 0.0; tick++) {
                velocity = BallPhysics.applyFlight(velocity, BallSpec.GOLF);
            }
            assertTrue(velocity.y() <= 0.0, "upward velocity must be exhausted by gravity");
        }

        @Test
        void everyClubEventuallyComesToRest() {
            for (ClubSpec club : Clubs.ALL) {
                for (SurfaceType surface : SurfaceType.values()) {
                    if (!surface.playable()) {
                        continue;
                    }
                    BallSimulator.Flight flight = fullSwing(club, surface);
                    assertTrue(flight.cameToRest(),
                            club.id() + " on " + surface + " never came to rest");
                    assertTrue(flight.ticks() < 200,
                            club.id() + " on " + surface + " took " + flight.ticks()
                                    + " ticks to settle — too long to wait between strokes");
                }
            }
        }
    }

    @Nested
    @DisplayName("bounce")
    class Bounce {

        @Test
        void aBounceLosesEnergy() {
            Vec3c falling = new Vec3c(0.0, -0.8, 0.0);
            Vec3c bounced = BallPhysics.bounceVertical(falling, BallSpec.GOLF, SurfaceType.FAIRWAY);
            assertTrue(bounced.y() > 0.0, "a fast falling ball should bounce back up");
            assertTrue(bounced.y() < 0.8, "a bounce must not return all the energy");
        }

        @Test
        void successiveBouncesGetSmallerAndEventuallyStop() {
            double speed = 1.0;
            double previous = Double.MAX_VALUE;
            int bounces = 0;

            while (speed > 0.0) {
                Vec3c bounced = BallPhysics.bounceVertical(
                        new Vec3c(0.0, -speed, 0.0), BallSpec.GOLF, SurfaceType.FAIRWAY);
                assertTrue(bounced.y() < previous,
                        "bounce " + bounces + " should be weaker than the one before it");
                previous = bounced.y();
                speed = bounced.y();
                bounces++;
                assertTrue(bounces < 50, "the ball should stop bouncing, not bounce forever");
            }
            assertTrue(bounces > 1, "a ball dropped at speed should bounce more than once");
        }

        @Test
        void aSlowImpactStopsBouncingAndStartsRolling() {
            Vec3c crawling = new Vec3c(0.5, -0.05, 0.0);
            Vec3c settled = BallPhysics.bounceVertical(crawling, BallSpec.GOLF, SurfaceType.FAIRWAY);
            assertEquals(0.0, settled.y(), 1e-9, "below the cutoff the ball should settle, not bounce");
        }

        /**
         * Regression: a rolling ball collides with the ground every single tick.
         * If bounce drag were charged on those contacts it would compound once
         * per tick and a full-power putt would die in about two blocks.
         */
        @Test
        void rollingContactDoesNotChargeBounceDrag() {
            Vec3c rolling = new Vec3c(0.4, -0.04, 0.0);
            Vec3c settled = BallPhysics.bounceVertical(rolling, BallSpec.GOLF, SurfaceType.GREEN);
            assertEquals(0.4, settled.x(), 1e-9,
                    "a non-bouncing ground contact must leave horizontal speed to roll()");
        }

        @Test
        void restitutionOrderingMatchesTheSurfaces() {
            assertTrue(SurfaceType.SAND.restitution() < SurfaceType.ROUGH.restitution());
            assertTrue(SurfaceType.ROUGH.restitution() < SurfaceType.GREEN.restitution());
            assertTrue(SurfaceType.GREEN.restitution() < SurfaceType.FAIRWAY.restitution());
        }

        @Test
        void horizontalBounceReflectsOnlyTheBlockedAxis() {
            Vec3c travelling = new Vec3c(0.5, 0.1, 0.5);
            Vec3c offAWall = BallPhysics.bounceHorizontal(
                    travelling, true, false, BallSpec.GOLF, SurfaceType.GENERIC);
            assertTrue(offAWall.x() < 0.0, "the blocked axis should reverse");
            assertEquals(0.5, offAWall.z(), 1e-9, "the free axis should be untouched");
            assertEquals(0.1, offAWall.y(), 1e-9, "a wall should not change vertical speed");
        }
    }

    @Nested
    @DisplayName("roll")
    class Roll {

        @Test
        void frictionOrderingMatchesTheSurfaces() {
            assertTrue(SurfaceType.SAND.rollFriction() < SurfaceType.ROUGH.rollFriction());
            assertTrue(SurfaceType.ROUGH.rollFriction() < SurfaceType.GENERIC.rollFriction());
            assertTrue(SurfaceType.GENERIC.rollFriction() < SurfaceType.FAIRWAY.rollFriction());
            assertTrue(SurfaceType.FAIRWAY.rollFriction() < SurfaceType.GREEN.rollFriction());
        }

        @Test
        void slowerSurfacesGiveShorterTotalDistance() {
            double green = fullSwing(Clubs.DRIVER, SurfaceType.GREEN).totalDistance();
            double fairway = fullSwing(Clubs.DRIVER, SurfaceType.FAIRWAY).totalDistance();
            double rough = fullSwing(Clubs.DRIVER, SurfaceType.ROUGH).totalDistance();
            double sand = fullSwing(Clubs.DRIVER, SurfaceType.SAND).totalDistance();

            assertTrue(sand < rough, "sand should stop a ball faster than rough");
            assertTrue(rough < fairway, "rough should stop a ball faster than fairway");
            assertTrue(fairway < green, "a green should run out further than fairway");
        }

        @Test
        void aCrawlingBallIsSnappedToAStop() {
            Vec3c crawling = new Vec3c(0.001, 0.0, 0.001);
            Vec3c stopped = BallPhysics.roll(crawling, BallSpec.GOLF, SurfaceType.FAIRWAY);
            assertEquals(0.0, stopped.horizontalLength(), 1e-9);
        }
    }

    @Nested
    @DisplayName("rest hysteresis")
    class Rest {

        @Test
        void restNeedsSeveralConsecutiveSlowTicks() {
            int restTicks = 0;
            for (int i = 0; i < BallSpec.GOLF.restTicksRequired() - 1; i++) {
                restTicks = BallPhysics.nextRestTicks(restTicks, Vec3c.ZERO, BallSpec.GOLF, true);
                assertFalse(BallPhysics.isAtRest(restTicks, BallSpec.GOLF),
                        "should not be at rest after only " + (i + 1) + " slow ticks");
            }
            restTicks = BallPhysics.nextRestTicks(restTicks, Vec3c.ZERO, BallSpec.GOLF, true);
            assertTrue(BallPhysics.isAtRest(restTicks, BallSpec.GOLF));
        }

        @Test
        void oneFastTickResetsTheCounter() {
            int restTicks = BallPhysics.nextRestTicks(3, new Vec3c(0.5, 0, 0), BallSpec.GOLF, true);
            assertEquals(0, restTicks, "a moving ball is not settling");
        }

        @Test
        void aBallInTheAirIsNeverAtRest() {
            int restTicks = BallPhysics.nextRestTicks(4, Vec3c.ZERO, BallSpec.GOLF, false);
            assertEquals(0, restTicks, "the apex of a bounce is slow but still in play");
        }

        @Test
        void losingSupportWakesARestingBall() {
            assertTrue(BallPhysics.shouldWake(Vec3c.ZERO, BallSpec.GOLF, false),
                    "a ball whose ground was removed must start falling again");
            assertFalse(BallPhysics.shouldWake(Vec3c.ZERO, BallSpec.GOLF, true),
                    "a supported, still ball should stay asleep");
        }
    }

    /**
     * The numbers that decide whether golf is fun. Ranges bracket the measured
     * behaviour on fairway at full power; see the plan's hole lengths (40–80
     * blocks) for why these matter — a driver has to be able to reach a par 3.
     */
    @Nested
    @DisplayName("per-club carry distance")
    class CarryDistance {

        @Test
        void driverIsTheLongClub() {
            BallSimulator.Flight flight = fullSwing(Clubs.DRIVER, SurfaceType.FAIRWAY);
            assertInRange("driver carry", flight.carry(), 30.0, 40.0);
            assertInRange("driver total", flight.totalDistance(), 46.0, 60.0);
            assertInRange("driver apex", flight.apexHeight(), 2.0, 5.0);
        }

        @Test
        void ironIsShorterAndHigher() {
            BallSimulator.Flight iron = fullSwing(Clubs.IRON, SurfaceType.FAIRWAY);
            BallSimulator.Flight driver = fullSwing(Clubs.DRIVER, SurfaceType.FAIRWAY);

            assertInRange("iron carry", iron.carry(), 19.0, 28.0);
            assertInRange("iron total", iron.totalDistance(), 29.0, 41.0);
            assertTrue(iron.totalDistance() < driver.totalDistance(),
                    "the iron must not out-drive the driver");
            assertTrue(iron.apexHeight() > driver.apexHeight(),
                    "more loft must mean a higher ball flight");
        }

        @Test
        void putterRollsAndNeverFlies() {
            BallSimulator.Flight putt = fullSwing(Clubs.PUTTER, SurfaceType.GREEN);
            assertEquals(0.0, putt.apexHeight(), 1e-9, "a putt must stay on the deck");
            assertEquals(0, putt.bounces(), "a putt must not bounce");
            assertInRange("putt on the green", putt.totalDistance(), 6.0, 12.0);
        }

        @Test
        void aDriverCanReachTheShortestHole() {
            double reach = fullSwing(Clubs.DRIVER, SurfaceType.FAIRWAY).totalDistance();
            assertTrue(reach >= 40.0,
                    "a 40-block par 3 should be drivable, but the driver only goes " + reach);
        }

        @Test
        void harderSwingsGoFurther() {
            double previous = 0.0;
            for (double power : new double[]{0.25, 0.5, 0.75, 1.0}) {
                BallSimulator.Flight flight = BallSimulator.simulate(
                        LaunchSolver.solve(0.0, Clubs.DRIVER, power),
                        GROUND_LAUNCH, BallSpec.GOLF, SurfaceType.FAIRWAY);
                assertTrue(flight.totalDistance() > previous,
                        "power " + power + " should beat the weaker swing");
                previous = flight.totalDistance();
            }
        }
    }

    private static void assertInRange(String what, double actual, double min, double max) {
        assertTrue(actual >= min && actual <= max,
                what + " was " + String.format("%.2f", actual)
                        + ", outside the expected [" + min + ", " + max + "] — "
                        + "physics tuning changed, confirm golf still plays well before widening this");
    }
}
