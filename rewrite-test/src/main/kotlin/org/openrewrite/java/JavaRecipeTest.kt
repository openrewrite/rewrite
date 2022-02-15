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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.tree.J
import java.io.File
import java.nio.file.Path

interface JavaRecipeTest : RecipeTest<J.CompilationUnit> {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion().build()

    override val executionContext: ExecutionContext
        get() {
            val ctx = super.executionContext
            ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true)
            return ctx
        }

    @BeforeEach
    fun beforeRecipe() {
        J.clearCaches()
    }

    @AfterEach
    fun afterRecipe() {
        parser.reset()
    }

    fun assertChanged(
        parser: JavaParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String> = emptyArray(),
        @Language("java") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        typeValidation: TypeValidator.ValidationOptions.Companion.Builder.() -> Unit = {},
        afterConditions: (J.CompilationUnit) -> Unit = { }
    ) {
        val typeValidatingAfterConditions: (J.CompilationUnit) -> Unit = { cu ->
            TypeValidator.assertTypesValid(cu, TypeValidator.ValidationOptions.builder(typeValidation))
            afterConditions(cu)
        }

        super.assertChangedBase(
            parser,
            recipe,
            executionContext,
            before,
            dependsOn,
            after,
            cycles,
            expectedCyclesThatMakeChanges,
            typeValidatingAfterConditions
        )
    }

    fun assertChanged(
        parser: JavaParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("java") before: File,
        relativeTo: Path? = null,
        @Language("java") dependsOn: Array<File> = emptyArray(),
        @Language("java") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        typeValidation: TypeValidator.ValidationOptions.Companion.Builder.() -> Unit = {},
        afterConditions: (J.CompilationUnit) -> Unit = { }
    ) {
        val typeValidatingAfterConditions: (J.CompilationUnit) -> Unit = { cu ->
            TypeValidator.assertTypesValid(cu, TypeValidator.ValidationOptions.builder(typeValidation))
            afterConditions(cu)
        }
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
            typeValidatingAfterConditions
        )
    }

    fun assertUnchanged(
        parser: JavaParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("java") before: String,
        @Language("java") dependsOn: Array<String> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, executionContext, before, dependsOn)
    }

    fun assertUnchanged(
        parser: JavaParser = this.parser,
        recipe: Recipe = this.recipe!!,
        executionContext: ExecutionContext = this.executionContext,
        @Language("java") before: File,
        relativeTo: Path? = null,
        @Language("java") dependsOn: Array<File> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, executionContext, before, relativeTo, dependsOn)
    }
}
