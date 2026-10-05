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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * JavaScript's semantics for the values and strings npm reads from package and lock data.
 */
final class Js {

    /**
     * The characters {@code \s} matches in a JavaScript regular expression, for use inside a character class.
     */
    static final String WHITESPACE_CLASS =
            "\\t\\n\\x0B\\f\\r \\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff";

    /**
     * What JavaScript reads from a property that is not there. JSON's {@code null} is Java's.
     */
    static final Object UNDEFINED = new Object() {
        @Override
        public String toString() {
            return "undefined";
        }
    };

    /**
     * A member a value inherits from its prototype, such as {@code constructor}: truthy and never a string.
     */
    static final Object INHERITED = new Object() {
        @Override
        public String toString() {
            return "function";
        }
    };

    // the prototypes of V8 13.6, which Node 24 runs npm 11 on
    private static final Set<String> OBJECT_MEMBERS = members(
            "__defineGetter__ __defineSetter__ __lookupGetter__ __lookupSetter__ __proto__ constructor " +
            "hasOwnProperty isPrototypeOf propertyIsEnumerable toLocaleString toString valueOf");
    private static final Set<String> ARRAY_MEMBERS = members(
            "at concat copyWithin entries every fill filter find findIndex findLast findLastIndex flat flatMap " +
            "forEach includes indexOf join keys lastIndexOf map pop push reduce reduceRight reverse shift slice " +
            "some sort splice toReversed toSorted toSpliced unshift values with");
    private static final Set<String> STRING_MEMBERS = members(
            "anchor at big blink bold charAt charCodeAt codePointAt concat endsWith fixed fontcolor fontsize " +
            "includes indexOf isWellFormed italics lastIndexOf link localeCompare match matchAll normalize padEnd " +
            "padStart repeat replace replaceAll search slice small split startsWith strike sub substr substring " +
            "sup toLocaleLowerCase toLocaleUpperCase toLowerCase toUpperCase toWellFormed trim trimEnd trimLeft " +
            "trimRight trimStart");
    private static final Set<String> NUMBER_MEMBERS = members("toExponential toFixed toPrecision");

    private static final long MAX_ARRAY_INDEX = 4294967294L;

    private Js() {
    }

    private static Set<String> members(String names) {
        return new HashSet<>(Arrays.asList(names.split(" ")));
    }

    /**
     * {@code receiver[key]} for a value parsed from JSON.
     *
     * @return {@link #UNDEFINED} when there is no such property, {@link #INHERITED} for a prototype member.
     * @throws IllegalArgumentException for JavaScript's TypeError on reading from null or undefined.
     */
    static @Nullable Object get(@Nullable Object receiver, String key) {
        if (receiver == null || receiver == UNDEFINED) {
            String type = receiver == null ? "null" : "undefined";
            throw new IllegalArgumentException("Cannot read properties of " + type + " (reading '" + key + "')");
        }
        if (receiver instanceof Map) {
            Map<?, ?> object = (Map<?, ?>) receiver;
            if (object.containsKey(key)) {
                return object.get(key);
            }
        } else if (receiver instanceof List) {
            List<?> array = (List<?>) receiver;
            long index = arrayIndex(key);
            if (index >= 0 && index < array.size()) {
                return array.get((int) index);
            }
            if ("length".equals(key)) {
                return array.size();
            }
            if (ARRAY_MEMBERS.contains(key)) {
                return INHERITED;
            }
        } else if (receiver instanceof String) {
            String string = (String) receiver;
            long index = arrayIndex(key);
            if (index >= 0 && index < string.length()) {
                return String.valueOf(string.charAt((int) index));
            }
            if ("length".equals(key)) {
                return string.length();
            }
            if (STRING_MEMBERS.contains(key)) {
                return INHERITED;
            }
        } else if (receiver instanceof Number && NUMBER_MEMBERS.contains(key)) {
            return INHERITED;
        }
        return OBJECT_MEMBERS.contains(key) ? INHERITED : UNDEFINED;
    }

    /**
     * {@code Object.entries(value)} for a value parsed from JSON: an object's integer keys in ascending
     * order and then its other keys as inserted, or the indexes of an array or a string.
     *
     * @throws IllegalArgumentException for JavaScript's TypeError on null or undefined.
     */
    @SuppressWarnings("unchecked")
    static List<Map.Entry<String, @Nullable Object>> entries(@Nullable Object value) {
        if (value == null || value == UNDEFINED) {
            throw new IllegalArgumentException("Cannot convert undefined or null to object");
        }
        List<Map.Entry<String, @Nullable Object>> entries = new ArrayList<>();
        if (value instanceof Map) {
            Map<Long, Map.Entry<String, @Nullable Object>> indexed = new TreeMap<>();
            for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) value).entrySet()) {
                long index = arrayIndex(entry.getKey());
                if (index >= 0) {
                    indexed.put(index, entry);
                } else {
                    entries.add(entry);
                }
            }
            entries.addAll(0, indexed.values());
        } else if (value instanceof List) {
            List<?> array = (List<?>) value;
            for (int i = 0; i < array.size(); i++) {
                entries.add(new SimpleImmutableEntry<>(String.valueOf(i), array.get(i)));
            }
        } else if (value instanceof String) {
            String string = (String) value;
            for (int i = 0; i < string.length(); i++) {
                entries.add(new SimpleImmutableEntry<>(String.valueOf(i), String.valueOf(string.charAt(i))));
            }
        }
        return entries;
    }

    /**
     * {@code Object.keys(value)}, in the order of {@link #entries}.
     */
    static List<String> keys(@Nullable Object value) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, @Nullable Object> entry : entries(value)) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    private static long arrayIndex(String key) {
        int length = key.length();
        if (length == 0 || length > 10 || (length > 1 && key.charAt(0) == '0')) {
            return -1;
        }
        long index = 0;
        for (int i = 0; i < length; i++) {
            char c = key.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            index = index * 10 + (c - '0');
        }
        return index <= MAX_ARRAY_INDEX ? index : -1;
    }

    /**
     * {@code a === b} for values parsed from JSON: objects and arrays are equal only to themselves.
     */
    static boolean strictEquals(@Nullable Object a, @Nullable Object b) {
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        if (a instanceof String || a instanceof Boolean) {
            return a.equals(b);
        }
        return a == b;
    }

    static boolean truthy(@Nullable Object val) {
        if (val == null || val == UNDEFINED || Boolean.FALSE.equals(val)) {
            return false;
        }
        if (val instanceof String) {
            return !((String) val).isEmpty();
        }
        if (val instanceof Number) {
            double d = ((Number) val).doubleValue();
            return d != 0 && !Double.isNaN(d);
        }
        return true;
    }

    static boolean isWhitespace(char c) {
        return c == '\t' || c == '\n' || c == 0x0B || c == '\f' || c == '\r' || c == ' ' || c == 0xA0 ||
               c == 0x1680 || (c >= 0x2000 && c <= 0x200A) || c == 0x2028 || c == 0x2029 || c == 0x202F ||
               c == 0x205F || c == 0x3000 || c == 0xFEFF;
    }

    static String trim(String s) {
        return trimEnd(trimStart(s));
    }

    static String trimStart(String s) {
        int start = 0;
        while (start < s.length() && isWhitespace(s.charAt(start))) {
            start++;
        }
        return s.substring(start);
    }

    static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    /**
     * {@code JSON.stringify} of a string.
     */
    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    boolean leads = Character.isHighSurrogate(c) && i + 1 < s.length() &&
                                    Character.isLowSurrogate(s.charAt(i + 1));
                    boolean trails = Character.isLowSurrogate(c) && i > 0 && Character.isHighSurrogate(s.charAt(i - 1));
                    boolean loneSurrogate = Character.isSurrogate(c) && !leads && !trails;
                    if (c < 0x20 || loneSurrogate) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
