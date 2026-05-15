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
}
