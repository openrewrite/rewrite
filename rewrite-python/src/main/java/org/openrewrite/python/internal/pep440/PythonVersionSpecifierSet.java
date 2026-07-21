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
package org.openrewrite.python.internal.pep440;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A comma-separated set of PEP 440 specifier clauses, ported from pypa/packaging's
 * {@code specifiers.SpecifierSet}. An empty set matches all versions, subject to the
 * pre-release policy: pre-releases are excluded by default unless some clause itself
 * references a pre-release version, or {@code includePrereleases} is passed explicitly.
 */
public class PythonVersionSpecifierSet {
    private final List<PythonVersionSpecifier> specifiers;

    private PythonVersionSpecifierSet(List<PythonVersionSpecifier> specifiers) {
        this.specifiers = specifiers;
    }

    public static @Nullable PythonVersionSpecifierSet parse(@Nullable String specifiers) {
        if (specifiers == null) {
            return null;
        }
        List<PythonVersionSpecifier> parsed = new ArrayList<>();
        for (String clause : specifiers.split(",", -1)) {
            String stripped = PythonVersionSpecifier.strip(clause);
            if (stripped.isEmpty()) {
                continue;
            }
            PythonVersionSpecifier spec = PythonVersionSpecifier.parse(stripped);
            if (spec == null) {
                return null;
            }
            parsed.add(spec);
        }
        return new PythonVersionSpecifierSet(parsed);
    }

    public List<PythonVersionSpecifier> getSpecifiers() {
        return specifiers;
    }

    public boolean isMatchAll() {
        return specifiers.isEmpty();
    }

    /**
     * Whether the version satisfies every clause, excluding pre-releases unless some
     * clause itself references a pre-release version.
     */
    public boolean contains(PythonVersion version) {
        return contains(version, referencesPrerelease());
    }

    public boolean contains(PythonVersion version, boolean includePrereleases) {
        if (!includePrereleases && version.isPrerelease()) {
            return false;
        }
        for (PythonVersionSpecifier spec : specifiers) {
            if (!spec.contains(version)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether any clause references a pre-release version.
     */
    public boolean referencesPrerelease() {
        return specifiers.stream().anyMatch(PythonVersionSpecifier::referencesPrerelease);
    }

    // Deduplicated, string-sorted clauses, mirroring packaging's canonical ordering.
    private List<PythonVersionSpecifier> canonicalSpecifiers() {
        Set<String> seen = new LinkedHashSet<>();
        return specifiers.stream()
                .sorted(Comparator.comparing(PythonVersionSpecifier::toString))
                .filter(s -> seen.add(s.toString()))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return canonicalSpecifiers().stream()
                .map(PythonVersionSpecifier::toString)
                .collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PythonVersionSpecifierSet)) {
            return false;
        }
        return canonicalSpecifiers().equals(((PythonVersionSpecifierSet) o).canonicalSpecifiers());
    }

    @Override
    public int hashCode() {
        return canonicalSpecifiers().hashCode();
    }
}
