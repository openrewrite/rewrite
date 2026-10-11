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
package org.openrewrite.javascript.internal.lock.bun;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Port of bun's src/ini.zig {@code Parser.parse} (bun 1.3.10), which reads an .npmrc. */
final class Ini {

    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private final HostEnvironment env;
    private final Map<String, @Nullable Object> out = new LinkedHashMap<>();

    private Ini(HostEnvironment env) {
        this.env = env;
    }

    static Map<String, @Nullable Object> parse(String src, HostEnvironment env) {
        Ini parser = new Ini(env);
        parser.parse(src);
        return parser.out;
    }

    private static boolean shouldSkipLine(String line) {
        if (line.isEmpty() || line.charAt(0) == ';' || line.charAt(0) == '#') {
            return true;
        }
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '#' || c == ';') {
                return true;
            }
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void parse(String src) {
        Map<String, @Nullable Object> head = out;
        boolean skipUntilNextSection = false;
        for (String rawLine : src.split("\n", -1)) {
            String line = rawLine.endsWith("\r") ? rawLine.substring(0, rawLine.length() - 1) : rawLine;
            if (shouldSkipLine(line)) {
                continue;
            }
            if (line.charAt(0) == '[') {
                int close = line.indexOf(']');
                if (close < 0) {
                    continue;
                }
                boolean treatAsKey = false;
                for (int i = close + 1; i < line.length(); i++) {
                    if (line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                        treatAsKey = true;
                        break;
                    }
                }
                if (!treatAsKey) {
                    skipUntilNextSection = false;
                    Map<String, @Nullable Object> section = getOrPutObject(sectionPath(line.substring(1, close)));
                    if (section == null) {
                        skipUntilNextSection = true;
                    } else {
                        head = section;
                    }
                    continue;
                }
            }
            if (skipUntilNextSection) {
                continue;
            }
            int eq = line.indexOf('=');
            String keyRaw = (String) prepare(eq < 0 ? line : line.substring(0, eq), false);
            boolean isArray = keyRaw.length() > 2 && keyRaw.endsWith("[]");
            String key = isArray ? keyRaw.substring(0, keyRaw.length() - 2) : keyRaw;
            if ("__proto__".equals(key)) {
                continue;
            }
            Object value;
            if (eq < 0) {
                value = Boolean.TRUE;
            } else if (eq + 1 < line.length()) {
                value = prepare(line.substring(eq + 1), true);
            } else {
                value = "";
            }
            if ("true".equals(value)) {
                value = Boolean.TRUE;
            } else if ("false".equals(value)) {
                value = Boolean.FALSE;
            } else if ("null".equals(value)) {
                value = null;
            }
            if (isArray) {
                Object existing = head.get(key);
                if (head.containsKey(key)) {
                    if (!(existing instanceof List)) {
                        List<@Nullable Object> list = new ArrayList<>();
                        list.add(existing);
                        head.put(key, list);
                    }
                } else {
                    head.put(key, new ArrayList<>());
                }
            }
            Object current = head.get(key);
            if (current instanceof List) {
                ((List<@Nullable Object>) current).add(value);
            } else {
                head.put(key, value);
            }
        }
    }

    // getOrPutObject over the section's dotted path: null where a value that is not an object is in the way
    @SuppressWarnings("unchecked")
    private @Nullable Map<String, @Nullable Object> getOrPutObject(List<String> path) {
        Map<String, @Nullable Object> object = out;
        for (String part : path) {
            Object next = object.get(part);
            if (next == null && !object.containsKey(part)) {
                next = new LinkedHashMap<String, @Nullable Object>();
                object.put(part, next);
            } else if (!(next instanceof Map)) {
                return null;
            }
            object = (Map<String, @Nullable Object>) next;
        }
        return object;
    }

    // prepareStr(.section): the name split on its unescaped dots
    private List<String> sectionPath(String raw) {
        String val = trim(raw);
        List<String> parts = new ArrayList<>();
        if (isQuoted(val)) {
            Object parsed = quoted(val, false);
            parts.add(parsed instanceof String ? (String) parsed : string(parsed));
            return parts;
        }
        StringBuilder unesc = new StringBuilder();
        boolean esc = false;
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (esc) {
                if (c == '\\' || c == ';' || c == '#' || c == '$' || c == '.') {
                    unesc.append(c);
                } else {
                    unesc.append('\\').append(c);
                }
                esc = false;
            } else if (c == ';' || c == '#') {
                break;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '.') {
                parts.add(unesc.toString());
                unesc.setLength(0);
            } else {
                unesc.append(c);
            }
        }
        if (esc) {
            unesc.append('\\');
        }
        parts.add(unesc.toString());
        return parts;
    }

    // prepareStr(.key) and prepareStr(.value): a value without escapes or substitutions is kept whole, comment and all
    private @Nullable Object prepare(String raw, boolean value) {
        String val = trim(raw);
        if (isQuoted(val)) {
            Object parsed = quoted(val, value);
            if (parsed instanceof String || value) {
                return parsed;
            }
            return string(parsed);
        }
        boolean didAnyEscape = false;
        boolean esc = false;
        StringBuilder unesc = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (esc) {
                if (c == '\\') {
                    unesc.append('\\');
                } else if (c == ';' || c == '#' || c == '$') {
                    unesc.append(c);
                } else {
                    unesc.append('\\').append(c);
                }
                esc = false;
            } else if (c == '$') {
                int end = value ? substitute(val, i, i, unesc) : -1;
                if (end >= 0) {
                    didAnyEscape = true;
                    i = end;
                    continue;
                }
                unesc.append('$');
            } else if (c == ';' || c == '#') {
                break;
            } else if (c == '\\') {
                esc = true;
                didAnyEscape = true;
            } else {
                unesc.append(c);
            }
        }
        if (esc) {
            unesc.append('\\');
        }
        return didAnyEscape ? unesc.toString() : val;
    }

    // a quoted value: single quotes stripped, then JSON, whose strings take the environment's substitutions
    private @Nullable Object quoted(String quoted, boolean value) {
        String val = quoted;
        if (val.charAt(0) == '\'') {
            val = val.length() > 1 ? val.substring(1, val.length() - 1) : val.substring(1);
        }
        Object parsed;
        try {
            parsed = JSON.readValue(val, Object.class);
        } catch (JsonProcessingException e) {
            return value ? expandEnvVars(val) : val;
        }
        if (parsed instanceof String && value) {
            return expandEnvVars((String) parsed);
        }
        return parsed;
    }

    private String expandEnvVars(String val) {
        if (!val.contains("${")) {
            return val;
        }
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < val.length()) {
            if (val.charAt(i) == '$' && i + 2 < val.length() && val.charAt(i + 1) == '{') {
                int j = i + 2;
                int depth = 1;
                while (j < val.length() && depth > 0) {
                    if (val.charAt(j) == '{') {
                        depth++;
                    } else if (val.charAt(j) == '}') {
                        depth--;
                    }
                    if (depth > 0) {
                        j++;
                    }
                }
                if (depth == 0) {
                    String raw = val.substring(i + 2, j);
                    boolean optional = !raw.isEmpty() && raw.charAt(raw.length() - 1) == '?';
                    String expanded = env.reference(optional ? raw.substring(0, raw.length() - 1) : raw);
                    if (expanded != null) {
                        result.append(expanded);
                    } else if (!optional) {
                        result.append(val, i, j + 1);
                    }
                    i = j + 1;
                    continue;
                }
            }
            result.append(val.charAt(i));
            i++;
        }
        return result.toString();
    }

    // parseEnvSubstitution: the index of the closing brace once the substitution is written, else -1
    private int substitute(String val, int start, int i, StringBuilder unesc) {
        if (i + 2 < val.length() && val.charAt(i + 1) == '{') {
            boolean esc = false;
            int j = i + 2;
            boolean foundClosing = false;
            for (; j < val.length(); j++) {
                char c = val.charAt(j);
                if (c == '\\') {
                    esc = !esc;
                } else if (c == '$' && !esc) {
                    return substitute(val, start, j, unesc);
                } else if (c == '{' && !esc) {
                    return -1;
                } else if (c == '}' && !esc) {
                    foundClosing = true;
                    break;
                }
            }
            if (!foundClosing) {
                return -1;
            }
            String raw = val.substring(i + 2, j);
            boolean optional = !raw.isEmpty() && raw.charAt(raw.length() - 1) == '?';
            String expanded = env.reference(optional ? raw.substring(0, raw.length() - 1) : raw);
            if (expanded == null && !optional) {
                return -1;
            }
            if (start != i) {
                unesc.append(val, start, i);
            }
            if (expanded != null) {
                unesc.append(expanded);
            }
            return j;
        }
        return -1;
    }

    // what a JSON value other than a string becomes as a key, as npm's ini makes it
    private static String string(@Nullable Object value) {
        if (value instanceof Map) {
            return "[Object object]";
        }
        if (value instanceof List) {
            List<String> items = new ArrayList<>();
            for (Object item : (List<?>) value) {
                items.add(item == null ? "" : string(item));
            }
            return String.join(",", items);
        }
        return String.valueOf(value);
    }

    private static boolean isQuoted(String val) {
        return val.length() > 0 && (val.charAt(0) == '"' && val.charAt(val.length() - 1) == '"' ||
                                    val.charAt(0) == '\'' && val.charAt(val.length() - 1) == '\'');
    }

    // std.mem.trim(u8, val, " \n\r\t")
    private static String trim(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && " \n\r\t".indexOf(s.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && " \n\r\t".indexOf(s.charAt(end - 1)) >= 0) {
            end--;
        }
        return s.substring(start, end);
    }
}
