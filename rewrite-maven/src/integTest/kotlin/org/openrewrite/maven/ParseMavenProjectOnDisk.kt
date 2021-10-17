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

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.maven.cache.LocalMavenArtifactCache
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache
import org.openrewrite.maven.cache.RocksdbMavenPomCache
import org.openrewrite.maven.internal.MavenParsingException
import org.openrewrite.maven.utilities.MavenArtifactDownloader
import org.openrewrite.maven.utilities.MavenProjectParser
import java.nio.file.Paths
import java.util.function.Consumer

object ParseMavenProjectOnDisk {
    @JvmStatic
    fun main(args: Array<String>) {
        val projectDir = Paths.get(args.first())

        val errorConsumer = Consumer<Throwable> { t ->
            if (t is MavenParsingException) {
                println("  ${t.message}")
            } else {
                t.printStackTrace()
            }
        }

        val downloader = MavenArtifactDownloader(
            ReadOnlyLocalMavenArtifactCache.mavenLocal().orElse(
                LocalMavenArtifactCache(Paths.get(System.getProperty("user.home"), ".rewrite", "cache", "artifacts"))
            ),
            null,
            errorConsumer
        )

        val pomCache = RocksdbMavenPomCache(
            Paths.get(System.getProperty("user.home"), ".rewrite", "cache", "poms"),
        )

        val mavenParserBuilder = MavenParser.builder()
            .cache(pomCache)
            .mavenConfig(projectDir.resolve(".mvn/maven.config"))

        val parser = MavenProjectParser(
            downloader,
            mavenParserBuilder,
            JavaParser.fromJavaVersion(),
            InMemoryExecutionContext(errorConsumer)
        )

        parser.parse(projectDir).forEach {
            println(it.sourcePath)
        }
    }
}
