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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.javascript.semver.NodeSemver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yarn's packages/yarnpkg-core/tests/semverUtils.test.ts ({@code simplifyRanges} serves no lock), and what the
 * function pnpm 11.2.2 bundles answers under node over semver 7.8.0 for a version whose prerelease the fallback drops.
 */
class YarnpkgSemverUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
      "^5.0.0|5.0.0-beta.13|true",
      "^1.0.0|1.0.0-rc1|true",
      "2.x|3.0.0-pre.0|false",
      "2.x|2.0.0-pre.0|true",
      "2.x|2.1.0-pre.0|true",
      "1.1.x|1.1.0-a|true",
      "1.1.x|1.1.1-a|true",
      "*|1.0.0-rc1|true",
      "^1.0.0-0|1.0.1-rc1|true",
      "^1.0.0|1.0.1-rc1|true",
      "^1.0.0|1.1.0-rc1|true",
      "1 - 2|2.0.0-pre|true",
      "1 - 2|1.0.0-pre|true",
      "1.0 - 2|1.0.0-pre|true",
      "=0.7.x|0.7.0-asdf|true",
      ">=0.7.x|0.7.0-asdf|true",
      "<=0.7.x|0.7.0-asdf|true",
      ">=1.0.0 <=1.1.0|1.1.0-pre|true",
      "^1.0.0|2.0.0-rc1|false",
      "^1.2.3-rc2|2.0.0|false",
      "1.0.0-alpha.37|1.0.0-alpha.39|false",
      "1.0.0-rc2|1.0.0-rc4|false",
      "1.0.0-rc4|1.0.0-rc2|false",
      "1.0.0-beta.1|1.0.0-beta.2|false",
      "<=5.0.0-beta.0|5.0.0-alpha.7|true",
    })
    void satisfiesWithPrereleases(String spec) {
        String[] row = spec.split("\\|");
        boolean expected = Boolean.parseBoolean(row[2]);
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases(row[1], row[0], false)).as(spec).isEqualTo(expected);
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases(row[1], row[0], true)).as(spec).isEqualTo(expected);
    }

    @Test
    void theDroppedPrereleaseIsTestedAgainstTheComparatorsAsWritten() {
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("1.0.0-beta", ">1.0.0-rc", true)).isTrue();
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("1.0.0-rc.1", "<=1.0.0-rc", true)).isFalse();
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("1.0.0", "<=1.0.0-rc", true)).isFalse();
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("1.0.0-rc", "<1.0.0-beta", true)).isFalse();
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("1.0.0-rc", "<1.0.0-beta", true)).isFalse();
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("2.0.0-rc1", "^1.0.0", true)).isFalse();
    }

    @Test
    void nothingSatisfiesAnInvalidRangeOrNoVersion() {
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("1.0.0", "not a range", true)).isFalse();
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases("", "*", true)).isFalse();
        assertThat(YarnpkgSemverUtils.satisfiesWithPrereleases(null, "*", true)).isFalse();
    }

    // the upstream rows Yarn 4.10.3 tests, which includePrerelease alone answers otherwise
    private static final String[] BERRY_SPECS = {
      "^5.0.0|5.0.0-beta.13|true",
      "^1.0.0|1.0.0-rc1|true",
      "2.x|3.0.0-pre.0|false",
      "2.x|2.0.0-pre.0|true",
      "2.x|2.1.0-pre.0|true",
      "1.1.x|1.1.0-a|true",
      "1.1.x|1.1.1-a|true",
      "*|1.0.0-rc1|true",
      "^1.0.0-0|1.0.1-rc1|true",
      "^1.0.0|1.0.1-rc1|true",
      "^1.0.0|1.1.0-rc1|true",
      "1 - 2|2.0.0-pre|true",
      "1 - 2|1.0.0-pre|true",
      "1.0 - 2|1.0.0-pre|true",
      "=0.7.x|0.7.0-asdf|true",
      ">=0.7.x|0.7.0-asdf|true",
      "<=0.7.x|0.7.0-asdf|true",
      ">=1.0.0 <=1.1.0|1.1.0-pre|true",
      "^1.0.0|2.0.0-rc1|false",
      "^1.2.3-rc2|2.0.0|false",
      "1.0.0-alpha.37|1.0.0-alpha.39|false",
      "1.0.0-rc2|1.0.0-rc4|false",
      "1.0.0-rc4|1.0.0-rc2|false",
      "1.0.0-beta.1|1.0.0-beta.2|false",
      "<=5.0.0-beta.0|5.0.0-alpha.7|true",
    };

    @Test
    void shouldNotBeObsoleteWithIncludePrerelease() {
        int differentResults = 0;
        for (String spec : BERRY_SPECS) {
            String[] row = spec.split("\\|");
            if (YarnpkgSemverUtils.satisfiesWithPrereleases(row[1], row[0], false) !=
                NodeSemver.satisfies(row[1], row[0], NodeSemver.Options.INCLUDE_PRERELEASE)) {
                differentResults++;
            }
        }
        assertThat(differentResults).isGreaterThan(0);
    }

    @Test
    void clean() {
        assertThat(YarnpkgSemverUtils.clean("1.0.0")).isEqualTo("1.0.0");
        assertThat(YarnpkgSemverUtils.clean("1.0.0+123")).isEqualTo("1.0.0+123");
        assertThat(YarnpkgSemverUtils.clean(" \t\r\nv=1.0.0+123 \t\r\n")).isEqualTo("1.0.0+123");
        assertThat(YarnpkgSemverUtils.clean("1.0.0!")).isNull();
    }

    @Test
    void validRangeRefusesAProtocol() {
        assertThat(YarnpkgSemverUtils.validRange("npm:^1.0.0")).isNull();
        assertThat(YarnpkgSemverUtils.validRange("^1.0.0")).isNotNull();
        assertThat(YarnpkgSemverUtils.validRange("latest")).isNull();
    }
}
