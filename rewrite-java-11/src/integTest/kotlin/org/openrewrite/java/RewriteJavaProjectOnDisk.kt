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
package org.openrewrite.java

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.config.YamlResourceLoader
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.function.BiPredicate
import kotlin.io.path.ExperimentalPathApi
import kotlin.streams.toList

object RewriteJavaProjectOnDisk {
    @ExperimentalPathApi
    @JvmStatic
    fun main(args: Array<String>) {
        val srcDir = Paths.get(args[0])
//        val recipe: Recipe = Class.forName(args[1]).getDeclaredConstructor().newInstance() as Recipe
        val recipe = OrderImports(false)

        val predicate = BiPredicate<Path, BasicFileAttributes> { p, bfa ->
            bfa.isRegularFile && p.fileName.toString().endsWith(".java") &&
                    !p.toString().contains("/grammar/") &&
                    !p.toString().contains("/gen/")
        }

        val paths = Files.find(srcDir, 999, predicate)
            .limit(if (args.size > 2) args[2].toLong() else Long.MAX_VALUE)
            .toList()

        val listener = object : Parser.Listener {
            override fun onWarn(message: String, t: Throwable) {
                t.printStackTrace()
            }

            override fun onError(message: String, t: Throwable) {
                t.printStackTrace()
            }
        }

        val parser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(false) // optional, for quiet parsing
            .doOnParse(listener)
            .build()

        val style = YamlResourceLoader("""
            type: specs.openrewrite.org/v1beta/style
            name: com.netflix.eureka.Style
            displayName: Eureka style
            styleConfigs:
              - org.openrewrite.java.style.ImportLayoutStyle:
                  classCountToUseStarImport: 999
                  nameCountToUseStarImport: 999
                  layout:
                    - import java.*
                    - <blank line>
                    - import all other imports
                    - <blank line>
                    - import javax.*
                    - <blank line>
                    - import static all other imports
        """.trimIndent().byteInputStream(), URI.create("eureka.yml"), Properties()).listStyles().first()

        val sourceFiles: List<SourceFile> = parser.parse(paths, srcDir, InMemoryExecutionContext())
            .map { it.withMarker(style) }

        for (sourceFile in sourceFiles) {
            println(sourceFile.sourcePath)
            recipe.run(listOf(sourceFile)).map {
                println(it.diff())
                if(System.getenv("rewrite.autofix")?.equals("true") == true) {
                    it.after!!.sourcePath.toFile().writeText(it.after!!.print(), Charsets.UTF_8)
                }
            }
        }
    }
}
