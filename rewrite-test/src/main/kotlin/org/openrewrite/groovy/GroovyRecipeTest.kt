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
package org.openrewrite.groovy

import org.intellij.lang.annotations.Language
import org.openrewrite.*
import org.openrewrite.groovy.tree.G
import org.openrewrite.test.GroovyTestingSupport
import java.io.File
import java.nio.file.Path

@Suppress("unused")
interface GroovyRecipeTest : GroovyTestingSupport {
    val parser: Parser<G.CompilationUnit>
        get() = GroovyParser.builder().build()

    fun assertChanged(
        parser: Parser<G.CompilationUnit> = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("groovy") before: String,
        @Language("groovy") dependsOn: Array<String> = emptyArray(),
        @Language("groovy") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (G.CompilationUnit) -> Unit = { }
    ) {
        val sourceFiles = parser.parse(executionContext, *(listOf(before) + dependsOn).map { it.trimIndent() }.toTypedArray())

        super.assertChangedBase(
            before = sourceFiles[0],
            after = after,
            additionalSources = sourceFiles.drop(1),
            recipe = recipe,
            ctx = executionContext,
            cycles,
            expectedCyclesThatMakeChanges,
            afterConditions
        )
    }

    fun assertChanged(
        parser: Parser<G.CompilationUnit> = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("groovy") before: File,
        relativeTo: Path? = null,
        @Language("groovy") dependsOn: Array<File> = emptyArray(),
        @Language("groovy") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (G.CompilationUnit) -> Unit = { }
    ) {
        val sourceFiles = parser.parse(listOf(before).plus(dependsOn).map { it.toPath() }, relativeTo, executionContext)

        super.assertChangedBase(
            before = sourceFiles[0],
            after = after,
            additionalSources = sourceFiles.drop(1),
            recipe = recipe,
            ctx = executionContext,
            cycles,
            expectedCyclesThatMakeChanges,
            afterConditions
        )
    }

    fun assertUnchanged(
        parser: Parser<G.CompilationUnit> = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("groovy") before: String,
        @Language("groovy") dependsOn: Array<String> = emptyArray()
    ) {
        val sourceFiles = parser.parse(executionContext, *(listOf(before) + dependsOn).map { it.trimIndent() }.toTypedArray())

        super.assertUnchanged(
            before = sourceFiles[0],
            additionalSources = sourceFiles.drop(1),
            recipe = recipe,
            ctx = executionContext
        )
    }

    fun assertUnchanged(
        parser: Parser<G.CompilationUnit> = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("groovy") before: File,
        relativeTo: Path? = null,
        @Language("groovy") dependsOn: Array<File> = emptyArray()
    ) {
        val sourceFiles = parser.parse(listOf(before).plus(dependsOn).map { it.toPath() }, relativeTo, executionContext)

        super.assertUnchanged(
            before = sourceFiles[0],
            additionalSources = sourceFiles.drop(1),
            recipe = recipe,
            ctx = executionContext
        )
    }
}
