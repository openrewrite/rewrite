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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of fill-range over the text of a range, as braces asks it: 2.2.4 with is-number 2.1.0, repeat-element 1.1.2
 * and randomatic 3.0.0's determinism (yarn 1.22.22's expand-range 1.8.2), and 7.0.1 (braces 3.0.2, Yarn 4.10.3).
 */
public final class FillRange {

    public enum Release {
        V2_2_4,
        V7_0_1
    }

    @Value
    public static class Options {
        Release release;

        /**
         * 2.2.4's {@code makeRe}: one regex source for the range where it can.
         */
        boolean makeRe;
    }

    private static final double MAX_SAFE_INTEGER = 9007199254740991d;
    private static final int MAX_VALUES = 10000;

    private static final Pattern STEP_RE = V8RegExp.of("\\?|>|\\||\\+|\\~");
    private static final Pattern INVALID_RANGE = V8RegExp.of("[.&*()[\\]^%$#@!]");
    private static final Pattern ALPHANUMERIC = Pattern.compile("[a-z0-9]", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOTH = Pattern.compile("[a-z][0-9]|[0-9][a-z]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZEROS = V8RegExp.of("^-*0+$");
    private static final Pattern PADDED = V8RegExp.of("[^.]\\.|^-*0+[0-9]");
    private static final int MAX_RANGE = 1_000_000;
    private static final Set<Character> INVALID_CHARS = new HashSet<>(Arrays.asList('\\', '[', ']', '^', '(', ')',
            '`'));
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBER = "0123456789";
    private static final String SPECIAL = "~!@#$%^&()_+-={}[];',.";

    private FillRange() {
    }

    /**
     * The range's values, or under 2.2.4's {@code makeRe} one regex source for them; null where 2.2.4 rejects its
     * arguments, empty where 7.0.1 does; CannotKnow where JavaScript cannot count the range exactly.
     */
    public static @Nullable List<String> fill(String start, String end, @Nullable String step, Options options) {
        return options.getRelease() == Release.V2_2_4 ? fill224(start, end, step, options.isMakeRe()) :
                fill701(start, end, step);
    }

    private static List<String> fill701(String start, String end, @Nullable String stepArg) {
        if (start.isEmpty() || end.isEmpty()) {
            return Collections.emptyList();
        }
        String step = stepArg != null && !stepArg.isEmpty() ? stepArg : null;
        if (step != null && !isNumber701(step)) {
            return Collections.emptyList();
        }
        double stepValue = Math.max(Math.abs(step == null ? 1 : EcmaScript.toNumber(step)), 1);
        if (isNumber701(start) && isNumber701(end)) {
            return fillNumbers(start, end, step == null ? "1" : step, stepValue);
        }
        return fillLetters(start, end, stepValue);
    }

    private static List<String> fillNumbers(String start, String end, String stepString, double step) {
        double a = EcmaScript.toNumber(start);
        double b = EcmaScript.toNumber(end);
        if (Math.abs(a) > MAX_SAFE_INTEGER || Math.abs(b) > MAX_SAFE_INTEGER || step > MAX_SAFE_INTEGER) {
            throw new CannotKnow("the brace range {" + start + ".." + end + "} (beyond JavaScript's safe integers)");
        }
        if (Math.abs(b - a) / step >= MAX_VALUES) {
            throw new CannotKnow("the brace range {" + start + ".." + end + "} (more than " + MAX_VALUES +
                                 " values)");
        }
        boolean descending = a > b;
        boolean padded = zeros701(start) || zeros701(end) || zeros701(stepString);
        int maxLen = padded ? Math.max(start.length(), Math.max(end.length(), stepString.length())) : 0;
        List<String> range = new ArrayList<>();
        long value = (long) a;
        long last = (long) b;
        long by = (long) step;
        while (descending ? value >= last : value <= last) {
            range.add(pad(Long.toString(value), maxLen));
            value = descending ? value - by : value + by;
        }
        return range;
    }

    private static List<String> fillLetters(String start, String end, double step) {
        if ((!isNumber701(start) && start.length() > 1) || (!isNumber701(end) && end.length() > 1)) {
            return Collections.emptyList();
        }
        double a = start.charAt(0);
        double b = end.charAt(0);
        boolean descending = a > b;
        List<String> range = new ArrayList<>();
        while (descending ? a >= b : a <= b) {
            range.add(String.valueOf((char) a));
            a = descending ? a - step : a + step;
        }
        return range;
    }

    // is-number 7.0.0 of a string, as fill-range 7.0.1 inlines it
    private static boolean isNumber701(String num) {
        return EcmaScript.isInteger(EcmaScript.toNumber(num));
    }

    private static boolean zeros701(String input) {
        String value = input.startsWith("-") ? input.substring(1) : input;
        if ("0".equals(value)) {
            return false;
        }
        int index = 0;
        while (index < value.length() && value.charAt(index) == '0') {
            index++;
        }
        return index > 0;
    }

    private static String pad(String input, int maxLength) {
        if (maxLength > 0) {
            String dash = input.startsWith("-") ? "-" : "";
            String digits = dash.isEmpty() ? input : input.substring(1);
            StringBuilder out = new StringBuilder(dash);
            for (int i = digits.length(); i < (dash.isEmpty() ? maxLength : maxLength - 1); i++) {
                out.append('0');
            }
            return out.append(digits).toString();
        }
        return input;
    }

    private static @Nullable List<String> fill224(String a, String b, @Nullable String stepArg, boolean makeRe) {
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

        if (makeRe) {
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
            } else if (!isNumber210(step)) {
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

        boolean isNumA = isNumber210(zeros224(a));
        boolean isNumB = isNumber210(zeros224(b));
        if (isNumA != isNumB) {
            return null;
        }
        boolean isNum = isNumA;
        double num = step == null ? 0 : Math.abs((double) EcmaScript.toInt32(EcmaScript.toNumber(step)));
        if (num == 0) {
            num = 1;
        }

        double x;
        double y;
        if (isNum) {
            x = EcmaScript.toNumber(a);
            y = EcmaScript.toNumber(b);
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
            String pad = padTo != null && isNum ? zeroes(padTo - EcmaScript.numberToString(x).length()) : null;
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
        String digits = EcmaScript.numberToString(ch);
        if (pad == null || pad.isEmpty()) {
            return digits;
        }
        if (digits.startsWith("-")) {
            return "-" + pad + digits.substring(1);
        }
        return pad + digits;
    }

    private static String zeros224(String val) {
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
    private static boolean isNumber210(String num) {
        double n = EcmaScript.toNumber(num);
        return !Double.isNaN(n) && !Double.isInfinite(n) && !num.isEmpty();
    }

    // repeat-element of the range's end, which fill-range passes as the string it holds
    private static List<String> repeat(String ele, String num, boolean zero) {
        if (zero) {
            return new ArrayList<>();
        }
        double n = EcmaScript.toNumber(num);
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
        double n = zero ? 0 : EcmaScript.toNumber(length);
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
        // while (length--) ends only on zero
        if (n < 0 || n != Math.floor(n) || Double.isInfinite(n)) {
            throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, (mask.length() == 0 ? "" :
                    "FATAL ERROR: Reached heap limit Allocation failed - JavaScript heap out of memory: ") +
                    "the range {" + pattern + ".." + length + "..?} never ends");
        }
        if (mask.length() == 0) {
            return "";
        }
        throw new CannotKnow("the random characters micromatch 2.3.11 draws for the range {" + pattern + ".." +
                             length + "..?}");
    }
}
