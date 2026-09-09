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
     * Normalize a requires-dist version specifier the way uv records it: all whitespace
     * removed, clauses joined by a bare comma (no space — unlike the header's
     * {@code requires-python}, which keeps its {@code ", "} separator), clauses sorted
     * ascending by version.
     */
    public static String normalizeRequiresDistSpecifier(String specifier) {
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
        // Stable sort keeps original order for equal versions
        clauses.sort(Map.Entry.comparingByKey());
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(clauses.get(i).getValue());
        }
        return b.toString();
    }

    private static PythonVersion clauseVersion(String clause) {
        int i = 0;
        while (i < clause.length() && "=<>!~".indexOf(clause.charAt(i)) >= 0) {
            i++;
        }
        String version = clause.substring(i);
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
