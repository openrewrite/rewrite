/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.marker.Marker

interface JavaTestingSupport : RecipeTestingSupport {

    val javaParser: JavaParser
        get() = JavaParser.fromJavaVersion().build()

    fun JavaParser.parse(
        @Language("java") source: String,
        sourceSet: String = "main",
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): J.CompilationUnit {
        setSourceSet(sourceSet)
        return parse(ctx, source.trimIndent()).map { j -> j.addMarkers(markers) }.single()
    }

    fun JavaParser.parse(
        @Language("java") vararg sources: String,
        sourceSet: String = "main",
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<J.CompilationUnit> {
        setSourceSet(sourceSet)
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map { j -> j.addMarkers(markers) }
    }

    fun assertChanged(
        before: J.CompilationUnit,
        @Language("java") after: String,
        additionalSources: List<SourceFile> = emptyList(),
        recipe: Recipe? = this.recipe,
        executionContext: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (J.CompilationUnit) -> Unit = { }
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}