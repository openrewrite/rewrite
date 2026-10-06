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

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Port of brace-expansion 5.0.6 (with balanced-match 4.0.4): {@code a{b,c}d} to {@code abd acd}.
 */
final class BraceExpansion {

    private static final int EXPANSION_MAX = 100_000;

    private static final String ESC_SLASH = "\0SLASH\0";
    private static final String ESC_OPEN = "\0OPEN\0";
    private static final String ESC_CLOSE = "\0CLOSE\0";
    private static final String ESC_COMMA = "\0COMMA\0";
    private static final String ESC_PERIOD = "\0PERIOD\0";

    private static final Pattern NUMERIC_SEQUENCE = Pattern.compile("^-?\\d+\\.\\.-?\\d+(?:\\.\\.-?\\d+)?$");
    private static final Pattern ALPHA_SEQUENCE = Pattern.compile("^[a-zA-Z]\\.\\.[a-zA-Z](?:\\.\\.-?\\d+)?$");
    private static final Pattern POST_HAS_COMMA_BRACE = Pattern.compile(",(?!,).*}");
    private static final Pattern PADDED = Pattern.compile("^-?0\\d");
    private static final Pattern DOTS = Pattern.compile("\\.\\.");

    private BraceExpansion() {
    }

    @Value
    private static class Balanced {
        String pre;
        String body;
        String post;
    }

    private static @Nullable Balanced balanced(String str) {
        int[] r = range(str);
        return r == null ? null : new Balanced(str.substring(0, r[0]), str.substring(r[0] + 1, r[1]), str.substring(r[1] + 1));
    }

    private static int @Nullable [] range(String str) {
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

    private static int numeric(String str) {
        return !Double.isNaN(Js.toNumber(str)) ? (int) Double.parseDouble(str.trim()) : str.charAt(0);
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

    static List<String> expand(String str) {
        if (str.isEmpty()) {
            return Collections.emptyList();
        }
        if (str.startsWith("{}")) {
            str = "\\{\\}" + str.substring(2);
        }
        List<String> out = new ArrayList<>();
        for (String s : expand(escapeBraces(str), true)) {
            out.add(unescapeBraces(s));
        }
        return out;
    }

    private static List<String> expand(String str, boolean isTop) {
        List<String> expansions = new ArrayList<>();
        Balanced m = balanced(str);
        if (m == null) {
            return Collections.singletonList(str);
        }
        String pre = m.getPre();
        List<String> post = m.getPost().isEmpty() ? Collections.singletonList("") : expand(m.getPost(), false);
        if (pre.endsWith("$")) {
            for (int k = 0; k < post.size() && k < EXPANSION_MAX; k++) {
                expansions.add(pre + "{" + m.getBody() + "}" + post.get(k));
            }
            return expansions;
        }
        boolean isNumericSequence = NUMERIC_SEQUENCE.matcher(m.getBody()).find();
        boolean isAlphaSequence = ALPHA_SEQUENCE.matcher(m.getBody()).find();
        boolean isSequence = isNumericSequence || isAlphaSequence;
        boolean isOptions = m.getBody().contains(",");
        if (!isSequence && !isOptions) {
            if (POST_HAS_COMMA_BRACE.matcher(m.getPost()).find()) {
                return expand(pre + "{" + m.getBody() + ESC_CLOSE + m.getPost(), true);
            }
            return Collections.singletonList(str);
        }
        List<String> n;
        if (isSequence) {
            n = Arrays.asList(DOTS.split(m.getBody(), -1));
        } else {
            n = parseCommaParts(m.getBody());
            if (n.size() == 1) {
                List<String> embraced = new ArrayList<>();
                for (String e : expand(n.get(0), false)) {
                    embraced.add("{" + e + "}");
                }
                n = embraced;
                if (n.size() == 1) {
                    List<String> out = new ArrayList<>();
                    for (String p : post) {
                        out.add(pre + n.get(0) + p);
                    }
                    return out;
                }
            }
        }
        List<String> big = new ArrayList<>();
        if (isSequence && n.size() >= 2) {
            int x = numeric(n.get(0));
            int y = numeric(n.get(1));
            int width = Math.max(n.get(0).length(), n.get(1).length());
            int incr = n.size() == 3 ? Math.max(Math.abs(numeric(n.get(2))), 1) : 1;
            boolean reverse = y < x;
            if (reverse) {
                incr *= -1;
            }
            boolean pad = false;
            for (String el : n) {
                pad |= PADDED.matcher(el).find();
            }
            for (int i = x; (reverse ? i >= y : i <= y) && big.size() < EXPANSION_MAX; i += incr) {
                String c;
                if (isAlphaSequence) {
                    c = String.valueOf((char) i);
                    if ("\\".equals(c)) {
                        c = "";
                    }
                } else {
                    c = String.valueOf(i);
                    if (pad) {
                        int need = width - c.length();
                        if (need > 0) {
                            StringBuilder z = new StringBuilder();
                            for (int j = 0; j < need; j++) {
                                z.append('0');
                            }
                            c = i < 0 ? "-" + z + c.substring(1) : z + c;
                        }
                    }
                }
                big.add(c);
            }
        } else {
            for (String part : n) {
                big.addAll(expand(part, false));
            }
        }
        for (String value : big) {
            for (int k = 0; k < post.size() && expansions.size() < EXPANSION_MAX; k++) {
                String expansion = pre + value + post.get(k);
                if (!isTop || isSequence || !expansion.isEmpty()) {
                    expansions.add(expansion);
                }
            }
        }
        return expansions;
    }
}
