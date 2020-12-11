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

import org.openrewrite.MetricsDestinations
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.streams.toList

object ParseJavaProjectOnDisk {
    @JvmStatic
    fun main(args: Array<String>) {
        val meterRegistry = MetricsDestinations.prometheus()

        val srcDir = Paths.get(args[0])
        val predicate = BiPredicate<Path, BasicFileAttributes> { p, bfa ->
            bfa.isRegularFile && p.fileName.toString().endsWith(".java")
        }

        val paths = Files.find(srcDir, 999, predicate)
            .limit(25)
            .toList()

        var start = System.nanoTime()
        val parser: JavaParser = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(false) // optional, for quiet parsing
            .meterRegistry(meterRegistry)
            .build()

        println("Loaded ${paths.size} files and project dependencies in ${(System.nanoTime() - start) * 1e-6}ms")

        start = System.nanoTime()
        parser.parse(paths, srcDir)
        println("Parsed ${paths.size} files in ${(System.nanoTime() - start) * 1e-6}ms")
    }
}
