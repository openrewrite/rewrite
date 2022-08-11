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
package org.openrewrite.maven

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.xml.tree.Xml
import java.io.File
import java.nio.file.Path

@Suppress("unused")
interface MavenRecipeTest : RecipeTest<Xml.Document> {

    override val parser: MavenParser
        get() = MavenParser.builder().build()

    override val executionContext: ExecutionContext
        get() {
        return MavenExecutionContextView.view(super.executionContext)
            .also { if (MavenSettings.readFromDiskEnabled()) {
                it.setMavenSettings(MavenSettings.readMavenSettingsFromDisk(it))
            }}
    }

    fun assertChanged(
        parser: MavenParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String> = emptyArray(),
        @Language("xml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Xml.Document) -> Unit = { }
    ) {
        super.assertChangedBase(
            parser,
            recipe,
            executionContext,
            before,
            dependsOn,
            after,
            cycles,
            expectedCyclesThatMakeChanges,
            afterConditions
        )
    }

    fun assertChanged(
        parser: MavenParser = this.parser,
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
        super.assertChangedBase(
            parser,
            recipe,
            executionContext,
            before,
            relativeTo,
            dependsOn,
            after,
            cycles,
            expectedCyclesThatMakeChanges,
            afterConditions
        )
    }

    fun assertUnchanged(
        parser: MavenParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, executionContext, before, dependsOn)
    }

    fun assertUnchanged(
        parser: MavenParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("xml") before: File,
        relativeTo: Path? = null,
        @Language("xml") dependsOn: Array<File> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, executionContext, before, relativeTo, dependsOn)
    }
}
