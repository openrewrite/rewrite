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

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PythonVersionTest {

    private static List<String> orderedVersions() {
        return Corpus.lines("/pep440/version-ordering.txt");
    }

    @Test
    void orderingMatchesPackagingCorpus() {
        List<String> versions = orderedVersions();
        for (int i = 0; i < versions.size(); i++) {
            PythonVersion left = PythonVersion.parse(versions.get(i));
            assertThat(left).as(versions.get(i)).isNotNull();
            for (int j = i + 1; j < versions.size(); j++) {
                PythonVersion right = PythonVersion.parse(versions.get(j));
                assertThat(left.compareTo(right))
                  .as("%s < %s", versions.get(i), versions.get(j))
                  .isLessThan(0);
                assertThat(right.compareTo(left))
                  .as("%s > %s", versions.get(j), versions.get(i))
                  .isGreaterThan(0);
            }
        }
    }

    @Test
    void equalityAndHashAcrossSpellings() {
        assertThat(PythonVersion.parse("1.0rc1")).isEqualTo(PythonVersion.parse("1.0c1"));
        assertThat(PythonVersion.parse("1.0")).isEqualTo(PythonVersion.parse("1.0.0"));
        assertThat(PythonVersion.parse("1.0.0").hashCode()).isEqualTo(PythonVersion.parse("1").hashCode());
        assertThat(PythonVersion.parse("1.2+abc")).isEqualTo(PythonVersion.parse("1.2+AbC"));
        assertThat(PythonVersion.parse("1.0")).isNotEqualTo(PythonVersion.parse("1.0+local"));
    }

    @ParameterizedTest
    @MethodSource("invalidVersions")
    void invalidVersions(String version) {
        assertThat(PythonVersion.parse(version)).isNull();
    }

    static Stream<String> invalidVersions() {
        return Corpus.lines("/pep440/version-invalid.txt").stream().map(Corpus::unescape);
    }

    @ParameterizedTest
    @MethodSource("normalizedVersions")
    void normalization(String input, String normalized) {
        PythonVersion version = PythonVersion.parse(input);
        assertThat(version).as(input).isNotNull();
        assertThat(version.toString()).isEqualTo(normalized);
        assertThat(version.getValue()).isEqualTo(input);
    }

    static Stream<Arguments> normalizedVersions() {
        return Corpus.rows("/pep440/version-normalization.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("epochs")
    void epoch(String input, int epoch) {
        assertThat(PythonVersion.parse(input).getEpoch()).isEqualTo(epoch);
    }

    static Stream<Arguments> epochs() {
        return Corpus.rows("/pep440/version-epoch.tsv").stream()
          .map(row -> Arguments.of(row[0], Integer.parseInt(row[1])));
    }

    @ParameterizedTest
    @MethodSource("prereleaseFlags")
    void isPrerelease(String input, boolean expected) {
        assertThat(PythonVersion.parse(input).isPrerelease()).isEqualTo(expected);
    }

    static Stream<Arguments> prereleaseFlags() {
        return booleanRows("/pep440/version-is-prerelease.tsv");
    }

    @ParameterizedTest
    @MethodSource("postreleaseFlags")
    void isPostrelease(String input, boolean expected) {
        assertThat(PythonVersion.parse(input).isPostrelease()).isEqualTo(expected);
    }

    static Stream<Arguments> postreleaseFlags() {
        return booleanRows("/pep440/version-is-postrelease.tsv");
    }

    @ParameterizedTest
    @MethodSource("devreleaseFlags")
    void isDevrelease(String input, boolean expected) {
        assertThat(PythonVersion.parse(input).isDevrelease()).isEqualTo(expected);
    }

    static Stream<Arguments> devreleaseFlags() {
        return booleanRows("/pep440/version-is-devrelease.tsv");
    }

    private static Stream<Arguments> booleanRows(String resource) {
        return Corpus.rows(resource).stream()
          .map(row -> Arguments.of(row[0], Boolean.parseBoolean(row[1])));
    }

    @ParameterizedTest
    @MethodSource("baseVersions")
    void baseVersion(String input, String base) {
        assertThat(PythonVersion.parse(input).getBaseVersion()).isEqualTo(base);
    }

    static Stream<Arguments> baseVersions() {
        return Corpus.rows("/pep440/version-base.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("publicVersions")
    void publicVersion(String input, String publicVersion) {
        assertThat(PythonVersion.parse(input).getPublicVersion()).isEqualTo(publicVersion);
    }

    static Stream<Arguments> publicVersions() {
        return Corpus.rows("/pep440/version-public.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @Test
    void localSegments() {
        assertThat(PythonVersion.parse("1.2.3").getLocal()).isNull();
        assertThat(PythonVersion.parse("1.2.3+abc").getLocal()).isEqualTo("abc");
        assertThat(PythonVersion.parse("1.2.3+abc_def-1.00").getLocal()).isEqualTo("abc.def.1.0");
    }
}
