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

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PythonVersionSpecifierSetTest {

    private static boolean contains(String set, String version) {
        return Objects.requireNonNull(PythonVersionSpecifierSet.parse(set))
          .contains(Objects.requireNonNull(PythonVersion.parse(version)));
    }

    @Test
    void emptySetMatchesAll() {
        PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse("");
        assertThat(set).isNotNull();
        assertThat(set.isMatchAll()).isTrue();
        assertThat(set.contains(PythonVersion.parse("1.0"))).isTrue();
        assertThat(set.contains(PythonVersion.parse("0.0.1"))).isTrue();
        // No clause references a prerelease, so prereleases are excluded by default.
        assertThat(set.contains(PythonVersion.parse("1.0.dev1"))).isFalse();
        assertThat(set.contains(PythonVersion.parse("1.0.dev1"), true)).isTrue();
    }

    @Test
    void invalidSets() {
        assertThat(PythonVersionSpecifierSet.parse("lolwat")).isNull();
        assertThat(PythonVersionSpecifierSet.parse(">=1.0,invalid")).isNull();
        assertThat(PythonVersionSpecifierSet.parse("=>2.0")).isNull();
    }

    @Test
    void multipleClausesAreAnded() {
        assertThat(contains(">=1.0.0,!=1.0.1,<2.0", "1.5")).isTrue();
        assertThat(contains(">=1.0.0,!=1.0.1,<2.0", "1.0.1")).isFalse();
        assertThat(contains(">=1.0.0,!=1.0.1,<2.0", "2.0")).isFalse();
        assertThat(contains(">=1.0.0,!=1.0.1,<2.0", "0.9")).isFalse();
        assertThat(contains(">=1.0.0,!=1.0.1", "1.0.1.0")).isFalse();
    }

    @Test
    void whitespaceAndEmptyClausesTolerated() {
        assertThat(contains(" >= 1.0 , < 2.0 ", "1.5")).isTrue();
        assertThat(contains("==1.0,,", "1.0")).isTrue();
    }

    @Test
    void prereleasesExcludedByDefault() {
        assertThat(contains(">=1.0", "2.0.dev1")).isFalse();
        assertThat(contains(">=1.0", "2.0a1")).isFalse();
        assertThat(PythonVersionSpecifierSet.parse(">=1.0")
          .contains(PythonVersion.parse("2.0.dev1"), true)).isTrue();
    }

    @Test
    void prereleaseClauseEnablesPrereleases() {
        assertThat(contains(">=1.0.dev1", "1.0.dev2")).isTrue();
        assertThat(contains(">=2.0.dev1", "2.0a1")).isTrue();
        assertThat(contains(">=1.0a1,<2.0", "1.5b2")).isTrue();
        // != clauses do not imply prereleases.
        assertThat(contains(">=1.0,!=2.0a1", "2.0a2")).isFalse();
        // Nor do wildcard == clauses.
        assertThat(contains("==2.0.*", "2.0a1")).isFalse();
    }

    @Test
    void explicitOverrideBeatsAutodetection() {
        PythonVersionSpecifierSet set = PythonVersionSpecifierSet.parse(">=1.0.dev1");
        assertThat(set.referencesPrerelease()).isTrue();
        assertThat(set.contains(PythonVersion.parse("1.0.dev2"), false)).isFalse();
    }

    @Test
    void canonicalStringForm() {
        assertThat(PythonVersionSpecifierSet.parse(">=1.0.0,!=1.0.1").toString())
          .isEqualTo("!=1.0.1,>=1.0.0");
        assertThat(PythonVersionSpecifierSet.parse("")).hasToString("");
    }

    @Test
    void equalityIgnoresOrderDuplicatesAndTrailingZeros() {
        assertThat(PythonVersionSpecifierSet.parse(">=1.0.0,!=1.0.1"))
          .isEqualTo(PythonVersionSpecifierSet.parse("!=1.0.1,>=1.0.0,>=1.0.0"));
        assertThat(PythonVersionSpecifierSet.parse(">=1.0"))
          .isEqualTo(PythonVersionSpecifierSet.parse(">=1.0.0"));
        assertThat(PythonVersionSpecifierSet.parse(">=1.0.0,!=1.0.1"))
          .isNotEqualTo(PythonVersionSpecifierSet.parse(">=1.0.0"));
    }

    @Test
    void arbitraryEqualityClause() {
        assertThat(contains("===1.0", "1.0")).isTrue();
        assertThat(contains("===1.0", "1.0.0")).isFalse();
    }
}
