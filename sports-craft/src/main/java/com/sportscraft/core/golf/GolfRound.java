package com.sportscraft.core.golf;

import com.sportscraft.core.physics.Vec3c;

/**
 * One player's round on one hole: a pure state machine, in the spirit of
 * {@code PlayerQuestState}. It counts strokes, applies penalties and decides
 * when the ball is holed, without ever touching an entity or a world — the
 * caller passes in what happened and gets back a {@link StrokeOutcome} saying
 * what should happen to the ball.
 *
 * <pre>
 *   AWAITING_STROKE --stroke--> BALL_MOVING --at rest--> AWAITING_STROKE
 *                                          --in cup---> HOLED_OUT
 *                                          --water----> AWAITING_STROKE (+1)
 * </pre>
 */
public final class GolfRound {

    public enum Phase {
        AWAITING_STROKE,
        BALL_MOVING,
        HOLED_OUT
    }

    private final GolfHoleDef hole;

    private Phase phase = Phase.AWAITING_STROKE;
    private int strokes;
    private int penalties;
    private Vec3c lastRestPosition;

    public GolfRound(GolfHoleDef hole) {
        this.hole = hole;
        this.lastRestPosition = new Vec3c(hole.teeX() + 0.5, hole.teeY(), hole.teeZ() + 0.5);
    }

    public GolfHoleDef hole() {
        return hole;
    }

    public Phase phase() {
        return phase;
    }

    /** Strokes played, including penalty strokes. */
    public int strokes() {
        return strokes;
    }

    public int penalties() {
        return penalties;
    }

    public Vec3c lastRestPosition() {
        return lastRestPosition;
    }

    public boolean isComplete() {
        return phase == Phase.HOLED_OUT;
    }

    /**
     * The player swung and connected. Illegal while the ball is still moving —
     * you cannot hit a ball twice in the air — and illegal once holed out.
     */
    public StrokeOutcome onStrokeTaken() {
        if (phase != Phase.AWAITING_STROKE) {
            return StrokeOutcome.REJECTED;
        }
        strokes++;
        phase = Phase.BALL_MOVING;
        return StrokeOutcome.NONE;
    }

    /**
     * The ball stopped. If it stopped in the cup the hole is done; otherwise the
     * spot becomes the new drop point for any later penalty.
     */
    public StrokeOutcome onBallAtRest(Vec3c position) {
        if (phase != Phase.BALL_MOVING) {
            return StrokeOutcome.REJECTED;
        }

        if (hole.isHoledOut(position.x(), position.y(), position.z())) {
            phase = Phase.HOLED_OUT;
            return StrokeOutcome.HOLED_OUT;
        }

        lastRestPosition = position;
        phase = Phase.AWAITING_STROKE;
        return StrokeOutcome.READY_FOR_NEXT_STROKE;
    }

    /**
     * Ball in the water: one penalty stroke, and replay from where it last sat
     * safely. Same treatment for leaving the world entirely.
     */
    public StrokeOutcome onBallLost() {
        if (phase != Phase.BALL_MOVING) {
            return StrokeOutcome.REJECTED;
        }
        penalties++;
        strokes++;
        phase = Phase.AWAITING_STROKE;
        return StrokeOutcome.REPLACE_AT_LAST_REST;
    }

    public int par() {
        return hole.par();
    }

    public String scoreName() {
        return Scorecard.scoreName(strokes, hole.par());
    }

    public String toParString() {
        return Scorecard.formatToPar(strokes, hole.par());
    }

    /** Restores a round from persistence without replaying its history. */
    public static GolfRound restore(GolfHoleDef hole, Phase phase, int strokes, int penalties,
                                    Vec3c lastRestPosition) {
        GolfRound round = new GolfRound(hole);
        round.phase = phase;
        round.strokes = strokes;
        round.penalties = penalties;
        if (lastRestPosition != null) {
            round.lastRestPosition = lastRestPosition;
        }
        return round;
    }
}
