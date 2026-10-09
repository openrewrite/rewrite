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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.V8Date;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The JavaScript semantics pnpm's code relies on, over the values {@link JsYaml} loads: objects in
 * {@code Object.keys} order, arrays as lists, numbers as doubles.
 */
final class Js extends EcmaScript {

    private Js() {
    }

    /**
     * A copy holding the keys in {@code Object.keys} order, as a spread or {@code Object.fromEntries} builds it.
     */
    static <V> Map<String, V> ordered(Map<String, V> object) {
        Map<String, V> out = new LinkedHashMap<>();
        for (String key : keys(object)) {
            out.put(key, object.get(key));
        }
        return out;
    }

    /**
     * {@code String(value)} for the values a YAML document or package.json holds.
     */
    static String string(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value == JsYaml.UNDEFINED) {
            return "undefined";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number) {
            return numberToString(((Number) value).doubleValue());
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List) {
            StringBuilder out = new StringBuilder();
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                Object item = list.get(i);
                if (item != null) {
                    out.append(string(item));
                }
            }
            return out.toString();
        }
        if (value instanceof byte[]) {
            StringBuilder out = new StringBuilder();
            byte[] bytes = (byte[]) value;
            for (int i = 0; i < bytes.length; i++) {
                if (i > 0) {
                    out.append(',');
                }
                out.append(bytes[i] & 0xFF);
            }
            return out.toString();
        }
        if (value instanceof JsYaml.Timestamp) {
            throw new CannotKnow("the text of a date (Date.prototype.toString follows the host's time zone)");
        }
        return "[object Object]";
    }

    private static final Pattern DECIMAL_LITERAL =
            Pattern.compile("[+-]?(?:Infinity|(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)");

    /**
     * {@code a === b} for values held by reference only when they are objects.
     */
    static boolean strictEquals(@Nullable Object a, @Nullable Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        if (a instanceof String || a instanceof Boolean) {
            return a.equals(b);
        }
        return a == b;
    }

    /**
     * Ramda's {@code equals}: by value, objects by their own keys whatever the order.
     */
    @SuppressWarnings("unchecked")
    static boolean equals(@Nullable Object a, @Nullable Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Number && b instanceof Number) {
            double x = ((Number) a).doubleValue();
            double y = ((Number) b).doubleValue();
            if (Double.isNaN(x)) {
                return Double.isNaN(y);
            }
            return x == y && (x != 0 || 1 / x == 1 / y);
        }
        if (a instanceof Map && b instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) a;
            Map<String, Object> n = (Map<String, Object>) b;
            if (!m.keySet().equals(n.keySet())) {
                return false;
            }
            for (Map.Entry<String, Object> entry : m.entrySet()) {
                if (!equals(entry.getValue(), n.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof List && b instanceof List) {
            List<Object> l = (List<Object>) a;
            List<Object> k = (List<Object>) b;
            if (l.size() != k.size()) {
                return false;
            }
            for (int i = 0; i < l.size(); i++) {
                if (!equals(l.get(i), k.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(a, b);
    }

    /**
     * {@code JSON.stringify} without indentation; {@code "undefined"} for a value it does not serialize, as a
     * template literal prints its result.
     */
    static String jsonStringify(@Nullable Object value) {
        if (value == JsYaml.UNDEFINED) {
            return "undefined";
        }
        StringBuilder out = new StringBuilder();
        stringify(value, out);
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    private static void stringify(@Nullable Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String) {
            quote((String) value, out);
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            out.append(Double.isNaN(d) || Double.isInfinite(d) ? "null" : numberToString(d));
        } else if (value instanceof JsYaml.Timestamp) {
            quote(((JsYaml.Timestamp) value).toIsoString(), out);
        } else if (value instanceof List) {
            out.append('[');
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                Object item = list.get(i);
                stringify(item == JsYaml.UNDEFINED ? null : item, out);
            }
            out.append(']');
        } else if (value instanceof Map) {
            out.append('{');
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) value;
            boolean first = true;
            for (String key : keys(map)) {
                Object member = map.get(key);
                if (member == JsYaml.UNDEFINED) {
                    continue;
                }
                if (!first) {
                    out.append(',');
                }
                first = false;
                quote(key, out);
                out.append(':');
                stringify(member, out);
            }
            out.append('}');
        } else {
            out.append("{}");
        }
    }

    /**
     * {@code typeof}.
     */
    static String typeOf(@Nullable Object value) {
        if (value == null) {
            return "object";
        }
        if (value == JsYaml.UNDEFINED) {
            return "undefined";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        return "object";
    }

    /**
     * The own enumerable keys of a JavaScript value used as an object: an object's, an array's indices, a
     * string's character positions; nothing for anything else.
     */
    @SuppressWarnings("unchecked")
    static Set<String> ownKeys(@Nullable Object value) {
        Set<String> keys = new LinkedHashSet<>();
        if (value instanceof Map) {
            keys.addAll(keys((Map<String, ?>) value));
        } else if (value instanceof List) {
            for (int i = 0; i < ((List<?>) value).size(); i++) {
                keys.add(String.valueOf(i));
            }
        } else if (value instanceof String) {
            for (int i = 0; i < ((String) value).length(); i++) {
                keys.add(String.valueOf(i));
            }
        }
        return keys;
    }

    /**
     * {@code new Date(text).getTime()}: NaN for an invalid date; cannot-know for a date-time without an offset, which
     * JavaScript reads in the host's time zone.
     */
    static double dateParse(String text) {
        Double time = V8Date.parse(text);
        if (time == null) {
            throw new CannotKnow("the time \"" + text + "\" (JavaScript reads it in the host's time zone)");
        }
        return time;
    }

    /**
     * {@code Date.prototype.toISOString} of a valid time value.
     */
    static String toISOString(double time) {
        Instant instant = Instant.ofEpochMilli((long) time);
        int year = instant.atZone(ZoneOffset.UTC).getYear();
        String yearText = year >= 0 && year <= 9999 ? String.format("%04d", year) :
                (year < 0 ? "-" : "+") + String.format("%06d", Math.abs(year));
        return yearText + ISO_REST.format(instant);
    }

    private static final DateTimeFormatter ISO_REST =
            DateTimeFormatter.ofPattern("-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
}
