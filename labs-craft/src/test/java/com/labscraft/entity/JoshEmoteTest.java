package com.labscraft.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JoshEmoteTest {

    @Test
    void protocolEmoteIdsAllResolve() {
        // The exact EMOTE vocabulary from PROTOCOL-V2.md.
        for (String id : new String[] {"nod", "shake_head", "shrug", "point", "facepalm", "clap"}) {
            JoshEmote emote = JoshEmote.fromId(id);
            assertEquals(id, emote.id());
        }
        assertEquals(6, JoshEmote.values().length);
    }

    @Test
    void fromIdIsCaseInsensitiveAndTrims() {
        assertEquals(JoshEmote.SHAKE_HEAD, JoshEmote.fromId("  SHAKE_HEAD "));
        assertEquals(JoshEmote.NOD, JoshEmote.fromId("Nod"));
    }

    @Test
    void unknownIdsReturnNull() {
        assertNull(JoshEmote.fromId(null));
        assertNull(JoshEmote.fromId(""));
        assertNull(JoshEmote.fromId("dab"));
    }

    @Test
    void trackedValueRoundTrips() {
        for (JoshEmote emote : JoshEmote.values()) {
            assertEquals(emote, JoshEmote.fromTrackedValue(emote.trackedValue()));
            assertTrue(emote.trackedValue() > 0, "0 is reserved for no-emote");
        }
        assertNull(JoshEmote.fromTrackedValue(0));
        assertNull(JoshEmote.fromTrackedValue(-1));
        assertNull(JoshEmote.fromTrackedValue(99));
    }

    @Test
    void everyEmoteHasAVerbForTheActionLine() {
        for (JoshEmote emote : JoshEmote.values()) {
            assertTrue(emote.verb() != null && !emote.verb().isBlank());
        }
    }
}
