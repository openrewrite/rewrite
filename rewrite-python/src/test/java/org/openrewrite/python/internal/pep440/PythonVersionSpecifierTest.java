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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PythonVersionSpecifierTest {

    @ParameterizedTest
    @MethodSource("validSpecifiers")
    void validSpecifiers(String specifier) {
        PythonVersionSpecifier spec = PythonVersionSpecifier.parse(specifier);
        assertThat(spec).isNotNull();
        assertThat(spec.toString()).isEqualTo(specifier);
    }

    static Stream<String> validSpecifiers() {
        return Corpus.lines("/pep440/specifier-valid.txt").stream().map(Corpus::unescape);
    }

    @ParameterizedTest
    @MethodSource("invalidSpecifiers")
    void invalidSpecifiers(String specifier) {
        assertThat(PythonVersionSpecifier.parse(specifier)).isNull();
    }

    static Stream<String> invalidSpecifiers() {
        return Corpus.lines("/pep440/specifier-invalid.txt").stream().map(Corpus::unescape);
    }

    @ParameterizedTest
    @MethodSource("containsCases")
    void contains(String version, String specifier, boolean expected) {
        PythonVersionSpecifier spec = PythonVersionSpecifier.parse(specifier);
        assertThat(spec).as(specifier).isNotNull();
        PythonVersion parsed = PythonVersion.parse(version);
        assertThat(parsed).as(version).isNotNull();
        assertThat(spec.contains(parsed))
          .as("'%s' in '%s'", version, specifier)
          .isEqualTo(expected);
    }

    static Stream<Arguments> containsCases() {
        return Corpus.rows("/pep440/specifier-contains.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1], Boolean.parseBoolean(row[2])));
    }

    @ParameterizedTest
    @MethodSource("prereleaseCases")
    void prereleaseHandling(String specifier, String version, String includePrereleases, boolean expected) {
        PythonVersionSpecifier spec = PythonVersionSpecifier.parse(specifier);
        PythonVersion parsed = PythonVersion.parse(version);
        boolean actual = "null".equals(includePrereleases) ?
          spec.contains(parsed) :
          spec.contains(parsed, Boolean.parseBoolean(includePrereleases));
        assertThat(actual)
          .as("'%s' in '%s' (includePrereleases=%s)", version, specifier, includePrereleases)
          .isEqualTo(expected);
    }

    static Stream<Arguments> prereleaseCases() {
        return Corpus.rows("/pep440/specifier-prereleases.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1], row[2], Boolean.parseBoolean(row[3])));
    }

    @ParameterizedTest
    @MethodSource("prereleaseDetectionCases")
    void prereleaseDetection(String specifier, boolean expected) {
        assertThat(PythonVersionSpecifier.parse(specifier).referencesPrerelease()).isEqualTo(expected);
    }

    static Stream<Arguments> prereleaseDetectionCases() {
        return Corpus.rows("/pep440/specifier-prerelease-detection.tsv").stream()
          .map(row -> Arguments.of(row[0], Boolean.parseBoolean(row[1])));
    }

    @ParameterizedTest
    @MethodSource("arbitraryEqualityCases")
    void arbitraryEquality(String specifier, String version, boolean expected) {
        PythonVersionSpecifier spec = PythonVersionSpecifier.parse(specifier);
        assertThat(spec).as(specifier).isNotNull();
        assertThat(spec.contains(PythonVersion.parse(version)))
          .as("'%s' in '%s'", version, specifier)
          .isEqualTo(expected);
    }

    static Stream<Arguments> arbitraryEqualityCases() {
        return Corpus.rows("/pep440/specifier-arbitrary.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1], Boolean.parseBoolean(row[2])));
    }

    @Test
    void arbitraryEqualityIsCaseInsensitive() {
        assertThat(PythonVersionSpecifier.parse("===1.0A1.POST2.DEV3")
          .contains(PythonVersion.parse("1.0a1.post2.dev3"))).isTrue();
        assertThat(PythonVersionSpecifier.parse("===1.0")
          .contains(PythonVersion.parse("1.0.0"))).isFalse();
    }

    @Test
    void operatorAndVersionAccessors() {
        PythonVersionSpecifier spec = PythonVersionSpecifier.parse("== 1.2.3");
        assertThat(spec.getOperator()).isEqualTo("==");
        assertThat(spec.getVersion()).isEqualTo("1.2.3");
    }

    @Test
    void canonicalEquality() {
        assertThat(PythonVersionSpecifier.parse("==1.2.3")).isEqualTo(PythonVersionSpecifier.parse("== 1.2.3.0"));
        assertThat(PythonVersionSpecifier.parse("==1.2.3")).isNotEqualTo(PythonVersionSpecifier.parse("==1.2.4"));
        assertThat(PythonVersionSpecifier.parse("==1.2.3")).isNotEqualTo(PythonVersionSpecifier.parse("~=1.2.3"));
        assertThat(PythonVersionSpecifier.parse("~=1.2.0")).isNotEqualTo(PythonVersionSpecifier.parse("~=1.2"));
    }
}
