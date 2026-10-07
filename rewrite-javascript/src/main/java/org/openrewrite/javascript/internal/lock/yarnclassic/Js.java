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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JavaScript semantics yarn's values follow: truthiness, {@code typeof}, the order {@code Object.keys} gives,
 * {@code Object.assign}, {@code JSON.parse} and {@code JSON.stringify} of strings and numbers.
 */
final class Js {

    /**
     * The characters JavaScript's {@code \s} matches.
     */
    static final String WHITESPACE_CLASS = "\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029" +
                                           "\\u202F\\u205F\\u3000\\uFEFF";

    /**
     * The characters JavaScript's {@code .} matches; Java's also leaves out {@code \u0085}.
     */
    static final String DOT = "[^\\n\\r\\u2028\\u2029]";

    private static final long MAX_ARRAY_INDEX = 4294967294L;

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private Js() {
    }

    static boolean truthy(@Nullable Object value) {
        if (value == null || Boolean.FALSE.equals(value)) {
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

    static String typeOf(@Nullable Object value) {
        if (value == null) {
            return "undefined";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Number) {
            return "number";
        }
        return "object";
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
     * {@code Object.keys} of any value yarn iterates: an array's and a string's indices, a primitive's none.
     */
    @SuppressWarnings("unchecked")
    static List<String> keys(@Nullable Object value) {
        if (value instanceof Map) {
            return keys((Map<String, ?>) value);
        }
        List<String> indices = new ArrayList<>();
        int length = value instanceof List ? ((List<?>) value).size() :
                value instanceof String ? ((String) value).length() : 0;
        for (int i = 0; i < length; i++) {
            indices.add(String.valueOf(i));
        }
        return indices;
    }

    /**
     * {@code value[key]}: a property, an array element or a string's character.
     */
    @SuppressWarnings("unchecked")
    static @Nullable Object get(@Nullable Object value, String key) {
        if (value instanceof Map) {
            return ((Map<String, ?>) value).get(key);
        }
        long index = arrayIndex(key);
        if (index < 0) {
            return null;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return index < list.size() ? list.get((int) index) : null;
        }
        if (value instanceof String) {
            String s = (String) value;
            return index < s.length() ? String.valueOf(s.charAt((int) index)) : null;
        }
        return null;
    }

    /**
     * {@code value.length}: an array's or a string's, {@code -1} where JavaScript has {@code undefined}.
     */
    static int length(@Nullable Object value) {
        if (value instanceof List) {
            return ((List<?>) value).size();
        }
        return value instanceof String ? ((String) value).length() : -1;
    }

    static boolean hasOwnProperty(@Nullable Object value, String key) {
        if (value instanceof Map) {
            return ((Map<?, ?>) value).containsKey(key);
        }
        return (value instanceof List || value instanceof String) && get(value, key) != null;
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
     * {@code Object.assign(target, source)} for the sources yarn passes: objects, and strings, whose characters
     * become indexed properties.
     */
    @SuppressWarnings("unchecked")
    static void assign(Map<String, @Nullable Object> target, @Nullable Object source) {
        if (source instanceof Map) {
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) source;
            for (String key : keys(map)) {
                target.put(key, map.get(key));
            }
        } else if (source instanceof String) {
            String s = (String) source;
            for (int i = 0; i < s.length(); i++) {
                target.put(String.valueOf(i), String.valueOf(s.charAt(i)));
            }
        } else if (source instanceof List) {
            List<?> list = (List<?>) source;
            for (int i = 0; i < list.size(); i++) {
                target.put(String.valueOf(i), list.get(i));
            }
        }
    }

    /**
     * {@code JSON.parse}, with every object's keys in the order JavaScript gives them.
     *
     * @throws JsError a {@code SyntaxError} when the text is not JSON.
     */
    static @Nullable Object jsonParse(String text) {
        try {
            return order(MAPPER.readValue(text, Object.class));
        } catch (JsonProcessingException e) {
            throw new JsError(JsError.SYNTAX_ERROR, e.getOriginalMessage());
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
     * {@code JSON.stringify} of a string.
     */
    static String quote(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2).append('"');
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
                    if (c < 0x20 || isLoneSurrogate(s, i)) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }

    /**
     * {@code JSON.stringify} without indentation; {@code undefined} for a value it has no representation of.
     */
    static @Nullable String jsonStringify(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return quote((String) value);
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return Double.isNaN(d) || Double.isInfinite(d) ? "null" : numberToString(d);
        }
        if (value instanceof Map) {
            StringBuilder out = new StringBuilder("{");
            Map<?, ?> map = (Map<?, ?>) value;
            for (String key : keys(value)) {
                String item = jsonStringify(map.get(key));
                if (item != null) {
                    out.append(out.length() > 1 ? "," : "").append(quote(key)).append(':').append(item);
                }
            }
            return out.append('}').toString();
        }
        if (value instanceof List) {
            List<String> items = new ArrayList<>();
            for (Object item : (List<?>) value) {
                String s = jsonStringify(item);
                items.add(s == null ? "null" : s);
            }
            return "[" + String.join(",", items) + "]";
        }
        return value instanceof Boolean ? value.toString() : null;
    }

    private static boolean isLoneSurrogate(String s, int i) {
        char c = s.charAt(i);
        if (Character.isHighSurrogate(c)) {
            return i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1));
        }
        if (Character.isLowSurrogate(c)) {
            return i == 0 || !Character.isHighSurrogate(s.charAt(i - 1));
        }
        return false;
    }

    static boolean isWhitespace(char c) {
        return c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r' || c == ' ' || c == ' ' ||
               c == ' ' || (c >= ' ' && c <= ' ') || c == ' ' || c == ' ' ||
               c == ' ' || c == ' ' || c == '　' || c == '﻿';
    }

    /**
     * {@code String.prototype.trim}.
     */
    static String trim(String s) {
        int start = 0;
        int end = s.length();
        while (start < end && isWhitespace(s.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(start, end);
    }

    /**
     * {@code String(value)} of a JavaScript value as yarn holds them.
     */
    static String string(@Nullable Object value) {
        if (value == null) {
            return "undefined";
        }
        if (value instanceof Number) {
            return numberToString(((Number) value).doubleValue());
        }
        if (value instanceof Map) {
            return "[object Object]";
        }
        if (value instanceof List) {
            List<String> parts = new ArrayList<>();
            for (Object item : (List<?>) value) {
                parts.add(item == null ? "" : string(item));
            }
            return String.join(",", parts);
        }
        return value.toString();
    }

    /**
     * {@code Number.prototype.toString()}.
     */
    static String numberToString(double d) {
        if (Double.isNaN(d)) {
            return "NaN";
        }
        if (Double.isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
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
        int exponent = n - 1;
        return sign + mantissa + "e" + (exponent >= 0 ? "+" : "-") + Math.abs(exponent);
    }
}
