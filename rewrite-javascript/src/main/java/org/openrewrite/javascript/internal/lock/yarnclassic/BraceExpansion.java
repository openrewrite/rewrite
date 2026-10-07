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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Port of brace-expansion 1.1.11 with balanced-match 1.0.0, as yarn 1.22.22 bundles them.
 */
final class BraceExpansion {

    private static final String ESC_SLASH = "\0SLASH\0";
    private static final String ESC_OPEN = "\0OPEN\0";
    private static final String ESC_CLOSE = "\0CLOSE\0";
    private static final String ESC_COMMA = "\0COMMA\0";
    private static final String ESC_PERIOD = "\0PERIOD\0";

    private static final Pattern NUMERIC_SEQUENCE = Pattern.compile("^-?\\d+\\.\\.-?\\d+(?:\\.\\.-?\\d+)?\\z");
    private static final Pattern ALPHA_SEQUENCE = Pattern.compile("^[a-zA-Z]\\.\\.[a-zA-Z](?:\\.\\.-?\\d+)?\\z");
    private static final Pattern PADDED = Pattern.compile("^-?0\\d");
    private static final Pattern COMMA_THEN_CLOSE = Pattern.compile("," + Js.DOT + "*}");
    private static final Pattern DOT_DOT = Pattern.compile("\\.\\.");

    private BraceExpansion() {
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

    private static String escapeBraces(String str) {
        return str.replace("\\\\", ESC_SLASH).replace("\\{", ESC_OPEN).replace("\\}", ESC_CLOSE)
                .replace("\\,", ESC_COMMA).replace("\\.", ESC_PERIOD);
    }

    private static String unescapeBraces(String str) {
        return str.replace(ESC_SLASH, "\\").replace(ESC_OPEN, "{").replace(ESC_CLOSE, "}")
                .replace(ESC_COMMA, ",").replace(ESC_PERIOD, ".");
    }

    // str.split(","), keeping nested brace sets whole
    private static List<String> parseCommaParts(String str) {
        if (str.isEmpty()) {
            return new ArrayList<>(Collections.singletonList(""));
        }
        Balanced m = balanced(str);
        if (m == null) {
            return new ArrayList<>(Arrays.asList(str.split(",", -1)));
        }
        List<String> p = new ArrayList<>(Arrays.asList(m.pre.split(",", -1)));
        p.set(p.size() - 1, p.get(p.size() - 1) + "{" + m.body + "}");
        List<String> postParts = parseCommaParts(m.post);
        if (!m.post.isEmpty()) {
            p.set(p.size() - 1, p.get(p.size() - 1) + postParts.remove(0));
            p.addAll(postParts);
        }
        return p;
    }

    private static List<String> expand(String str, boolean isTop) {
        List<String> expansions = new ArrayList<>();
        Balanced m = balanced(str);
        if (m == null || m.pre.endsWith("$")) {
            return new ArrayList<>(Collections.singletonList(str));
        }
        boolean isNumericSequence = NUMERIC_SEQUENCE.matcher(m.body).find();
        boolean isAlphaSequence = ALPHA_SEQUENCE.matcher(m.body).find();
        boolean isSequence = isNumericSequence || isAlphaSequence;
        boolean isOptions = m.body.indexOf(',') >= 0;
        if (!isSequence && !isOptions) {
            if (COMMA_THEN_CLOSE.matcher(m.post).find()) {
                return expand(m.pre + "{" + m.body + ESC_CLOSE + m.post, false);
            }
            return new ArrayList<>(Collections.singletonList(str));
        }

        List<String> n;
        if (isSequence) {
            n = Arrays.asList(DOT_DOT.split(m.body, -1));
        } else {
            n = parseCommaParts(m.body);
            if (n.size() == 1) {
                List<String> embraced = new ArrayList<>();
                for (String e : expand(n.get(0), false)) {
                    embraced.add("{" + e + "}");
                }
                n = embraced;
                if (n.size() == 1) {
                    List<String> post = m.post.isEmpty() ? Collections.singletonList("") : expand(m.post, false);
                    List<String> out = new ArrayList<>();
                    for (String p : post) {
                        out.add(m.pre + n.get(0) + p);
                    }
                    return out;
                }
            }
        }

        String pre = m.pre;
        List<String> post = m.post.isEmpty() ? Collections.singletonList("") : expand(m.post, false);

        List<String> values;
        if (isSequence) {
            values = sequence(n, isAlphaSequence);
        } else {
            values = new ArrayList<>();
            for (String el : n) {
                values.addAll(expand(el, false));
            }
        }

        for (String value : values) {
            for (String p : post) {
                String expansion = pre + value + p;
                if (!isTop || isSequence || !expansion.isEmpty()) {
                    expansions.add(expansion);
                }
            }
        }
        return expansions;
    }

    private static List<String> sequence(List<String> n, boolean isAlphaSequence) {
        long x = numeric(n.get(0));
        long y = numeric(n.get(1));
        int width = Math.max(n.get(0).length(), n.get(1).length());
        long incr = n.size() == 3 ? Math.abs(numeric(n.get(2))) : 1;
        boolean reverse = y < x;
        if (reverse) {
            incr *= -1;
        }
        boolean pad = false;
        for (String el : n) {
            pad |= PADDED.matcher(el).find();
        }
        List<String> values = new ArrayList<>();
        for (long i = x; reverse ? i >= y : i <= y; i += incr) {
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
                        for (int k = 0; k < need; k++) {
                            z.append('0');
                        }
                        c = i < 0 ? "-" + z + c.substring(1) : z + c;
                    }
                }
            }
            values.add(c);
            if (incr == 0) {
                break;
            }
        }
        return values;
    }

    // parseInt(str, 10) == str ? parseInt(str, 10) : str.charCodeAt(0)
    private static long numeric(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return str.charAt(0);
        }
    }

    private static final class Balanced {
        final String pre;
        final String body;
        final String post;

        Balanced(String pre, String body, String post) {
            this.pre = pre;
            this.body = body;
            this.post = post;
        }
    }

    private static @Nullable Balanced balanced(String str) {
        int[] r = range(str);
        return r == null ? null : new Balanced(str.substring(0, r[0]), str.substring(r[0] + 1, r[1]),
                str.substring(r[1] + 1));
    }

    private static int @Nullable [] range(String str) {
        int ai = str.indexOf('{');
        int bi = str.indexOf('}', ai + 1);
        int i = ai;
        int[] result = null;
        if (ai >= 0 && bi > 0) {
            List<Integer> begs = new ArrayList<>();
            int left = str.length();
            int right = -1;
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
            if (!begs.isEmpty() && right >= 0) {
                result = new int[]{left, right};
            }
        }
        return result;
    }
}
