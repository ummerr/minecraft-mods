package com.labscraft.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionParserTest {

    @Test
    void parsesAFullProtocolV2Response() {
        String body = """
                {"protocol_version": 2,
                 "actions": [
                   {"type": "LOOK_AT", "target": "player", "delay_ticks": 0},
                   {"type": "SAY", "text": "Not in my OKRs.", "delay_ticks": 5},
                   {"type": "WALK_TO", "position": {"x": 1.5, "y": 64.0, "z": -3.0}, "speed": 0.7, "delay_ticks": 10},
                   {"type": "EMOTE", "emote": "shrug", "delay_ticks": 2},
                   {"type": "GIVE_ITEM", "item": "labscraft:tpu", "quantity": 3, "delay_ticks": 0},
                   {"type": "COMPLETE_OBJECTIVE", "objective_id": "meet_josh", "delay_ticks": 0}
                 ],
                 "debug": {"trigger": "chat_message", "reasoning": "r", "latency_ms": 12, "provider": "static"}}
                """;
        ActionParser.Result result = ActionParser.parse(body);
        assertTrue(result.protocolOk());
        assertEquals("chat_message", result.trigger());
        assertEquals("static", result.provider());

        List<AgentAction> actions = result.actions();
        assertEquals(6, actions.size());
        assertTrue(actions.get(0).targetPlayer());
        assertEquals("SAY", actions.get(1).type());
        assertEquals("Not in my OKRs.", actions.get(1).text());
        assertEquals(5, actions.get(1).delayTicks());
        assertEquals(1.5, actions.get(2).x());
        assertEquals(-3.0, actions.get(2).z());
        assertEquals(0.7, actions.get(2).speed());
        assertEquals("shrug", actions.get(3).emote());
        assertEquals("labscraft:tpu", actions.get(4).item());
        assertEquals(3, actions.get(4).quantity());
        assertEquals("meet_josh", actions.get(5).objectiveId());
    }

    @Test
    void emptyActionsIsTheNormalCase() {
        ActionParser.Result result = ActionParser.parse("{\"protocol_version\":2,\"actions\":[]}");
        assertTrue(result.protocolOk());
        assertTrue(result.actions().isEmpty());
    }

    @Test
    void wrongProtocolVersionIsNotOk() {
        assertFalse(ActionParser.parse("{\"protocol_version\":1,\"actions\":[]}").protocolOk());
        assertFalse(ActionParser.parse("{\"protocol_version\":3,\"actions\":[]}").protocolOk());
        assertFalse(ActionParser.parse("{\"actions\":[]}").protocolOk());
    }

    @Test
    void malformedBodiesAreNotOkAndNeverThrow() {
        assertFalse(ActionParser.parse("").protocolOk());
        assertFalse(ActionParser.parse("not json at all").protocolOk());
        assertFalse(ActionParser.parse("{\"protocol_version\":2,\"actions\":").protocolOk());
        assertFalse(ActionParser.parse("[]").protocolOk());
        assertFalse(ActionParser.parse(null).protocolOk());
    }

    @Test
    void nonObjectActionEntriesAreSkipped() {
        ActionParser.Result result = ActionParser.parse(
                "{\"protocol_version\":2,\"actions\":[42,\"x\",{\"type\":\"WAIT\",\"delay_ticks\":0}]}");
        assertTrue(result.protocolOk());
        assertEquals(1, result.actions().size());
        assertEquals("WAIT", result.actions().get(0).type());
    }

    @Test
    void partialPositionIsTreatedAsAbsent() {
        ActionParser.Result result = ActionParser.parse(
                "{\"protocol_version\":2,\"actions\":[{\"type\":\"WALK_TO\",\"position\":{\"x\":1.0},"
                        + "\"speed\":0.5,\"delay_ticks\":0}]}");
        assertTrue(result.protocolOk());
        assertFalse(result.actions().get(0).hasPosition());
        assertNull(result.actions().get(0).x());
    }

    @Test
    void missingDebugBlockYieldsEmptyStrings() {
        ActionParser.Result result = ActionParser.parse("{\"protocol_version\":2,\"actions\":[]}");
        assertEquals("", result.trigger());
        assertEquals("", result.provider());
    }
}
