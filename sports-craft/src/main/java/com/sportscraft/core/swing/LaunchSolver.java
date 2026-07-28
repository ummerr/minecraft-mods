package com.sportscraft.core.swing;

import com.sportscraft.core.physics.Vec3c;

/**
 * Turns "which way am I looking, which club am I holding, how hard did I swing"
 * into a launch velocity.
 *
 * <p>The load-bearing decision: <strong>yaw comes from the player's look,
 * elevation comes from the club's loft.</strong> Look pitch is ignored. This is
 * the Wii Golf model — the player aims, the club decides trajectory — and it is
 * what stops a putter being aimed at the sky and makes club selection a real
 * choice rather than a cosmetic one.</p>
 */
public final class LaunchSolver {

    private LaunchSolver() {
    }

    /**
     * @param yawDegrees Minecraft yaw: 0 faces +Z (south), 90 faces -X (west)
     * @param club       supplies loft and top speed
     * @param power      swing power in [0, 1], normally from {@link SwingPower}
     */
    public static Vec3c solve(double yawDegrees, ClubSpec club, double power) {
        double clamped = Math.max(0.0, Math.min(1.0, power));
        double speed = club.maxSpeed() * clamped;

        double loftRadians = Math.toRadians(club.loftDegrees());
        double horizontalSpeed = speed * Math.cos(loftRadians);
        double verticalSpeed = speed * Math.sin(loftRadians);

        Vec3c heading = headingFromYaw(yawDegrees);
        return new Vec3c(heading.x() * horizontalSpeed, verticalSpeed, heading.z() * horizontalSpeed);
    }

    /**
     * The unit horizontal direction a player at {@code yawDegrees} is facing,
     * in Minecraft's convention (yaw 0 → +Z, yaw 90 → -X).
     */
    public static Vec3c headingFromYaw(double yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        return new Vec3c(-Math.sin(radians), 0.0, Math.cos(radians));
    }
}
