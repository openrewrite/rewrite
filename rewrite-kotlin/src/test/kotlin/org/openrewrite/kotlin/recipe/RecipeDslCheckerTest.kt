/*
 * Copyright 2026 the original author or authors.
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
@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
package org.openrewrite.kotlin.recipe

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Compile-time validation that the recipe-DSL checker fires the expected diagnostics.
 *
 * Strategy: feed the rewrite-kotlin compiler plugin a recipe declaration that violates
 * a rule, drive kotlinc via kotlin-compile-testing, and assert that the compilation
 * fails with a message containing the relevant phrase from
 * [org.openrewrite.kotlin.recipe.internal.RecipeDslPropertyChecker].
 */
class RecipeDslCheckerTest {

    @Test
    fun `mixing pattern and phase modes in one recipe fails compilation`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val Bad: Recipe = recipe(
                displayName = "Bad",
                description = "Mixes pattern and phase modes.",
            ) {
                rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                scan(mutableSetOf<String>()) { }
            }
            """.trimIndent()
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
        assertTrue(
            result.messages.contains("mixes pattern mode"),
            "Expected a 'mixes pattern mode' error, got:\n${result.messages}",
        )
    }

    @Test
    fun `pattern mode alone compiles cleanly`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val PatternOnly: Recipe = recipe(
                displayName = "PatternOnly",
                description = "Pattern only.",
            ) {
                rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
            }
            """.trimIndent()
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }

    @Test
    fun `phase mode alone compiles cleanly`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val PhaseOnly: Recipe = recipe(
                displayName = "PhaseOnly",
                description = "Phase only.",
            ) {
                scan(mutableSetOf<String>()) { }
                edit { }
            }
            """.trimIndent()
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
    }
}
