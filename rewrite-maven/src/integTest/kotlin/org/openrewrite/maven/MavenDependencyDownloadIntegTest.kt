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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.cache.LocalMavenArtifactCache
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache
import org.openrewrite.maven.tree.Scope
import org.openrewrite.maven.utilities.MavenArtifactDownloader
import java.nio.file.Path

class MavenDependencyDownloadIntegTest {
    private val ctx = InMemoryExecutionContext { t -> t.printStackTrace() }

    private fun downloader(path: Path) = MavenArtifactDownloader(
        ReadOnlyLocalMavenArtifactCache.mavenLocal().orElse(
            LocalMavenArtifactCache(path)
        ),
        null
    ) { t -> throw t }

    @Test
    fun springWebMvc(@TempDir tempDir: Path) {
        val maven = MavenParser.builder()
            .build()
            .parse(ctx, singleDependencyPom("org.springframework:spring-webmvc:5.3.8"))
            .first()

        val compileDependencies = maven.mavenResolutionResult().dependencies[Scope.Compile]!!

        compileDependencies.forEach { dep ->
            println("${dep.repository} ${dep.gav}")
        }

        val downloader = downloader(tempDir)
        compileDependencies.forEach { dep ->
            println(dep.gav.toString() + downloader.downloadArtifact(dep))
        }
    }

    @Test
    fun rewriteCore(@TempDir tempDir: Path) {
        val maven = MavenParser.builder()
            .build()
            .parse(ctx, singleDependencyPom("org.openrewrite:rewrite-core:6.0.1"))
            .first()

        val runtimeDependencies = maven.mavenResolutionResult().dependencies[Scope.Runtime]!!
            .sortedBy { d -> d.gav.toString() }

        val downloader = downloader(tempDir)
        runtimeDependencies.forEach { dep ->
            println(dep.gav.toString() + downloader.downloadArtifact(dep))
        }
    }
}
