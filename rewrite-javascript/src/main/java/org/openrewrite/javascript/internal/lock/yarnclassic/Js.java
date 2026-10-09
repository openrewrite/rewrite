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

import org.openrewrite.javascript.internal.lock.EcmaScript;
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
import java.util.regex.Pattern;

/**
 * The JavaScript semantics yarn's values follow: truthiness, {@code typeof}, the order {@code Object.keys} gives,
 * {@code Object.assign}, {@code JSON.parse} and {@code JSON.stringify} of strings and numbers.
 */
final class Js extends EcmaScript {

    /**
     * The characters JavaScript's {@code \s} matches.
     */
    static final String WHITESPACE_CLASS = "\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029" +
                                           "\\u202F\\u205F\\u3000\\uFEFF";

    /**
     * The characters JavaScript's {@code .} matches; Java's also leaves out {@code \u0085}.
     */
    static final String DOT = "[^\\n\\r\\u2028\\u2029]";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private Js() {
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

    /**
     * {@code String.prototype.split} by a string.
     */
    static String[] splitString(String s, String separator) {
        if (separator.isEmpty()) {
            String[] chars = new String[s.length()];
            for (int i = 0; i < s.length(); i++) {
                chars[i] = String.valueOf(s.charAt(i));
            }
            return chars;
        }
        return s.split(Pattern.quote(separator), -1);
    }

    /**
     * {@code s.split(separator).join(with)}.
     */
    static String splitJoin(String s, String separator, String with) {
        return String.join(with, splitString(s, separator));
    }

    private static final Pattern NON_DECIMAL = Pattern.compile("0([xX][0-9a-fA-F]+|[oO][0-7]+|[bB][01]+)");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?(Infinity|([0-9]+\\.?[0-9]*|\\.[0-9]+)" +
                                                           "([eE][+-]?[0-9]+)?)");

    /**
     * ToInt32, as {@code x >> 0} applies it.
     */
    static int toInt32(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return 0;
        }
        return (int) (new BigDecimal(d).toBigInteger().longValue() & 0xFFFFFFFFL);
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
     * {@code `${value}`} of a value read from a lock, whose objects from yarn's parser have no prototype to convert.
     */
    static String templateString(@Nullable Object value) {
        // without a callable toString or valueOf of its own (an own key holds a string, not a function) or
        // inherited, an object has no primitive value
        if (value instanceof Map && (!(value instanceof PlainObject) || ((Map<?, ?>) value).containsKey("toString"))) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot convert object to primitive value");
        }
        if (value instanceof List) {
            List<String> parts = new ArrayList<>();
            for (Object item : (List<?>) value) {
                parts.add(item == null ? "" : templateString(item));
            }
            return String.join(",", parts);
        }
        return string(value);
    }

}
