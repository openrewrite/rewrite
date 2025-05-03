/*
 * Copyright 2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

class SemverTest {
    @Test
    void validToVersion() {
        assertThat(Semver.validate("latest.release", null).getValue())
          .isInstanceOf(LatestRelease.class);
        assertThat(Semver.validate("latest.integration", null).getValue())
          .isInstanceOf(LatestIntegration.class);
        assertThat(Semver.validate("latest.snapshot", null).getValue())
          .isInstanceOf(LatestIntegration.class);
        assertThat(Semver.validate("1.5 - 2", null).getValue())
          .isInstanceOf(HyphenRange.class);
        assertThat(Semver.validate("1.x", null).getValue())
          .isInstanceOf(XRange.class);
        assertThat(Semver.validate("~1.5", null).getValue())
          .isInstanceOf(TildeRange.class);
        assertThat(Semver.validate("^1.5", null).getValue())
          .isInstanceOf(CaretRange.class);
        assertThat(Semver.validate("[1.5,2)", null).getValue())
          .isInstanceOf(SetRange.class);
        assertThat(Semver.validate("1.5.1", null).getValue())
          .isInstanceOf(ExactVersion.class);
        assertThat(Semver.validate("=1.5.1", null).getValue())
          .isInstanceOf(ExactVersion.class);
        assertThat(Semver.validate("=1.5-1", null).getValue())
          .isInstanceOf(ExactVersion.class);
    }

    @Test
    void majorVersion() {
        assertThat(Semver.majorVersion("")).isEqualTo("");
        assertThat(Semver.majorVersion("1")).isEqualTo("1");
        assertThat(Semver.majorVersion("1.2")).isEqualTo("1");
        assertThat(Semver.majorVersion("1.2.3")).isEqualTo("1");
    }

    @Test
    void minorVersion() {
        assertThat(Semver.minorVersion("")).isEqualTo("");
        assertThat(Semver.minorVersion("1")).isEqualTo("1"); // takes the major also as minor
        assertThat(Semver.minorVersion("1.2")).isEqualTo("2");
        assertThat(Semver.minorVersion("1.2.3")).isEqualTo("2");
    }

    @Test
    void maxVersion() {
        assertThat(Semver.max(null, null)).isNull();
        assertThat(Semver.max(null, "")).isNull();
        assertThat(Semver.max("",  null)).isNull();
        assertThat(Semver.max("3.3.3", null)).isEqualTo("3.3.3");
        assertThat(Semver.max("3.3.3", "")).isEqualTo("3.3.3");
        assertThat(Semver.max(null, "3.3.3")).isEqualTo("3.3.3");
        assertThat(Semver.max("", "3.3.3")).isEqualTo("3.3.3");
        assertThat(Semver.max("4.3.30", "4.3.30.RELEASE")).isEqualTo("4.3.30"); // No label over label
        assertThat(Semver.max("1.0.1RC", "1.0.1-release")).isEqualTo("1.0.1-release");
        assertThat(Semver.max("4.3.30.RELEASE", "4.3.30.RELEASE-2")).isEqualTo("4.3.30.RELEASE"); // Multiple labels with same version takes first label
        assertThat(Semver.max("4.3.30.RELEASE", "4.3.31.RELEASE")).isEqualTo("4.3.31.RELEASE");
        assertThat(Semver.max("INVALID-2023.1.0.1", "1.0.2")).isEqualTo("1.0.2");
        assertThat(Semver.max("INVALID-2023.1.0.3", "1.0.2")).isEqualTo("1.0.2");
        assertThat(Semver.max("1.0.2", "INVALID-2023.1.0.3")).isEqualTo("1.0.2");
    }
}
