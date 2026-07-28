package com.sportscraft.course.plan;

/**
 * One square metre of golf hole, in the pure planning layer. {@code CoursePlacer}
 * maps these to real block states; nothing here knows what a block is.
 */
public enum SurfaceCell {

    /** The tee pad you drive from. */
    TEE(true),

    /** Mown grass — the intended route. */
    FAIRWAY(true),

    /** The putting surface. */
    GREEN(true),

    /** The cup itself, always inside the green. */
    CUP(true),

    /** Unmown edges; playable but punishing. */
    ROUGH(true),

    /** A bunker. Playable, but it will cost you. */
    SAND(true),

    /** A water hazard. Not playable — ball in, penalty stroke. */
    WATER(false);

    private final boolean playable;

    SurfaceCell(boolean playable) {
        this.playable = playable;
    }

    /** Whether a ball can come to rest here and be played from where it lies. */
    public boolean playable() {
        return playable;
    }
}
