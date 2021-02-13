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
import org.openrewrite.Parser
import org.openrewrite.java.JavaParser
import org.openrewrite.maven.cache.LocalMavenArtifactCache
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache
import org.openrewrite.maven.internal.MavenParsingException
import org.openrewrite.maven.utilities.MavenArtifactDownloader
import org.openrewrite.maven.utilities.MavenProjectParser
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

object ParseMavenProjectOnDisk {
    @JvmStatic
    fun main(args: Array<String>) {
        val errorConsumer = Consumer<Throwable> { t ->
            if (t is MavenParsingException) {
                println("  ${t.message}")
            } else {
                t.printStackTrace()
            }
        }

        val onParse = object : Parser.Listener {
            var n = 1
            override fun onParseSucceeded(sourcePath: Path) {
                println("${n++} SUCCESS - $sourcePath")
            }

            override fun onParseFailed(sourcePath: Path) {
                println("${n++} FAILED - $sourcePath")
            }
        }

        val downloader = MavenArtifactDownloader(
            ReadOnlyLocalMavenArtifactCache.MAVEN_LOCAL.orElse(
                LocalMavenArtifactCache(Paths.get(System.getProperty("user.home"), ".rewrite-cache"))
            ),
            null,
            errorConsumer
        )

        val parser = MavenProjectParser(
            downloader,
            MavenParser.builder()
                .doOnParse(onParse)
                .resolveOptional(false),
            JavaParser.fromJavaVersion(),
            InMemoryExecutionContext(errorConsumer)
        )

        parser.parse(Paths.get(args.first())).forEach {
            println(it.sourcePath)
        }
    }
}
