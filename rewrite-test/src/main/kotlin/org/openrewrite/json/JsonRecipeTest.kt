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
package org.openrewrite.json

import org.intellij.lang.annotations.Language
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.json.tree.Json
import java.io.File
import java.nio.file.Path

@Suppress("unused")
interface JsonRecipeTest : RecipeTest<Json.Document> {
    override val parser: JsonParser
        get() = JsonParser()

    fun assertChanged(
        recipe: Recipe = this.recipe!!,
        moderneAstLink: String,
        moderneApiBearerToken: String = apiTokenFromUserHome(),
        @Language("json") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Json.Document) -> Unit = { }
    ) {
        super.assertChangedBase(recipe, moderneAstLink, moderneApiBearerToken, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun assertChanged(
        parser: JsonParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("json") before: String,
        @Language("json") dependsOn: Array<String> = emptyArray(),
        @Language("json") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Json.Document) -> Unit = { }
    ) {
        super.assertChangedBase(parser, recipe, before, dependsOn, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun assertChanged(
        parser: JsonParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("json") before: File,
        relativeTo: Path? = null,
        @Language("json") dependsOn: Array<File> = emptyArray(),
        @Language("json") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Json.Document) -> Unit = { }
    ) {
        super.assertChangedBase(parser, recipe, before, relativeTo, dependsOn, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun assertUnchanged(
        parser: JsonParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("json") before: String,
        @Language("json") dependsOn: Array<String> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, before, dependsOn)
    }

    fun assertUnchanged(
        parser: JsonParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("json") before: File,
        relativeTo: Path? = null,
        @Language("json") dependsOn: Array<File> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, before, relativeTo, dependsOn)
    }
}
