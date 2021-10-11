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

class LatestPatchTest {
    private val latestPatch = LatestPatch(null)

    @Test
    fun isValid() {
        assertThat(latestPatch.isValid("1.0.0", "1.0.0")).isTrue
        assertThat(latestPatch.isValid("1.0.0", "1.0.0.1")).isTrue
        assertThat(latestPatch.isValid("1.0.0", "1.0.1")).isTrue
        assertThat(latestPatch.isValid("1.0", "1.0.1")).isTrue
        assertThat(latestPatch.isValid("1.0.0", "1.1.0")).isFalse
        assertThat(latestPatch.isValid("1.0.0", "2.0.0")).isFalse
    }

    @Test
    fun compare() {
        assertThat(latestPatch.compare("1.0", "1.0.1", "1.0.2")).isLessThan(0)
        assertThat(latestPatch.compare("1.0", "1.0.0.1", "1.0.1")).isLessThan(0)
    }
}
