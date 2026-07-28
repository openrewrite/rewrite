/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.semver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class NodeSemverTest {

    @ParameterizedTest
    @MethodSource("include")
    void satisfies(String range, String version, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is out of Phase-A scope");
        boolean incPre = "incPre".equals(options);
        assertThat(NodeSemver.satisfies(version, range, incPre))
          .as("%s satisfies %s (%s)", version, range, options)
          .isTrue();
    }

    static Stream<Arguments> include() {
        return options3("/node-semver/range-include.tsv");
    }

    @ParameterizedTest
    @MethodSource("exclude")
    void notSatisfies(String range, String version, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is out of Phase-A scope");
        boolean incPre = "incPre".equals(options);
        assertThat(NodeSemver.satisfies(version, range, incPre))
          .as("%s does not satisfy %s (%s)", version, range, options)
          .isFalse();
    }

    static Stream<Arguments> exclude() {
        return options3("/node-semver/range-exclude.tsv");
    }

    private static Stream<Arguments> options3(String resource) {
        return NodeSemverCorpus.rows(resource).stream()
          .map(row -> Arguments.of(row[0], row[1], row.length > 2 ? row[2] : ""));
    }

    @ParameterizedTest
    @MethodSource("gtRange")
    void gtr(String range, String version) {
        assertThat(NodeSemver.gtr(version, range)).as("%s > %s", version, range).isTrue();
    }

    static Stream<Arguments> gtRange() {
        return NodeSemverCorpus.rows("/node-semver/version-gt-range.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("ltRange")
    void ltr(String range, String version) {
        assertThat(NodeSemver.ltr(version, range)).as("%s < %s", version, range).isTrue();
    }

    static Stream<Arguments> ltRange() {
        return NodeSemverCorpus.rows("/node-semver/version-lt-range.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("maxSatisfying")
    void maxSatisfying(String versions, String range, String expected, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is out of Phase-A scope");
        assertThat(NodeSemver.maxSatisfying(Arrays.asList(versions.split(" ")), range))
          .as("maxSatisfying(%s, %s)", versions, range)
          .isEqualTo(expected);
    }

    static Stream<Arguments> maxSatisfying() {
        return satisfyingRows("/node-semver/max-satisfying.tsv");
    }

    @ParameterizedTest
    @MethodSource("minSatisfying")
    void minSatisfying(String versions, String range, String expected, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is out of Phase-A scope");
        assertThat(NodeSemver.minSatisfying(Arrays.asList(versions.split(" ")), range))
          .as("minSatisfying(%s, %s)", versions, range)
          .isEqualTo(expected);
    }

    static Stream<Arguments> minSatisfying() {
        return satisfyingRows("/node-semver/min-satisfying.tsv");
    }

    private static Stream<Arguments> satisfyingRows(String resource) {
        return NodeSemverCorpus.rows(resource).stream()
          .map(row -> Arguments.of(row[0], row[1], row[2], row.length > 3 ? row[3] : ""));
    }

    @Test
    void prereleaseUpperBoundGuard() {
        // The `-0` bound excludes prerelease of the next major.
        assertThat(NodeSemver.satisfies("2.0.0-alpha", "^1.2.3")).isFalse();
        assertThat(NodeSemver.satisfies("2.0.0-alpha", "^1.2.3", true)).isFalse();
    }

    @Test
    void prereleaseInclusionGuard() {
        // No comparator names 1.2.4 with a prerelease -> rejected.
        assertThat(NodeSemver.satisfies("1.2.4-beta.1", ">=1.2.3")).isFalse();
        // A comparator naming the same tuple with a prerelease -> allowed.
        assertThat(NodeSemver.satisfies("1.2.3-beta.2", ">=1.2.3-beta.1 <1.2.4")).isTrue();
        // includePrerelease disables the guard entirely.
        assertThat(NodeSemver.satisfies("1.2.4-beta.1", ">=1.2.3", true)).isTrue();
    }

    @Test
    void nonSemverRangesAreInvalid() {
        assertThat(NodeSemver.satisfies("1.2.3", "workspace:*")).isFalse();
        assertThat(NodeSemver.satisfies("1.2.3", "npm:foo@1.2.3")).isFalse();
        assertThat(NodeSemver.satisfies("1.2.3", "latest")).isFalse();
        assertThat(NodeSemver.validRange("git+https://example.com/repo.git")).isFalse();
    }

    @Test
    void maxSatisfyingReturnsNullWhenNoneMatch() {
        assertThat(NodeSemver.maxSatisfying(Arrays.asList("1.0.0", "1.1.0"), "^2.0.0")).isNull();
    }

    @Test
    void compare() {
        assertThat(NodeSemver.compare("1.2.3", "1.2.4")).isNegative();
        assertThat(NodeSemver.compare("1.2.3", "1.2.3+build")).isZero();
        assertThat(NodeSemver.compare("2.0.0", "1.9.9")).isPositive();
    }
}
