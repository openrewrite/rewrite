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
package org.openrewrite.semver

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SemverTest {
    @Test
    fun validToVersion() {
        assertThat(Semver.validate("latest.release", null).getValue<VersionComparator>())
            .isInstanceOf(LatestRelease::class.java)
        assertThat(Semver.validate("1.5 - 2", null).getValue<VersionComparator>())
            .isInstanceOf(HyphenRange::class.java)
        assertThat(Semver.validate("1.x", null).getValue<VersionComparator>())
            .isInstanceOf(XRange::class.java)
        assertThat(Semver.validate("~1.5", null).getValue<VersionComparator>())
            .isInstanceOf(TildeRange::class.java)
        assertThat(Semver.validate("^1.5", null).getValue<VersionComparator>())
            .isInstanceOf(CaretRange::class.java)
        assertThat(Semver.validate("[1.5,2)", null).getValue<VersionComparator>())
            .isInstanceOf(SetRange::class.java)
    }
}
