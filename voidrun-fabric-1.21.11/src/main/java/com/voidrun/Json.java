package com.voidrun;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON reader and writer for the flat config object. Values are strings, numbers or
 * booleans. Nested objects are skipped, which is enough for a config file and keeps the mod
 * free of a parser dependency.
 */
public final class Json {
    private Json() {
    }

    public static Map<String, Object> parseObject(String text) {
        Reader r = new Reader(text);
        r.skipSpace();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (r.peek() != '{') {
            throw new IllegalArgumentException("Expected an object");
        }
        r.pos++;
        r.skipSpace();
        if (r.peek() == '}') {
            r.pos++;
            return map;
        }
        while (true) {
            r.skipSpace();
            String key = r.readString();
            r.skipSpace();
            if (r.next() != ':') {
                throw new IllegalArgumentException("Expected ':'");
            }
            r.skipSpace();
            Object value = r.readValue();
            map.put(key, value);
            r.skipSpace();
            char c = r.next();
            if (c == '}') {
                break;
            }
            if (c != ',') {
                throw new IllegalArgumentException("Expected ',' or '}'");
            }
        }
        return map;
    }

    public static String escape(String in) {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(in.length() + 8);
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format(java.util.Locale.ROOT, "\\u%04x",
                                Integer.valueOf(c)));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static final class Reader {
        private final String text;
        private int pos;

        Reader(String text) {
            this.text = text == null ? "" : text;
        }

        void skipSpace() {
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        char peek() {
            return pos < text.length() ? text.charAt(pos) : '\0';
        }

        char next() {
            return pos < text.length() ? text.charAt(pos++) : '\0';
        }

        String readString() {
            if (next() != '"') {
                throw new IllegalArgumentException("Expected a string");
            }
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '\0') {
                    throw new IllegalArgumentException("Unterminated string");
                }
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    char e = next();
                    if (e == 'n') {
                        sb.append('\n');
                    } else if (e == 't') {
                        sb.append('\t');
                    } else if (e == 'r') {
                        sb.append('\r');
                    } else if (e == 'b') {
                        sb.append('\b');
                    } else if (e == 'f') {
                        sb.append('\f');
                    } else if (e == 'u') {
                        String hex = text.substring(pos, Math.min(text.length(), pos + 4));
                        pos += hex.length();
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (RuntimeException ex) {
                            sb.append('?');
                        }
                    } else {
                        sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Object readValue() {
            char c = peek();
            if (c == '"') {
                return readString();
            }
            if (c == '{') {
                int depth = 0;
                while (pos < text.length()) {
                    char ch = text.charAt(pos++);
                    if (ch == '{') {
                        depth++;
                    } else if (ch == '}') {
                        depth--;
                        if (depth == 0) {
                            break;
                        }
                    }
                }
                return Boolean.valueOf(false);
            }
            if (text.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            if (text.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            int start = pos;
            while (pos < text.length()) {
                char ch = text.charAt(pos);
                if (ch == ',' || ch == '}' || ch == ' ' || ch == '\n' || ch == '\r'
                        || ch == '\t') {
                    break;
                }
                pos++;
            }
            String number = text.substring(start, pos);
            try {
                return Double.valueOf(number);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
