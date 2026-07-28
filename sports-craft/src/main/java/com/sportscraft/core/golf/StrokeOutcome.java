package com.sportscraft.core.golf;

/**
 * What the rules layer decided should happen to the ball, returned by
 * {@link GolfRound} so that the pure state machine never touches the world.
 * {@code GolfManager} is what actually moves or removes entities.
 */
public enum StrokeOutcome {

    /** Nothing to do — the ball is in play where it lies. */
    NONE,

    /** The ball is at rest and the player may take their next stroke. */
    READY_FOR_NEXT_STROKE,

    /** Penalty incurred; the ball must be replaced at its last resting place. */
    REPLACE_AT_LAST_REST,

    /** The ball is in the cup; the round on this hole is over. */
    HOLED_OUT,

    /** The event did not apply to this round's current phase and was ignored. */
    REJECTED
}
