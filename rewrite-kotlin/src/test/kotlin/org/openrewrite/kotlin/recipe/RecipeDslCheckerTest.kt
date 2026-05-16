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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Compile-time checks for the recipe DSL shape rules — exercised through the
 * `RecipeFirDslCheckers` FIR extension. Each negative case asserts a
 * compilation failure with the expected diagnostic message; each positive
 * case asserts a clean compile.
 *
 * The other rules in plan §Phase 3 are enforced by Kotlin's type system and
 * `@DslMarker` (nested language scopes, language scopes at recipe-block
 * level, phase-level helpers in language scopes). Those don't need FIR rules
 * and aren't exercised here — they show up as ordinary unresolved-reference
 * or "@DslMarker prohibits" diagnostics, which would catch any regression in
 * the receiver-scope declarations.
 */
class RecipeDslCheckerTest {

    @Test
    fun `single bare edit compiles cleanly`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            val r = recipe("d", "desc") { edit { java { } } }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `two bare edit blocks fail with a rule-1 diagnostic`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            val r = recipe("d", "desc") {
                edit { java { } }
                edit { java { } }
            }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("more than one bare `edit { }`")
    }

    @Test
    fun `two scan chains fail with a single-scan-v1 diagnostic`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            val r = recipe("d", "desc") {
                scan<String>("a") { _ -> java { } }.edit { _ -> java { } }
                scan<String>("b") { _ -> java { } }.edit { _ -> java { } }
            }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("more than one `scan { … }` chain")
    }

    @Test
    fun `orphan scan without chained consumer fails`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            val r = recipe("d", "desc") {
                scan<String>("a") { _ -> java { } }
            }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("must be chained with `.edit { ... }`")
    }

    @Test
    fun `scan chained into edit compiles cleanly`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            val r = recipe("d", "desc") {
                scan<String>("a") { _ -> java { } }.edit { _ -> java { } }
            }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `scan chained into generate then edit compiles cleanly`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            val r = recipe("d", "desc") {
                scan<String>("a") { _ -> java { } }
                    .generate { _ -> emptyList() }
                    .edit { _ -> java { } }
            }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test
    fun `mixing scan chain with bare edit fails`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            val r = recipe("d", "desc") {
                scan<String>("a") { _ -> java { } }.edit { _ -> java { } }
                edit { java { } }
            }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("Bare `edit { }` cannot run alongside a `scan { … }` chain")
    }

    @Test
    fun `bare rewrite without trailing to fails`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.recipe
            import org.openrewrite.RewriteAdvice1
            val r = recipe("d", "desc") {
                edit {
                    @Suppress("UNUSED_VARIABLE")
                    val a: RewriteAdvice1<String, String> = rewrite { s: String -> s }
                }
            }
            """.trimIndent(),
        )
        assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains("without a trailing `to { ... }`")
    }
}
