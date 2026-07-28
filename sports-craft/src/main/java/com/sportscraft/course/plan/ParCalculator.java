package com.sportscraft.course.plan;

/**
 * Hole length to par. The thresholds are set against the measured club
 * distances in {@code BallPhysicsTest}: a driver runs out around 53 blocks, an
 * iron around 34, so a sub-50 hole is a one-shotter (par 3), 50–65 is drive
 * plus approach (par 4), and anything longer needs three (par 5). Each par then
 * allows two putts, as in real golf.
 */
public final class ParCalculator {

    public static final int PAR_4_MIN_LENGTH = 50;
    public static final int PAR_5_MIN_LENGTH = 66;

    private ParCalculator() {
    }

    /** @param length tee-to-cup distance in blocks */
    public static int parFor(int length) {
        if (length < PAR_4_MIN_LENGTH) {
            return 3;
        }
        if (length < PAR_5_MIN_LENGTH) {
            return 4;
        }
        return 5;
    }
}
