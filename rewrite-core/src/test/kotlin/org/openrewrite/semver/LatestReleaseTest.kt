/*
 * Copyright 2020 the original author or authors.
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

class LatestReleaseTest {
    private val latestRelease = LatestRelease()

    @Test
    fun onlyNumericPartsValid() {
        assertThat(latestRelease.isValid("1.1.1")).isTrue()
        assertThat(latestRelease.isValid("1.1")).isTrue()
        assertThat(latestRelease.isValid("1")).isTrue()
        assertThat(latestRelease.isValid("1.1.a")).isFalse()
    }

    @Test
    fun differentPatchVersions() {
        assertThat(latestRelease.compare("1.1.1", "1.1.2")).isLessThan(0)
    }

    @Test
    fun differentMinorVersions() {
        assertThat(latestRelease.compare("1.1.1", "1.2.1")).isLessThan(0)
        assertThat(latestRelease.compare("1.1", "1.2")).isLessThan(0)
    }

    @Test
    fun differentMajorVersions() {
        assertThat(latestRelease.compare("1.1.1", "2.1.1")).isLessThan(0)
        assertThat(latestRelease.compare("1.1", "2.1")).isLessThan(0)
        assertThat(latestRelease.compare("1", "2")).isLessThan(0)
    }

    @Test
    fun differentNumberOfParts() {
        assertThat(latestRelease.compare("1.1", "1.1.1")).isLessThan(0)
        assertThat(latestRelease.compare("1", "1.1")).isLessThan(0)
    }
}
