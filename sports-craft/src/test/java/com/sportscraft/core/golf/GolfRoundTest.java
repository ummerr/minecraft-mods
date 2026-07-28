package com.sportscraft.core.golf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sportscraft.core.physics.Vec3c;
import org.junit.jupiter.api.Test;

class GolfRoundTest {

    private static final GolfHoleDef HOLE =
            new GolfHoleDef(10, 64, 10, 10, 64, 70, 4, 60, 0L);

    /** A position that counts as in the cup. */
    private static Vec3c inCup() {
        return new Vec3c(HOLE.cupX() + 0.5, HOLE.cupY() + 0.5, HOLE.cupZ() + 0.5);
    }

    private static Vec3c onFairway() {
        return new Vec3c(10.5, 65.0, 40.5);
    }

    @Test
    void aRoundStartsAwaitingAStrokeAtTheTee() {
        GolfRound round = new GolfRound(HOLE);
        assertEquals(GolfRound.Phase.AWAITING_STROKE, round.phase());
        assertEquals(0, round.strokes());
        assertEquals(0, round.penalties());
        assertFalse(round.isComplete());
    }

    @Test
    void theHappyPathIsStrokeThenRestThenStroke() {
        GolfRound round = new GolfRound(HOLE);

        assertEquals(StrokeOutcome.NONE, round.onStrokeTaken());
        assertEquals(GolfRound.Phase.BALL_MOVING, round.phase());
        assertEquals(1, round.strokes());

        assertEquals(StrokeOutcome.READY_FOR_NEXT_STROKE, round.onBallAtRest(onFairway()));
        assertEquals(GolfRound.Phase.AWAITING_STROKE, round.phase());
        assertEquals(onFairway(), round.lastRestPosition());

        assertEquals(StrokeOutcome.NONE, round.onStrokeTaken());
        assertEquals(2, round.strokes());
    }

    @Test
    void restingInTheCupEndsTheHole() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();
        round.onBallAtRest(onFairway());
        round.onStrokeTaken();

        assertEquals(StrokeOutcome.HOLED_OUT, round.onBallAtRest(inCup()));
        assertEquals(GolfRound.Phase.HOLED_OUT, round.phase());
        assertTrue(round.isComplete());
        assertEquals(2, round.strokes());
    }

    @Test
    void aBallBesideTheCupIsNotHoled() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();

        // On the green, level with the rim, but a metre away.
        Vec3c beside = new Vec3c(HOLE.cupX() + 1.6, HOLE.cupY() + 1.0, HOLE.cupZ() + 0.5);
        assertEquals(StrokeOutcome.READY_FOR_NEXT_STROKE, round.onBallAtRest(beside));
        assertFalse(round.isComplete());
    }

    @Test
    void aBallOverTheCupButHighIsNotHoled() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();

        // Directly above the cup but resting on a block over it.
        Vec3c above = new Vec3c(HOLE.cupX() + 0.5, HOLE.cupY() + 4.0, HOLE.cupZ() + 0.5);
        assertEquals(StrokeOutcome.READY_FOR_NEXT_STROKE, round.onBallAtRest(above));
        assertFalse(round.isComplete());
    }

    @Test
    void waterCostsAPenaltyAndReplaysFromTheLastRest() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();
        round.onBallAtRest(onFairway());
        round.onStrokeTaken();

        assertEquals(StrokeOutcome.REPLACE_AT_LAST_REST, round.onBallLost());
        assertEquals(1, round.penalties());
        assertEquals(3, round.strokes(), "two strokes plus one penalty");
        assertEquals(GolfRound.Phase.AWAITING_STROKE, round.phase());
        assertEquals(onFairway(), round.lastRestPosition(),
                "the drop point should be where the ball last sat safely");
    }

    @Test
    void aFirstStrokeIntoWaterReplaysFromTheTee() {
        GolfRound round = new GolfRound(HOLE);
        Vec3c tee = round.lastRestPosition();
        round.onStrokeTaken();

        assertEquals(StrokeOutcome.REPLACE_AT_LAST_REST, round.onBallLost());
        assertEquals(tee, round.lastRestPosition());
    }

    @Test
    void youCannotSwingAtABallThatIsStillMoving() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();

        assertEquals(StrokeOutcome.REJECTED, round.onStrokeTaken());
        assertEquals(1, round.strokes(), "a rejected stroke must not be counted");
    }

    @Test
    void eventsAfterHolingOutAreRejected() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();
        round.onBallAtRest(inCup());

        assertEquals(StrokeOutcome.REJECTED, round.onStrokeTaken());
        assertEquals(StrokeOutcome.REJECTED, round.onBallAtRest(onFairway()));
        assertEquals(StrokeOutcome.REJECTED, round.onBallLost());
        assertEquals(1, round.strokes());
    }

    @Test
    void aBallComingToRestWithoutAStrokeIsRejected() {
        GolfRound round = new GolfRound(HOLE);
        assertEquals(StrokeOutcome.REJECTED, round.onBallAtRest(onFairway()));
    }

    @Test
    void oneStrokeIntoTheCupIsAnAce() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();
        round.onBallAtRest(inCup());

        assertEquals("Hole in One", round.scoreName());
        assertTrue(Scorecard.isBirdieOrBetter(round.strokes(), round.par()));
    }

    @Test
    void scoreNamesFollowGolf() {
        assertEquals("Hole in One", Scorecard.scoreName(1, 4));
        assertEquals("Eagle", Scorecard.scoreName(2, 4));
        assertEquals("Birdie", Scorecard.scoreName(3, 4));
        assertEquals("Par", Scorecard.scoreName(4, 4));
        assertEquals("Bogey", Scorecard.scoreName(5, 4));
        assertEquals("Double Bogey", Scorecard.scoreName(6, 4));
        assertEquals("Triple Bogey", Scorecard.scoreName(7, 4));
        assertEquals("+4", Scorecard.scoreName(8, 4));
        assertEquals("Albatross", Scorecard.scoreName(2, 5));
    }

    @Test
    void toParFormatsLikeALeaderboard() {
        assertEquals("E", Scorecard.formatToPar(4, 4));
        assertEquals("-1", Scorecard.formatToPar(3, 4));
        assertEquals("+2", Scorecard.formatToPar(6, 4));
    }

    @Test
    void aRestoredRoundKeepsItsProgress() {
        Vec3c where = onFairway();
        GolfRound restored = GolfRound.restore(
                HOLE, GolfRound.Phase.AWAITING_STROKE, 3, 1, where);

        assertEquals(3, restored.strokes());
        assertEquals(1, restored.penalties());
        assertEquals(where, restored.lastRestPosition());
        assertEquals(GolfRound.Phase.AWAITING_STROKE, restored.phase());
    }

    @Test
    void penaltiesAreIncludedInTheFinalScoreName() {
        GolfRound round = new GolfRound(HOLE);
        round.onStrokeTaken();
        round.onBallLost();          // 2 strokes (1 + penalty)
        round.onStrokeTaken();       // 3
        round.onBallAtRest(inCup());

        assertEquals(3, round.strokes());
        assertEquals("Birdie", round.scoreName(), "par 4 in 3 with a penalty is still a birdie");
    }
}
