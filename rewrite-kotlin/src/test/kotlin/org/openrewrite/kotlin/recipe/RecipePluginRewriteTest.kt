/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
package org.openrewrite.kotlin.recipe

import com.tschuchort.compiletesting.KotlinCompilation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.test.RewriteTest
import org.openrewrite.kotlin.Assertions.kotlin

/**
 * End-to-end check that a Kotlin-DSL recipe with a `rewrite { p -> p.foo() } to
 * { p -> p.bar() }` clause actually transforms code via the generated
 * `getVisitor()` override. The compiler plugin's IR pass lowers the lambdas to
 * KotlinTemplate strings and emits a `getVisitor()` that returns a
 * `KotlinVisitor` from `GeneratedRecipeSupport.methodInvocationReceiverRewrite`.
 *
 * If the lowering is missing, the generated recipe still instantiates but the
 * visitor is the framework's no-op and the test fails on the unchanged source.
 */
class RecipePluginRewriteTest : RewriteTest {

    @Test
    fun `lowercase to uppercase recipe transforms via generated visitor`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val UseUpper: Recipe = recipe(
                displayName = "Use upper",
                description = "Replace lowercase() with uppercase().",
            ) {
                rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
            }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getUseUpper").invoke(null) as Recipe

        // The generated class lives in the compile-test classloader, not the
        // test runner's; Jackson roundtrip would fail. Skip serialization here —
        // it's not what we're verifying.
        rewriteRun(
            { spec -> spec.recipe(recipe).validateRecipeSerialization(false) },
            kotlin(
                """
                val s: String = "hello".lowercase()
                """.trimIndent(),
                """
                val s: String = "hello".uppercase()
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `named-capture multi-arg recipe substitutes both receiver and arg`() {
        // Two-param lambda: param[0]=s is the receiver, param[1]=n is a named
        // capture appearing as the single arg of s.substring(n). After body
        // re-uses both params; placeholders bind receiver -> method.getSelect()
        // and n -> method.getArguments().get(0).
        //
        // Chose `substring(n)` → `take(n)` (not `l.get(i)` → `l[i]`) because the
        // matcher `* substring(..)` doesn't fire on `take(..)`, so the recipe
        // stops after one pass instead of looping.
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val SubstringToTake: Recipe = recipe(
                displayName = "Use take for substring(n)",
                description = "Replace s.substring(n) with s.take(n).",
            ) {
                rewrite { s: String, n: Int -> s.substring(n) } to { s, n -> s.take(n) }
            }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getSubstringToTake").invoke(null) as Recipe

        rewriteRun(
            { spec -> spec.recipe(recipe).validateRecipeSerialization(false) },
            kotlin(
                """
                fun head(text: String, k: Int): String = text.substring(k)
                """.trimIndent(),
                """
                fun head(text: String, k: Int): String = text.take(k)
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `multi-before recipe matches every before lambda and rewrites to a shared after`() {
        // Two before lambdas share a canonical capture shape (only the receiver
        // is captured) and pair with a single after lambda. The plugin lowers
        // them to two MethodMatcher specs joined into the helper as a
        // newline-delimited list; at runtime, any matching spec triggers the
        // substitution.
        //
        // `lowercase()` and `trim()` are both zero-arg String members and
        // neither is deprecated. Semantics aren't meaningful — the test only
        // verifies that both names route through the same after template.
        // After the rewrite the call becomes `uppercase()` which matches
        // neither matcher, so the recipe terminates after one pass.
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val ToUpper: Recipe = recipe(
                displayName = "Use uppercase",
                description = "Replace lowercase()/trim() with uppercase() (illustrative).",
            ) {
                rewrite(
                    { s: String -> s.lowercase() },
                    { s: String -> s.trim() },
                ) to { s -> s.uppercase() }
            }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getToUpper").invoke(null) as Recipe

        rewriteRun(
            { spec -> spec.recipe(recipe).validateRecipeSerialization(false) },
            kotlin(
                """
                val a: String = "hello".lowercase()
                val b: String = "  WORLD  ".trim()
                """.trimIndent(),
                """
                val a: String = "hello".uppercase()
                val b: String = "  WORLD  ".uppercase()
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `literal-capture multi-arg recipe re-emits matched args at literal positions`() {
        // Single-param lambda whose before's root call has two literal string args.
        // The after body references the same literals at the same source-order
        // positions; the plugin pairs them positionally and emits placeholders
        // bound to method.getArguments().get(0) and .get(1) respectively.
        //
        // Chose `replace("a", "b")` → `replaceFirst("a", "b")` so the matcher
        // `* replace(..)` doesn't match the after's call.
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val ReplaceFirstOnly: Recipe = recipe(
                displayName = "Replace only first occurrence",
                description = "Replace s.replace(\"a\", \"b\") with s.replaceFirst(\"a\", \"b\").",
            ) {
                rewrite { s: String -> s.replace("a", "b") } to { s -> s.replaceFirst("a", "b") }
            }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getReplaceFirstOnly").invoke(null) as Recipe

        rewriteRun(
            { spec -> spec.recipe(recipe).validateRecipeSerialization(false) },
            kotlin(
                """
                val s: String = "banana".replace("a", "o")
                """.trimIndent(),
                """
                val s: String = "banana".replaceFirst("a", "o")
                """.trimIndent(),
            ),
        )
    }
}
