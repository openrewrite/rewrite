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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The JavaScript semantics pnpm's code relies on, over the values {@link JsYaml} loads: objects are
 * {@code LinkedHashMap}s whose own key order is computed on read (array indices first, ascending, then insertion
 * order), arrays are lists, numbers are doubles.
 */
final class Js {

    private static final long MAX_ARRAY_INDEX = 4294967294L;

    private Js() {
    }

    /**
     * {@code Object.keys}.
     */
    static List<String> keys(Map<String, ?> object) {
        List<String> indices = new ArrayList<>();
        List<String> rest = new ArrayList<>();
        for (String key : object.keySet()) {
            if (arrayIndex(key) >= 0) {
                indices.add(key);
            } else {
                rest.add(key);
            }
        }
        indices.sort((a, b) -> Long.compare(arrayIndex(a), arrayIndex(b)));
        indices.addAll(rest);
        return indices;
    }

    static long arrayIndex(String key) {
        if (key.isEmpty() || key.length() > 10 || (key.length() > 1 && key.charAt(0) == '0')) {
            return -1;
        }
        long index = 0;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            index = index * 10 + (c - '0');
        }
        return index <= MAX_ARRAY_INDEX ? index : -1;
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

    /**
     * {@code Number.prototype.toString()}.
     */
    static String numberToString(double d) {
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (d == 0) {
            return "0";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
        }
        if (d < 0) {
            return "-" + numberToString(-d);
        }
        BigDecimal decimal = shortest(d);
        String digits = decimal.unscaledValue().toString();
        int n = digits.length() - decimal.scale();
        if (n >= -5 && n <= 21) {
            return decimal.toPlainString();
        }
        String mantissa = digits.length() == 1 ? digits : digits.charAt(0) + "." + digits.substring(1);
        return mantissa + "e" + (n - 1 >= 0 ? "+" : "-") + Math.abs(n - 1);
    }

    // the shortest decimal that reads back as the same double, the nearest when several do, as JavaScript prints
    private static BigDecimal shortest(double d) {
        BigDecimal exact = new BigDecimal(d);
        for (int precision = 1; precision < 17; precision++) {
            BigDecimal nearest = exact.round(new MathContext(precision, RoundingMode.HALF_EVEN));
            if (nearest.doubleValue() == d) {
                return nearest.stripTrailingZeros();
            }
            BigDecimal floor = exact.round(new MathContext(precision, RoundingMode.FLOOR));
            BigDecimal other = floor.compareTo(nearest) == 0 ?
                    exact.round(new MathContext(precision, RoundingMode.CEILING)) : floor;
            if (other.doubleValue() == d) {
                return other.stripTrailingZeros();
            }
        }
        return exact.round(new MathContext(17, RoundingMode.HALF_EVEN)).stripTrailingZeros();
    }

    /**
     * {@code String.prototype.trim}: ECMAScript white space and line terminators.
     */
    static String trim(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isWhiteSpace(s.charAt(start))) {
            start++;
        }
        while (end > start && isWhiteSpace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(start, end);
    }

    private static boolean isWhiteSpace(char c) {
        return c == '\t' || c == '\n' || c == 0x0B || c == '\f' || c == '\r' || c == ' ' || c == 0xA0 ||
               c == 0x1680 || (0x2000 <= c && c <= 0x200A) || c == 0x2028 || c == 0x2029 || c == 0x202F ||
               c == 0x205F || c == 0x3000 || c == 0xFEFF;
    }

    static boolean truthy(@Nullable Object value) {
        if (value == null || value == JsYaml.UNDEFINED || Boolean.FALSE.equals(value)) {
            return false;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        return true;
    }

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
}
