package com.labscraft.entity;

import java.util.Locale;

/**
 * The emotes Josh Woodward can perform, matching the PROTOCOL-V2 EMOTE action
 * (nod | shake_head | shrug | point | facepalm | clap). Pure Java — no
 * Minecraft imports — so it is unit-testable and shareable with the pure
 * dialogue layer.
 *
 * <p>Wire ids are the lowercase snake_case names ({@link #id()}); the tracked
 * data value synced to clients is {@link #trackedValue()} (0 is reserved for
 * "no emote").</p>
 */
public enum JoshEmote {
    NOD("nod", "nods"),
    SHAKE_HEAD("shake_head", "shakes his head"),
    SHRUG("shrug", "shrugs"),
    POINT("point", "points"),
    FACEPALM("facepalm", "facepalms"),
    CLAP("clap", "claps");

    private final String id;
    private final String verb;

    JoshEmote(String id, String verb) {
        this.id = id;
        this.verb = verb;
    }

    /** The wire/command name, e.g. {@code "shake_head"}. */
    public String id() {
        return id;
    }

    /** Third-person verb phrase for the complementary chat text, e.g. "shakes his head". */
    public String verb() {
        return verb;
    }

    /** Value stored in the entity's tracked data. 0 means "no emote". */
    public int trackedValue() {
        return ordinal() + 1;
    }

    /** Case-insensitive lookup by wire id; returns null when unknown. */
    public static JoshEmote fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (JoshEmote emote : values()) {
            if (emote.id.equals(normalized)) {
                return emote;
            }
        }
        return null;
    }

    /** Lookup by tracked data value; returns null for 0 or out-of-range values. */
    public static JoshEmote fromTrackedValue(int value) {
        if (value < 1 || value > values().length) {
            return null;
        }
        return values()[value - 1];
    }
}
