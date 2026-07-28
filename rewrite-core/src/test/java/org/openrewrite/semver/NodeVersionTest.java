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

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NodeVersionTest {

    @ParameterizedTest
    @MethodSource("comparisons")
    void greaterThan(String greater, String lesser) {
        NodeVersion g = NodeVersion.parse(greater);
        NodeVersion l = NodeVersion.parse(lesser);
        assertThat(g).as(greater).isNotNull();
        assertThat(l).as(lesser).isNotNull();
        assertThat(g.compareTo(l)).as("%s > %s", greater, lesser).isGreaterThan(0);
        assertThat(l.compareTo(g)).as("%s < %s", lesser, greater).isLessThan(0);
    }

    static Stream<Arguments> comparisons() {
        return NodeSemverCorpus.rows("/node-semver/comparisons.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("equalities")
    void equalIgnoringBuildMetadata(String a, String b) {
        NodeVersion va = NodeVersion.parse(a);
        NodeVersion vb = NodeVersion.parse(b);
        assertThat(va).as(a).isNotNull();
        assertThat(vb).as(b).isNotNull();
        assertThat(va.compareTo(vb)).as("%s == %s", a, b).isZero();
        assertThat(va).isEqualTo(vb);
        assertThat(va.hashCode()).isEqualTo(vb.hashCode());
    }

    static Stream<Arguments> equalities() {
        return NodeSemverCorpus.rows("/node-semver/equality.tsv").stream()
          .map(row -> Arguments.of(row[0], row[1]));
    }

    @ParameterizedTest
    @MethodSource("invalidVersions")
    void invalidVersions(String version) {
        assertThat(NodeVersion.parse(version)).as(version).isNull();
    }

    static Stream<String> invalidVersions() {
        return NodeSemverCorpus.lines("/node-semver/invalid-versions.txt").stream()
          .filter(l -> !l.isEmpty())
          .map(NodeSemverCorpus::unescape);
    }

    @Test
    void prereleaseHasLowerPrecedenceThanRelease() {
        assertThat(NodeVersion.parse("1.0.0-rc.1")).isLessThan(NodeVersion.parse("1.0.0"));
        assertThat(NodeVersion.parse("1.0.0")).isGreaterThan(NodeVersion.parse("1.0.0-rc.1"));
    }

    @Test
    void numericIdentifierLowerThanAlphanumeric() {
        assertThat(NodeVersion.parse("1.0.0-alpha.1")).isLessThan(NodeVersion.parse("1.0.0-alpha.beta"));
        assertThat(NodeVersion.parse("1.0.0-1")).isLessThan(NodeVersion.parse("1.0.0-alpha"));
    }

    @Test
    void longerPrereleaseListIsHigherWhenPrefixEqual() {
        assertThat(NodeVersion.parse("1.0.0-alpha")).isLessThan(NodeVersion.parse("1.0.0-alpha.1"));
    }

    @Test
    void buildMetadataIgnoredInPrecedence() {
        assertThat(NodeVersion.parse("1.0.0+a").compareTo(NodeVersion.parse("1.0.0+b"))).isZero();
        assertThat(NodeVersion.parse("1.2.3+build")).isEqualTo(NodeVersion.parse("1.2.3"));
    }

    @Test
    void numericAndBuildRetainedButPreserveRaw() {
        NodeVersion v = NodeVersion.parse("1.2.3-alpha.1+build.5");
        assertThat(v).isNotNull();
        assertThat(v.getMajor()).isEqualTo(1);
        assertThat(v.getMinor()).isEqualTo(2);
        assertThat(v.getPatch()).isEqualTo(3);
        assertThat(v.isPrerelease()).isTrue();
        assertThat(v.getRaw()).isEqualTo("1.2.3-alpha.1+build.5");
        assertThat(v.toString()).isEqualTo("1.2.3-alpha.1+build.5");
    }

    @Test
    void leadingVAndWhitespaceTolerated() {
        assertThat(NodeVersion.parse(" v1.2.3 ")).isEqualTo(NodeVersion.parse("1.2.3"));
    }

    @Test
    void semverOrgPrecedenceChain() {
        String[] ascending = {
          "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
          "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0"
        };
        for (int i = 0; i < ascending.length - 1; i++) {
            assertThat(NodeVersion.parse(ascending[i]))
              .as("%s < %s", ascending[i], ascending[i + 1])
              .isLessThan(NodeVersion.parse(ascending[i + 1]));
        }
    }
}
