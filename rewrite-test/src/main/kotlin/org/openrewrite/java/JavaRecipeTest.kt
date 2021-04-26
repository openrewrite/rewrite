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

import org.intellij.lang.annotations.Language
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.SourceFile

interface JavaRecipeTest : RecipeTest {
    override val parser: Parser<*>?
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()

    fun assertChanged(
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>,
        @Language("java") after: String,
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, 2, 1) {}
    }

    fun assertChanged(
        @Language("java") before: String,
        @Language("java") after: String,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, 2, 1) {}
    }

    fun assertChanged(
        parser: Parser<*>?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>,
        @Language("java") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, cycles -1) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>,
        @Language("java") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, cycles -1) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>,
        @Language("java") after: String,
        cycles: Int,
        expectedCyclesToComplete: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, expectedCyclesToComplete) {}
    }

    fun assertChanged(
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>,
        @Language("java") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, cycles -1) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") after: String,
        cycles: Int,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, cycles, cycles - 1) {}
    }

    fun assertChanged(
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") after: String,
        cycles: Int,
        expectedCyclesToComplete: Int
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, cycles, expectedCyclesToComplete) {}
    }

    fun <T : SourceFile> assertChanged(
        parser: Parser<T>?,
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") after: String,
        cycles: Int,
    ) {
        super.assertChanged(parser, recipe, before, emptyArray(), after, cycles, cycles - 1) {}
    }

    override fun assertChanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>,
        @Language("java") after: String,
        cycles: Int
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, cycles - 1) {}
    }

    override fun <T : SourceFile> assertChanged(
        parser: Parser<T>?,
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>,
        @Language("java") after: String,
        cycles: Int,
        expectedCyclesToComplete: Int,
        afterConditions: (T) -> Unit
    ) {
        super.assertChanged(parser, recipe, before, dependsOn, after, cycles, expectedCyclesToComplete, afterConditions)
    }

    fun assertUnchanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("java") before: String,
    ) {
        super.assertUnchanged(parser, recipe, before, emptyArray())
    }

    fun assertUnchanged(
        recipe: Recipe?,
        @Language("java") before: String,
    ) {
        super.assertUnchanged(parser, recipe, before, emptyArray())
    }

    fun assertUnchanged(
        @Language("java") before: String
    ) {
        super.assertUnchanged(parser, recipe, before, emptyArray())
    }

    fun assertUnchanged(
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>
    ) {
        super.assertUnchanged(parser, recipe, before, dependsOn)
    }

    fun assertUnchanged(
        parser: Parser<*>?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>
    ) {
        super.assertUnchanged(parser, recipe, before, dependsOn)
    }

    override fun assertUnchanged(
        parser: Parser<*>?,
        recipe: Recipe?,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String>
    ) {
        super.assertUnchanged(parser, recipe, before, dependsOn)
    }
}
