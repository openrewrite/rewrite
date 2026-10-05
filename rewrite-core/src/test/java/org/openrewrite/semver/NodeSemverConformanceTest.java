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
import org.openrewrite.Validated;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openrewrite.semver.Semver.Ecosystem.MAVEN;
import static org.openrewrite.semver.Semver.Ecosystem.NODE;

/**
 * Drives the node-semver conformance corpus (transcribed from npm/node-semver's own test fixtures,
 * see {@code src/test/resources/node-semver}) against the {@link Semver.Ecosystem#NODE} selector
 * chain and its supporting internals.
 */
class NodeSemverConformanceTest {

    // --- strict version parsing and SemVer 2.0.0 section 11 precedence ---

    @ParameterizedTest
    @MethodSource("comparisons")
    void greaterThan(String greater, String lesser) {
        ParsedVersion g = ParsedVersion.parse(greater);
        ParsedVersion l = ParsedVersion.parse(lesser);
        assertThat(g.isStrictSemver()).as(greater).isTrue();
        assertThat(l.isStrictSemver()).as(lesser).isTrue();
        assertThat(g.comparePrecedence(l)).as("%s > %s", greater, lesser).isGreaterThan(0);
        assertThat(l.comparePrecedence(g)).as("%s < %s", lesser, greater).isLessThan(0);
    }

    static Stream<Arguments> comparisons() {
        return NodeSemverCorpus.rows("/node-semver/comparisons.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("equalities")
    void equalIgnoringBuildMetadata(String a, String b) {
        ParsedVersion va = ParsedVersion.parse(a);
        ParsedVersion vb = ParsedVersion.parse(b);
        assertThat(va.isStrictSemver()).as(a).isTrue();
        assertThat(vb.isStrictSemver()).as(b).isTrue();
        assertThat(va.comparePrecedence(vb)).as("%s == %s", a, b).isZero();
        assertThat(vb.comparePrecedence(va)).as("%s == %s", b, a).isZero();
    }

    static Stream<Arguments> equalities() {
        return NodeSemverCorpus.rows("/node-semver/equality.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("invalidVersions")
    void invalidVersions(String version) {
        assertThat(ParsedVersion.parse(version).isStrictSemver()).as(version).isFalse();
    }

    static Stream<String> invalidVersions() {
        return NodeSemverCorpus.lines("/node-semver/invalid-versions.txt").stream()
          .filter(l -> !l.isEmpty())
          .map(NodeSemverCorpus::unescape);
    }

    @Test
    void semverOrgPrecedenceChain() {
        String[] ascending = {
          "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
          "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0"
        };
        for (int i = 0; i < ascending.length - 1; i++) {
            assertThat(Semver.compare(ascending[i], ascending[i + 1], NODE))
              .as("%s < %s", ascending[i], ascending[i + 1])
              .isNegative();
        }
    }

    @Test
    void strictComponentsAndCanonicalForm() {
        ParsedVersion v = ParsedVersion.parse("1.2.3-alpha.1+build.5");
        assertThat(v.isStrictSemver()).isTrue();
        assertThat(v.strictMajor()).isEqualTo(1);
        assertThat(v.strictMinor()).isEqualTo(2);
        assertThat(v.strictPatch()).isEqualTo(3);
        assertThat(v.hasPrerelease()).isTrue();
        assertThat(v.strictToString()).isEqualTo("1.2.3-alpha.1+build.5");
    }

    @Test
    void leadingVAndWhitespaceTolerated() {
        assertThat(Semver.compare(" v1.2.3 ", "1.2.3", NODE)).isZero();
    }

    @Test
    void largeNumericComponentsParseWithoutOverflow() {
        ParsedVersion v = ParsedVersion.parse("2147483648.0.0");
        assertThat(v.isStrictSemver()).isTrue();
        assertThat(v.strictMajor()).isEqualTo(2147483648L);
        assertThat(Semver.compare("2147483648.0.0", "2147483647.999.999", NODE)).isPositive();
    }

    @Test
    void numericComponentsOverflowingLongAreInvalidNotCrashing() {
        // Beyond Long.MAX_VALUE — must be reported as invalid, never throw.
        assertThat(ParsedVersion.parse("99999999999999999999.0.0").isStrictSemver()).isFalse();
        assertThat(Semver.satisfies("99999999999999999999.0.0", "^1.0.0", NODE)).isFalse();
    }

    // --- range desugaring ---

    @ParameterizedTest
    @MethodSource("rangeParse")
    void desugars(String range, String expected) {
        Validated<VersionComparator> validated = Semver.validate(range, null, NODE);
        assertThat(validated.isValid()).as(range).isTrue();
        assertThat(requireNonNull(validated.getValue()).toString()).as(range).isEqualTo(expected);
    }

    static Stream<Arguments> rangeParse() {
        return NodeSemverCorpus.rows("/node-semver/range-parse.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @Test
    void upperBoundCarriesPrereleaseTail() {
        // The npm-specific `-0` convention that excludes 2.0.0-alpha from ^1.2.3.
        assertThat(requireNonNull(Semver.validate("^1.2.3", null, NODE).getValue()).toString())
          .isEqualTo(">=1.2.3 <2.0.0-0");
        assertThat(requireNonNull(Semver.validate("~1.2.3", null, NODE).getValue()).toString())
          .isEqualTo(">=1.2.3 <1.3.0-0");
    }

    // --- satisfies ---

    @ParameterizedTest
    @MethodSource("include")
    void satisfies(String range, String version, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is not supported");
        if ("incPre".equals(options)) {
            assertThat(UnionRange.satisfies(version, range, true))
              .as("%s satisfies %s (%s)", version, range, options)
              .isTrue();
        } else {
            assertThat(Semver.satisfies(version, range, NODE))
              .as("%s satisfies %s", version, range)
              .isTrue();
        }
    }

    static Stream<Arguments> include() {
        return options3("/node-semver/range-include.tsv");
    }

    @ParameterizedTest
    @MethodSource("exclude")
    void notSatisfies(String range, String version, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is not supported");
        if ("incPre".equals(options)) {
            assertThat(UnionRange.satisfies(version, range, true))
              .as("%s does not satisfy %s (%s)", version, range, options)
              .isFalse();
        } else {
            assertThat(Semver.satisfies(version, range, NODE))
              .as("%s does not satisfy %s", version, range)
              .isFalse();
        }
    }

    static Stream<Arguments> exclude() {
        return options3("/node-semver/range-exclude.tsv");
    }

    private static Stream<Arguments> options3(String resource) {
        return NodeSemverCorpus.rows(resource).stream()
          .map(row -> Arguments.of(row[0], row[1], row.length > 2 ? row[2] : ""));
    }

    @Test
    void prereleaseUpperBoundGuard() {
        // The `-0` bound excludes prerelease of the next major.
        assertThat(Semver.satisfies("2.0.0-alpha", "^1.2.3", NODE)).isFalse();
        assertThat(UnionRange.satisfies("2.0.0-alpha", "^1.2.3", true)).isFalse();
    }

    @Test
    void prereleaseInclusionGuard() {
        // No comparator names 1.2.4 with a prerelease -> rejected.
        assertThat(Semver.satisfies("1.2.4-beta.1", ">=1.2.3", NODE)).isFalse();
        // A comparator naming the same tuple with a prerelease -> allowed.
        assertThat(Semver.satisfies("1.2.3-beta.2", ">=1.2.3-beta.1 <1.2.4", NODE)).isTrue();
        // includePrerelease disables the guard entirely.
        assertThat(UnionRange.satisfies("1.2.4-beta.1", ">=1.2.3", true)).isTrue();
    }

    @Test
    void wildcardRanges() {
        // Wildcard collapse forms aren't in range-parse.tsv; assert them via satisfies behavior.
        assertThat(Semver.satisfies("1.2.3", "*", NODE)).isTrue();
        assertThat(Semver.satisfies("1.2.3", "", NODE)).isTrue();
        assertThat(Semver.satisfies("1.2.3", "x", NODE)).isTrue();
        assertThat(Semver.satisfies("1.2.3", ">=*", NODE)).isTrue();
    }

    // --- validRange ---

    @Test
    void validRange() {
        assertThat(Semver.validate("^1.2.3", null, NODE).isValid()).isTrue();
        assertThat(Semver.validate(">=1.0.0 <2.0.0", null, NODE).isValid()).isTrue();
        assertThat(Semver.validate("1.2.x || 2.x", null, NODE).isValid()).isTrue();
        assertThat(Semver.validate("not-a-range", null, NODE).isValid()).isFalse();
        assertThat(Semver.validate("^1.2.3.4", null, NODE).isValid()).isFalse();
        assertThat(Semver.validate(">=1.0.0 <>2.0.0", null, NODE).isValid()).isFalse();
    }

    @Test
    void nonSemverRangesAreInvalid() {
        assertThat(Semver.satisfies("1.2.3", "workspace:*", NODE)).isFalse();
        assertThat(Semver.satisfies("1.2.3", "npm:foo@1.2.3", NODE)).isFalse();
        assertThat(Semver.satisfies("1.2.3", "latest", NODE)).isFalse();
        assertThat(Semver.validate("git+https://example.com/repo.git", null, NODE).isValid()).isFalse();
    }

    @Test
    void oversizedComponentsInvalidateTheRangeInsteadOfThrowing() {
        // Desugaring `^2147483648.0.0` requires incrementing a component beyond Integer range.
        assertThat(Semver.validate("^2147483648.0.0", null, NODE).isValid()).isTrue();
        assertThat(Semver.satisfies("2147483648.5.0", "^2147483648.0.0", NODE)).isTrue();
        // A component beyond Long range invalidates the range rather than crashing.
        assertThat(Semver.validate("^9223372036854775807.0.0", null, NODE).isValid()).isFalse();
    }

    // --- maxSatisfying / minSatisfying ---

    @ParameterizedTest
    @MethodSource("maxSatisfying")
    void maxSatisfying(String versions, String range, String expected, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is not supported");
        assertThat(Semver.maxSatisfying(Arrays.asList(versions.split(" ")), range, NODE))
          .as("maxSatisfying(%s, %s)", versions, range)
          .isEqualTo(expected);
    }

    static Stream<Arguments> maxSatisfying() {
        return satisfyingRows("/node-semver/max-satisfying.tsv");
    }

    @ParameterizedTest
    @MethodSource("minSatisfying")
    void minSatisfying(String versions, String range, String expected, String options) {
        assumeTrue(!"loose".equals(options), "loose parsing is not supported");
        assertThat(UnionRange.minSatisfying(Arrays.asList(versions.split(" ")), range, false))
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
    void maxSatisfyingReturnsNullWhenNoneMatch() {
        assertThat(Semver.maxSatisfying(Arrays.asList("1.0.0", "1.1.0"), "^2.0.0", NODE)).isNull();
    }

    @Test
    void maxSatisfyingReturnsOriginalSpellingAndFirstSeenTieWinner() {
        // The winner keeps the caller's spelling verbatim; the lock engine writes it into lock files.
        assertThat(Semver.maxSatisfying(Arrays.asList("1.2.3", "v1.2.4"), "^1.2.3", NODE)).isEqualTo("v1.2.4");
        // Build-metadata-only differences tie under section 11; the first seen wins deterministically.
        assertThat(Semver.maxSatisfying(Arrays.asList("1.2.3+a", "1.2.3+b"), "^1.2.3", NODE)).isEqualTo("1.2.3+a");
    }

    @Test
    void maxSatisfyingDefaultAppliesToMavenComparatorsToo() {
        LatestRelease latestRelease = new LatestRelease(null);
        assertThat(latestRelease.maxSatisfying(Arrays.asList("1.0.0", "2.0.0-RC1", "2.0.0", "1.9.9")))
          .isEqualTo(Optional.of("2.0.0"));
    }

    // --- gtr / ltr ---

    @ParameterizedTest
    @MethodSource("gtRange")
    void gtr(String range, String version) {
        assertThat(UnionRange.gtr(version, range)).as("%s > %s", version, range).isTrue();
    }

    static Stream<Arguments> gtRange() {
        return NodeSemverCorpus.rows("/node-semver/version-gt-range.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("ltRange")
    void ltr(String range, String version) {
        assertThat(UnionRange.ltr(version, range)).as("%s < %s", version, range).isTrue();
    }

    static Stream<Arguments> ltRange() {
        return NodeSemverCorpus.rows("/node-semver/version-lt-range.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    // --- compare ---

    @Test
    void compare() {
        assertThat(Semver.compare("1.2.3", "1.2.4", NODE)).isNegative();
        assertThat(Semver.compare("1.2.3", "1.2.3+build", NODE)).isZero();
        assertThat(Semver.compare("2.0.0", "1.9.9", NODE)).isPositive();
        assertThatThrownBy(() -> Semver.compare("not-a-version", "1.0.0", NODE))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("not-a-version");
    }

    @Test
    void mavenCompareIsTotalOverArbitraryStrings() {
        assertThat(Semver.compare("2.0.0", "1.9.9", MAVEN)).isPositive();
        assertThat(Semver.compare("3.5.0-RC1", "3.5.0", MAVEN)).isNegative();
    }

    // --- VersionComparator family integration ---

    @Test
    void instanceCompareIsLenientForFamilyIntegration() {
        VersionComparator comparator = requireNonNull(Semver.validate("^1.2.3", null, NODE).getValue());
        assertThat(comparator.compare(null, "1.2.4", "1.2.3")).isPositive();
        // Non-semver strings sort below any strict version and never throw.
        assertThat(comparator.compare(null, "not-a-version", "1.2.3")).isNegative();
        // Two non-semver strings order lexicographically, deterministically.
        assertThat(comparator.compare(null, "also-not", "not-a-version")).isNegative();
        assertThat(comparator.compare(null, "not-a-version", "also-not")).isPositive();
    }

    @Test
    void upgradeIsSafeWhenCurrentVersionIsARangeExpression() {
        // In a package.json the "current version" a recipe holds is itself a range constraint.
        VersionComparator comparator = requireNonNull(Semver.validate("^2.0.0", null, NODE).getValue());
        assertThat(comparator.upgrade("^1.2.3", Arrays.asList("1.9.0", "2.0.5", "2.1.0")))
          .isEqualTo(Optional.of("2.1.0"));
    }

    @Test
    void metadataPatternComposesWithNodeRanges() {
        VersionComparator comparator = requireNonNull(Semver.validate("^1.0.0", "\\+backpatch.*", NODE).getValue());
        assertThat(comparator.isValid(null, "1.0.5+backpatch.001")).isTrue();
        assertThat(comparator.isValid(null, "1.0.5")).isFalse();
        assertThat(comparator.isValid(null, "2.0.0+backpatch.001")).isFalse();
        // The metadata pattern does not bypass npm's prerelease gating.
        VersionComparator beta = requireNonNull(Semver.validate("^1.0.0", "-beta.*", NODE).getValue());
        assertThat(beta.isValid(null, "1.0.0-beta.1")).isFalse();
    }
}
