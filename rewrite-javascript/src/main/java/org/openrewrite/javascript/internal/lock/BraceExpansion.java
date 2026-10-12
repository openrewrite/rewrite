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
import java.util.List;
import java.util.regex.Pattern;

/**
 * Port of brace-expansion with balanced-match, {@code a{b,c}d} to {@code abd acd}: 1.1.11 with 1.0.0, which minimatch
 * 3.0.4 calls in yarn 1.22.22, and 5.0.6 and 5.0.9 with 4.0.4, which minimatch 10.2.5 calls in npm 11.17.0 and in npm
 * 11.19.1. 5.0.7 expands as 5.0.6 does, without the depth of stack that 5.0.6 needs for a run of sets that do not
 * expand.
 */
public final class BraceExpansion {

    public enum Release {
        V1_1_11,
        V5_0_6,
        V5_0_9
    }

    private static final int EXPANSION_MAX = 100_000;

    // 5.0.9 bounds the characters an expansion may hold at any point (CVE-2026-14257)
    private static final int EXPANSION_MAX_LENGTH = 4_000_000;

    private static final String ESC_SLASH = "\0SLASH\0";
    private static final String ESC_OPEN = "\0OPEN\0";
    private static final String ESC_CLOSE = "\0CLOSE\0";
    private static final String ESC_COMMA = "\0COMMA\0";
    private static final String ESC_PERIOD = "\0PERIOD\0";

    private static final Pattern NUMERIC_SEQUENCE = V8RegExp.of("^-?\\d+\\.\\.-?\\d+(?:\\.\\.-?\\d+)?$");
    private static final Pattern ALPHA_SEQUENCE = V8RegExp.of("^[a-zA-Z]\\.\\.[a-zA-Z](?:\\.\\.-?\\d+)?$");
    private static final Pattern COMMA_THEN_CLOSE_1_1_11 = V8RegExp.of(",.*\\}");
    private static final Pattern COMMA_THEN_CLOSE_5_0_6 = V8RegExp.of(",(?!,).*\\}");
    private static final Pattern PADDED = V8RegExp.of("^-?0\\d");
    private static final Pattern DOTS = V8RegExp.of("\\.\\.");

    private BraceExpansion() {
    }

    @Value
    static class Balanced {
        String pre;
        String body;
        String post;
    }

    static @Nullable Balanced balanced(String str) {
        int[] r = range(str);
        return r == null ? null : new Balanced(str.substring(0, r[0]), str.substring(r[0] + 1, r[1]),
                                               str.substring(r[1] + 1));
    }

    // balanced-match's range of '{' and '}', the same in 1.0.0 and 4.0.4 for two different characters
    static int @Nullable [] range(String str) {
        List<Integer> begs = new ArrayList<>();
        int ai = str.indexOf('{');
        int bi = str.indexOf('}', ai + 1);
        int i = ai;
        int left = str.length();
        int right = -1;
        int[] result = null;
        if (ai >= 0 && bi > 0) {
            while (i >= 0 && result == null) {
                if (i == ai) {
                    begs.add(i);
                    ai = str.indexOf('{', i + 1);
                } else if (begs.size() == 1) {
                    result = new int[]{begs.remove(begs.size() - 1), bi};
                } else {
                    int beg = begs.remove(begs.size() - 1);
                    if (beg < left) {
                        left = beg;
                        right = bi;
                    }
                    bi = str.indexOf('}', i + 1);
                }
                i = ai < bi && ai >= 0 ? ai : bi;
            }
            if (!begs.isEmpty() && right != -1) {
                result = new int[]{left, right};
            }
        }
        return result;
    }

    // 1.1.11's parseInt(str, 10) == str and 5.0.6's !isNaN(str) agree on the parts a sequence's pattern lets through
    private static double numeric(String str) {
        double parsed = EcmaScript.parseInt(str, 10);
        return Double.isNaN(parsed) ? str.charAt(0) : parsed;
    }

    private static String escapeBraces(String str) {
        return str.replace("\\\\", ESC_SLASH).replace("\\{", ESC_OPEN).replace("\\}", ESC_CLOSE)
                .replace("\\,", ESC_COMMA).replace("\\.", ESC_PERIOD);
    }

    private static String unescapeBraces(String str) {
        return str.replace(ESC_SLASH, "\\").replace(ESC_OPEN, "{").replace(ESC_CLOSE, "}")
                .replace(ESC_COMMA, ",").replace(ESC_PERIOD, ".");
    }

    private static List<String> parseCommaParts(String str) {
        if (str.isEmpty()) {
            return new ArrayList<>(Collections.singletonList(""));
        }
        Balanced m = balanced(str);
        if (m == null) {
            return new ArrayList<>(Arrays.asList(str.split(",", -1)));
        }
        List<String> p = new ArrayList<>(Arrays.asList(m.getPre().split(",", -1)));
        p.set(p.size() - 1, p.get(p.size() - 1) + "{" + m.getBody() + "}");
        List<String> postParts = parseCommaParts(m.getPost());
        if (!m.getPost().isEmpty()) {
            p.set(p.size() - 1, p.get(p.size() - 1) + postParts.remove(0));
            p.addAll(postParts);
        }
        return p;
    }

    public static List<String> expand(String str, Release release) {
        if (str.isEmpty()) {
            return Collections.emptyList();
        }
        if (str.startsWith("{}")) {
            str = "\\{\\}" + str.substring(2);
        }
        List<String> out = new ArrayList<>();
        for (String s : release == Release.V5_0_9 ? expand509(escapeBraces(str), true) :
                expand(escapeBraces(str), true, release)) {
            out.add(unescapeBraces(s));
        }
        return out;
    }

    // 5.0.9's combine: acc[a] + pre + values[v] for every pair, at most EXPANSION_MAX results and EXPANSION_MAX_LENGTH
    // characters
    private static List<String> combine(List<String> acc, String pre, List<String> values, boolean dropEmpties) {
        List<String> out = new ArrayList<>();
        int length = 0;
        for (String a : acc) {
            for (String v : values) {
                if (out.size() >= EXPANSION_MAX) {
                    return out;
                }
                String expansion = a + pre + v;
                if (dropEmpties && expansion.isEmpty()) {
                    continue;
                }
                if (length + expansion.length() > EXPANSION_MAX_LENGTH) {
                    return out;
                }
                out.add(expansion);
                length += expansion.length();
            }
        }
        return out;
    }

    // 5.0.9's expandSequence: the values of a numeric (1..5) or alphabetic (a..e..2) sequence body
    private static List<String> expandSequence(String body, boolean isAlphaSequence) {
        List<String> n = Arrays.asList(DOTS.split(body, -1));
        List<String> values = new ArrayList<>();
        double x = numeric(n.get(0));
        double y = numeric(n.get(1));
        int width = Math.max(n.get(0).length(), n.get(1).length());
        double incr = n.size() == 3 ? Math.max(Math.abs(numeric(n.get(2))), 1) : 1;
        boolean reverse = y < x;
        if (reverse) {
            incr *= -1;
        }
        boolean pad = false;
        for (String el : n) {
            pad |= PADDED.matcher(el).find();
        }
        int length = 0;
        for (double i = x; (reverse ? i >= y : i <= y) && values.size() < EXPANSION_MAX; i += incr) {
            String c;
            if (isAlphaSequence) {
                c = String.valueOf((char) EcmaScript.toInt32(i));
                if ("\\".equals(c)) {
                    c = "";
                }
            } else {
                c = EcmaScript.numberToString(i);
                if (pad) {
                    int need = width - c.length();
                    if (need > 0) {
                        String z = EcmaScript.repeat("0", need);
                        c = i < 0 ? "-" + z + c.substring(1) : z + c;
                    }
                }
            }
            if (length + c.length() > EXPANSION_MAX_LENGTH) {
                break;
            }
            values.add(c);
            length += c.length();
        }
        return values;
    }

    // 5.0.9's expand_: the top-level sets are combined left to right into one accumulator, and an empty result is
    // dropped by the first set alone
    private static List<String> expand509(String str, boolean isTop) {
        List<String> acc = new ArrayList<>(Collections.singletonList(""));
        List<String> literal = Collections.singletonList("");
        boolean dropEmpties = false;
        boolean firstGroup = true;
        while (true) {
            Balanced m = balanced(str);
            if (m == null) {
                return combine(acc, str, literal, dropEmpties);
            }
            String pre = m.getPre();
            boolean last = m.getPost().isEmpty();
            if (pre.endsWith("$")) {
                acc = combine(acc, pre + "{" + m.getBody() + "}", literal, dropEmpties && last);
                firstGroup = false;
                if (last) {
                    break;
                }
                str = m.getPost();
                continue;
            }
            boolean isNumericSequence = NUMERIC_SEQUENCE.matcher(m.getBody()).find();
            boolean isAlphaSequence = ALPHA_SEQUENCE.matcher(m.getBody()).find();
            boolean isSequence = isNumericSequence || isAlphaSequence;
            boolean isOptions = m.getBody().contains(",");
            if (!isSequence && !isOptions) {
                if (COMMA_THEN_CLOSE_5_0_6.matcher(m.getPost()).find()) {
                    str = pre + "{" + m.getBody() + ESC_CLOSE + m.getPost();
                    isTop = true;
                    continue;
                }
                return combine(acc, pre + "{" + m.getBody() + "}" + m.getPost(), literal, dropEmpties);
            }
            if (firstGroup) {
                dropEmpties = isTop && !isSequence;
                firstGroup = false;
            }
            List<String> values;
            if (isSequence) {
                values = expandSequence(m.getBody(), isAlphaSequence);
            } else {
                List<String> n = parseCommaParts(m.getBody());
                if (n.size() == 1) {
                    List<String> embraced = new ArrayList<>();
                    for (String e : expand509(n.get(0), false)) {
                        embraced.add("{" + e + "}");
                    }
                    n = embraced;
                    if (n.size() == 1) {
                        acc = combine(acc, pre + n.get(0), literal, dropEmpties && last);
                        if (last) {
                            break;
                        }
                        str = m.getPost();
                        continue;
                    }
                }
                // a value combine would drop as empty does not count against the maximum
                boolean dropsEmpties = dropEmpties && last && pre.isEmpty();
                for (int d = 0; dropsEmpties && d < acc.size(); d++) {
                    if (!acc.get(d).isEmpty()) {
                        dropsEmpties = false;
                    }
                }
                values = new ArrayList<>();
                int valuesLength = 0;
                outer:
                for (String part : n) {
                    for (String v : expand509(part, false)) {
                        if (dropsEmpties && v.isEmpty()) {
                            continue;
                        }
                        if (values.size() >= EXPANSION_MAX || valuesLength + v.length() > EXPANSION_MAX_LENGTH) {
                            break outer;
                        }
                        values.add(v);
                        valuesLength += v.length();
                    }
                }
            }
            acc = combine(acc, pre, values, dropEmpties && last);
            if (last) {
                break;
            }
            str = m.getPost();
        }
        return acc;
    }

    private static List<String> expand(String str, boolean isTop, Release release) {
        boolean v5 = release == Release.V5_0_6;
        int max = v5 ? EXPANSION_MAX : Integer.MAX_VALUE;
        List<String> expansions = new ArrayList<>();
        Balanced m = balanced(str);
        if (m == null || !v5 && m.getPre().endsWith("$")) {
            return new ArrayList<>(Collections.singletonList(str));
        }
        String pre = m.getPre();
        // 5.0.6 expands the rest before the body, 1.1.11 only once the body is a sequence or a set
        List<String> post = v5 ? post(m, release) : Collections.emptyList();
        if (pre.endsWith("$")) {
            for (int k = 0; k < post.size() && k < max; k++) {
                expansions.add(pre + "{" + m.getBody() + "}" + post.get(k));
            }
            return expansions;
        }
        boolean isNumericSequence = NUMERIC_SEQUENCE.matcher(m.getBody()).find();
        boolean isAlphaSequence = ALPHA_SEQUENCE.matcher(m.getBody()).find();
        boolean isSequence = isNumericSequence || isAlphaSequence;
        boolean isOptions = m.getBody().contains(",");
        if (!isSequence && !isOptions) {
            if ((v5 ? COMMA_THEN_CLOSE_5_0_6 : COMMA_THEN_CLOSE_1_1_11).matcher(m.getPost()).find()) {
                // 1.1.11 calls expand(str), whose isTop is undefined
                return expand(pre + "{" + m.getBody() + ESC_CLOSE + m.getPost(), v5, release);
            }
            return new ArrayList<>(Collections.singletonList(str));
        }
        List<String> n;
        if (isSequence) {
            n = Arrays.asList(DOTS.split(m.getBody(), -1));
        } else {
            n = parseCommaParts(m.getBody());
            if (n.size() == 1) {
                List<String> embraced = new ArrayList<>();
                for (String e : expand(n.get(0), false, release)) {
                    embraced.add("{" + e + "}");
                }
                n = embraced;
                if (n.size() == 1) {
                    List<String> out = new ArrayList<>();
                    for (String p : v5 ? post : post(m, release)) {
                        out.add(pre + n.get(0) + p);
                    }
                    return out;
                }
            }
        }
        if (!v5) {
            post = post(m, release);
        }
        List<String> big = new ArrayList<>();
        if (isSequence) {
            double x = numeric(n.get(0));
            double y = numeric(n.get(1));
            int width = Math.max(n.get(0).length(), n.get(1).length());
            double incr = n.size() == 3 ? Math.abs(numeric(n.get(2))) : 1;
            if (v5) {
                incr = Math.max(incr, 1);
            }
            boolean reverse = y < x;
            if (reverse) {
                incr *= -1;
            }
            boolean pad = false;
            for (String el : n) {
                pad |= PADDED.matcher(el).find();
            }
            for (double i = x; (reverse ? i >= y : i <= y) && big.size() < max; i += incr) {
                if (!v5 && i + incr == i) {
                    throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "FATAL ERROR: Reached heap limit " +
                            "Allocation failed - JavaScript heap out of memory: the range {" + m.getBody() +
                            "} never ends");
                }
                String c;
                if (isAlphaSequence) {
                    c = String.valueOf((char) EcmaScript.toInt32(i));
                    if ("\\".equals(c)) {
                        c = "";
                    }
                } else {
                    c = EcmaScript.numberToString(i);
                    if (pad) {
                        int need = width - c.length();
                        if (need > 0) {
                            String z = EcmaScript.repeat("0", need);
                            c = i < 0 ? "-" + z + c.substring(1) : z + c;
                        }
                    }
                }
                big.add(c);
            }
        } else {
            for (String part : n) {
                big.addAll(expand(part, false, release));
            }
        }
        for (String value : big) {
            for (int k = 0; k < post.size() && expansions.size() < max; k++) {
                String expansion = pre + value + post.get(k);
                if (!isTop || isSequence || !expansion.isEmpty()) {
                    expansions.add(expansion);
                }
            }
        }
        return expansions;
    }

    private static List<String> post(Balanced m, Release release) {
        return m.getPost().isEmpty() ? Collections.singletonList("") : expand(m.getPost(), false, release);
    }
}
