package com.labscraft.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses a PROTOCOL-V2 /tick response body into {@link AgentAction}s. Pure
 * Java. Malformed JSON or {@code protocol_version != 2} yields
 * {@code protocolOk == false} — the bridge treats that as "fall back to static
 * dialogue" (hard rule 1); it never throws into the caller.
 */
public final class ActionParser {

    public static final int PROTOCOL_VERSION = 2;

    /**
     * @param protocolOk false when the body was malformed or the version wrong
     * @param actions parsed (not yet validated) actions; empty when not ok
     * @param trigger the server's {@code debug.trigger}, or "" when absent
     * @param provider the server's {@code debug.provider}, or "" when absent
     */
    public record Result(boolean protocolOk, List<AgentAction> actions, String trigger, String provider) {

        public static Result malformed() {
            return new Result(false, List.of(), "", "");
        }
    }

    private ActionParser() {
    }

    public static Result parse(String body) {
        Map<String, Object> root;
        try {
            root = JsonLite.asObject(JsonLite.parse(body));
        } catch (RuntimeException e) {
            return Result.malformed();
        }
        if (root == null || (int) JsonLite.num(root, "protocol_version", -1) != PROTOCOL_VERSION) {
            return Result.malformed();
        }

        List<AgentAction> actions = new ArrayList<>();
        List<Object> rawActions = JsonLite.asArray(root.get("actions"));
        if (rawActions != null) {
            for (Object raw : rawActions) {
                Map<String, Object> obj = JsonLite.asObject(raw);
                if (obj != null) {
                    actions.add(toAction(obj));
                }
            }
        }

        Map<String, Object> debug = JsonLite.asObject(root.get("debug"));
        return new Result(true, actions,
                JsonLite.str(debug, "trigger", ""),
                JsonLite.str(debug, "provider", ""));
    }

    private static AgentAction toAction(Map<String, Object> obj) {
        Map<String, Object> position = JsonLite.asObject(obj.get("position"));
        Double x = null;
        Double y = null;
        Double z = null;
        if (position != null
                && position.get("x") instanceof Double px
                && position.get("y") instanceof Double py
                && position.get("z") instanceof Double pz) {
            x = px;
            y = py;
            z = pz;
        }
        return new AgentAction(
                JsonLite.str(obj, "type", null),
                JsonLite.str(obj, "text", null),
                "player".equals(JsonLite.str(obj, "target", null)),
                x, y, z,
                JsonLite.num(obj, "speed", Double.NaN),
                JsonLite.str(obj, "emote", null),
                JsonLite.str(obj, "item", null),
                (int) JsonLite.num(obj, "quantity", 0),
                JsonLite.str(obj, "objective_id", null),
                (int) JsonLite.num(obj, "delay_ticks", 0));
    }
}
