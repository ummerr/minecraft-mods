package com.sportscraft.core.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SwingPowerTest {

    @Test
    void aTapStillMovesTheBall() {
        assertEquals(SwingPower.TAP_FLOOR, SwingPower.forTicks(0), 1e-9);
        assertTrue(SwingPower.forTicks(1) >= SwingPower.TAP_FLOOR,
                "an accidental tap must still count for something");
    }

    @Test
    void negativeHoldIsTreatedAsATap() {
        assertEquals(SwingPower.TAP_FLOOR, SwingPower.forTicks(-5), 1e-9);
    }

    @Test
    void powerRisesMonotonicallyWithCharge() {
        double previous = -1.0;
        for (int ticks = 0; ticks <= SwingPower.FULL_CHARGE_TICKS; ticks++) {
            double power = SwingPower.forTicks(ticks);
            assertTrue(power >= previous, "power dipped at tick " + ticks);
            previous = power;
        }
    }

    @Test
    void fullChargeIsExactlyFullPower() {
        assertEquals(1.0, SwingPower.forTicks(SwingPower.FULL_CHARGE_TICKS), 1e-9);
    }

    @Test
    void holdingLongerThanFullChargeDoesNotOvercharge() {
        assertEquals(1.0, SwingPower.forTicks(SwingPower.FULL_CHARGE_TICKS * 10), 1e-9);
        assertTrue(SwingPower.isFullyCharged(SwingPower.FULL_CHARGE_TICKS));
        assertFalse(SwingPower.isFullyCharged(SwingPower.FULL_CHARGE_TICKS - 1));
    }

    @Test
    void powerNeverEscapesItsRange() {
        for (int ticks = -10; ticks < 200; ticks++) {
            double power = SwingPower.forTicks(ticks);
            assertTrue(power >= SwingPower.TAP_FLOOR && power <= 1.0,
                    "power " + power + " out of range at " + ticks + " ticks");
        }
    }

    /**
     * The curve is quadratic like the vanilla bow, so the second half of the
     * hold is worth more than the first — that is what makes timing a swing
     * feel like a skill rather than a stopwatch.
     */
    @Test
    void theCurveAcceleratesTowardsFullCharge() {
        double firstHalf = SwingPower.forTicks(SwingPower.FULL_CHARGE_TICKS / 2)
                - SwingPower.forTicks(0);
        double secondHalf = SwingPower.forTicks(SwingPower.FULL_CHARGE_TICKS)
                - SwingPower.forTicks(SwingPower.FULL_CHARGE_TICKS / 2);
        assertTrue(secondHalf > firstHalf,
                "later charge ticks should be worth more than early ones");
    }

    @Test
    void meterShowsTheClubAndFillsUp() {
        String empty = SwingPower.meter(Clubs.DRIVER, 0, 10);
        String full = SwingPower.meter(Clubs.DRIVER, SwingPower.FULL_CHARGE_TICKS, 10);

        assertTrue(empty.startsWith("Driver"), "the meter should name the club: " + empty);
        assertTrue(full.contains("||||||||||"), "a full swing should fill the bar: " + full);
        assertTrue(full.contains("100%"), "a full swing should read 100%: " + full);
        assertTrue(empty.contains("-"), "an uncharged swing should show empty segments: " + empty);
    }
}
