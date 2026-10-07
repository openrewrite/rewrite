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
package org.openrewrite.javascript.internal.lock.berry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.semver.NodeSemver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Yarn's packages/yarnpkg-core/tests/semverUtils.test.ts: {@code satisfiesWithPrereleases} and {@code clean}
 * ({@code simplifyRanges} serves commands that never write a lock and is not ported).
 */
class SemverUtilsTest {

    @ParameterizedTest(name = "{0} with {1}: {2}")
    @CsvSource(delimiter = '|', value = {
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
    void satisfiesWithPrereleases(String range, String version, boolean satisfied) {
        assertThat(SemverUtils.satisfiesWithPrereleases(version, range)).isEqualTo(satisfied);
    }

    @Test
    void shouldNotBeObsoleteWithIncludePrerelease() {
        assertThat(SemverUtils.satisfiesWithPrereleases("5.0.0-alpha.7", "<=5.0.0-beta.0")).isTrue();
        assertThat(NodeSemver.satisfies("1.0.0-alpha.39", "1.0.0-alpha.37", NodeSemver.Options.INCLUDE_PRERELEASE))
                .isFalse();
    }

    @Test
    void clean() {
        assertThat(SemverUtils.clean("1.0.0")).isEqualTo("1.0.0");
        assertThat(SemverUtils.clean("1.0.0+123")).isEqualTo("1.0.0+123");
        assertThat(SemverUtils.clean(" \t\r\nv=1.0.0+123 \t\r\n")).isEqualTo("1.0.0+123");
        assertThat(SemverUtils.clean("1.0.0!")).isNull();
    }

    @Test
    void validRangeRefusesAProtocol() {
        assertThat(SemverUtils.validRange("npm:^1.0.0")).isNull();
        assertThat(SemverUtils.validRange("^1.0.0")).isNotNull();
        assertThat(SemverUtils.validRange("latest")).isNull();
    }
}
