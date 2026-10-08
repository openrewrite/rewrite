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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.javascript.internal.lock.npm.Js.truthy;

/**
 * Port of ini 6.0.0 {@code decode}, which npm reads an npmrc with.
 */
public final class Ini {

    /**
     * A section: {@code Object.create(null)}, an object with no prototype and so no string form.
     */
    static final class Section extends LinkedHashMap<String, @Nullable Object> {
    }

    private static final Pattern LINES = Pattern.compile("[\\r\\n]+");
    private static final Pattern BLANK_OR_COMMENT = Pattern.compile("^[" + Js.WHITESPACE_CLASS + "]*(?:[;#]|\\z)");
    private static final Pattern LINE = Pattern.compile(
            "^\\[([^\\]]*)\\][" + Js.WHITESPACE_CLASS + "]*\\z|^([^=]+)(=([^\\n\\r\\u2028\\u2029]*))?\\z");

    private Ini() {
    }

    /**
     * {@code ini.parse(text)}: sections are nested maps, {@code key[]} lines a list, {@code true},
     * {@code false} and {@code null} the JSON values, a quoted value what {@code JSON.parse} makes of it.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, @Nullable Object> decode(String str) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        Object p = out;
        for (String line : LINES.split(str, -1)) {
            if (line.isEmpty() || BLANK_OR_COMMENT.matcher(line).find()) {
                continue;
            }
            Matcher match = LINE.matcher(line);
            if (!match.find()) {
                continue;
            }
            if (match.group(1) != null) {
                Object section = unsafe(match.group(1));
                if ("__proto__".equals(section)) {
                    p = new Section();
                    continue;
                }
                String name = Js.string(section);
                Object existing = out.get(name);
                p = truthy(existing) ? existing : new Section();
                out.put(name, p);
                continue;
            }
            Object keyRaw = unsafe(match.group(2));
            if (keyRaw == null) {
                throw new IllegalArgumentException("Cannot read properties of null (reading 'length')");
            }
            String keyText = Js.string(keyRaw);
            boolean isArray = keyRaw instanceof String && keyText.length() > 2 && keyText.endsWith("[]");
            String key = isArray ? keyText.substring(0, keyText.length() - 2) : keyText;
            if ("__proto__".equals(key)) {
                continue;
            }
            Object valueRaw = match.group(3) != null ? unsafe(match.group(4)) : Boolean.TRUE;
            Object value = "true".equals(valueRaw) ? Boolean.TRUE : "false".equals(valueRaw) ? Boolean.FALSE :
                    "null".equals(valueRaw) ? null : valueRaw;

            // a property assigned on a primitive or an array section value is not seen again
            if (!(p instanceof Map)) {
                continue;
            }
            Map<String, Object> target = (Map<String, Object>) p;
            if (isArray) {
                if (!target.containsKey(key)) {
                    target.put(key, new ArrayList<>());
                } else if (!(target.get(key) instanceof List)) {
                    List<Object> wrapped = new ArrayList<>();
                    wrapped.add(target.get(key));
                    target.put(key, wrapped);
                }
            }
            if (target.get(key) instanceof List) {
                ((List<Object>) target.get(key)).add(value);
            } else {
                target.put(key, value);
            }
        }

        List<String> remove = new ArrayList<>();
        for (String k : Js.keys(out)) {
            Object section = out.get(k);
            if (!(section == null || section instanceof Map)) {
                continue;
            }
            List<String> parts = splitSections(k, ".");
            p = out;
            String l = parts.remove(parts.size() - 1);
            String nl = l.replace("\\.", ".");
            for (String part : parts) {
                if ("__proto__".equals(part)) {
                    continue;
                }
                if (p == null) {
                    throw new IllegalArgumentException("Cannot convert undefined or null to object");
                }
                Object child;
                if (p instanceof Map) {
                    Map<String, Object> parent = (Map<String, Object>) p;
                    child = parent.get(part);
                    boolean object = child == null || child instanceof Map || child instanceof List;
                    if (!parent.containsKey(part) || !object) {
                        child = new Section();
                        parent.put(part, child);
                    }
                } else {
                    child = new Section();
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
                ((Map<String, Object>) p).put(nl, section);
            }
            remove.add(k);
        }
        for (String del : remove) {
            out.remove(del);
        }
        return out;
    }

    private static List<String> splitSections(String str, String separator) {
        int lastMatchIndex = 0;
        int lastSeparatorIndex = 0;
        int nextIndex;
        List<String> sections = new ArrayList<>();
        do {
            nextIndex = str.indexOf(separator, lastMatchIndex);
            if (nextIndex != -1) {
                lastMatchIndex = nextIndex + separator.length();
                if (nextIndex > 0 && str.charAt(nextIndex - 1) == '\\') {
                    continue;
                }
                sections.add(str.substring(lastSeparatorIndex, nextIndex));
                lastSeparatorIndex = nextIndex + separator.length();
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
            !val.equals(val.trim())) {
            return Js.quote(val);
        }
        return val.replace(";", "\\;").replace("#", "\\#");
    }

    static @Nullable Object unsafe(@Nullable String raw) {
        String val = Js.trim(raw == null ? "" : raw);
        if (isQuoted(val)) {
            if (val.charAt(0) == '\'') {
                val = val.substring(1, Math.max(1, val.length() - 1));
            }
            try {
                return JsonParseEvenBetterErrors.jsonParse(val);
            } catch (JsonParseEvenBetterErrors.JsonParseException ignored) {
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
        return Js.trim(unesc.toString());
    }
}
