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
import org.openrewrite.Preconditions
import org.openrewrite.Recipe
import org.openrewrite.java.search.UsesMethod

/**
 * Regression test for the mapped-type fallback extension shadowing real Kotlin
 * stdlib extensions.
 *
 * [org.openrewrite.kotlin.recipe.internal.RecipeFirMappedTypeFallbackExtension]
 * re-exposes JDK instance methods that Kotlin's mapped-type customizer hides.
 * It must only fill genuine gaps: for a method that ALSO exists as a real
 * Kotlin stdlib extension (e.g. `String.toUpperCase`/`trim`/`substring` in
 * `kotlin.text`), generating a synthetic stand-in shadows the real declaration.
 * The recipe DSL's IR matcher-spec computation then records the synthetic facade
 * `__GENERATED__CALLABLES__Kt` instead of the canonical `kotlin.text.StringsKt`,
 * so the authored recipe matches nothing at runtime (parsing — which has no
 * plugin active — resolves the real `StringsKt`). This regressed 9 downstream
 * `moderneinc/recipes-kotlin` tests.
 *
 * Each test below compiles a `rewrite { } to { }` recipe whose before-pattern
 * calls a mapped-class method that DOES have a Kotlin stdlib extension and
 * asserts the generated `MethodMatcher` names the canonical `kotlin.text.StringsKt`
 * owner — not the synthetic facade.
 */
class MappedTypeFallbackShadowingTest {

    private fun matcherFor(beforeBody: String): String {
        val recipe = loadRecipe(
            // Suppress deprecation just as the real recipes-kotlin recipe does:
            // after the fix, `toUpperCase()`/`toLowerCase()` resolve to the REAL
            // (deprecated) `kotlin.text.StringsKt` extension rather than the
            // non-deprecated synthetic stand-in — which is exactly the point.
            """
            @file:Suppress("DEPRECATION", "DEPRECATION_ERROR")
            import org.openrewrite.recipe
            val R = recipe("d", "desc") {
                edit {
                    $beforeBody
                }
            }
            """.trimIndent(),
            "R",
        )
        val visitor = recipe.visitor
        val check = (visitor as Preconditions.Check).check
        return (check as UsesMethod<*>).methodMatcher.toString()
    }

    private fun loadRecipe(source: String, propertyName: String): Recipe {
        val result = RecipePluginCompileFixture.compile(source)
        check(result.exitOk()) { "compile failed:\n${result.messages}" }
        val cls = result.classLoader.loadClass("RecipesKt")
        val getter = cls.getDeclaredMethod("get" + propertyName.replaceFirstChar { it.uppercase() })
        getter.isAccessible = true
        return getter.invoke(null) as Recipe
    }

    @Test
    fun `toUpperCase matcher targets kotlin_text_StringsKt`() {
        assertThat(matcherFor("rewrite { s: String -> s.toUpperCase() } to { s -> s.uppercase() }"))
            .isEqualTo("kotlin.text.StringsKt toUpperCase(*)")
    }

    @Test
    fun `toLowerCase matcher targets kotlin_text_StringsKt`() {
        assertThat(matcherFor("rewrite { s: String -> s.toLowerCase() } to { s -> s.lowercase() }"))
            .isEqualTo("kotlin.text.StringsKt toLowerCase(*)")
    }

    @Test
    fun `trim matcher targets kotlin_text_StringsKt`() {
        assertThat(matcherFor("rewrite { s: String -> s.trim() } to { s -> s.trim() }"))
            .isEqualTo("kotlin.text.StringsKt trim(*)")
    }

    @Test
    fun `substring matcher targets kotlin_text_StringsKt`() {
        // The DSL emits arg-count-precise patterns; the receiver plus two args
        // produce three wildcards. The owner is what matters: not the synthetic facade.
        assertThat(matcherFor("rewrite { s: String, n: Int -> s.substring(0, n) } to { s, n -> s.substring(0, n) }"))
            .isEqualTo("kotlin.text.StringsKt substring(*, *, *)")
    }
}
