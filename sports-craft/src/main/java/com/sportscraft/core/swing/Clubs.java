package com.sportscraft.core.swing;

import java.util.List;

/**
 * The three-club set. Loft is what makes the choice interesting: because
 * elevation comes from the club and not from where the player is looking
 * (the Wii Golf model), picking the driver on the green is a real mistake
 * rather than something the player can aim their way out of.
 */
public final class Clubs {

    /** Long and low: the tee shot. */
    public static final ClubSpec DRIVER = new ClubSpec("driver", "Driver", 20.0, 1.6);

    /** High and short: approach shots and escaping the rough. */
    public static final ClubSpec IRON = new ClubSpec("iron", "Iron", 35.0, 1.1);

    /** Flat and gentle: rolls, never flies. */
    public static final ClubSpec PUTTER = new ClubSpec("putter", "Putter", 0.0, 0.45);

    public static final List<ClubSpec> ALL = List.of(DRIVER, IRON, PUTTER);

    private Clubs() {
    }

    /** Looks up a club by {@link ClubSpec#id()}, or null if there is no such club. */
    public static ClubSpec byId(String id) {
        for (ClubSpec club : ALL) {
            if (club.id().equals(id)) {
                return club;
            }
        }
        return null;
    }
}
