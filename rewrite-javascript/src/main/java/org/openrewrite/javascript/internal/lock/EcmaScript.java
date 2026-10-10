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
package org.openrewrite.javascript.internal.lock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The JavaScript value model every port follows: truthiness, {@code typeof}, {@code String()}, {@code JSON.stringify},
 * strict equality, keys and their order, white space. Each port's {@code Js} extends it with its own object model.
 */
public class EcmaScript {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
            .build();

    /**
     * What JavaScript reads from a property that is not there. JSON's {@code null} is Java's.
     */
    public static final Object UNDEFINED = new Object() {
        @Override
        public String toString() {
            return "undefined";
        }
    };

    /**
     * The hole {@code delete array[i]} leaves: no key, {@link #UNDEFINED} when read.
     */
    public static final Object HOLE = new Object() {
        @Override
        public String toString() {
            return "undefined";
        }
    };

    /**
     * The characters {@code \s} matches in a JavaScript regular expression, for use inside a character class.
     */
    public static final String WHITESPACE_CLASS =
            "\\t\\n\\x0B\\f\\r \\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff";

    private static final List<String> OBJECT_PROTOTYPE = Arrays.asList("__defineGetter__", "__defineSetter__",
            "hasOwnProperty", "__lookupGetter__", "__lookupSetter__", "isPrototypeOf", "propertyIsEnumerable",
            "toString", "valueOf", "toLocaleString");

    private static final long MAX_ARRAY_INDEX = 4294967294L;
    private static final long MAX_SAFE_INTEGER = (1L << 53) - 1;
    private static final Pattern DECIMAL_LITERAL =
            Pattern.compile("[+-]?(?:Infinity|(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?)");
    private static final Pattern RADIX_INTEGER = Pattern.compile("0(?:[xX][0-9a-fA-F]+|[oO][0-7]+|[bB][01]+)");

    protected EcmaScript() {
    }

    public static boolean truthy(@Nullable Object value) {
        if (value == null || value == UNDEFINED || value == HOLE || Boolean.FALSE.equals(value)) {
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
     * {@code JSON.parse}: objects with their keys in the order JavaScript gives them, numbers as doubles.
     * @throws JsonSyntaxError for text that is not JSON, which each port raises as its own SyntaxError
     */
    public static @Nullable Object jsonParse(String text) {
        try {
            return jsonValue(MAPPER.readValue(text, Object.class));
        } catch (JsonProcessingException e) {
            throw new JsonSyntaxError(e);
        }
    }

    /**
     * A value Jackson read, as {@code JSON.parse} gives it.
     */
    @SuppressWarnings("unchecked")
    public static @Nullable Object jsonValue(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            Map<String, @Nullable Object> ordered = new LinkedHashMap<>();
            for (String key : keys(map)) {
                ordered.put(key, jsonValue(map.get(key)));
            }
            return ordered;
        }
        if (value instanceof List) {
            List<@Nullable Object> list = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                list.add(jsonValue(item));
            }
            return list;
        }
        if (value instanceof Number && !(value instanceof Double)) {
            return ((Number) value).doubleValue();
        }
        return value;
    }

    /**
     * {@code JSON.parse}'s SyntaxError, with Jackson's message; its cause's message adds the location.
     */
    public static final class JsonSyntaxError extends IllegalArgumentException {
        JsonSyntaxError(JsonProcessingException cause) {
            super(cause.getOriginalMessage(), cause);
        }
    }

    /**
     * {@code Object.keys}: the integer keys in ascending order, then the rest in insertion order.
     * @throws IllegalArgumentException for JavaScript's TypeError on null
     */
    public static List<String> keys(@Nullable Map<String, ?> object) {
        if (object == null) {
            throw new IllegalArgumentException("Cannot convert undefined or null to object");
        }
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
     * {@code Object.keys(value)}: an array's indices but its holes', a string's; none for a number or a boolean.
     * @throws IllegalArgumentException for JavaScript's TypeError on null or undefined
     */
    public static List<String> keys(@Nullable Object value) {
        if (value == null || value == UNDEFINED) {
            throw new IllegalArgumentException("Cannot convert undefined or null to object");
        }
        return ownKeys(value);
    }

    /**
     * The keys a spread or {@code for...in} enumerates: {@link #keys(Object)}, and none for null or undefined.
     */
    @SuppressWarnings("unchecked")
    public static List<String> ownKeys(@Nullable Object value) {
        if (value instanceof Map) {
            return keys((Map<String, ?>) value);
        }
        List<String> indices = new ArrayList<>();
        if (value instanceof List) {
            List<?> array = (List<?>) value;
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i) != HOLE) {
                    indices.add(String.valueOf(i));
                }
            }
        } else if (value instanceof String) {
            for (int i = 0; i < ((String) value).length(); i++) {
                indices.add(String.valueOf(i));
            }
        }
        return indices;
    }

    /**
     * {@code Object.entries(value)}: each of {@link #keys(Object)} with its value, a string's characters as values.
     * @throws JsError JavaScript's TypeError on null or undefined
     */
    @SuppressWarnings("unchecked")
    public static List<Map.Entry<String, @Nullable Object>> entries(@Nullable Object value) {
        if (value == null || value == UNDEFINED) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot convert undefined or null to object");
        }
        List<Map.Entry<String, @Nullable Object>> entries = new ArrayList<>();
        for (String key : ownKeys(value)) {
            Object v;
            if (value instanceof Map) {
                v = ((Map<String, @Nullable Object>) value).get(key);
            } else if (value instanceof List) {
                v = ((List<?>) value).get(Integer.parseInt(key));
            } else {
                v = String.valueOf(((String) value).charAt(Integer.parseInt(key)));
            }
            entries.add(new SimpleImmutableEntry<>(key, v));
        }
        return entries;
    }

    /**
     * {@code {}[key]} for a key a plain object does not own: what it inherits from {@code Object.prototype}, as
     * {@code String()} gives it, or null.
     */
    public static @Nullable String objectPrototypeMember(String key) {
        if ("__proto__".equals(key)) {
            return "[object Object]";
        }
        if ("constructor".equals(key)) {
            return "function Object() { [native code] }";
        }
        return OBJECT_PROTOTYPE.contains(key) ? "function " + key + "() { [native code] }" : null;
    }

    /**
     * {@code typeof}; {@code "object"} for null and for every object, of the model or a port's own.
     */
    public static String typeOf(@Nullable Object value) {
        if (value == UNDEFINED) {
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
     * {@code a === b}: numbers by value, strings and booleans by content, everything else by identity.
     */
    public static boolean strictEquals(@Nullable Object a, @Nullable Object b) {
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        if (a instanceof String || a instanceof Boolean) {
            return a.equals(b);
        }
        return a == b;
    }

    /**
     * {@code String(value)}: an array joined with commas, its null, undefined and holes empty, a byte array as a
     * {@code Uint8Array}, any other object {@code [object Object]}; a port's own sentinel by its {@code toString}.
     */
    public static String string(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof List) {
            StringBuilder out = new StringBuilder();
            List<?> array = (List<?>) value;
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) {
                    out.append(',');
                }
                Object element = array.get(i);
                if (element != null && element != UNDEFINED && element != HOLE) {
                    out.append(string(element));
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
        if (value instanceof Map) {
            return "[object Object]";
        }
        return value.toString();
    }

    /**
     * A {@code JSON.stringify} replacer function: what to write for the value a key holds.
     */
    @FunctionalInterface
    public interface JsonReplacer {
        @Nullable Object replace(String key, @Nullable Object value);
    }

    /**
     * {@code JSON.stringify(value)}, objects in {@link #keys(Map)} order; null where it gives undefined, as it does for
     * undefined and for anything that is not a JSON value.
     */
    public static @Nullable String jsonStringify(@Nullable Object value) {
        return jsonStringify(value, null, null);
    }

    /**
     * {@code JSON.stringify(value, replacer, space)}, {@code space} a number of spaces or an indent string.
     * @throws JsError JavaScript's TypeError for a circular structure, worded as V8 words it
     */
    public static @Nullable String jsonStringify(@Nullable Object value, @Nullable JsonReplacer replacer,
                                                 @Nullable Object space) {
        String gap = "";
        if (space instanceof Number) {
            double d = ((Number) space).doubleValue();
            gap = repeat(" ", Double.isNaN(d) ? 0 : (int) Math.max(0, Math.min(10, Math.floor(d))));
        } else if (space instanceof String) {
            gap = slice((String) space, 0, 10);
        }
        Map<String, @Nullable Object> wrapper = new LinkedHashMap<>();
        wrapper.put("", value);
        return new JsonStringifier(replacer, gap).property("", wrapper);
    }

    private static final class JsonStringifier {
        private final @Nullable JsonReplacer replacer;
        private final String gap;
        private final List<Object> stack = new ArrayList<>();
        private final List<String> stackKeys = new ArrayList<>();
        private String indent = "";

        JsonStringifier(@Nullable JsonReplacer replacer, String gap) {
            this.replacer = replacer;
            this.gap = gap;
        }

        // SerializeJSONProperty
        @SuppressWarnings("unchecked")
        @Nullable
        String property(String key, Object holder) {
            Object value = holder instanceof List ? ((List<?>) holder).get(Integer.parseInt(key)) :
                    ((Map<String, ?>) holder).get(key);
            if (value == HOLE) {
                value = UNDEFINED;
            }
            if (replacer != null) {
                value = replacer.replace(key, value);
            }
            if (value == null) {
                return "null";
            }
            if (value instanceof String) {
                return quote((String) value);
            }
            if (value instanceof Boolean) {
                return value.toString();
            }
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                return Double.isNaN(d) || Double.isInfinite(d) ? "null" : numberToString((Number) value);
            }
            if (value instanceof List || value instanceof Map) {
                enter(value, holder instanceof List ? "index " + key : key.isEmpty() ? "<anonymous>" :
                        "property '" + key + "'");
                String stepback = indent;
                indent = indent + gap;
                List<String> partial = new ArrayList<>();
                if (value instanceof List) {
                    for (int i = 0; i < ((List<?>) value).size(); i++) {
                        String element = property(String.valueOf(i), value);
                        partial.add(element == null ? "null" : element);
                    }
                } else {
                    for (String k : keys((Map<String, ?>) value)) {
                        String member = property(k, value);
                        if (member != null) {
                            partial.add(quote(k) + (gap.isEmpty() ? ":" : ": ") + member);
                        }
                    }
                }
                String open = value instanceof List ? "[" : "{";
                String close = value instanceof List ? "]" : "}";
                String out = partial.isEmpty() ? open + close : gap.isEmpty() ?
                        open + String.join(",", partial) + close :
                        open + "\n" + indent + String.join(",\n" + indent, partial) + "\n" + stepback + close;
                stack.remove(stack.size() - 1);
                stackKeys.remove(stackKeys.size() - 1);
                indent = stepback;
                return out;
            }
            return null;
        }

        // V8's message names the circle: its first two links, an ellipsis, its last link, then the key that closes it
        private void enter(Object value, String key) {
            for (int start = 0; start < stack.size(); start++) {
                if (stack.get(start) != value) {
                    continue;
                }
                StringBuilder message = new StringBuilder("Converting circular structure to JSON\n    --> starting " +
                                                          "at object with constructor ").append(constructor(value));
                int index = start + 1;
                int prefixEnd = Math.min(stack.size(), index + 2);
                for (; index < prefixEnd; index++) {
                    link(message, index);
                }
                if (stack.size() > index + 1) {
                    message.append("\n    |     ...");
                }
                for (index = Math.max(index, stack.size() - 1); index < stack.size(); index++) {
                    link(message, index);
                }
                throw new JsError(JsError.TYPE_ERROR, message.append("\n    --- ").append(key)
                        .append(" closes the circle").toString());
            }
            stack.add(value);
            stackKeys.add(key);
        }

        private void link(StringBuilder message, int index) {
            message.append("\n    |     ").append(stackKeys.get(index)).append(" -> object with constructor ")
                    .append(constructor(stack.get(index)));
        }

        private static String constructor(Object value) {
            return value instanceof List ? "'Array'" : "'Object'";
        }
    }

    /**
     * The canonical array index {@code key} names, or -1.
     */
    public static long arrayIndex(String key) {
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
     * ECMAScript white space and line terminators, which {@code trim}, {@code Number} and {@code \s} skip.
     */
    public static boolean isWhiteSpace(char c) {
        return c == '\t' || c == '\n' || c == 0x0B || c == '\f' || c == '\r' || c == ' ' || c == 0xA0 ||
               c == 0x1680 || (c >= 0x2000 && c <= 0x200A) || c == 0x2028 || c == 0x2029 || c == 0x202F ||
               c == 0x205F || c == 0x3000 || c == 0xFEFF;
    }

    public static String trim(String s) {
        return trimEnd(trimStart(s));
    }

    /**
     * {@code String.prototype.split} by a string.
     */
    public static String[] splitString(String s, String separator) {
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
     * {@code Number.isInteger}.
     */
    public static boolean isInteger(double d) {
        return !Double.isInfinite(d) && !Double.isNaN(d) && d == Math.floor(d);
    }

    /**
     * ToInt32, as {@code x >> 0} applies it.
     */
    public static int toInt32(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return 0;
        }
        return (int) (new BigDecimal(d).toBigInteger().longValue() & 0xFFFFFFFFL);
    }

    public static String trimStart(String s) {
        int start = 0;
        while (start < s.length() && isWhiteSpace(s.charAt(start))) {
            start++;
        }
        return s.substring(start);
    }

    public static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && isWhiteSpace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    /**
     * {@code Number(s)}: a decimal literal or {@code Infinity}, or a hexadecimal, octal or binary integer, between
     * white space; 0 for none, NaN for anything else.
     */
    public static double toNumber(String s) {
        String t = trim(s);
        if (t.isEmpty()) {
            return 0;
        }
        if (DECIMAL_LITERAL.matcher(t).matches()) {
            return Double.parseDouble(t);
        }
        if (RADIX_INTEGER.matcher(t).matches()) {
            char radix = Character.toLowerCase(t.charAt(1));
            return new BigInteger(t.substring(2), radix == 'x' ? 16 : radix == 'o' ? 8 : 2).doubleValue();
        }
        return Double.NaN;
    }

    /**
     * {@code Number(value)} of a value a port holds: null 0, a boolean 1 or 0, a string or an array by its text; NaN
     * for undefined and an object.
     */
    public static double toNumber(@Nullable Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        if (value instanceof String || value instanceof List) {
            return toNumber(string(value));
        }
        return Double.NaN;
    }

    /**
     * {@code parseInt(s, radix)} as V8 computes it, a radix of 0 standing for none ({@code 0x} then reads as
     * hexadecimal).
     */
    public static double parseInt(String s, int radix) {
        String t = trimStart(s);
        int i = 0;
        double sign = 1;
        if (!t.isEmpty() && (t.charAt(0) == '+' || t.charAt(0) == '-')) {
            sign = t.charAt(0) == '-' ? -1 : 1;
            i++;
        }
        int r = radix;
        boolean stripPrefix = r == 0 || r == 16;
        if (r == 0) {
            r = 10;
        } else if (r < 2 || r > 36) {
            return Double.NaN;
        }
        if (stripPrefix && (t.startsWith("0x", i) || t.startsWith("0X", i))) {
            i += 2;
            r = 16;
        }
        int start = i;
        while (i < t.length() && t.charAt(i) < 128 && Character.digit(t.charAt(i), r) >= 0) {
            i++;
        }
        if (i == start) {
            return Double.NaN;
        }
        while (start + 1 < i && t.charAt(start) == '0') {
            start++;
        }
        String digits = t.substring(start, i);
        if (r == 10) {
            // strtod over the first 310 digits
            return sign * new BigInteger(digits.substring(0, Math.min(digits.length(), 310))).doubleValue();
        }
        if (Integer.bitCount(r) == 1) {
            return sign * new BigInteger(digits, r).doubleValue();
        }
        // V8's generic radix: parts of up to 32 bits, each folded in by a multiply-add an arm64 build fuses
        double result = 0;
        int at = 0;
        while (at < digits.length() && !Double.isInfinite(result)) {
            long part = 0;
            long multiplier = 1;
            while (at < digits.length() && multiplier * r <= 0xFFFFFFFFL / 36) {
                part = part * r + Character.digit(digits.charAt(at++), r);
                multiplier *= r;
            }
            double fused = new BigDecimal(result).multiply(BigDecimal.valueOf(multiplier))
                    .add(BigDecimal.valueOf(part)).doubleValue();
            if (fused != result * multiplier + part) {
                throw new CannotKnow("parseInt(\"" + s + "\", " + radix + "), whose rounding depends on whether " +
                                     "the Node build fuses a multiply-add (an arm64 one does)");
            }
            result = fused;
        }
        return sign * result;
    }

    /**
     * {@code String.prototype.slice}: a negative index counts back from the end.
     */
    public static String slice(String s, int start, int end) {
        int from = start < 0 ? Math.max(s.length() + start, 0) : Math.min(start, s.length());
        int to = end < 0 ? Math.max(s.length() + end, 0) : Math.min(end, s.length());
        return from < to ? s.substring(from, to) : "";
    }

    public static String slice(String s, int start) {
        return slice(s, start, s.length());
    }

    /**
     * {@code String.prototype.repeat} for a count that is not negative.
     */
    public static String repeat(String s, int count) {
        StringBuilder out = new StringBuilder(Math.max(0, s.length() * count));
        for (int i = 0; i < count; i++) {
            out.append(s);
        }
        return out.toString();
    }

    /**
     * {@code Number.prototype.toString()} of a number parsed from JSON.
     */
    public static String numberToString(Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
            long l = n.longValue();
            return Math.abs(l) <= MAX_SAFE_INTEGER ? Long.toString(l) : numberToString((double) l);
        }
        return numberToString(n.doubleValue());
    }

    /**
     * {@code Number.prototype.toString()}.
     */
    public static String numberToString(double d) {
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

    // the shortest decimal that reads back as the same double, the nearest when several do, as V8 prints
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

    /**
     * {@code JSON.stringify} of a string.
     */
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        quote(s, sb);
        return sb.toString();
    }

    public static void quote(String s, StringBuilder sb) {
        sb.append('"');
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
                    if (c < 0x20 || isLoneSurrogate(s, i)) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
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
}
