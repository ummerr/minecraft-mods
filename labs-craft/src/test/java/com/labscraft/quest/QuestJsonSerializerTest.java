package com.labscraft.quest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Locks the wire shape of the PROTOCOL-V2.md `quest` block. */
class QuestJsonSerializerTest {

    @Test
    void matchesProtocolShapeForFlowIntroWithPartialProgress() {
        FakeClock clock = new FakeClock(0);
        PlayerQuestState state = new PlayerQuestState(clock);
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO, resets stage timer
        state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        clock.advanceSeconds(130);

        String expected = "{\"current_stage\":\"FLOW_INTRO\",\"objectives\":["
                + "{\"id\":\"mine_tpu_ore\",\"description\":\"Mine 3 TPU Ore\",\"done\":false,\"progress\":1,\"goal\":3},"
                + "{\"id\":\"craft_tpu\",\"description\":\"Craft 2 TPU\",\"done\":false,\"progress\":0,\"goal\":2}"
                + "],\"seconds_in_stage\":130}";
        assertEquals(expected, QuestJsonSerializer.toJson(state));
    }

    @Test
    void doneObjectivesSerializeWithDoneTrueAndProgressAtGoal() {
        FakeClock clock = new FakeClock(0);
        PlayerQuestState state = new PlayerQuestState(clock);
        state.handleEvent(QuestEvent.talkedToJosh()); // -> FLOW_INTRO
        for (int i = 0; i < 3; i++) {
            state.handleEvent(QuestEvent.blockMined(QuestLine.TPU_ORE));
        }

        String json = QuestJsonSerializer.toJson(state);
        assertTrue(json.contains(
                "{\"id\":\"mine_tpu_ore\",\"description\":\"Mine 3 TPU Ore\",\"done\":true,\"progress\":3,\"goal\":3}"),
                json);
    }

    @Test
    void completedStageSerializesWithEmptyObjectivesArray() {
        FakeClock clock = new FakeClock(0);
        PlayerQuestState state = new PlayerQuestState(clock);
        state.forceSetStage(QuestStage.COMPLETED);
        clock.advanceSeconds(5);

        assertEquals("{\"current_stage\":\"COMPLETED\",\"objectives\":[],\"seconds_in_stage\":5}",
                QuestJsonSerializer.toJson(state));
    }

    @Test
    void stringEscapingIsJsonSafe() {
        assertEquals("say \\\"hi\\\"", QuestJsonSerializer.escape("say \"hi\""));
        assertEquals("back\\\\slash", QuestJsonSerializer.escape("back\\slash"));
        assertEquals("line\\nbreak\\ttab", QuestJsonSerializer.escape("line\nbreak\ttab"));
        assertEquals("ctrl\\u0001char", QuestJsonSerializer.escape("ctrlchar"));
    }
}
