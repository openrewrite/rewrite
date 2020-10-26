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
package org.openrewrite

import io.github.classgraph.ClassGraph
import org.openrewrite.Assertions.whenParsedBy
import java.nio.file.Path
import java.nio.file.Paths

fun <S : SourceFile> String.whenParsedBy(parser: Parser<S>): Assertions.StringSourceFileAssert<S> =
        whenParsedBy(parser, this)

fun <S : SourceFile> Path.whenParsedBy(parser: Parser<S>): Assertions.PathSourceFileAssert<S> =
        whenParsedBy(parser, this)

/**
 * Retrieve the visitors associated with the specified recipes from the classpath.
 * Intended to be used for testing, not guaranteed to replicate how build plugins or other rewrite consumers
 * may construct environments.
 */
fun loadVisitorsForTest(vararg recipeNames: String): Collection<RefactorVisitor<*>> {
    val classpath = ClassGraph()
            .classpathURIs.map { Paths.get(it) }
    return Environment.builder()
            .scanClasspath(classpath)
            .build()
            .visitors(*recipeNames)
}
