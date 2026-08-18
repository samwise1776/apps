package util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small dependency-free JSON reader and writer.
 * Supports objects, arrays, strings, numbers, booleans and null.
 */
public final class Json {
    private Json() {}

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos < text.length()) {
            throw new IllegalArgumentException("Trailing content after JSON value at " + parser.pos);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object parsed) {
        return (Map<String, Object>) parsed;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object parsed) {
        return (List<Object>) parsed;
    }

    public static String string(Object node, String key, String fallback) {
        Object value = node instanceof Map ? ((Map<?, ?>) node).get(key) : null;
        return value instanceof String ? (String) value : fallback;
    }

    public static double number(Object node, String key, double fallback) {
        Object value = node instanceof Map ? ((Map<?, ?>) node).get(key) : null;
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    public static boolean bool(Object node, String key, boolean fallback) {
        Object value = node instanceof Map ? ((Map<?, ?>) node).get(key) : null;
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    public static String writePretty(Object value) {
        StringBuilder out = new StringBuilder();
        writePrettyValue(value, out, 0);
        return out.toString();
    }

    private static void writePrettyValue(Object value, StringBuilder out, int depth) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                out.append("{}");
                return;
            }
            out.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                indent(out, depth + 1);
                writeString(String.valueOf(entry.getKey()), out);
                out.append(": ");
                writePrettyValue(entry.getValue(), out, depth + 1);
                if (++i < map.size()) out.append(',');
                out.append('\n');
            }
            indent(out, depth);
            out.append('}');
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                indent(out, depth + 1);
                writePrettyValue(list.get(i), out, depth + 1);
                if (i < list.size() - 1) out.append(',');
                out.append('\n');
            }
            indent(out, depth);
            out.append(']');
        } else {
            writeValue(value, out);
        }
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(Math.max(0, depth)));
    }

    private static void writeValue(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            writeString((String) value, out);
        } else if (value instanceof Boolean || value instanceof Number) {
            out.append(value.toString());
        } else if (value instanceof Map) {
            out.append('{');
            int i = 0;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (i++ > 0) out.append(',');
                writeString(String.valueOf(entry.getKey()), out);
                out.append(':');
                writeValue(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List) {
            out.append('[');
            int i = 0;
            for (Object item : (List<?>) value) {
                if (i++ > 0) out.append(',');
                writeValue(item, out);
            }
            out.append(']');
        } else {
            writeString(String.valueOf(value), out);
        }
    }

    private static void writeString(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= text.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char c = text.charAt(pos);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default:
                    if (c == '-' || Character.isDigit(c)) return parseNumber();
                    throw new IllegalArgumentException("Unexpected character '" + c + "' at " + pos);
            }
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (pos < text.length() && text.charAt(pos) == '}') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos >= text.length() || text.charAt(pos) != ':') {
                    throw new IllegalArgumentException("Expected ':' at " + pos);
                }
                pos++;
                result.put(key, parseValue());
                skipWhitespace();
                if (pos >= text.length()) {
                    throw new IllegalArgumentException("Unterminated object");
                }
                char c = text.charAt(pos);
                pos++;
                if (c == '}') return result;
                if (c != ',') throw new IllegalArgumentException("Expected ',' or '}' at " + (pos - 1));
            }
        }

        private List<Object> parseArray() {
            List<Object> result = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (pos < text.length() && text.charAt(pos) == ']') {
                pos++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (pos >= text.length()) {
                    throw new IllegalArgumentException("Unterminated array");
                }
                char c = text.charAt(pos);
                pos++;
                if (c == ']') return result;
                if (c != ',') throw new IllegalArgumentException("Expected ',' or ']' at " + (pos - 1));
            }
        }

        private String parseString() {
            if (pos >= text.length() || text.charAt(pos) != '"') {
                throw new IllegalArgumentException("Expected string at " + pos);
            }
            pos++;
            StringBuilder out = new StringBuilder();
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '"') {
                    pos++;
                    return out.toString();
                }
                if (c == '\\') {
                    pos++;
                    if (pos >= text.length()) break;
                    char esc = text.charAt(pos);
                    switch (esc) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case '/': out.append('/'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'b': out.append('\b'); break;
                        case 'f': out.append('\f'); break;
                        case 'u':
                            if (pos + 4 < text.length()) {
                                out.append((char) Integer.parseInt(text.substring(pos + 1, pos + 5), 16));
                                pos += 4;
                            }
                            break;
                        default: out.append(esc);
                    }
                } else {
                    out.append(c);
                }
                pos++;
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private Number parseNumber() {
            int start = pos;
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || Character.isDigit(c)) {
                    pos++;
                } else {
                    break;
                }
            }
            String raw = text.substring(start, pos);
            try {
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                    return Double.parseDouble(raw);
                }
                return Long.parseLong(raw);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number '" + raw + "'");
            }
        }

        private void expect(String literal) {
            if (!text.startsWith(literal, pos)) {
                throw new IllegalArgumentException("Expected '" + literal + "' at " + pos);
            }
            pos += literal.length();
        }

        private void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }
    }
}
