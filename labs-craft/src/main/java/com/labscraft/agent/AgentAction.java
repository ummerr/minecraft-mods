package com.labscraft.agent;

/**
 * One action from a PROTOCOL-V2 /tick response, in pure-Java form (parsed but
 * not yet validated — validation is {@link ActionValidator}'s job, because the
 * LLM behind the wire is untrusted input).
 *
 * <p>Nullable fields are simply absent for action types that don't use them.
 * {@code targetPlayer} is true when {@code "target": "player"} was sent for
 * WALK_TO / LOOK_AT.</p>
 */
public record AgentAction(
        String type,
        String text,
        boolean targetPlayer,
        Double x,
        Double y,
        Double z,
        double speed,
        String emote,
        String item,
        int quantity,
        String objectiveId,
        int delayTicks) {

    public static AgentAction say(String text, int delayTicks) {
        return new AgentAction("SAY", text, false, null, null, null, 0, null, null, 0, null, delayTicks);
    }

    public boolean hasPosition() {
        return x != null && y != null && z != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(type == null ? "?" : type);
        if (text != null) {
            sb.append(" text=\"").append(text.length() > 40 ? text.substring(0, 40) + "…" : text).append('"');
        }
        if (targetPlayer) {
            sb.append(" target=player");
        }
        if (hasPosition()) {
            sb.append(" pos=(").append(x).append(',').append(y).append(',').append(z).append(')');
        }
        if (emote != null) {
            sb.append(" emote=").append(emote);
        }
        if (item != null) {
            sb.append(" item=").append(item).append(" x").append(quantity);
        }
        if (objectiveId != null) {
            sb.append(" objective=").append(objectiveId);
        }
        sb.append(" delay=").append(delayTicks);
        return sb.toString();
    }
}
