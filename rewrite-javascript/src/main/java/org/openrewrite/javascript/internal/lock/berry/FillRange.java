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

import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of fill-range 7.0.1 as braces 3.0.2 expands a range.
 */
final class FillRange {

    private static final double MAX_SAFE_INTEGER = 9007199254740991d;
    private static final int MAX_VALUES = 10000;

    private FillRange() {
    }

    /**
     * @param args the range's bounds and its step, as text
     * @throws CannotKnow for bounds JavaScript cannot count exactly or a range of more than ten thousand values
     */
    static List<String> fill(List<String> args) {
        if (args.size() < 2 || args.get(0).isEmpty() || args.get(1).isEmpty()) {
            return Collections.emptyList();
        }
        String start = args.get(0);
        String end = args.get(1);
        String step = args.size() > 2 && !args.get(2).isEmpty() ? args.get(2) : null;
        if (step != null && !isNumber(step)) {
            return Collections.emptyList();
        }
        double stepValue = Math.max(Math.abs(step == null ? 1 : Js.toNumber(step)), 1);
        if (isNumber(start) && isNumber(end)) {
            return fillNumbers(start, end, step == null ? "1" : step, stepValue);
        }
        return fillLetters(start, end, stepValue);
    }

    private static List<String> fillNumbers(String start, String end, String stepString, double step) {
        double a = Js.toNumber(start);
        double b = Js.toNumber(end);
        if (Math.abs(a) > MAX_SAFE_INTEGER || Math.abs(b) > MAX_SAFE_INTEGER || step > MAX_SAFE_INTEGER) {
            throw new CannotKnow("the brace range {" + start + ".." + end + "} (beyond JavaScript's safe integers)");
        }
        if (Math.abs(b - a) / step >= MAX_VALUES) {
            throw new CannotKnow("the brace range {" + start + ".." + end + "} (more than " + MAX_VALUES +
                                 " values)");
        }
        boolean descending = a > b;
        boolean padded = zeros(start) || zeros(end) || zeros(stepString);
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
        if ((!isNumber(start) && start.length() > 1) || (!isNumber(end) && end.length() > 1)) {
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

    private static boolean isNumber(String num) {
        return Js.isInteger(Js.toNumber(num));
    }

    private static boolean zeros(String input) {
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
}
