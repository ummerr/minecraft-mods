package com.labscraft.agent;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROTOCOL-V2 hard rule 2: the LLM is untrusted input; every action is
 * validated before execution and bad ones are dropped individually.
 * Coordinates are sanity-bounded relative to Josh at (0, 64, 0).
 */
class ActionValidatorTest {

    private static final double JX = 0;
    private static final double JY = 64;
    private static final double JZ = 0;

    private static Optional<String> reject(AgentAction action) {
        return ActionValidator.reject(action, JX, JY, JZ);
    }

    private static AgentAction action(String type, String text, boolean targetPlayer,
            Double x, Double y, Double z, double speed, String emote, String item,
            int quantity, String objectiveId, int delayTicks) {
        return new AgentAction(type, text, targetPlayer, x, y, z, speed, emote, item,
                quantity, objectiveId, delayTicks);
    }

    // ---- type -------------------------------------------------------------

    @Test
    void unknownTypeIsRejected() {
        assertTrue(reject(action("EXPLODE", null, false, null, null, null, 0, null, null, 0, null, 0)).isPresent());
        assertTrue(reject(action(null, null, false, null, null, null, 0, null, null, 0, null, 0)).isPresent());
        assertTrue(ActionValidator.reject(null, JX, JY, JZ).isPresent());
    }

    @Test
    void waitAndAdvanceQuestNeedNoFields() {
        assertFalse(reject(action("WAIT", null, false, null, null, null, 0, null, null, 0, null, 0)).isPresent());
        assertFalse(reject(action("ADVANCE_QUEST", null, false, null, null, null, 0, null, null, 0, null, 10)).isPresent());
    }

    // ---- SAY --------------------------------------------------------------

    @Test
    void sayWithinLimitPasses() {
        assertFalse(reject(AgentAction.say("Working as intended.", 0)).isPresent());
        assertFalse(reject(AgentAction.say("x".repeat(200), 0)).isPresent());
    }

    @Test
    void sayOver200CharsIsDropped() {
        assertTrue(reject(AgentAction.say("x".repeat(201), 0)).isPresent());
    }

    @Test
    void sayWithoutTextIsDropped() {
        assertTrue(reject(AgentAction.say(null, 0)).isPresent());
        assertTrue(reject(AgentAction.say("   ", 0)).isPresent());
    }

    // ---- delay ------------------------------------------------------------

    @Test
    void delayOutOfRangeIsDropped() {
        assertTrue(reject(AgentAction.say("hi", -1)).isPresent());
        assertTrue(reject(AgentAction.say("hi", ActionValidator.MAX_DELAY_TICKS + 1)).isPresent());
        assertFalse(reject(AgentAction.say("hi", ActionValidator.MAX_DELAY_TICKS)).isPresent());
    }

    // ---- WALK_TO / LOOK_AT ------------------------------------------------

    @Test
    void walkToPlayerWithSaneSpeedPasses() {
        assertFalse(reject(action("WALK_TO", null, true, null, null, null, 0.6, null, null, 0, null, 0)).isPresent());
    }

    @Test
    void walkToAbsurdCoordinatesIsDropped() {
        assertTrue(reject(action("WALK_TO", null, false, 10_000.0, 64.0, 0.0, 0.5,
                null, null, 0, null, 0)).isPresent());
        assertTrue(reject(action("WALK_TO", null, false, Double.NaN, 64.0, 0.0, 0.5,
                null, null, 0, null, 0)).isPresent());
    }

    @Test
    void walkToNearbyCoordinatesPasses() {
        assertFalse(reject(action("WALK_TO", null, false, 10.0, 65.0, -10.0, 0.5,
                null, null, 0, null, 0)).isPresent());
    }

    @Test
    void walkToBadSpeedIsDropped() {
        assertTrue(reject(action("WALK_TO", null, true, null, null, null, 5.0, null, null, 0, null, 0)).isPresent());
        assertTrue(reject(action("WALK_TO", null, true, null, null, null, Double.NaN, null, null, 0, null, 0)).isPresent());
        assertTrue(reject(action("WALK_TO", null, true, null, null, null, 0.0, null, null, 0, null, 0)).isPresent());
    }

    @Test
    void walkToWithNeitherTargetNorPositionIsDropped() {
        assertTrue(reject(action("WALK_TO", null, false, null, null, null, 0.5, null, null, 0, null, 0)).isPresent());
    }

    @Test
    void lookAtPlayerOrNearbyPositionPasses() {
        assertFalse(reject(action("LOOK_AT", null, true, null, null, null, 0, null, null, 0, null, 0)).isPresent());
        assertFalse(reject(action("LOOK_AT", null, false, 5.0, 64.0, 5.0, 0, null, null, 0, null, 0)).isPresent());
        assertTrue(reject(action("LOOK_AT", null, false, 5000.0, 64.0, 5.0, 0, null, null, 0, null, 0)).isPresent());
    }

    // ---- EMOTE ------------------------------------------------------------

    @Test
    void protocolEmotesPassUnknownEmotesDrop() {
        for (String id : new String[] {"nod", "shake_head", "shrug", "point", "facepalm", "clap"}) {
            assertFalse(reject(action("EMOTE", null, false, null, null, null, 0, id, null, 0, null, 0)).isPresent(), id);
        }
        assertTrue(reject(action("EMOTE", null, false, null, null, null, 0, "dab", null, 0, null, 0)).isPresent());
        assertTrue(reject(action("EMOTE", null, false, null, null, null, 0, null, null, 0, null, 0)).isPresent());
    }

    // ---- GIVE_ITEM --------------------------------------------------------

    @Test
    void allowlistedItemWithSaneQuantityPasses() {
        assertFalse(reject(action("GIVE_ITEM", null, false, null, null, null, 0,
                null, "labscraft:tpu", 5, null, 0)).isPresent());
    }

    @Test
    void nonAllowlistedItemIsDropped() {
        assertTrue(reject(action("GIVE_ITEM", null, false, null, null, null, 0,
                null, "minecraft:netherite_block", 1, null, 0)).isPresent());
        assertTrue(reject(action("GIVE_ITEM", null, false, null, null, null, 0,
                null, null, 1, null, 0)).isPresent());
    }

    @Test
    void quantityOutOfOneToSixtyFourIsDropped() {
        assertTrue(reject(action("GIVE_ITEM", null, false, null, null, null, 0,
                null, "labscraft:tpu", 0, null, 0)).isPresent());
        assertTrue(reject(action("GIVE_ITEM", null, false, null, null, null, 0,
                null, "labscraft:tpu", 65, null, 0)).isPresent());
        assertFalse(reject(action("GIVE_ITEM", null, false, null, null, null, 0,
                null, "labscraft:tpu", 64, null, 0)).isPresent());
    }

    // ---- COMPLETE_OBJECTIVE -----------------------------------------------

    @Test
    void completeObjectiveNeedsAnId() {
        assertFalse(reject(action("COMPLETE_OBJECTIVE", null, false, null, null, null, 0,
                null, null, 0, "mine_tpu_ore", 0)).isPresent());
        assertTrue(reject(action("COMPLETE_OBJECTIVE", null, false, null, null, null, 0,
                null, null, 0, " ", 0)).isPresent());
        assertTrue(reject(action("COMPLETE_OBJECTIVE", null, false, null, null, null, 0,
                null, null, 0, null, 0)).isPresent());
    }
}
