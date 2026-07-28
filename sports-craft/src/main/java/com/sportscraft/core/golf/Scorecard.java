package com.sportscraft.core.golf;

/** Naming for a finished hole, in golf's own vocabulary. */
public final class Scorecard {

    private Scorecard() {
    }

    /**
     * @param strokes total strokes including penalties
     * @param par     the hole's par
     */
    public static String scoreName(int strokes, int par) {
        if (strokes == 1) {
            return "Hole in One";
        }
        int relative = strokes - par;
        return switch (relative) {
            case -3 -> "Albatross";
            case -2 -> "Eagle";
            case -1 -> "Birdie";
            case 0 -> "Par";
            case 1 -> "Bogey";
            case 2 -> "Double Bogey";
            case 3 -> "Triple Bogey";
            default -> relative < -3 ? "Condor" : "+" + relative;
        };
    }

    /** Strokes relative to par, e.g. -1 for a birdie. */
    public static int toPar(int strokes, int par) {
        return strokes - par;
    }

    /** Birdie or better — worth a celebratory sound. */
    public static boolean isBirdieOrBetter(int strokes, int par) {
        return strokes < par;
    }

    /** Renders a signed score the way a leaderboard would: E, -2, +3. */
    public static String formatToPar(int strokes, int par) {
        int relative = toPar(strokes, par);
        if (relative == 0) {
            return "E";
        }
        return relative > 0 ? "+" + relative : String.valueOf(relative);
    }
}
