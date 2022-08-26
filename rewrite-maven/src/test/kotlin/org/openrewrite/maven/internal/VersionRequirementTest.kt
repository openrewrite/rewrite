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
package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.function.Supplier

class VersionRequirementTest {
    private val available = Supplier<Iterable<String>> { (1..10).map { it.toString() } }

    @Test
    fun rangeSet() {
        assertThat(VersionRequirement.fromVersion("[1,11)", 0).resolve(available))
            .isEqualTo("10")
    }

    @Test
    fun multipleSoftRequirements() {
        assertThat(VersionRequirement.fromVersion("1", 1).addRequirement("2").resolve(available))
            .isEqualTo("1")
    }

    @Test
    fun softRequirementThenHardRequirement() {
        assertThat(VersionRequirement.fromVersion("1", 1).addRequirement("[1,11]")
            .resolve(available))
            .isEqualTo("10")
    }

    @Test
    fun hardRequirementThenSoftRequirement() {
        assertThat(VersionRequirement.fromVersion("[1,11]", 1).addRequirement("1")
            .resolve(available))
            .isEqualTo("10")
    }

    @Test
    fun nearestRangeWins() {
        assertThat(VersionRequirement.fromVersion("[1,2]", 1).addRequirement("[9,10]")
            .resolve(available))
            .isEqualTo("2")
    }
}
