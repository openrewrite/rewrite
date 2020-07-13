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
package org.openrewrite.maven

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.maven.tree.MavenModel

class MavenModelTest {
    @Test
    fun compareModuleVersionIdDifferentGroups() {
        assertThat(mvid("o.s.b", "a").compareTo(mvid("o.s", "a"))).isGreaterThan(0)
        assertThat(mvid("o.s.c", "a").compareTo(mvid("o.s.b", "a"))).isGreaterThan(0)
        assertThat(mvid("o.s.b", "a").compareTo(mvid("o.s.b", "a"))).isEqualTo(0)
    }

    @Test
    fun compareModuleVersionIdDifferentArtifacts() {
        assertThat(mvid("g", "a-b").compareTo(mvid("g", "a"))).isGreaterThan(0)
        assertThat(mvid("g", "a.b").compareTo(mvid("g", "a"))).isGreaterThan(0)
        assertThat(mvid("g", "a-c").compareTo(mvid("g", "a-b"))).isGreaterThan(0)
        assertThat(mvid("g", "a").compareTo(mvid("g", "a"))).isEqualTo(0)
    }

    private fun mvid(group: String, artifact: String) = MavenModel.ModuleVersionId(group, artifact, "1")
}
