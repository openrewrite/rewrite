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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of braces 1.8.5 with expand-range 1.8.2, fill-range 2.2.4 and preserve 0.2.0, as yarn 1.22.22 bundles them
 * for micromatch 2.3.11.
 */
final class Braces {

    static final class Options {
        @Nullable
        Boolean nodupes;
        boolean makeRe;
    }

    private static final Pattern PATTERN_RE = Pattern.compile("\\$\\{|( (?=[{,}])|(?=[{,}]) )|\\{}|\\{,}|" +
                                                              "\\\\,(?=" + Js.DOT + "*[{}])|/\\.(?=" + Js.DOT +
                                                              "*[{}])|\\\\\\.(?=\\{)|\\\\\\{|\\\\}");
    private static final Pattern BRACE_RE = Pattern.compile(Js.DOT + "*(\\\\?\\{([^}]+)})");
    private static final Pattern ES6_RE = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern NESTED = Pattern.compile("\\{[^{]+\\{");
    private static final Pattern QUOTE = Pattern.compile("['\"]");
    private static final Pattern DOT_SEGMENT = Pattern.compile("(\\.[^./])");
    private static final Pattern INNER_BRACES = Pattern.compile("\\{[^{}]+?}");
    private static final Pattern ESCAPED_DOTS = Pattern.compile("[^\\\\]\\." + Js.DOT + "+\\\\\\.");
    private static final Pattern WORD_COMMA = Pattern.compile("\\w,");
    private static final Pattern PRESERVED = Pattern.compile("__ID(" + Js.DOT + "{5})__");
    private static final Pattern STEP_RE = Pattern.compile("\\?|>|\\||\\+|~");
    private static final Pattern INVALID_RANGE = Pattern.compile("[.&*()\\[\\]^%$#@!]");
    private static final Pattern ALPHANUMERIC = Pattern.compile("[a-z0-9]", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOTH = Pattern.compile("[a-z][0-9]|[0-9][a-z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZEROS = Pattern.compile("^-*0+\\z");
    private static final Pattern PADDED = Pattern.compile("[^.]\\.|^-*0+[0-9]");
    private static final int MAX_RANGE = 1_000_000;
    private static final Set<Character> INVALID_CHARS = new HashSet<>(Arrays.asList('\\', '[', ']', '^', '(', ')',
            '`'));
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBER = "0123456789";
    private static final String SPECIAL = "~!@#$%^&()_+-={}[];',.";

    // preserve's tokens; it draws a random five-digit id for each
    private final Map<String, String> preserved = new HashMap<>();

    /**
     * The module's export: the expansions of {@code str}.
     */
    List<String> braces(String str, Options options) {
        return braces(str, null, options);
    }

    private List<String> braces(String str, @Nullable List<String> arrArg, @Nullable Options options) {
        if (str.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> arr = arrArg == null ? new ArrayList<>() : arrArg;
        Options opts = options == null ? new Options() : options;
        if (opts.nodupes == null) {
            opts.nodupes = true;
        }
        boolean es6 = false;

        Matcher matches = PATTERN_RE.matcher(str);
        String m = matches.find() ? matches.group() : "";
        switch (m) {
            case "\\,":
                return escapeCommas(str, arr, opts);
            case "\\.":
                return escapeDots(str, arr, opts);
            case "/.":
                return escapePaths(str, arr, opts);
            case " ":
                return splitWhitespace(str);
            case "{,}":
                return braces(Js.splitJoin(str, "{,}", ""), null, opts);
            case "{}":
                return braces(Js.splitJoin(str, "{}", "\\{\\}"), arr, opts);
            case "\\{":
            case "\\}":
                return escapeBraces(str, arr, opts);
            case "${":
                if (!NESTED.matcher(str).find()) {
                    return concat(arr, str);
                }
                es6 = true;
                str = JsRegExp.replace(str, ES6_RE, false, match -> {
                    String id = String.format("%05d", preserved.size());
                    preserved.put(id, match.group());
                    return "__ID" + id + "__";
                });
                break;
            default:
                break;
        }

        Matcher match = BRACE_RE.matcher(str);
        if (!match.find()) {
            return new ArrayList<>(Arrays.asList(str));
        }
        String outter = match.group(1);
        String inner = match.group(2);

        List<String> segs;
        int segsLength;
        if (inner.contains("..")) {
            Object range = expandRange(inner, opts);
            if (range == null) {
                segs = new ArrayList<>(Arrays.asList(Js.splitString(inner, ",")));
                segsLength = segs.size();
            } else if (range instanceof String) {
                segs = new ArrayList<>();
                for (char c : ((String) range).toCharArray()) {
                    segs.add(String.valueOf(c));
                }
                segsLength = ((String) range).length();
            } else {
                @SuppressWarnings("unchecked")
                List<String> expanded = (List<String>) range;
                segs = expanded;
                segsLength = segs.size();
            }
        } else if (inner.charAt(0) == '"' || inner.charAt(0) == '\'') {
            return concat(arr, QUOTE.matcher(str).replaceAll(""));
        } else {
            segs = new ArrayList<>(Arrays.asList(Js.splitString(inner, ",")));
            if (opts.makeRe) {
                return braces(JsRegExp.replace(str, outter, "(" + String.join("|", segs) + ")"), null, opts);
            }
            segsLength = segs.size();
        }

        for (String path : segs) {
            if (DOT_SEGMENT.matcher(path).find()) {
                return segsLength > 1 ? segs : new ArrayList<>(Arrays.asList(str));
            }
            String val = splice(str, outter, path);
            if (INNER_BRACES.matcher(val).find()) {
                arr = braces(val, arr, opts);
            } else if (!val.isEmpty()) {
                if (opts.nodupes && arr.contains(val)) {
                    continue;
                }
                arr.add(es6 ? after(val) : val);
            }
        }
        return arr;
    }

    /**
     * Whether a regex source still holds one of preserve's ids, which yarn draws from {@code Math.random()}.
     */
    boolean holdsPreservedId(String source) {
        for (String id : preserved.keySet()) {
            if (source.contains("__ID" + id + "__")) {
                return true;
            }
        }
        return false;
    }

    private String after(String str) {
        return JsRegExp.replace(str, PRESERVED, true, m -> {
            String token = preserved.get(m.group(1));
            return token == null ? "undefined" : token;
        });
    }

    private static List<String> concat(List<String> arr, String value) {
        List<String> out = new ArrayList<>(arr);
        out.add(value);
        return out;
    }

    private static List<String> map(List<String> arr, UnaryOperator<String> fn) {
        List<String> out = new ArrayList<>(arr.size());
        for (String ele : arr) {
            out.add(fn.apply(ele));
        }
        return out;
    }

    private List<String> splitWhitespace(String str) {
        List<String> res = new ArrayList<>();
        for (String seg : Js.splitString(str, " ")) {
            res.addAll(braces(seg, null, null));
        }
        return res;
    }

    private List<String> escapeBraces(String str, List<String> arr, Options opts) {
        if (!NESTED.matcher(str).find()) {
            return concat(arr, Js.splitJoin(str, "\\", ""));
        }
        str = Js.splitJoin(str, "\\{", "__LT_BRACE__");
        str = Js.splitJoin(str, "\\}", "__RT_BRACE__");
        return map(braces(str, arr, opts), ele -> Js.splitJoin(Js.splitJoin(ele, "__LT_BRACE__", "{"),
                "__RT_BRACE__", "}"));
    }

    private List<String> escapeDots(String str, List<String> arr, Options opts) {
        if (!ESCAPED_DOTS.matcher(str).find()) {
            return concat(arr, Js.splitJoin(str, "\\", ""));
        }
        str = Js.splitJoin(str, "\\.", "__ESC_DOT__");
        return map(braces(str, arr, opts), ele -> Js.splitJoin(ele, "__ESC_DOT__", "."));
    }

    private List<String> escapePaths(String str, List<String> arr, Options opts) {
        str = Js.splitJoin(str, "/.", "__ESC_PATH__");
        return map(braces(str, arr, opts), ele -> Js.splitJoin(ele, "__ESC_PATH__", "/."));
    }

    private List<String> escapeCommas(String str, List<String> arr, Options opts) {
        if (!WORD_COMMA.matcher(str).find()) {
            return concat(arr, Js.splitJoin(str, "\\", ""));
        }
        str = Js.splitJoin(str, "\\,", "__ESC_COMMA__");
        return map(braces(str, arr, opts), ele -> Js.splitJoin(ele, "__ESC_COMMA__", ","));
    }

    private static String splice(String str, String token, String replacement) {
        int i = str.indexOf(token);
        return str.substring(0, i) + replacement + str.substring(i + token.length());
    }

    /**
     * expand-range: the range's expansion, the range itself when it has more than three parts, or null.
     */
    private static @Nullable Object expandRange(String str, Options opts) {
        String[] args = Js.splitString(str, "..");
        if (args.length > 3) {
            return str;
        }
        return fillRange(args[0], args[1], args.length == 3 ? args[2] : null, opts);
    }

    /**
     * fill-range: the values from {@code a} to {@code b}, or one regex source for them under {@code makeRe}; null
     * for arguments it rejects (silently, as braces never asks it not to be).
     */
    private static @Nullable List<String> fillRange(String a, String b, @Nullable String stepArg, Options opts) {
        boolean expand = false;
        boolean regex = false;
        String sep = "";
        String step = stepArg == null || stepArg.isEmpty() ? null : stepArg;

        String origA = a;
        String origB = b;
        boolean bIsZero = "-0".equals(b);
        if (bIsZero) {
            b = "0";
        }

        if (opts.makeRe) {
            step = step != null ? step + "~" : null;
            expand = true;
            regex = true;
            sep = "~";
        }

        if (step != null) {
            Matcher match = STEP_RE.matcher(step);
            if (match.find()) {
                int i = match.start();
                String m = match.group();
                switch (m) {
                    case "+":
                        return repeat(a, b, bIsZero);
                    case "?":
                        return new ArrayList<>(Arrays.asList(randomize(a, b, bIsZero)));
                    case ">":
                        step = step.substring(0, i) + step.substring(i + 1);
                        expand = true;
                        break;
                    default:
                        step = step.substring(0, i) + step.substring(i + 1);
                        expand = true;
                        regex = true;
                        sep = m;
                        break;
                }
            } else if (!isNumber(step)) {
                return null;
            }
        }

        if (INVALID_RANGE.matcher(a).find() || INVALID_RANGE.matcher(b).find()) {
            return null;
        }
        if (!ALPHANUMERIC.matcher(a).find() || !ALPHANUMERIC.matcher(b).find() || BOTH.matcher(a).find() ||
            BOTH.matcher(b).find()) {
            return null;
        }

        boolean isNumA = isNumber(zeros(a));
        boolean isNumB = isNumber(zeros(b));
        if (isNumA != isNumB) {
            return null;
        }
        boolean isNum = isNumA;
        double num = step == null ? 0 : Math.abs((double) Js.toInt32(Js.toNumber(step)));
        if (num == 0) {
            num = 1;
        }

        double x;
        double y;
        if (isNum) {
            x = Js.toNumber(a);
            y = Js.toNumber(b);
        } else {
            x = a.charAt(0);
            y = b.charAt(0);
        }

        boolean isDescending = x > y;
        if (x < 0 || y < 0) {
            expand = false;
            regex = false;
        }

        Integer padTo = isPadded(origA, origB);
        if (regex && shouldExpand(x, y, num, isNum, padTo)) {
            if ("|".equals(sep) || "~".equals(sep)) {
                sep = detectSeparator(x, y, num, isNum, isDescending);
            }
            return wrap(Arrays.asList(origA, origB), sep);
        }

        if (Math.abs(y - x) / num > MAX_RANGE) {
            throw new CannotKnow("the expansion micromatch 2.3.11 makes of the range {" + origA + ".." + origB +
                                 "}, of more than " + MAX_RANGE + " values");
        }
        List<String> arr = new ArrayList<>();
        while (isDescending ? x >= y : x <= y) {
            String pad = padTo != null && isNum ? zeroes(padTo - Js.numberToString(x).length()) : null;
            String res;
            if (!isNum) {
                res = regex && INVALID_CHARS.contains((char) x) ? null : String.valueOf((char) x);
            } else {
                res = formatPadding(x, pad);
            }
            if (res != null) {
                arr.add(res);
            }
            if (isDescending) {
                x -= num;
            } else {
                x += num;
            }
        }

        if (regex || expand) {
            if ("|".equals(sep) || "~".equals(sep)) {
                sep = detectSeparator(x, y, num, isNum, isDescending);
            }
            if (arr.size() == 1 || x < 0 || y < 0) {
                return arr;
            }
            return wrap(arr, sep);
        }
        return arr;
    }

    private static List<String> wrap(List<String> arr, String sep) {
        if ("~".equals(sep)) {
            sep = "-";
        }
        String str = String.join(sep, arr);
        if ("|".equals(sep)) {
            str = "(" + str + ")";
        }
        if ("-".equals(sep)) {
            str = "[" + str + "]";
        }
        return new ArrayList<>(Arrays.asList(str));
    }

    private static boolean shouldExpand(double a, double b, double num, boolean isNum, @Nullable Integer padTo) {
        if (isNum && (a > 9 || b > 9)) {
            return false;
        }
        return padTo == null && num == 1 && a < b;
    }

    private static String detectSeparator(double a, double b, double step, boolean isNum, boolean isDescending) {
        boolean isChar;
        if (isDescending) {
            isChar = false;
        } else if (isNum) {
            isChar = a <= 9 && b <= 9;
        } else {
            isChar = a < b && step == 1;
        }
        return isChar ? "~" : "|";
    }

    private static String formatPadding(double ch, @Nullable String pad) {
        String digits = Js.numberToString(ch);
        if (pad == null || pad.isEmpty()) {
            return digits;
        }
        if (digits.startsWith("-")) {
            return "-" + pad + digits.substring(1);
        }
        return pad + digits;
    }

    private static String zeros(String val) {
        return ZEROS.matcher(val).find() ? "0" : val;
    }

    // the length every padded value is padded to, null when neither end is padded
    private static @Nullable Integer isPadded(String origA, String origB) {
        if (PADDED.matcher(origA).find() || PADDED.matcher(origB).find()) {
            return Math.max(origA.length(), origB.length());
        }
        return null;
    }

    private static String zeroes(int n) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) {
            out.append('0');
        }
        return out.toString();
    }

    // is-number 2.1.0 of a string
    private static boolean isNumber(String num) {
        double n = Js.toNumber(num);
        return !Double.isNaN(n) && !Double.isInfinite(n) && !num.isEmpty();
    }

    // repeat-element of the range's end, which fill-range passes as the string it holds
    private static List<String> repeat(String ele, String num, boolean zero) {
        if (zero) {
            return new ArrayList<>();
        }
        double n = Js.toNumber(num);
        List<String> arr = new ArrayList<>(Arrays.asList(num));
        if (n > MAX_RANGE) {
            throw new CannotKnow("the expansion micromatch 2.3.11 makes of the step + over " + num + " repetitions");
        }
        for (int i = 0; i < n; i++) {
            if (i < arr.size()) {
                arr.set(i, ele);
            } else {
                arr.add(ele);
            }
        }
        return arr;
    }

    // randomatic 3.0.0 of a mask pattern and a length: random unless every draw is the same character
    private static String randomize(String pattern, String length, boolean zero) {
        double n = zero ? 0 : Js.toNumber(length);
        if (Double.isNaN(n) || n == 0) {
            return "";
        }
        StringBuilder mask = new StringBuilder();
        if (pattern.contains("?")) {
            mask.append("undefined");
        }
        if (pattern.contains("a")) {
            mask.append(LOWER);
        }
        if (pattern.contains("A")) {
            mask.append(UPPER);
        }
        if (pattern.contains("0")) {
            mask.append(NUMBER);
        }
        if (pattern.contains("!")) {
            mask.append(SPECIAL);
        }
        if (pattern.contains("*")) {
            mask.append(LOWER).append(UPPER).append(NUMBER).append(SPECIAL);
        }
        if (n < 0 || n != Math.floor(n)) {
            throw new CannotKnow("what micromatch 2.3.11 makes of the range step ? with a length of " + length +
                                 ", which never ends");
        }
        if (mask.length() == 0) {
            return "";
        }
        throw new CannotKnow("the random characters micromatch 2.3.11 draws for the range {" + pattern + ".." +
                             length + "..?}");
    }
}
