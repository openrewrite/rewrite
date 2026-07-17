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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serializes to the exact bytes of Python's {@code json.dumps(obj, sort_keys=True)}: keys sorted
 * by code point at every level and {@code ensure_ascii=True} escaping. Poetry and PDM both hash
 * this default (spaced) form ({@code separators=(", ", ": ")}) of a relevant slice of pyproject.toml.
 */
public final class CanonicalJson {

    private CanonicalJson() {
    }

    /**
     * @param value  the value tree (Map/List/String/Boolean/Long/null)
     * @param spaced {@code true} for Python's default separators {@code (", ", ": ")};
     *               {@code false} for the compact {@code (",", ":")} form
     */
    public static String emit(@Nullable Object value, boolean spaced) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out, spaced ? ", " : ",", spaced ? ": " : ":");
        return out.toString();
    }

    private static void writeValue(@Nullable Object value, StringBuilder out, String itemSep, String keySep) {
        if (value instanceof Map) {
            writeMap((Map<?, ?>) value, out, itemSep, keySep);
        } else if (value instanceof List) {
            writeList((List<?>) value, out, itemSep, keySep);
        } else {
            writeScalar(value, out);
        }
    }

    private static void writeMap(Map<?, ?> map, StringBuilder out, String itemSep, String keySep) {
        out.append('{');
        boolean first = true;
        for (String key : sortedKeys(map)) {
            if (!first) {
                out.append(itemSep);
            }
            first = false;
            writeString(key, out);
            out.append(keySep);
            writeValue(map.get(key), out, itemSep, keySep);
        }
        out.append('}');
    }

    private static void writeList(List<?> list, StringBuilder out, String itemSep, String keySep) {
        out.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                out.append(itemSep);
            }
            writeValue(list.get(i), out, itemSep, keySep);
        }
        out.append(']');
    }

    private static void writeScalar(@Nullable Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            writeString((String) value, out);
        } else if (value instanceof Boolean) {
            out.append((Boolean) value ? "true" : "false");
        } else if (value instanceof Long || value instanceof Integer ||
                value instanceof Short || value instanceof Byte || value instanceof BigInteger) {
            out.append(value);
        } else {
            throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass().getName());
        }
    }

    private static List<String> sortedKeys(Map<?, ?> map) {
        List<String> keys = new ArrayList<>(map.size());
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("JSON object keys must be strings, got: " +
                        (key == null ? "null" : key.getClass().getName()));
            }
            keys.add((String) key);
        }
        keys.sort(CanonicalJson::compareCodePoints);
        return keys;
    }

    // Python compares str by code point; String.compareTo would order astral chars before U+E000..U+FFFF
    private static int compareCodePoints(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            int ca = a.codePointAt(i);
            int cb = b.codePointAt(j);
            if (ca != cb) {
                return Integer.compare(ca, cb);
            }
            i += Character.charCount(ca);
            j += Character.charCount(cb);
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static void writeString(String s, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                default:
                    if (c >= 0x20 && c < 0x7f) {
                        out.append(c);
                    } else {
                        out.append("\\u")
                                .append(HEX[(c >> 12) & 0xF])
                                .append(HEX[(c >> 8) & 0xF])
                                .append(HEX[(c >> 4) & 0xF])
                                .append(HEX[c & 0xF]);
                    }
            }
        }
        out.append('"');
    }
}
