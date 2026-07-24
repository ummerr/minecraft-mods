package com.labscraft.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal recursive-descent JSON parser + string escaper. Pure Java, zero
 * dependencies, mirroring the hand-rolled approach of
 * {@code QuestJsonSerializer} so the agent layer's pure core stays
 * unit-testable on the unremapped test classpath.
 *
 * <p>Parse results use plain types: {@link Map}&lt;String,Object&gt;,
 * {@link List}&lt;Object&gt;, {@link String}, {@link Double},
 * {@link Boolean}, or {@code null}.</p>
 */
public final class JsonLite {

    /** Thrown on malformed input. The bridge treats it as "malformed JSON". */
    public static class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }

    private final String src;
    private int pos;

    private JsonLite(String src) {
        this.src = src;
    }

    /** Parses a complete JSON document; trailing non-whitespace is an error. */
    public static Object parse(String text) {
        if (text == null) {
            throw new JsonException("null input");
        }
        JsonLite p = new JsonLite(text);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (p.pos != text.length()) {
            throw new JsonException("trailing characters at " + p.pos);
        }
        return value;
    }

    // ------------------------------------------------------------------
    // Typed accessors for Map results (lenient: wrong type -> fallback)
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        return value instanceof List ? (List<Object>) value : null;
    }

    public static String str(Map<String, Object> obj, String key, String fallback) {
        Object v = obj == null ? null : obj.get(key);
        return v instanceof String s ? s : fallback;
    }

    public static double num(Map<String, Object> obj, String key, double fallback) {
        Object v = obj == null ? null : obj.get(key);
        return v instanceof Double d ? d : fallback;
    }

    public static boolean bool(Map<String, Object> obj, String key, boolean fallback) {
        Object v = obj == null ? null : obj.get(key);
        return v instanceof Boolean b ? b : fallback;
    }

    /** JSON string escaping (same rules as QuestJsonSerializer). */
    public static String escape(String value) {
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

    // ------------------------------------------------------------------
    // Parser internals
    // ------------------------------------------------------------------

    private Object parseValue() {
        if (pos >= src.length()) {
            throw new JsonException("unexpected end of input");
        }
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, parseValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return map;
            }
            if (c != ',') {
                throw new JsonException("expected ',' or '}' at " + (pos - 1));
            }
        }
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(parseValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return list;
            }
            if (c != ',') {
                throw new JsonException("expected ',' or ']' at " + (pos - 1));
            }
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= src.length()) {
                throw new JsonException("unterminated string");
            }
            char c = src.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= src.length()) {
                throw new JsonException("unterminated escape");
            }
            char e = src.charAt(pos++);
            switch (e) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/' -> sb.append('/');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (pos + 4 > src.length()) {
                        throw new JsonException("bad unicode escape");
                    }
                    sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw new JsonException("bad escape '\\" + e + "'");
            }
        }
    }

    private Boolean parseBoolean() {
        if (src.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (src.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        throw new JsonException("bad literal at " + pos);
    }

    private Object parseNull() {
        if (src.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw new JsonException("bad literal at " + pos);
    }

    private Double parseNumber() {
        int start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw new JsonException("unexpected character at " + pos);
        }
        try {
            return Double.parseDouble(src.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new JsonException("bad number at " + start);
        }
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= src.length()) {
            throw new JsonException("unexpected end of input");
        }
        return src.charAt(pos);
    }

    private char next() {
        if (pos >= src.length()) {
            throw new JsonException("unexpected end of input");
        }
        return src.charAt(pos++);
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new JsonException("expected '" + c + "' at " + pos);
        }
        pos++;
    }
}
