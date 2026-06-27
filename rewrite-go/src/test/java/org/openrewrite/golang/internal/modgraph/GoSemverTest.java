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
package org.openrewrite.golang.internal.modgraph;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoSemverTest {

    private static int sign(int n) {
        return Integer.compare(n, 0);
    }

    @Test
    void totalOrderingMatchesGo() {
        // A strictly-increasing chain. Go's semver.Compare must order it exactly
        // this way; it includes the canonical pre-release precedence example from
        // the SemVer spec plus build metadata, +incompatible, and a pseudo-version.
        List<String> ascending = List.of(
          "",                                  // invalid sorts below every valid version
          "v0.0.0-20210101000000-abcdef012345", // pseudo-version (pre-release)
          "v0.0.0",
          "v0.0.1",
          "v0.1.0",
          "v1.0.0-alpha",
          "v1.0.0-alpha.1",
          "v1.0.0-alpha.beta",
          "v1.0.0-beta",
          "v1.0.0-beta.2",
          "v1.0.0-beta.11",
          "v1.0.0-rc.1",
          "v1.0.0",
          "v1.0.1",
          "v1.2.0",
          "v1.11.0",                           // 11 > 2 numerically, not lexically
          "v2.0.0+incompatible",
          "v2.1.0"
        );

        for (int i = 0; i < ascending.size(); i++) {
            for (int j = 0; j < ascending.size(); j++) {
                int want = Integer.compare(i, j);
                int got = sign(GoSemver.compare(ascending.get(i), ascending.get(j)));
                assertThat(got)
                  .as("compare(%s, %s)", ascending.get(i), ascending.get(j))
                  .isEqualTo(want);
            }
        }
    }

    @Test
    void buildMetadataIsIgnored() {
        assertThat(GoSemver.compare("v1.0.0+build.1", "v1.0.0")).isZero();
        assertThat(GoSemver.compare("v1.0.0+build.1", "v1.0.0+build.2")).isZero();
        // +incompatible is build metadata: equal to the same version without it.
        assertThat(GoSemver.compare("v2.0.0+incompatible", "v2.0.0")).isZero();
    }

    @Test
    void missingMinorAndPatchDefaultToZero() {
        assertThat(GoSemver.compare("v1", "v1.0.0")).isZero();
        assertThat(GoSemver.compare("v1.2", "v1.2.0")).isZero();
        assertThat(GoSemver.compare("v1", "v1.0.1")).isNegative();
    }

    @Test
    void invalidVersionsSortBelowValidAndEqualEachOther() {
        assertThat(GoSemver.compare("", "v0.0.0")).isNegative();
        assertThat(GoSemver.compare("1.0.0", "v1.0.0")).isNegative(); // missing 'v'
        assertThat(GoSemver.compare("v01.0.0", "v1.0.0")).isNegative(); // leading zero invalid
        assertThat(GoSemver.compare("garbage", "also-garbage")).isZero();
    }

    @Test
    void sortingShuffledVersionsYieldsGoOrder() {
        List<String> versions = new ArrayList<>(List.of(
          "v1.2.0", "v1.0.0", "v1.0.0-rc.1", "v0.9.0", "v1.0.0-alpha", "v1.10.0"
        ));
        Collections.shuffle(versions, new java.util.Random(42));
        versions.sort(GoSemver::compare);
        assertThat(versions).containsExactly(
          "v0.9.0", "v1.0.0-alpha", "v1.0.0-rc.1", "v1.0.0", "v1.2.0", "v1.10.0"
        );
    }
}
