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
package org.openrewrite.xml

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.test.XmlTestingSupport
import org.openrewrite.xml.tree.Xml
import java.io.File
import java.nio.file.Path

@Suppress("unused")
interface XmlRecipeTest : XmlTestingSupport {
    val parser: XmlParser
        get() = XmlParser()

    fun assertChanged(
        parser: XmlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String> = emptyArray(),
        @Language("xml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Xml.Document) -> Unit = { }
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
        parser: XmlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("xml") before: File,
        relativeTo: Path? = null,
        @Language("xml") dependsOn: Array<File> = emptyArray(),
        @Language("xml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Xml.Document) -> Unit = { }
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
        parser: XmlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String> = emptyArray()
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
        parser:XmlParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("xml") before: File,
        relativeTo: Path? = null,
        @Language("xml") dependsOn: Array<File> = emptyArray()
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
