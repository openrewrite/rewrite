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
    }
}
