package com.sportscraft.core.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportscraft.core.physics.Vec3c;
import org.junit.jupiter.api.Test;

class LaunchSolverTest {

    private static final double EPS = 1e-9;

    @Test
    void yawZeroLaunchesTowardsPositiveZ() {
        Vec3c heading = LaunchSolver.headingFromYaw(0.0);
        assertEquals(0.0, heading.x(), EPS);
        assertEquals(1.0, heading.z(), EPS);
    }

    @Test
    void yawNinetyLaunchesTowardsNegativeX() {
        Vec3c heading = LaunchSolver.headingFromYaw(90.0);
        assertEquals(-1.0, heading.x(), EPS);
        assertEquals(0.0, heading.z(), EPS);
    }

    @Test
    void headingIsAlwaysAUnitVectorInThePlane() {
        for (double yaw = -360.0; yaw <= 360.0; yaw += 17.0) {
            Vec3c heading = LaunchSolver.headingFromYaw(yaw);
            assertEquals(1.0, heading.horizontalLength(), 1e-9, "yaw " + yaw);
            assertEquals(0.0, heading.y(), EPS, "heading must be horizontal");
        }
    }

    /**
     * The Wii Golf model, and the reason club choice matters: where the player
     * is looking sets direction only. Elevation is the club's business.
     */
    @Test
    void aimSetsDirectionAndTheClubSetsElevation() {
        Vec3c driver = LaunchSolver.solve(0.0, Clubs.DRIVER, 1.0);
        Vec3c iron = LaunchSolver.solve(0.0, Clubs.IRON, 1.0);

        assertTrue(iron.y() > driver.y(),
                "the more lofted club must launch the ball higher");
        assertTrue(driver.horizontalLength() > iron.horizontalLength(),
                "the flatter, faster club must drive the ball further forward");
    }

    @Test
    void aPutterNeverLaunchesTheBallUpwards() {
        for (double yaw = 0.0; yaw < 360.0; yaw += 30.0) {
            for (double power : new double[]{0.1, 0.5, 1.0}) {
                Vec3c putt = LaunchSolver.solve(yaw, Clubs.PUTTER, power);
                assertEquals(0.0, putt.y(), EPS, "a putt must stay flat at yaw " + yaw);
            }
        }
    }

    @Test
    void launchSpeedScalesWithPower() {
        double half = LaunchSolver.solve(0.0, Clubs.DRIVER, 0.5).length();
        double full = LaunchSolver.solve(0.0, Clubs.DRIVER, 1.0).length();
        assertEquals(full / 2.0, half, 1e-9, "power should scale launch speed linearly");
    }

    @Test
    void fullPowerHitsTheClubsTopSpeed() {
        for (ClubSpec club : Clubs.ALL) {
            assertEquals(club.maxSpeed(), LaunchSolver.solve(0.0, club, 1.0).length(), 1e-9,
                    club.id() + " should reach its rated speed at full power");
        }
    }

    @Test
    void powerIsClampedToSaneBounds() {
        Vec3c overcharged = LaunchSolver.solve(0.0, Clubs.DRIVER, 5.0);
        assertEquals(Clubs.DRIVER.maxSpeed(), overcharged.length(), 1e-9,
                "no swing may exceed the club's rated speed");

        Vec3c negative = LaunchSolver.solve(0.0, Clubs.DRIVER, -1.0);
        assertEquals(0.0, negative.length(), 1e-9, "negative power must not fire the ball backwards");
    }

    @Test
    void oppositeAimsSendTheBallOppositeWays() {
        Vec3c north = LaunchSolver.solve(180.0, Clubs.DRIVER, 1.0);
        Vec3c south = LaunchSolver.solve(0.0, Clubs.DRIVER, 1.0);
        assertEquals(-north.z(), south.z(), 1e-9);
        assertEquals(north.y(), south.y(), 1e-9, "aim must not change elevation");
    }
}
