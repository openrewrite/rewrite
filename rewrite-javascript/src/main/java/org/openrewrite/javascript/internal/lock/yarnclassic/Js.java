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
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.EcmaScript.JsonSyntaxError;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Yarn's values as the port holds them, undefined as null: property reads, {@code Object.assign}, {@code JSON.parse},
 * string splits and template literals.
 */
final class Js extends EcmaScript {

    static final String INVARIANT_VIOLATION = "Invariant Violation";
    static final String INVALID_ARG_TYPE = "TypeError [ERR_INVALID_ARG_TYPE]";

    /**
     * Node's {@code ERR_INVALID_ARG_TYPE} for a value yarn read from JSON, whose objects have no prototype.
     */
    static JsError invalidArgType(String expected, @Nullable Object received) {
        return new JsError(INVALID_ARG_TYPE, expected + ". Received " + specificType(received));
    }

    private static String specificType(@Nullable Object value) {
        if (value == null) {
            return "undefined";
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).isEmpty() ? "[Object: null prototype] {}" : "[Object: null prototype]";
        }
        if (value instanceof List) {
            return "an instance of Array";
        }
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            return "type number (" + (d == 0 && 1 / d < 0 ? "-0" : numberToString(d)) + ")";
        }
        return "type " + typeOf(value) + " (" + (value instanceof String ? "'" + value + "'" : value) + ")";
    }

    private Js() {
    }

    public static String typeOf(@Nullable Object value) {
        return value instanceof UnknownPath ? "string" : EcmaScript.typeOf(value == null ? UNDEFINED : value);
    }

    // the keys of any value yarn iterates, as a spread finds them: none for undefined
    public static List<String> keys(@Nullable Object value) {
        return ownKeys(value);
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
     * @throws JsError a {@code SyntaxError} when the text is not JSON.
     */
    public static @Nullable Object jsonParse(String text) {
        try {
            return EcmaScript.jsonParse(text);
        } catch (JsonSyntaxError e) {
            throw new JsError(JsError.SYNTAX_ERROR, e.getMessage());
        }
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

    public static String string(@Nullable Object value) {
        if (value instanceof UnknownPath) {
            throw ((UnknownPath) value).read();
        }
        return EcmaScript.string(value == null ? UNDEFINED : value);
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
