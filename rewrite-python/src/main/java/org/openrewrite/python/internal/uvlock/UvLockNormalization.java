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

import org.openrewrite.python.internal.pep440.PythonVersion;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Normalization uv applies when recording declared metadata. The format layer stores strings
 * verbatim and never re-normalizes; these helpers are for constructing new entries.
 * <p>
 * Marker normalization is deliberately not offered: the empirical catalog covers spacing,
 * quoting, {@code python_version} renaming for simple comparisons and and-clause sorting,
 * but not {@code ==}/{@code ~=} python_version range expansion or or-chain handling, so an
 * exact implementation is not possible from the cataloged rules.
 */
public final class UvLockNormalization {

    private UvLockNormalization() {
    }

    /**
     * uv's operator order, the tie-break between clauses on equal versions (pep440_rs
     * {@code Operator} declaration order).
     */
    private static final List<String> OPERATOR_ORDER =
            Arrays.asList("==", "===", "!=", "~=", "<", "<=", ">", ">=");

    /**
     * Normalize a requires-dist version specifier the way uv records it: all whitespace
     * removed, clauses joined by a bare comma (no space — unlike the header's
     * {@code requires-python}, which keeps its {@code ", "} separator), clauses sorted
     * ascending by version, then by operator.
     */
    public static String normalizeRequiresDistSpecifier(String specifier) {
        return String.join(",", sortedClauses(specifier));
    }

    /**
     * Normalize the header's {@code requires-python} the way uv records it: a compatible
     * release expands to its bounds ({@code ~=3.10} is {@code >=3.10, <4}), a range covering
     * exactly one minor version collapses to its wildcard ({@code >=3.12, <3.13} is
     * {@code ==3.12.*}), and clauses are joined by {@code ", "} in version order.
     */
    public static String normalizeRequiresPython(String declared) {
        List<String> expanded = new ArrayList<>();
        for (String raw : declared.split(",")) {
            String clause = raw.replaceAll("\\s+", "");
            if (clause.isEmpty()) {
                continue;
            }
            if (clause.startsWith("~=")) {
                PythonVersion upper = PyRange.bumpTrailingRelease(clauseVersion(clause));
                if (upper == null) {
                    throw new IllegalArgumentException("Compatible release needs two components: " + clause);
                }
                expanded.add(">=" + clause.substring(2));
                expanded.add("<" + upper);
            } else {
                expanded.add(clause);
            }
        }
        List<String> clauses = sortedClauses(String.join(",", expanded));
        if (clauses.size() == 2 && clauses.get(0).startsWith(">=") && clauses.get(1).startsWith("<")
                && !clauses.get(1).startsWith("<=")) {
            long[] lower = clauseVersion(clauses.get(0)).getRelease();
            long[] upper = clauseVersion(clauses.get(1)).getRelease();
            if (isMinor(lower) && isMinor(upper) && lower[0] == upper[0] && lower[1] + 1 == upper[1]) {
                return "==" + lower[0] + "." + lower[1] + ".*";
            }
        }
        return String.join(", ", clauses);
    }

    /** Whether the release is X.Y or X.Y.0 (a whole minor version). */
    private static boolean isMinor(long[] release) {
        if (release.length < 2) {
            return false;
        }
        for (int i = 2; i < release.length; i++) {
            if (release[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static List<String> sortedClauses(String specifier) {
        String[] rawClauses = specifier.split(",");
        // Keys extracted eagerly so every clause is validated even when no comparison runs
        List<Map.Entry<PythonVersion, String>> clauses = new ArrayList<>(rawClauses.length);
        for (String raw : rawClauses) {
            String clause = raw.replaceAll("\\s+", "");
            if (clause.isEmpty()) {
                throw new IllegalArgumentException("Empty clause in specifier: " + specifier);
            }
            clauses.add(new AbstractMap.SimpleImmutableEntry<>(clauseVersion(clause), clause));
        }
        clauses.sort(Map.Entry.<PythonVersion, String>comparingByKey()
                .thenComparing(e -> OPERATOR_ORDER.indexOf(clauseOperator(e.getValue()))));
        List<String> sorted = new ArrayList<>(clauses.size());
        for (Map.Entry<PythonVersion, String> clause : clauses) {
            sorted.add(clause.getValue());
        }
        return sorted;
    }

    private static String clauseOperator(String clause) {
        int i = 0;
        while (i < clause.length() && "=<>!~".indexOf(clause.charAt(i)) >= 0) {
            i++;
        }
        return clause.substring(0, i);
    }

    private static PythonVersion clauseVersion(String clause) {
        String version = clause.substring(clauseOperator(clause).length());
        if (version.endsWith(".*")) {
            version = version.substring(0, version.length() - 2);
        }
        PythonVersion parsed = PythonVersion.parse(version);
        if (parsed == null) {
            throw new IllegalArgumentException("Cannot parse version in specifier clause: " + clause);
        }
        return parsed;
    }
}
