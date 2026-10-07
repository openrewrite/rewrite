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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of ini 1.3.5 {@code decode}, which yarn 1.22.22 reads an .npmrc with.
 */
final class Ini {

    private static final Pattern LINE = Pattern.compile("^\\[([^\\]]*)\\]$|^([^=]+)(=(.*))?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT = Pattern.compile("^[" + Js.WHITESPACE_CLASS + "]*[;#]");

    private Ini() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> decode(String str) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        Map<String, @Nullable Object> p = out;
        for (String line : str.split("[\\r\\n]+", -1)) {
            if (line.isEmpty() || COMMENT.matcher(line).find()) {
                continue;
            }
            Matcher match = LINE.matcher(line);
            if (!match.find()) {
                continue;
            }
            if (match.group(1) != null) {
                String section = unsafe(match.group(1));
                Object existing = out.get(section);
                if (!Js.truthy(existing)) {
                    existing = new LinkedHashMap<String, Object>();
                    out.put(section, existing);
                }
                p = existing instanceof Map ? (Map<String, @Nullable Object>) existing : new LinkedHashMap<>();
                continue;
            }
            String key = unsafe(match.group(2));
            Object value = match.group(3) != null && !match.group(3).isEmpty() ? unsafe(match.group(4)) : true;
            if ("true".equals(value)) {
                value = true;
            } else if ("false".equals(value)) {
                value = false;
            } else if ("null".equals(value)) {
                value = null;
            }
            if (key.length() > 2 && key.endsWith("[]")) {
                key = key.substring(0, key.length() - 2);
                Object existing = p.get(key);
                if (!Js.truthy(existing)) {
                    p.put(key, new ArrayList<>());
                } else if (!(existing instanceof List)) {
                    List<@Nullable Object> list = new ArrayList<>();
                    list.add(existing);
                    p.put(key, list);
                }
            }
            Object current = p.get(key);
            if (current instanceof List) {
                ((List<@Nullable Object>) current).add(value);
            } else {
                p.put(key, value);
            }
        }

        List<String> deletes = new ArrayList<>();
        for (String k : Js.keys(out)) {
            Object section = out.get(k);
            if (!(section instanceof Map)) {
                continue;
            }
            List<String> parts = dotSplit(k);
            Map<String, @Nullable Object> target = out;
            String l = parts.remove(parts.size() - 1);
            String nl = l.replace("\\.", ".");
            for (String part : parts) {
                if (!(target.get(part) instanceof Map)) {
                    target.put(part, new LinkedHashMap<String, Object>());
                }
                target = (Map<String, @Nullable Object>) target.get(part);
            }
            if (target == out && nl.equals(l)) {
                continue;
            }
            target.put(nl, section);
            deletes.add(k);
        }
        for (String del : deletes) {
            out.remove(del);
        }
        return out;
    }

    private static List<String> dotSplit(String str) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length() && str.charAt(i + 1) == '.') {
                part.append("\\.");
                i++;
            } else if (c == '.') {
                parts.add(part.toString());
                part.setLength(0);
            } else {
                part.append(c);
            }
        }
        parts.add(part.toString());
        return parts;
    }

    private static boolean isQuoted(String val) {
        return !val.isEmpty() && ((val.charAt(0) == '"' && val.endsWith("\"")) ||
                                  (val.charAt(0) == '\'' && val.endsWith("'")));
    }

    static String unsafe(@Nullable String raw) {
        String val = Js.trim(raw == null ? "" : raw);
        if (isQuoted(val)) {
            if (val.charAt(0) == '\'') {
                val = val.length() >= 2 ? val.substring(1, val.length() - 1) : "";
            }
            try {
                Object parsed = Js.jsonParse(val);
                return parsed instanceof String ? (String) parsed : Js.string(parsed);
            } catch (JsError e) {
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
