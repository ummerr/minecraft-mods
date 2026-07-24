package com.labscraft.agent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLiteTest {

    @Test
    void parsesNestedObjectsArraysAndScalars() {
        Object parsed = JsonLite.parse(
                "{\"a\": 1, \"b\": [true, false, null, \"x\"], \"c\": {\"d\": -2.5e1}}");
        Map<String, Object> root = JsonLite.asObject(parsed);
        assertNotNull(root);
        assertEquals(1.0, root.get("a"));
        List<Object> b = JsonLite.asArray(root.get("b"));
        assertEquals(List.of(true, false), b.subList(0, 2));
        assertNull(b.get(2));
        assertEquals("x", b.get(3));
        assertEquals(-25.0, JsonLite.num(JsonLite.asObject(root.get("c")), "d", 0));
    }

    @Test
    void parsesStringEscapes() {
        Map<String, Object> root = JsonLite.asObject(
                JsonLite.parse("{\"s\": \"a\\\"b\\\\c\\n\\t\\u0041\"}"));
        assertEquals("a\"b\\c\n\tA", root.get("s"));
    }

    @Test
    void parsesEmptyContainers() {
        assertEquals(Map.of(), JsonLite.parse("{}"));
        assertEquals(List.of(), JsonLite.parse("[]"));
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(JsonLite.JsonException.class, () -> JsonLite.parse("{\"a\": }"));
        assertThrows(JsonLite.JsonException.class, () -> JsonLite.parse("{\"a\": 1"));
        assertThrows(JsonLite.JsonException.class, () -> JsonLite.parse("not json"));
        assertThrows(JsonLite.JsonException.class, () -> JsonLite.parse("{} trailing"));
        assertThrows(JsonLite.JsonException.class, () -> JsonLite.parse(""));
    }

    @Test
    void typedAccessorsAreLenient() {
        Map<String, Object> root = JsonLite.asObject(JsonLite.parse("{\"n\": \"notanumber\"}"));
        assertEquals(7.0, JsonLite.num(root, "n", 7.0));
        assertEquals("fb", JsonLite.str(root, "missing", "fb"));
        assertTrue(JsonLite.bool(root, "missing", true));
        assertNull(JsonLite.asObject("a string"));
    }

    @Test
    void escapeRoundTripsThroughParse() {
        String nasty = "he said \"hi\"\n\ttab\\slash";
        Map<String, Object> root = JsonLite.asObject(
                JsonLite.parse("{\"v\":\"" + JsonLite.escape(nasty) + "\"}"));
        assertEquals(nasty, root.get("v"));
    }
}
