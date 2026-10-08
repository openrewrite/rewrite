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
package org.openrewrite.javascript.internal.lock.berry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The JavaScript semantics Yarn's plain objects, strings and numbers follow.
 */
final class Js {

    private static final long MAX_ARRAY_INDEX = 4294967294L;
    private static final Pattern DECIMAL =
            Pattern.compile("[+-]?(?:Infinity|(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)");
    private static final Pattern RADIX_INTEGER = Pattern.compile("0(?:[xX][0-9a-fA-F]+|[oO][0-7]+|[bB][01]+)");

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    /**
     * {@code JSON.parse} rejecting its input.
     */
    static final class SyntaxError extends RuntimeException {
        SyntaxError(String message) {
            super(message);
        }
    }

    private Js() {
    }

    /**
     * {@code value[key]}: the property, or {@link Syml#UNDEFINED} when the value is not an object or lacks it.
     */
    static @Nullable Object get(@Nullable Object value, String key) {
        if (value instanceof Map && ((Map<?, ?>) value).containsKey(key)) {
            return ((Map<?, ?>) value).get(key);
        }
        return Syml.UNDEFINED;
    }

    /**
     * {@code value[key]} where reading a property of {@code undefined} or {@code null} is V8's TypeError.
     */
    static @Nullable Object read(@Nullable Object value, String key) {
        if (value == null || value == Syml.UNDEFINED) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of " +
                                                  (value == null ? "null" : "undefined") + " (reading '" + key + "')");
        }
        return get(value, key);
    }

    static boolean isObject(@Nullable Object value) {
        return value instanceof Map || value instanceof List;
    }

    static boolean truthy(@Nullable Object value) {
        if (value == null || value == Syml.UNDEFINED || Boolean.FALSE.equals(value)) {
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
     * {@code Object.keys}: the integer keys in ascending order, then the rest in insertion order.
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

    /**
     * {@code Object.entries} of an object, or of an array by index.
     */
    @SuppressWarnings("unchecked")
    static List<Map.Entry<String, @Nullable Object>> entries(@Nullable Object value) {
        List<Map.Entry<String, @Nullable Object>> out = new ArrayList<>();
        if (value instanceof Map) {
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) value;
            for (String key : keys(map)) {
                out.add(new AbstractMap.SimpleEntry<>(key, map.get(key)));
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                out.add(new AbstractMap.SimpleEntry<>(String.valueOf(i), list.get(i)));
            }
        }
        return out;
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
     * {@code JSON.parse}, objects with their keys in the order JavaScript gives them and numbers as doubles.
     */
    static @Nullable Object jsonParse(String text) {
        try {
            return order(MAPPER.readValue(text, Object.class));
        } catch (JsonProcessingException e) {
            throw new SyntaxError(e.getOriginalMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object order(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, @Nullable Object> ordered = new LinkedHashMap<>();
            for (String key : keys(map)) {
                ordered.put(key, order(map.get(key)));
            }
            return ordered;
        }
        if (value instanceof List) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                list.add(order(item));
            }
            return list;
        }
        if (value instanceof Number && !(value instanceof Double)) {
            return ((Number) value).doubleValue();
        }
        return value;
    }

    /**
     * {@code String(value)} for the values a manifest holds.
     */
    static String string(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e21) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        if (value instanceof Map) {
            return "[object Object]";
        }
        if (value instanceof List) {
            StringBuilder out = new StringBuilder();
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                Object item = list.get(i);
                out.append(item == null || item == Syml.UNDEFINED ? "" : string(item));
            }
            return out.toString();
        }
        return value.toString();
    }

    /**
     * {@code StrWhiteSpaceChar}: the white space and line terminators {@code trim} and {@code Number} skip.
     */
    static boolean isWhiteSpace(char c) {
        return c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r' || c == '\u2028' || c == '\u2029' ||
               c == '\uFEFF' || Character.getType(c) == Character.SPACE_SEPARATOR;
    }

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

    /**
     * {@code Number(s)}: a decimal literal, {@code Infinity}, or a hexadecimal, octal or binary integer, between
     * white space; 0 for none, NaN for anything else.
     */
    static double stringToNumber(String s) {
        String t = trim(s);
        if (t.isEmpty()) {
            return 0;
        }
        if (DECIMAL.matcher(t).matches()) {
            return Double.parseDouble(t);
        }
        if (RADIX_INTEGER.matcher(t).matches()) {
            char prefix = Character.toLowerCase(t.charAt(1));
            return new BigInteger(t.substring(2), prefix == 'x' ? 16 : prefix == 'o' ? 8 : 2).doubleValue();
        }
        return Double.NaN;
    }

    static boolean isInteger(double d) {
        return !Double.isInfinite(d) && !Double.isNaN(d) && d == Math.floor(d);
    }

    /**
     * {@code Number.prototype.toString()}.
     */
    static String numberToString(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return Double.isNaN(d) ? "NaN" : d > 0 ? "Infinity" : "-Infinity";
        }
        if (d == 0) {
            return "0";
        }
        BigDecimal shortest = new BigDecimal(Double.toString(d)).stripTrailingZeros();
        String digits = shortest.unscaledValue().abs().toString();
        int n = digits.length() - shortest.scale();
        String sign = d < 0 ? "-" : "";
        if (n >= -5 && n <= 21) {
            return sign + shortest.abs().toPlainString();
        }
        String mantissa = digits.length() == 1 ? digits : digits.charAt(0) + "." + digits.substring(1);
        return sign + mantissa + "e" + (n - 1 >= 0 ? "+" : "-") + Math.abs(n - 1);
    }

    /**
     * {@code JSON.stringify} of the values a parsed object holds: maps in their order, lists, strings, numbers,
     * booleans and null.
     */
    static String jsonStringify(@Nullable Object value) {
        StringBuilder out = new StringBuilder();
        stringify(value, out);
        return out.toString();
    }

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
        } else if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (entry.getValue() == Syml.UNDEFINED) {
                    continue;
                }
                if (!first) {
                    out.append(',');
                }
                first = false;
                quote(String.valueOf(entry.getKey()), out);
                out.append(':');
                stringify(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof List) {
            out.append('[');
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                Object item = list.get(i);
                stringify(item == Syml.UNDEFINED ? null : item, out);
            }
            out.append(']');
        } else {
            throw new IllegalArgumentException("not a JSON value: " + value.getClass());
        }
    }

    private static void quote(String s, StringBuilder out) {
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
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
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
                default:
                    boolean loneSurrogate = Character.isHighSurrogate(c) ?
                            i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1)) :
                            Character.isLowSurrogate(c) && (i == 0 || !Character.isHighSurrogate(s.charAt(i - 1)));
                    if (c < 0x20 || loneSurrogate) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }
}
