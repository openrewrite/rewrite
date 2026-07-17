/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Line-oriented reader for the fixed TOML subset that lock generators (poetry, pdm) emit: leading
 * comment header, {@code [table]} / {@code [[array-of-tables]]} headers, and {@code key = value}
 * bodies whose values are basic/literal strings, integers, booleans, arrays and inline tables.
 * State is a {@code String[] lines} plus an {@code int[] pos} cursor. Anything outside this subset
 * throws {@link MachineTomlException}, so a re-emission can never silently drop data.
 */
public final class MachineTomlReader {

    private MachineTomlReader() {
    }

    /** Consume the leading run of {@code #} comment lines (and the blank after) as the header, or null. */
    public static @Nullable String readHeader(String[] lines, int[] pos) {
        StringBuilder header = new StringBuilder();
        boolean found = false;
        while (pos[0] < lines.length) {
            String line = lines[pos[0]];
            if (line.startsWith("#")) {
                if (found) {
                    header.append('\n');
                }
                header.append(line);
                found = true;
                pos[0]++;
            } else if (line.trim().isEmpty()) {
                pos[0]++;
                break;
            } else {
                break;
            }
        }
        return found ? header.toString() : null;
    }

    /** Advance past blank lines to the next table header, or null at EOF. */
    public static @Nullable String nextTableHeader(String[] lines, int[] pos) {
        while (pos[0] < lines.length) {
            String line = lines[pos[0]];
            if (line.trim().isEmpty()) {
                pos[0]++;
                continue;
            }
            if (line.startsWith("[")) {
                pos[0]++;
                return line.trim();
            }
            throw new MachineTomlException("Expected a table header but found: " + line);
        }
        return null;
    }

    /** Read {@code key = value} lines until a blank line, the next table header, or EOF. */
    public static Map<String, Object> readKeyValues(String[] lines, int[] pos) {
        Map<String, Object> result = new LinkedHashMap<>();
        while (pos[0] < lines.length) {
            String line = lines[pos[0]];
            if (line.trim().isEmpty() || line.startsWith("[")) {
                break;
            }
            int eq = indexOfTopLevelEquals(line);
            if (eq < 0) {
                throw new MachineTomlException("Expected 'key = value' but found: " + line);
            }
            String key = unquoteKey(line.substring(0, eq).trim());
            StringBuilder valueText = new StringBuilder(line.substring(eq + 1));
            pos[0]++;
            while (!isComplete(valueText)) {
                if (pos[0] >= lines.length) {
                    throw new MachineTomlException("Unterminated value for key: " + key);
                }
                valueText.append('\n').append(lines[pos[0]]);
                pos[0]++;
            }
            if (result.put(key, parseCompleteValue(key, valueText.toString())) != null) {
                throw new MachineTomlException("Duplicate key: " + key);
            }
        }
        return result;
    }

    private static int indexOfTopLevelEquals(String line) {
        boolean inString = false;
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inString) {
                if (c == quote) {
                    inString = false;
                }
            } else if (c == '"' || c == '\'') {
                inString = true;
                quote = c;
            } else if (c == '=') {
                return i;
            }
        }
        return -1;
    }

    private static String unquoteKey(String key) {
        if (key.length() >= 2 && key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"') {
            return unescape(key.substring(1, key.length() - 1));
        }
        return key;
    }

    private static boolean isComplete(CharSequence s) {
        int depth = 0;
        boolean inString = false;
        boolean literal = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (literal) {
                    if (c == '\'') {
                        inString = false;
                    }
                } else if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
                literal = false;
            } else if (c == '\'') {
                inString = true;
                literal = true;
            } else if (c == '[' || c == '{') {
                depth++;
            } else if (c == ']' || c == '}') {
                depth--;
            }
        }
        return depth <= 0 && !inString;
    }

    private static Object parseCompleteValue(String key, String text) {
        int[] p = {0};
        Object value = parseValue(text, p);
        skipWhitespace(text, p);
        if (p[0] != text.length()) {
            throw new MachineTomlException("Trailing content after value for key " + key + ": " + text.substring(p[0]));
        }
        return value;
    }

    private static Object parseValue(String s, int[] p) {
        skipWhitespace(s, p);
        if (p[0] >= s.length()) {
            throw new MachineTomlException("Unexpected end of value");
        }
        char c = s.charAt(p[0]);
        if (c == '"') {
            return parseBasicString(s, p);
        }
        if (c == '\'') {
            return parseLiteralString(s, p);
        }
        if (c == '[') {
            return parseArray(s, p);
        }
        if (c == '{') {
            return parseInlineTable(s, p);
        }
        if (s.startsWith("true", p[0])) {
            p[0] += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", p[0])) {
            p[0] += 5;
            return Boolean.FALSE;
        }
        if ((c >= '0' && c <= '9') || c == '-' || c == '+') {
            return parseInteger(s, p);
        }
        throw new MachineTomlException("Unsupported value syntax at: " + s.substring(p[0]));
    }

    private static String parseBasicString(String s, int[] p) {
        p[0]++;
        StringBuilder b = new StringBuilder();
        while (p[0] < s.length()) {
            char c = s.charAt(p[0]);
            if (c == '\\') {
                p[0]++;
                if (p[0] >= s.length()) {
                    throw new MachineTomlException("Unterminated escape in string");
                }
                b.append(unescapeChar(s, p));
                continue;
            }
            if (c == '"') {
                p[0]++;
                return b.toString();
            }
            b.append(c);
            p[0]++;
        }
        throw new MachineTomlException("Unterminated string");
    }

    private static String parseLiteralString(String s, int[] p) {
        p[0]++;
        int start = p[0];
        while (p[0] < s.length()) {
            if (s.charAt(p[0]) == '\'') {
                String value = s.substring(start, p[0]);
                p[0]++;
                return value;
            }
            p[0]++;
        }
        throw new MachineTomlException("Unterminated literal string");
    }

    private static char[] unescapeChar(String s, int[] p) {
        char e = s.charAt(p[0]);
        p[0]++;
        switch (e) {
            case '"':
                return new char[]{'"'};
            case '\\':
                return new char[]{'\\'};
            case 'n':
                return new char[]{'\n'};
            case 't':
                return new char[]{'\t'};
            case 'r':
                return new char[]{'\r'};
            case 'b':
                return new char[]{'\b'};
            case 'f':
                return new char[]{'\f'};
            case 'u':
                return Character.toChars(hex(s, p, 4));
            case 'U':
                return Character.toChars(hex(s, p, 8));
            default:
                throw new MachineTomlException("Unsupported escape sequence: \\" + e);
        }
    }

    private static int hex(String s, int[] p, int digits) {
        if (p[0] + digits > s.length()) {
            throw new MachineTomlException("Truncated unicode escape");
        }
        int value = Integer.parseInt(s.substring(p[0], p[0] + digits), 16);
        p[0] += digits;
        return value;
    }

    private static String unescape(String s) {
        int[] p = {0};
        StringBuilder b = new StringBuilder();
        while (p[0] < s.length()) {
            char c = s.charAt(p[0]);
            if (c == '\\') {
                p[0]++;
                b.append(unescapeChar(s, p));
            } else {
                b.append(c);
                p[0]++;
            }
        }
        return b.toString();
    }

    private static Long parseInteger(String s, int[] p) {
        int start = p[0];
        if (s.charAt(p[0]) == '-' || s.charAt(p[0]) == '+') {
            p[0]++;
        }
        while (p[0] < s.length() && s.charAt(p[0]) >= '0' && s.charAt(p[0]) <= '9') {
            p[0]++;
        }
        return Long.parseLong(s.substring(start, p[0]));
    }

    private static List<Object> parseArray(String s, int[] p) {
        p[0]++;
        List<Object> values = new ArrayList<>();
        while (true) {
            skipWhitespace(s, p);
            if (p[0] >= s.length()) {
                throw new MachineTomlException("Unterminated array");
            }
            if (s.charAt(p[0]) == ']') {
                p[0]++;
                return values;
            }
            values.add(parseValue(s, p));
            skipWhitespace(s, p);
            if (p[0] < s.length() && s.charAt(p[0]) == ',') {
                p[0]++;
            } else if (p[0] >= s.length() || s.charAt(p[0]) != ']') {
                throw new MachineTomlException("Expected ',' or ']' in array");
            }
        }
    }

    private static Map<String, Object> parseInlineTable(String s, int[] p) {
        p[0]++;
        Map<String, Object> table = new LinkedHashMap<>();
        skipWhitespace(s, p);
        if (p[0] < s.length() && s.charAt(p[0]) == '}') {
            p[0]++;
            return table;
        }
        while (true) {
            skipWhitespace(s, p);
            String key;
            if (p[0] < s.length() && s.charAt(p[0]) == '"') {
                key = parseBasicString(s, p);
            } else {
                int keyStart = p[0];
                while (p[0] < s.length() && isBareKeyChar(s.charAt(p[0]))) {
                    p[0]++;
                }
                key = s.substring(keyStart, p[0]);
            }
            if (key.isEmpty()) {
                throw new MachineTomlException("Expected a key in inline table");
            }
            skipWhitespace(s, p);
            if (p[0] >= s.length() || s.charAt(p[0]) != '=') {
                throw new MachineTomlException("Expected '=' after inline table key " + key);
            }
            p[0]++;
            if (table.put(key, parseValue(s, p)) != null) {
                throw new MachineTomlException("Duplicate inline table key: " + key);
            }
            skipWhitespace(s, p);
            if (p[0] >= s.length()) {
                throw new MachineTomlException("Unterminated inline table");
            }
            char c = s.charAt(p[0]);
            if (c == '}') {
                p[0]++;
                return table;
            }
            if (c != ',') {
                throw new MachineTomlException("Expected ',' or '}' in inline table");
            }
            p[0]++;
        }
    }

    private static boolean isBareKeyChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_';
    }

    private static void skipWhitespace(String s, int[] p) {
        while (p[0] < s.length() && (s.charAt(p[0]) == ' ' || s.charAt(p[0]) == '\t' || s.charAt(p[0]) == '\n')) {
            p[0]++;
        }
    }
}
