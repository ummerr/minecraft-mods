package com.sportscraft.core.swing;

/**
 * Charge-time to swing-power, using the vanilla bow's quadratic curve so the
 * feel is already familiar in the hand.
 *
 * <p>Two departures from the bow. Full charge takes {@link #FULL_CHARGE_TICKS}
 * rather than 20, because a golf swing that maxes out in one second gives the
 * player no room to modulate; and the result is floored at {@link #TAP_FLOOR}
 * so an accidental tap still moves the ball — a stroke that does nothing but
 * still counts against the scorecard is pure frustration.</p>
 */
public final class SwingPower {

    /** Ticks of holding right-click for a 100% swing (1.5 seconds). */
    public static final int FULL_CHARGE_TICKS = 30;

    /** The weakest swing a release can produce. */
    public static final double TAP_FLOOR = 0.15;

    private SwingPower() {
    }

    /**
     * @param ticksHeld how long the use button was held
     * @return swing power in [{@link #TAP_FLOOR}, 1.0]
     */
    public static double forTicks(int ticksHeld) {
        if (ticksHeld <= 0) {
            return TAP_FLOOR;
        }
        double charge = Math.min(1.0, ticksHeld / (double) FULL_CHARGE_TICKS);
        double curved = (charge * charge + charge * 2.0) / 3.0;
        return TAP_FLOOR + curved * (1.0 - TAP_FLOOR);
    }

    /** True once holding longer stops helping — the cue to show a full meter. */
    public static boolean isFullyCharged(int ticksHeld) {
        return ticksHeld >= FULL_CHARGE_TICKS;
    }

    /**
     * Renders the actionbar power meter, e.g. {@code Driver  [|||||||---] 72%}.
     * Pure string building so the wording is testable without a client.
     */
    public static String meter(ClubSpec club, int ticksHeld, int segments) {
        double power = forTicks(ticksHeld);
        int filled = (int) Math.round(power * segments);
        StringBuilder bar = new StringBuilder(segments + 2);
        bar.append('[');
        for (int i = 0; i < segments; i++) {
            bar.append(i < filled ? '|' : '-');
        }
        bar.append(']');
        return club.displayName() + "  " + bar + " " + (int) Math.round(power * 100) + "%";
    }
}
