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
package org.openrewrite.yaml

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.test.YamlTestingSupport
import org.openrewrite.yaml.tree.Yaml
import java.io.File
import java.nio.file.Path

@Suppress("unused")
interface YamlRecipeTest : YamlTestingSupport {
    val parser: YamlParser
        get() = YamlParser()

    fun assertChanged(
        parser: YamlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String> = emptyArray(),
        @Language("yaml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Yaml.Documents) -> Unit = { }
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
        parser: YamlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("yaml") before: File,
        relativeTo: Path? = null,
        @Language("yaml") dependsOn: Array<File> = emptyArray(),
        @Language("yaml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Yaml.Documents) -> Unit = { }
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
        parser: YamlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("yaml") before: String,
        @Language("yaml") dependsOn: Array<String> = emptyArray()
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
        parser: YamlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("yaml") before: File,
        relativeTo: Path? = null,
        @Language("yaml") dependsOn: Array<File> = emptyArray()
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
