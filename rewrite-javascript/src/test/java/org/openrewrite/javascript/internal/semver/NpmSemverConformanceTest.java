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
package org.openrewrite.javascript.internal.semver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conformance against node-semver's own test fixtures ({@code test/fixtures/} in
 * npm/node-semver), transposed to TSV with loose-mode and includePrerelease rows
 * removed. Regenerate per the README in {@code src/test/resources/npm-semver/}.
 */
class NpmSemverConformanceTest {

    private static Stream<org.junit.jupiter.params.provider.Arguments> corpus(String resource) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
          NpmSemverConformanceTest.class.getResourceAsStream("/npm-semver/" + resource),
          StandardCharsets.UTF_8));
        return reader.lines().filter(l -> !l.isEmpty())
          .map(l -> org.junit.jupiter.params.provider.Arguments.of((Object[]) l.split("\t", -1)));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> satisfies() {
        return corpus("satisfies.tsv");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> comparisons() {
        return corpus("comparisons.tsv");
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> equality() {
        return corpus("equality.tsv");
    }

    @ParameterizedTest
    @MethodSource("satisfies")
    void satisfiesCorpus(String rangeStr, String version, String expectedStr) {
        NpmRange range = NpmRange.parse(rangeStr);
        boolean expected = Boolean.parseBoolean(expectedStr);
        if (range == null) {
            assertThat(expected).as("unparseable range %s must only appear in exclude rows", rangeStr).isFalse();
        } else {
            assertThat(range.satisfies(version))
              .as("satisfies(%s, %s)", version, rangeStr)
              .isEqualTo(expected);
        }
    }

    @ParameterizedTest
    @MethodSource("comparisons")
    void greaterThanCorpus(String greater, String lesser) {
        NpmVersion v0 = NpmVersion.parse(greater);
        NpmVersion v1 = NpmVersion.parse(lesser);
        assertThat(v0).isNotNull();
        assertThat(v1).isNotNull();
        assertThat(v0.compareTo(v1)).as("%s > %s", greater, lesser).isPositive();
        assertThat(v1.compareTo(v0)).as("%s < %s", lesser, greater).isNegative();
    }

    @ParameterizedTest
    @MethodSource("equality")
    void equalityCorpus(String left, String right) {
        NpmVersion v0 = NpmVersion.parse(left);
        NpmVersion v1 = NpmVersion.parse(right);
        assertThat(v0).isNotNull();
        assertThat(v1).isNotNull();
        assertThat(v0.compareTo(v1)).as("%s == %s", left, right).isZero();
    }

    @Test
    void pickVersionPrefersLatestDistTag() {
        List<String> versions = asList("1.0.0", "1.5.0", "2.0.0", "2.1.0");
        NpmRange range = NpmRange.parse(">=1.0.0");
        assertThat(range).isNotNull();
        // "latest" pinned behind the newest satisfying version still wins.
        assertThat(NpmRange.pickVersion(versions, "2.0.0", range)).isEqualTo("2.0.0");
        // "latest" outside the range falls back to maxSatisfying.
        assertThat(NpmRange.pickVersion(versions, "2.0.0", NpmRange.parse("^1.0.0"))).isEqualTo("1.5.0");
        assertThat(NpmRange.pickVersion(versions, null, range)).isEqualTo("2.1.0");
    }

    @Test
    void prereleaseExclusionRule() {
        assertThat(NpmRange.parse("^1.2.3").satisfies("1.9.0-beta.1")).isFalse();
        assertThat(NpmRange.parse("^1.2.3-beta.2").satisfies("1.2.3-beta.4")).isTrue();
        assertThat(NpmRange.parse("^1.2.3-beta.2").satisfies("1.2.4-beta.1")).isFalse();
        assertThat(NpmRange.parse("*").satisfies("1.0.0-rc.1")).isFalse();
    }

    @Test
    void nonRegistrySpecsDoNotParse() {
        assertThat(NpmRange.parse("github:user/repo")).isNull();
        assertThat(NpmRange.parse("file:../local")).isNull();
        assertThat(NpmRange.parse("npm:other-pkg@^1.0.0")).isNull();
        assertThat(NpmRange.parse("https://example.com/x.tgz")).isNull();
        assertThat(NpmRange.parse("workspace:*")).isNull();
        assertThat(NpmRange.parse("latest")).isNull();
    }

    @Test
    void wildcardAndEmptyRanges() {
        List<String> all = collectSatisfying("*", asList("1.0.0", "2.3.4", "0.0.1"));
        assertThat(all).containsExactly("1.0.0", "2.3.4", "0.0.1");
        assertThat(NpmRange.parse("").satisfies("1.2.3")).isTrue();
        assertThat(NpmRange.parse("1.2.3 || ").satisfies("9.9.9")).isTrue();
    }

    private static List<String> collectSatisfying(String range, List<String> versions) {
        NpmRange r = NpmRange.parse(range);
        assertThat(r).isNotNull();
        return versions.stream().filter(r::satisfies).collect(Collectors.toList());
    }
}
