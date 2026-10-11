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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.EcmaScript.truthy;

/**
 * Port of ini {@code decode}: 1.3.5, which yarn 1.22.22 reads an .npmrc with, and 6.0.0, which npm 11.17.0 and pnpm
 * 11.2.2 (read-ini-file 5.0.0) read one with; {@code encode} of a flat map as 6.0.0 writes it.
 */
public final class Ini {

    public enum Release {
        V1_3_5,
        V6_0_0
    }

    /**
     * A 6.0.0 section: {@code Object.create(null)}, an object with no prototype and so no string form.
     */
    public static final class Section extends LinkedHashMap<String, @Nullable Object> {
    }

    private static final Pattern LINES = Pattern.compile("[\\r\\n]+");
    private static final Pattern COMMENT = Pattern.compile("^[" + EcmaScript.WHITESPACE_CLASS + "]*[;#]");
    private static final Pattern BLANK = Pattern.compile("^[" + EcmaScript.WHITESPACE_CLASS + "]*\\z");
    private static final String KEY_VALUE = "|^([^=]+)(=(" + V8RegExp.DOT + "*))?\\z";
    private static final Pattern LINE_1_3_5 = Pattern.compile("^\\[([^\\]]*)\\]\\z" + KEY_VALUE);
    private static final Pattern LINE_6_0_0 = Pattern.compile("^\\[([^\\]]*)\\][" + EcmaScript.WHITESPACE_CLASS +
                                                              "]*\\z" + KEY_VALUE);

    private Ini() {
    }

    /**
     * {@code ini.parse(text)}: sections are nested maps, {@code key[]} lines a list, {@code true},
     * {@code false} and {@code null} the JSON values, a quoted value what {@code JSON.parse} makes of it.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, @Nullable Object> decode(String str, Release release) {
        boolean v6 = release == Release.V6_0_0;
        Map<String, @Nullable Object> out = v6 ? new Section() : new LinkedHashMap<>();
        Object p = out;
        for (String line : LINES.split(str, -1)) {
            if (line.isEmpty() || COMMENT.matcher(line).find() || v6 && BLANK.matcher(line).find()) {
                continue;
            }
            Matcher match = (v6 ? LINE_6_0_0 : LINE_1_3_5).matcher(line);
            if (!match.find()) {
                continue;
            }
            if (match.group(1) != null) {
                Object section = unsafe(match.group(1));
                if (v6 && "__proto__".equals(section)) {
                    p = new Section();
                    continue;
                }
                String name = EcmaScript.string(section);
                Object existing = out.get(name);
                p = truthy(existing) ? existing : v6 ? new Section() : new LinkedHashMap<String, @Nullable Object>();
                out.put(name, p);
                continue;
            }
            Object keyRaw = unsafe(match.group(2));
            if (keyRaw == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'length')");
            }
            String keyText = EcmaScript.string(keyRaw);
            boolean isArray = keyRaw instanceof String && keyText.length() > 2 && keyText.endsWith("[]");
            String key = isArray ? keyText.substring(0, keyText.length() - 2) : keyText;
            if (v6 && "__proto__".equals(key)) {
                continue;
            }
            Object valueRaw = match.group(3) != null ? unsafe(match.group(4)) : Boolean.TRUE;
            Object value = "true".equals(valueRaw) ? Boolean.TRUE : "false".equals(valueRaw) ? Boolean.FALSE :
                    "null".equals(valueRaw) ? null : valueRaw;

            // a property assigned on a primitive or an array section value is not seen again
            if (!(p instanceof Map)) {
                continue;
            }
            Map<String, @Nullable Object> target = (Map<String, @Nullable Object>) p;
            if (isArray) {
                if (v6 ? !target.containsKey(key) : !truthy(target.get(key))) {
                    target.put(key, new ArrayList<>());
                } else if (!(target.get(key) instanceof List)) {
                    List<@Nullable Object> wrapped = new ArrayList<>();
                    wrapped.add(target.get(key));
                    target.put(key, wrapped);
                }
            }
            Object current = target.get(key);
            if (current instanceof List) {
                ((List<@Nullable Object>) current).add(value);
            } else {
                target.put(key, value);
            }
        }

        List<String> remove = new ArrayList<>();
        for (String k : EcmaScript.keys(out)) {
            Object section = out.get(k);
            // typeof null is 'object': 6.0.0 nests a null value, 1.3.5's falsy check skips it
            if (!(section instanceof Map || v6 && section == null)) {
                continue;
            }
            List<String> parts = splitSections(k);
            p = out;
            String l = parts.remove(parts.size() - 1);
            String nl = l.replace("\\.", ".");
            for (String part : parts) {
                if (v6 && "__proto__".equals(part)) {
                    continue;
                }
                if (p == null) {
                    throw new IllegalArgumentException("Cannot convert undefined or null to object");
                }
                Object child;
                if (p instanceof Map) {
                    Map<String, @Nullable Object> parent = (Map<String, @Nullable Object>) p;
                    child = parent.get(part);
                    boolean object = child instanceof Map || child instanceof List ||
                                     v6 && child == null && parent.containsKey(part);
                    if (!object) {
                        child = v6 ? new Section() : new LinkedHashMap<String, @Nullable Object>();
                        parent.put(part, child);
                    }
                } else {
                    child = v6 ? new Section() : new LinkedHashMap<String, @Nullable Object>();
                }
                p = child;
            }
            if (p == out && nl.equals(l)) {
                continue;
            }
            if (p == null) {
                throw new IllegalArgumentException("Cannot set properties of null (setting '" + nl + "')");
            }
            if (p instanceof Map) {
                ((Map<String, @Nullable Object>) p).put(nl, section);
            }
            remove.add(k);
        }
        for (String del : remove) {
            out.remove(del);
        }
        return out;
    }

    // 6.0.0's splitSections; 1.3.5's dotSplit splits at the same dots
    private static List<String> splitSections(String str) {
        int lastMatchIndex = 0;
        int lastSeparatorIndex = 0;
        int nextIndex;
        List<String> sections = new ArrayList<>();
        do {
            nextIndex = str.indexOf('.', lastMatchIndex);
            if (nextIndex != -1) {
                lastMatchIndex = nextIndex + 1;
                if (nextIndex > 0 && str.charAt(nextIndex - 1) == '\\') {
                    continue;
                }
                sections.add(str.substring(lastSeparatorIndex, nextIndex));
                lastSeparatorIndex = nextIndex + 1;
            }
        } while (nextIndex != -1);
        sections.add(str.substring(lastSeparatorIndex));
        return sections;
    }

    private static boolean isQuoted(String val) {
        return (val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"));
    }

    /**
     * {@code ini.stringify} of a flat map: one {@code key=value} line per entry, the bare key for a null value.
     */
    public static String encode(Map<String, @Nullable String> properties) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, @Nullable String> entry : properties.entrySet()) {
            out.append(safe(entry.getKey()));
            if (entry.getValue() != null) {
                out.append('=').append(safe(entry.getValue()));
            }
            out.append('\n');
        }
        return out.toString();
    }

    /**
     * {@code ini.safe(val)}: a key or value as {@code ini.stringify} writes it, so that {@link #decode} reads it
     * back as the same text.
     */
    static String safe(String val) {
        if (val.matches("(?s).*[=\\r\\n].*") || val.startsWith("[") || (val.length() > 1 && isQuoted(val)) ||
            !val.equals(EcmaScript.trim(val))) {
            return EcmaScript.quote(val);
        }
        return val.replace(";", "\\;").replace("#", "\\#");
    }

    static @Nullable Object unsafe(@Nullable String raw) {
        String val = EcmaScript.trim(raw == null ? "" : raw);
        if (isQuoted(val)) {
            if (val.charAt(0) == '\'') {
                val = val.substring(1, Math.max(1, val.length() - 1));
            }
            try {
                return EcmaScript.jsonParse(val);
            } catch (EcmaScript.JsonSyntaxError ignored) {
                return val;
            }
        }
        boolean esc = false;
        StringBuilder unesc = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            if (esc) {
                if ("\\;#".indexOf(c) != -1) {
                    unesc.append(c);
                } else {
                    unesc.append('\\').append(c);
                }
                esc = false;
            } else if (";#".indexOf(c) != -1) {
                break;
            } else if (c == '\\') {
                esc = true;
            } else {
                unesc.append(c);
            }
        }
        if (esc) {
            unesc.append('\\');
        }
        return EcmaScript.trim(unesc.toString());
    }
}
