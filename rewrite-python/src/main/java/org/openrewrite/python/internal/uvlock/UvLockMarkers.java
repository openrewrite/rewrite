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
package org.openrewrite.python.internal.uvlock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.python.internal.pep440.PythonVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * uv marker algebra: reproducing uv's recorded marker form (the verified normalization subset)
 * and evaluating a simple and-chain marker's truth over a {@link PyRange}. Pure functions with
 * no engine state.
 */
final class UvLockMarkers {

    /** Sentinel {@code simplifyClauses} returns when a clause is always false (the edge is dropped). */
    static final String DROP_EDGE = "\0drop";

    private static final Pattern SIMPLE_CLAUSE = Pattern.compile(
            "\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*(===|==|!=|<=|>=|<|>|~=|not\\s+in|in)\\s*(['\"])([^'\"]*)\\3\\s*");
    private static final Set<String> VERSION_VARS = new HashSet<>(Arrays.asList(
            "python_version", "python_full_version"));

    private UvLockMarkers() {
    }

    static boolean referencesExtraVariable(String marker) {
        return Pattern.compile("\\bextra\\b").matcher(marker).find();
    }

    static boolean referencesPythonVersion(String marker) {
        return Pattern.compile("\\bpython_(full_)?version\\b").matcher(marker).find();
    }

    /**
     * uv's recorded marker form for the cataloged subset: normalized spacing, single
     * quotes, {@code python_version} to {@code python_full_version} for order-preserving
     * comparisons, and and-chain clauses sorted by variable. Null when the marker is
     * outside the subset.
     */
    static @Nullable String recordMarker(String rawMarker, @Nullable String extraClause) {
        List<String[]> clauses = parseSimpleAndChain(rawMarker);
        if (clauses == null) {
            return null;
        }
        List<String[]> mapped = new ArrayList<>();
        for (String[] clause : clauses) {
            String var = clause[0];
            String op = clause[1];
            String value = clause[2];
            if (value.indexOf('\'') >= 0 || value.indexOf('"') >= 0) {
                return null;
            }
            if (VERSION_VARS.contains(var)) {
                // only >= and < survive the python_version -> python_full_version rename unchanged
                if (!">=".equals(op) && !"<".equals(op)) {
                    return null;
                }
                mapped.add(new String[]{"python_full_version", op, value});
            } else if ("platform_system".equals(var) && ("==".equals(op) || "!=".equals(op))) {
                // uv rewrites platform_system to sys_platform for the three OSes it maps;
                // other values have no sys_platform equivalent, so fail loud (verified subset)
                String sysPlatform = platformSystemToSysPlatform(value);
                if (sysPlatform == null) {
                    return null;
                }
                mapped.add(new String[]{"sys_platform", op, sysPlatform});
            } else if ("==".equals(op) || "!=".equals(op)) {
                mapped.add(clause);
            } else {
                return null;
            }
        }
        if (extraClause != null) {
            List<String[]> extra = parseSimpleAndChain(extraClause);
            if (extra == null) {
                return null;
            }
            mapped.addAll(extra);
        }
        // stable sort: uv orders and-chains alphabetically by variable
        mapped.sort(Comparator.comparing(a -> a[0]));
        List<String> parts = new ArrayList<>(mapped.size());
        for (String[] clause : mapped) {
            parts.add(clause[0] + " " + clause[1] + " '" + clause[2] + "'");
        }
        return String.join(" and ", parts);
    }

    private static @Nullable String platformSystemToSysPlatform(String value) {
        switch (value) {
            case "Windows":
                return "win32";
            case "Linux":
                return "linux";
            case "Darwin":
                return "darwin";
            default:
                return null;
        }
    }

    /**
     * Splits a marker into simple {@code var op 'value'} clauses joined by {@code and};
     * null for anything richer (or-chains, parentheses, reversed operands).
     */
    static @Nullable List<String[]> parseSimpleAndChain(String marker) {
        List<String> parts = splitTopLevelAnd(marker);
        if (parts == null) {
            return null;
        }
        List<String[]> clauses = new ArrayList<>(parts.size());
        for (String part : parts) {
            Matcher m = SIMPLE_CLAUSE.matcher(part);
            if (!m.matches()) {
                return null;
            }
            clauses.add(new String[]{m.group(1), m.group(2), m.group(4)});
        }
        return clauses;
    }

    private static @Nullable List<String> splitTopLevelAnd(String marker) {
        if (marker.indexOf('(') >= 0 || marker.indexOf(')') >= 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        String[] tokens = marker.split("(?<=\\s)|(?=\\s)");
        // scan word tokens, honoring quoted strings, so "and"/"or" inside values are kept
        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i];
            if (quote == 0 && ("and".equals(token))) {
                parts.add(current.toString());
                current.setLength(0);
                i++;
                continue;
            }
            if (quote == 0 && "or".equals(token)) {
                return null;
            }
            for (int c = 0; c < token.length(); c++) {
                char ch = token.charAt(c);
                if (quote == 0 && (ch == '\'' || ch == '"')) {
                    quote = ch;
                } else if (quote == ch) {
                    quote = 0;
                }
            }
            current.append(token);
            i++;
        }
        parts.add(current.toString());
        return parts;
    }

    /**
     * Drops always-true clauses and signals edge removal on any always-false clause;
     * null result means the whole marker became vacuous.
     */
    static @Nullable String simplifyClauses(PyRange range, List<String[]> clauses) {
        List<String> kept = new ArrayList<>();
        for (String[] clause : clauses) {
            Boolean truth = clauseTruth(range, clause[0], clause[1], clause[2]);
            if (Boolean.FALSE.equals(truth)) {
                return DROP_EDGE;
            }
            if (truth == null) {
                kept.add(clause[0] + " " + clause[1] + " '" + clause[2] + "'");
            }
        }
        return kept.isEmpty() ? null : String.join(" and ", kept);
    }

    /**
     * Truth of a simple marker clause over every Python in the lock's requires-python
     * range; null when it varies or cannot be decided. Deliberately still null:
     * non-version variables, unparseable values, python_version compared at micro
     * precision, wildcards under ordering operators, and ~= of a single-component
     * version. Pre-release Pythons at interval boundaries are outside the model,
     * matching the release-ordering treatment of the inequality operators.
     */
    static @Nullable Boolean clauseTruth(PyRange range, String var, String op, String value) {
        if (!VERSION_VARS.contains(var)) {
            return null;
        }
        boolean wildcard = value.endsWith(".*");
        if (wildcard && !"==".equals(op) && !"!=".equals(op)) {
            return null;
        }
        PythonVersion v = PythonVersion.parse(wildcard ? value.substring(0, value.length() - 2) : value);
        if (v == null) {
            return null;
        }
        // python_version truncates to major.minor; micro-precision comparisons diverge
        if ("python_version".equals(var) && v.getRelease().length > 2) {
            return null;
        }
        PythonVersion lower = range.lower;
        PythonVersion upper = range.upper;
        switch (op) {
            case ">=":
                if (lower != null && lower.compareTo(v) >= 0) {
                    return true;
                }
                if (upper != null && (range.upperInclusive ? upper.compareTo(v) < 0 : upper.compareTo(v) <= 0)) {
                    return false;
                }
                return null;
            case ">":
                if (lower != null && (range.lowerExclusive ? lower.compareTo(v) >= 0 : lower.compareTo(v) > 0)) {
                    return true;
                }
                if (upper != null && upper.compareTo(v) <= 0) {
                    return false;
                }
                return null;
            case "<":
                if (upper != null && (range.upperInclusive ? upper.compareTo(v) < 0 : upper.compareTo(v) <= 0)) {
                    return true;
                }
                if (lower != null && lower.compareTo(v) >= 0) {
                    return false;
                }
                return null;
            case "<=":
                if (upper != null && upper.compareTo(v) <= 0) {
                    return true;
                }
                if (lower != null && (range.lowerExclusive ? lower.compareTo(v) >= 0 : lower.compareTo(v) > 0)) {
                    return false;
                }
                return null;
            case "==":
                return equalityTruth(range, var, v, wildcard);
            case "!=": {
                Boolean eq = equalityTruth(range, var, v, wildcard);
                return eq == null ? null : !eq;
            }
            case "~=": {
                // ~=X.Y[.Z] is >=X.Y[.Z], <X.(Y+1) / <X.Y.(Z+1)-family upper
                if (v.getRelease().length < 2) {
                    return null;
                }
                PythonVersion next = PyRange.bumpTrailingRelease(v);
                return next == null ? null : intervalTruth(range, v, next);
            }
            default:
                return null;
        }
    }

    /**
     * ==/!= truth: a wildcard, or python_version's truncated equality, covers the
     * half-open interval [v, next); exact python_full_version equality is a single
     * point, decidable only when v lies outside the range or the range is that point.
     */
    private static @Nullable Boolean equalityTruth(PyRange range, String var, PythonVersion v, boolean wildcard) {
        if (wildcard || "python_version".equals(var)) {
            PythonVersion next = bumpLastComponent(v, wildcard ? v.getRelease().length : 2);
            return next == null ? null : intervalTruth(range, v, next);
        }
        if (range.lower != null && range.upper != null &&
                range.lower.compareTo(v) == 0 && range.upper.compareTo(v) == 0 &&
                !range.lowerExclusive && range.upperInclusive) {
            return true;
        }
        if ((range.lower != null && (range.lowerExclusive ? range.lower.compareTo(v) >= 0 : range.lower.compareTo(v) > 0)) ||
                (range.upper != null && (range.upperInclusive ? range.upper.compareTo(v) < 0 : range.upper.compareTo(v) <= 0))) {
            return false;
        }
        return null;
    }

    /**
     * Truth of "python in [lo, hi)" over the whole range: true when the range is
     * contained, false when disjoint, null when it straddles a boundary.
     */
    private static @Nullable Boolean intervalTruth(PyRange range, PythonVersion lo, PythonVersion hi) {
        if (range.lower != null && range.lower.compareTo(lo) >= 0 &&
                range.upper != null &&
                (range.upperInclusive ? range.upper.compareTo(hi) < 0 : range.upper.compareTo(hi) <= 0)) {
            return true;
        }
        if (range.lower != null && range.lower.compareTo(hi) >= 0) {
            return false;
        }
        if (range.upper != null && (range.upperInclusive ? range.upper.compareTo(lo) < 0 : range.upper.compareTo(lo) <= 0)) {
            return false;
        }
        return null;
    }

    private static @Nullable PythonVersion bumpLastComponent(PythonVersion v, int components) {
        if (components < 1) {
            return null;
        }
        long[] release = v.getRelease();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < components; i++) {
            if (i > 0) {
                b.append('.');
            }
            long part = i < release.length ? release[i] : 0;
            b.append(i == components - 1 ? part + 1 : part);
        }
        return PythonVersion.parse(b.toString());
    }
}
