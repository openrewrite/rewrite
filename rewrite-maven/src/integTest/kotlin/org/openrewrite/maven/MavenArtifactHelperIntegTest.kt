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
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.cache.InMemoryMavenPomCache
import org.openrewrite.maven.utilities.MavenArtifactHelper

class MavenArtifactHelperIntegTest {

    @Test
    fun downloadArtifactAndDependencies() {
        val artifactPaths = MavenArtifactHelper.downloadArtifactAndDependencies(
                "org.openrewrite.recipe",
                "rewrite-spring",
                "4.0.0",
                InMemoryExecutionContext { t -> t.printStackTrace() },
                InMemoryMavenPomCache()
        )
        assertThat(artifactPaths).hasSizeGreaterThan(0)
        val firstArtifact = artifactPaths[0]
        assertThat(firstArtifact.fileName.toString()).contains("rewrite-spring")
        assertThat(firstArtifact.fileName.toString()).endsWith(".jar")
        println(artifactPaths)
    }
}