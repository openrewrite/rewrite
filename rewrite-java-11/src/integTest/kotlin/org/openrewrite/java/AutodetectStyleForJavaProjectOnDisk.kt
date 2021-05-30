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
package org.openrewrite.java

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.java.style.Autodetect
import org.openrewrite.java.style.ImportLayoutStyle
import org.openrewrite.java.tree.J
import org.openrewrite.style.NamedStyles
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.io.path.ExperimentalPathApi
import kotlin.streams.toList

object AutodetectStyleForJavaProjectOnDisk {
    @ExperimentalPathApi
    @JvmStatic
    fun main(args: Array<String>) {
        val srcDir = Paths.get(args[0])

        val predicate = BiPredicate<Path, BasicFileAttributes> { p, bfa ->
            bfa.isRegularFile && p.fileName.toString().endsWith(".java") &&
                    !p.toString().contains("/grammar/") &&
                    !p.toString().contains("/gen/")
        }

        val paths = Files.find(srcDir, 999, predicate)
            .limit(if (args.size > 1) args[1].toLong() else Long.MAX_VALUE)
            .toList()

        val parser: JavaParser = JavaParser.fromJavaVersion().build()

        val sourceFiles: List<J.CompilationUnit> = parser.parse(paths, srcDir, InMemoryExecutionContext())

        val autodetect = Autodetect.detect(sourceFiles)

        val importLayout = NamedStyles.merge(ImportLayoutStyle::class.java, listOf(autodetect))
        println(importLayout)
    }
}
