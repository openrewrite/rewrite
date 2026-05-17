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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.kotlin.Assertions.kotlin

/**
 * End-to-end recipe execution for the canonical Phase 3 shape:
 *
 *     val UseFoo = recipe("d", "desc") {
 *         edit { rewrite { p -> p.foo() } to { p -> p.bar() } }
 *     }
 *
 * Each test compiles a recipe via the K2 plugin, instantiates the synthesized
 * `Generated$<Name>` class, then runs it through `RewriteTest` against a
 * Kotlin source fixture. This proves the full IR-pass pipeline — metadata
 * extraction, MethodMatcher spec computation, after-template synthesis,
 * Java-vs-Kotlin classifier, helper dispatch — works against real recipes.
 *
 * The pure-runtime DSL surface (no plugin) is covered separately by
 * `RecipeDslSurfaceTest` in `org.openrewrite`.
 */
class RecipePluginRewriteTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseUppercase = recipe(
                    displayName = "Replace lowercase with uppercase",
                    description = "..."
                ) {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "UseUppercase",
        ))
        // The synthesized `Generated$<Name>` class lives in the
        // kotlin-compile-testing classloader, not the test's. Jackson's
        // class-id deserializer can't resolve it. The recipe still executes
        // correctly via direct invocation; the round-trip is what fails.
        spec.validateRecipeSerialization(false)
    }

    @Test
    fun `member-call rewrite — lowercase to uppercase`() = rewriteRun(
        kotlin(
            """
            fun example() {
                val s = "hello"
                println(s.lowercase())
            }
            """.trimIndent(),
            """
            fun example() {
                val s = "hello"
                println(s.uppercase())
            }
            """.trimIndent(),
        ),
    )

    @Test
    fun `not-null-asserted before pattern rewrites the whole expression`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseReadln = recipe(
                    displayName = "Use readln() instead of readLine()!!",
                    description = "..."
                ) {
                    edit {
                        rewrite { -> readLine()!! } to { -> readln() }
                    }
                }
            """.trimIndent(),
            propertyName = "UseReadln",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun first(): String = readLine()!!
                """.trimIndent(),
                """
                fun first(): String = readln()
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `after-template preserves source-level FQN qualifier`() {
        // `Math.abs(x: Double)` -> `kotlin.math.abs(x)`. K2 IR resolves
        // `kotlin.math.abs(x)` to a single `IrCall(abs)` whose offsets cover
        // only `abs(x)`; the package qualifier has no IR node. The IR pass
        // extends the source slice backward through any `id(.id)*` chain so
        // the synthesized template emits the same FQN the recipe author wrote,
        // keeping the rewrite single-cycle stable without needing an
        // import-add post-visit.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                val UseKotlinMathAbs = recipe(
                    displayName = "Use kotlin.math.abs",
                    description = "..."
                ) {
                    edit {
                        rewrite { x: Double -> Math.abs(x) } to { x -> kotlin.math.abs(x) }
                    }
                }
            """.trimIndent(),
            propertyName = "UseKotlinMathAbs",
        )
        rewriteRun(
            { spec -> spec.recipe(r) },
            kotlin(
                """
                fun a(x: Double): Double = Math.abs(x)
                """.trimIndent(),
                """
                fun a(x: Double): Double = kotlin.math.abs(x)
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `metadata accessors populate correctly on generated recipe`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import java.time.Duration
                val MyRecipe = recipe(
                    displayName = "My recipe",
                    description = "Test description",
                    tags = setOf("test", "smoke"),
                    estimatedEffortPerOccurrence = Duration.ofMinutes(3),
                ) {
                    edit {
                        rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                    }
                }
            """.trimIndent(),
            propertyName = "MyRecipe",
        )
        assertThat(r.displayName).isEqualTo("My recipe")
        assertThat(r.description).isEqualTo("Test description")
        assertThat(r.getTags()).containsExactlyInAnyOrder("test", "smoke")
        assertThat(r.estimatedEffortPerOccurrence?.toMinutes()).isEqualTo(3L)
    }

    private fun loadCompiledRecipe(source: String, propertyName: String): Recipe {
        val result = RecipePluginCompileFixture.compile(source)
        check(result.exitOk()) { "compile failed:\n${result.messages}" }
        val topLevelClass = result.classLoader.loadClass("RecipesKt")
        val getter = topLevelClass.getDeclaredMethod("get" + propertyName.replaceFirstChar { it.uppercase() })
        getter.isAccessible = true
        return getter.invoke(null) as Recipe
    }
}
