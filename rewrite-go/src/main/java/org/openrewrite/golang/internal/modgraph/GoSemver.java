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
package org.openrewrite.golang.internal.modgraph;

/**
 * A faithful port of {@code golang.org/x/mod/semver}'s {@code Compare}, the
 * version ordering the Go toolchain uses for Minimal Version Selection. Go
 * semantic versions are {@code vMAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]}; the
 * leading {@code v} is required, build metadata is ignored, and a version that
 * does not parse sorts before every version that does.
 * <p>
 * Kept structurally parallel to the upstream Go source so divergence is easy to
 * audit — the Go resolver this replaces depended on {@code semver.Compare} for
 * MVS and the pruning version gate, where an off-by-one ordering silently
 * changes the selected build list.
 */
public final class GoSemver {

    private GoSemver() {
    }

    /**
     * Compares two Go semantic versions, returning -1, 0, or +1. Invalid
     * versions sort below valid ones (two invalid versions compare equal).
     */
    public static int compare(String v, String w) {
        Parsed pv = parse(v);
        Parsed pw = parse(w);
        if (pv == null && pw == null) {
            return 0;
        }
        if (pv == null) {
            return -1;
        }
        if (pw == null) {
            return +1;
        }
        int c = compareInt(pv.major, pw.major);
        if (c != 0) {
            return c;
        }
        c = compareInt(pv.minor, pw.minor);
        if (c != 0) {
            return c;
        }
        c = compareInt(pv.patch, pw.patch);
        if (c != 0) {
            return c;
        }
        return comparePrerelease(pv.prerelease, pw.prerelease);
    }

    private static final class Parsed {
        String major = "";
        String minor = "";
        String patch = "";
        String prerelease = "";
        @SuppressWarnings("unused")
        String build = "";
    }

    /** A holder for the {@code (token, rest, ok)} tuples the Go scanner returns. */
    private static final class Scan {
        final String token;
        final String rest;

        Scan(String token, String rest) {
            this.token = token;
            this.rest = rest;
        }
    }

    private static Parsed parse(String v) {
        if (v == null || v.isEmpty() || v.charAt(0) != 'v') {
            return null;
        }
        Parsed p = new Parsed();
        Scan s = parseInt(v.substring(1));
        if (s == null) {
            return null;
        }
        p.major = s.token;
        v = s.rest;
        if (v.isEmpty()) {
            p.minor = "0";
            p.patch = "0";
            return p;
        }
        if (v.charAt(0) != '.') {
            return null;
        }
        s = parseInt(v.substring(1));
        if (s == null) {
            return null;
        }
        p.minor = s.token;
        v = s.rest;
        if (v.isEmpty()) {
            p.patch = "0";
            return p;
        }
        if (v.charAt(0) != '.') {
            return null;
        }
        s = parseInt(v.substring(1));
        if (s == null) {
            return null;
        }
        p.patch = s.token;
        v = s.rest;
        if (!v.isEmpty() && v.charAt(0) == '-') {
            s = parsePrerelease(v);
            if (s == null) {
                return null;
            }
            p.prerelease = s.token;
            v = s.rest;
        }
        if (!v.isEmpty() && v.charAt(0) == '+') {
            s = parseBuild(v);
            if (s == null) {
                return null;
            }
            p.build = s.token;
            v = s.rest;
        }
        if (!v.isEmpty()) {
            return null;
        }
        return p;
    }

    private static Scan parseInt(String v) {
        if (v.isEmpty()) {
            return null;
        }
        if (v.charAt(0) < '0' || '9' < v.charAt(0)) {
            return null;
        }
        int i = 1;
        while (i < v.length() && '0' <= v.charAt(i) && v.charAt(i) <= '9') {
            i++;
        }
        if (v.charAt(0) == '0' && i != 1) {
            return null;
        }
        return new Scan(v.substring(0, i), v.substring(i));
    }

    private static Scan parsePrerelease(String v) {
        if (v.isEmpty() || v.charAt(0) != '-') {
            return null;
        }
        int i = 1;
        int start = 1;
        while (i < v.length() && v.charAt(i) != '+') {
            if (!isIdentChar(v.charAt(i)) && v.charAt(i) != '.') {
                return null;
            }
            if (v.charAt(i) == '.') {
                if (start == i || isBadNum(v.substring(start, i))) {
                    return null;
                }
                start = i + 1;
            }
            i++;
        }
        if (start == i || isBadNum(v.substring(start, i))) {
            return null;
        }
        return new Scan(v.substring(0, i), v.substring(i));
    }

    private static Scan parseBuild(String v) {
        if (v.isEmpty() || v.charAt(0) != '+') {
            return null;
        }
        int i = 1;
        int start = 1;
        while (i < v.length()) {
            if (!isIdentChar(v.charAt(i)) && v.charAt(i) != '.') {
                return null;
            }
            if (v.charAt(i) == '.') {
                if (start == i) {
                    return null;
                }
                start = i + 1;
            }
            i++;
        }
        if (start == i) {
            return null;
        }
        return new Scan(v.substring(0, i), v.substring(i));
    }

    private static int compareInt(String x, String y) {
        if (x.equals(y)) {
            return 0;
        }
        if (x.length() < y.length()) {
            return -1;
        }
        if (x.length() > y.length()) {
            return +1;
        }
        return x.compareTo(y) < 0 ? -1 : +1;
    }

    private static int comparePrerelease(String x, String y) {
        if (x.equals(y)) {
            return 0;
        }
        if (x.isEmpty()) {
            return +1;
        }
        if (y.isEmpty()) {
            return -1;
        }
        while (!x.isEmpty() && !y.isEmpty()) {
            x = x.substring(1); // skip - or .
            y = y.substring(1);
            String dx = nextIdent(x);
            x = x.substring(dx.length());
            String dy = nextIdent(y);
            y = y.substring(dy.length());
            if (!dx.equals(dy)) {
                boolean ix = isNum(dx);
                boolean iy = isNum(dy);
                if (ix != iy) {
                    return ix ? -1 : +1;
                }
                if (ix) {
                    if (dx.length() < dy.length()) {
                        return -1;
                    }
                    if (dx.length() > dy.length()) {
                        return +1;
                    }
                }
                return dx.compareTo(dy) < 0 ? -1 : +1;
            }
        }
        return x.isEmpty() ? -1 : +1;
    }

    private static String nextIdent(String x) {
        int i = 0;
        while (i < x.length() && x.charAt(i) != '.') {
            i++;
        }
        return x.substring(0, i);
    }

    private static boolean isNum(String v) {
        int i = 0;
        while (i < v.length() && '0' <= v.charAt(i) && v.charAt(i) <= '9') {
            i++;
        }
        return i == v.length();
    }

    private static boolean isIdentChar(char c) {
        return 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || '0' <= c && c <= '9' || c == '-';
    }

    private static boolean isBadNum(String v) {
        int i = 0;
        while (i < v.length() && '0' <= v.charAt(i) && v.charAt(i) <= '9') {
            i++;
        }
        return i == v.length() && i > 1 && v.charAt(0) == '0';
    }
}
