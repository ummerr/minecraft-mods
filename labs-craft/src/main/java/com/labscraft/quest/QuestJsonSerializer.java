package com.labscraft.quest;

/**
 * Produces the exact {@code quest} JSON block from PROTOCOL-V2.md for the
 * agent bridge:
 *
 * <pre>
 * {"current_stage":"FLOW_INTRO",
 *  "objectives":[{"id":"mine_tpu_ore","description":"Mine 3 TPU Ore","done":false,"progress":1,"goal":3}],
 *  "seconds_in_stage":130}
 * </pre>
 *
 * Hand-rolled (no JSON library) so the pure layer stays dependency-free and
 * the shape is locked by tests. Pure Java.
 */
public final class QuestJsonSerializer {

    private QuestJsonSerializer() {
    }

    public static String toJson(PlayerQuestState state) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"current_stage\":\"").append(escape(state.stage().name())).append("\",\"objectives\":[");
        boolean first = true;
        for (Objective objective : state.objectives()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"id\":\"").append(escape(objective.id()))
                    .append("\",\"description\":\"").append(escape(objective.description()))
                    .append("\",\"done\":").append(objective.isDone())
                    .append(",\"progress\":").append(objective.progress())
                    .append(",\"goal\":").append(objective.goal())
                    .append('}');
        }
        sb.append("],\"seconds_in_stage\":").append(state.secondsInStage()).append('}');
        return sb.toString();
    }

    static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
