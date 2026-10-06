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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * The hole {@code delete array[i]} leaves: no key for {@link #entries}, {@link #UNDEFINED} when read.
     */
    static final Object HOLE = new Object() {
        @Override
        public String toString() {
            return "undefined";
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
    private static final Pattern DECIMAL_LITERAL = Pattern.compile("[+-]?(?:Infinity|(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)");

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
                Object element = array.get((int) index);
                return element == HOLE ? UNDEFINED : element;
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
                if (array.get(i) != HOLE) {
                    entries.add(new SimpleImmutableEntry<>(String.valueOf(i), array.get(i)));
                }
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

    /**
     * {@code delete receiver[key]} in strict mode, which every class body is: an object loses the property,
     * an array keeps a {@link #HOLE}, and a property that cannot be deleted (a string's characters, a
     * length) is JavaScript's TypeError.
     */
    static void delete(@Nullable Object receiver, String key) {
        if (receiver instanceof String || receiver instanceof List) {
            long index = arrayIndex(key);
            boolean fixed = "length".equals(key) ||
                            receiver instanceof String && index >= 0 && index < ((String) receiver).length();
            if (fixed) {
                throw new IllegalArgumentException("Cannot delete property '" + key + "' of [object " +
                                                   (receiver instanceof String ? "String" : "Array") + "]");
            }
        }
        deleteSloppy(receiver, key);
    }

    /**
     * {@code delete receiver[key]} outside strict mode: a property that cannot be deleted is left alone.
     */
    @SuppressWarnings("unchecked")
    static void deleteSloppy(@Nullable Object receiver, String key) {
        if (receiver == null || receiver == UNDEFINED) {
            throw new IllegalArgumentException("Cannot convert undefined or null to object");
        }
        if (receiver instanceof Map) {
            ((Map<String, Object>) receiver).remove(key);
        } else if (receiver instanceof List) {
            List<Object> array = (List<Object>) receiver;
            long index = arrayIndex(key);
            if (index >= 0 && index < array.size()) {
                array.set((int) index, HOLE);
            }
        }
    }

    /**
     * The canonical array index {@code key} names, or -1.
     */
    static long arrayIndex(String key) {
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

    /**
     * {@code Number(value)} for a value parsed from JSON, as the relational operators coerce it.
     */
    static double toNumber(@Nullable Object val) {
        if (val == null) {
            return 0;
        }
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        if (val instanceof Boolean) {
            return (Boolean) val ? 1 : 0;
        }
        if (val instanceof String) {
            String s = trim((String) val);
            if (s.isEmpty()) {
                return 0;
            }
            if (DECIMAL_LITERAL.matcher(s).matches()) {
                return Double.parseDouble(s);
            }
            if (s.length() > 2 && s.charAt(0) == '0') {
                char radix = Character.toLowerCase(s.charAt(1));
                int base = radix == 'x' ? 16 : radix == 'o' ? 8 : radix == 'b' ? 2 : 0;
                if (base != 0) {
                    try {
                        return new BigInteger(s.substring(2), base).doubleValue();
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return Double.NaN;
        }
        if (val instanceof List) {
            return toNumber(string(val));
        }
        return Double.NaN;
    }

    /**
     * {@code String(value)} for a value parsed from JSON.
     */
    static String string(@Nullable Object val) {
        if (val == null || val == UNDEFINED || val == INHERITED || val instanceof Boolean) {
            return String.valueOf(val);
        }
        if (val instanceof Number) {
            return numberToString((Number) val);
        }
        if (val instanceof String) {
            return (String) val;
        }
        if (val instanceof List) {
            List<?> array = (List<?>) val;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                Object element = array.get(i);
                if (element != null && element != UNDEFINED && element != HOLE) {
                    sb.append(string(element));
                }
            }
            return sb.toString();
        }
        return "[object Object]";
    }

    /**
     * How JavaScript's TypeError names a primitive it cannot create a property on.
     */
    static String typeOf(Object val) {
        if (val instanceof Number) {
            return "number";
        }
        if (val instanceof Boolean) {
            return "boolean";
        }
        if (val instanceof String) {
            return "string";
        }
        return val == UNDEFINED ? "undefined" : "object";
    }

    static boolean truthy(@Nullable Object val) {
        if (val == null || val == UNDEFINED || val == HOLE || Boolean.FALSE.equals(val)) {
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

    /**
     * How Node's {@code ERR_INVALID_ARG_TYPE} names the value it received.
     */
    static String received(@Nullable Object value) {
        if (value == null || value == UNDEFINED) {
            return String.valueOf(value);
        }
        if (value instanceof Number) {
            return "type number (" + numberToString((Number) value) + ")";
        }
        if (value instanceof Boolean) {
            return "type boolean (" + value + ")";
        }
        if (value instanceof String) {
            return "type string ('" + value + "')";
        }
        return "an instance of " + (value instanceof List ? "Array" : "Object");
    }

    private static final Pattern DATE_TIME_STRING = Pattern.compile(
            "^([+-]\\d{6}|\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?" +
            "(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,3})\\d*)?)?(Z|[+-]\\d{2}:\\d{2})?)?$");

    /**
     * {@code Date.parse(text)} for the ECMAScript date-time string format: the time value in
     * milliseconds, or NaN, which V8's legacy parser may still turn into a date (surrounding whitespace
     * among its forms). A date-only form is UTC.
     *
     * @throws CannotKnow for a date-time without an offset, which JavaScript reads in the host's time zone
     */
    static double dateParse(String text) {
        Matcher m = DATE_TIME_STRING.matcher(text);
        if (!m.matches()) {
            return Double.NaN;
        }
        if (m.group(4) != null && m.group(8) == null) {
            throw new CannotKnow("the host's time zone (\"" + text + "\" names no offset)");
        }
        try {
            int year = Integer.parseInt(m.group(1));
            int month = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
            int day = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
            int hour = m.group(4) == null ? 0 : Integer.parseInt(m.group(4));
            int minute = m.group(5) == null ? 0 : Integer.parseInt(m.group(5));
            int second = m.group(6) == null ? 0 : Integer.parseInt(m.group(6));
            String fraction = m.group(7) == null ? "0" : (m.group(7) + "00").substring(0, 3);
            if (hour == 24 && (minute != 0 || second != 0 || !"000".equals(fraction))) {
                return Double.NaN;
            }
            ZoneOffset offset = m.group(8) == null || "Z".equals(m.group(8)) ? ZoneOffset.UTC : ZoneOffset.of(m.group(8));
            long millis = LocalDate.of(year, month, day).atStartOfDay(offset).toInstant().toEpochMilli() +
                          hour * 3600000L + minute * 60000L + second * 1000L + Integer.parseInt(fraction);
            return (double) millis;
        } catch (DateTimeException | NumberFormatException e) {
            return Double.NaN;
        }
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
     * {@code Number.prototype.toString()} of a number parsed from JSON.
     */
    static String numberToString(Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
            long l = n.longValue();
            return Math.abs(l) <= MAX_SAFE_INTEGER ? Long.toString(l) : numberToString((double) l);
        }
        return numberToString(n.doubleValue());
    }

    private static final long MAX_SAFE_INTEGER = (1L << 53) - 1;

    // the shortest decimal that reads back as the same double, the nearest when several do, as JavaScript prints
    private static BigDecimal shortest(double d) {
        BigDecimal exact = new BigDecimal(d);
        for (int precision = 1; precision < 17; precision++) {
            BigDecimal nearest = exact.round(new MathContext(precision, RoundingMode.HALF_EVEN));
            if (nearest.doubleValue() == d) {
                return nearest.stripTrailingZeros();
            }
            // a power of two's interval is asymmetric: the far neighbor may read back when the nearest does not
            BigDecimal floor = exact.round(new MathContext(precision, RoundingMode.FLOOR));
            BigDecimal other = floor.compareTo(nearest) == 0 ?
                    exact.round(new MathContext(precision, RoundingMode.CEILING)) : floor;
            if (other.doubleValue() == d) {
                return other.stripTrailingZeros();
            }
        }
        return exact.round(new MathContext(17, RoundingMode.HALF_EVEN)).stripTrailingZeros();
    }

    private static String numberToString(double d) {
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
        int k = digits.length();
        int n = k - decimal.scale();
        StringBuilder sb = new StringBuilder();
        if (k <= n && n <= 21) {
            sb.append(digits);
            for (int i = k; i < n; i++) {
                sb.append('0');
            }
        } else if (0 < n && n <= 21) {
            sb.append(digits, 0, n).append('.').append(digits, n, k);
        } else if (-6 < n && n <= 0) {
            sb.append("0.");
            for (int i = n; i < 0; i++) {
                sb.append('0');
            }
            sb.append(digits);
        } else {
            sb.append(digits.charAt(0));
            if (k > 1) {
                sb.append('.').append(digits, 1, k);
            }
            sb.append('e').append(n - 1 >= 0 ? "+" : "-").append(Math.abs(n - 1));
        }
        return sb.toString();
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
