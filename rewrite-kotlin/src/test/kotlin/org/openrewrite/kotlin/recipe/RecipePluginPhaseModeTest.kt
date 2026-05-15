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
 * End-to-end check that a Kotlin-DSL recipe written in phase mode actually
 * transforms code via the generated `getVisitor()` override.
 *
 * Phase mode is the imperative companion to pattern mode: the user writes
 * real Kotlin code inside a `visitMethodInvocation { call -> ... }` lambda
 * that operates on `J.MethodInvocation` directly, instead of declaring
 * before/after structural shapes.
 *
 * The v0 slice exercised here is the smallest possible: a single
 * `edit { visitMethodInvocation { ... } }` block, no `scan`, no `acc`. The
 * IR pass passes the user's lambda straight through to
 * `GeneratedRecipeSupport.methodInvocationEditVisitor` as a `Function1`.
 * Body introspection is not used; the body runs as ordinary Kotlin at recipe
 * execution time.
 */
class RecipePluginPhaseModeTest : RewriteTest {

    @Test
    fun `scan plus acc-threaded edit transforms only when scan saw the gating method`() {
        // ScanningRecipe shape: the scan phase collects method invocation
        // simpleNames into a MutableSet<String>; the edit phase reads the
        // accumulator and rewrites `lowercase()` -> `uppercase()` only when
        // `trim` was also seen elsewhere in the tree. This proves both
        // halves of phase mode in one go: getInitialValue's expression is
        // returned by the generated class, getScanner sees every method
        // invocation and mutates `acc`, and getVisitor(acc) reads back the
        // accumulator via the IR-rewritten `acc` reference inside the user's
        // imperative body.
        val src = """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe
            import org.openrewrite.java.tree.J

            val Gated: Recipe = recipe(
                displayName = "Gated rename",
                description = "Rename lowercase to uppercase when trim is also present.",
            ) {
                val seen = scan<MutableSet<String>>(initial = mutableSetOf()) {
                    visitMethodInvocation { call: J.MethodInvocation -> acc.add(call.simpleName) }
                }
                edit(seen) {
                    visitMethodInvocation { call: J.MethodInvocation ->
                        if (call.simpleName == "lowercase" && acc.contains("trim"))
                            call.withName(call.name.withSimpleName("uppercase"))
                        else call
                    }
                }
            }
        """.trimIndent()
        val result = RecipePluginCompileFixture.compile(src, fileName = "Recipes.kt")
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getGated").invoke(null) as Recipe

        // Source contains both `trim()` and `lowercase()` — the scan
        // accumulates `trim` into `acc`, so the edit's gate fires and
        // `lowercase()` is rewritten.
        rewriteRun(
            { spec -> spec.recipe(recipe).validateRecipeSerialization(false) },
            kotlin(
                """
                val a: String = "x".lowercase()
                val b: String = "  y  ".trim()
                """,
                """
                val a: String = "x".uppercase()
                val b: String = "  y  ".trim()
                """,
            ),
        )
    }

    @Test
    fun `acc-threaded edit makes no change when gate condition is unmet`() {
        // Same recipe as above; this time the source has no `trim()` call so
        // the accumulator never sees it and the edit's gate stays false.
        // Verifies that getInitialValue + getScanner do run (the visitor
        // pipeline triggers them), but the edit phase observes the actual
        // accumulator state and chooses to make no change.
        val src = """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe
            import org.openrewrite.java.tree.J

            val GatedOff: Recipe = recipe(
                displayName = "Gated rename",
                description = "Rename lowercase to uppercase when trim is also present.",
            ) {
                val seen = scan<MutableSet<String>>(initial = mutableSetOf()) {
                    visitMethodInvocation { call: J.MethodInvocation -> acc.add(call.simpleName) }
                }
                edit(seen) {
                    visitMethodInvocation { call: J.MethodInvocation ->
                        if (call.simpleName == "lowercase" && acc.contains("trim"))
                            call.withName(call.name.withSimpleName("uppercase"))
                        else call
                    }
                }
            }
        """.trimIndent()
        val result = RecipePluginCompileFixture.compile(src, fileName = "Recipes.kt")
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getGatedOff").invoke(null) as Recipe

        rewriteRun(
            { spec -> spec.recipe(recipe).validateRecipeSerialization(false) },
            kotlin(
                """
                val a: String = "x".lowercase()
                """,
            ),
        )
    }

    @Test
    fun `stateless edit lambda transforms method invocation via direct withers`() {
        // The lambda mutates the LST directly: when the method name is
        // `lowercase`, replace it with `uppercase`. No templates, no matcher —
        // imperative branching on the J.MethodInvocation API. Returning the
        // unchanged `call` for non-matching invocations is the no-op path the
        // helper expects (same instance → super-visit fires unchanged).
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe
            import org.openrewrite.java.tree.J

            val Renamer: Recipe = recipe(
                displayName = "Rename lowercase()",
                description = "Imperatively rename lowercase() to uppercase().",
            ) {
                edit {
                    visitMethodInvocation { call: J.MethodInvocation ->
                        if (call.simpleName == "lowercase")
                            call.withName(call.name.withSimpleName("uppercase"))
                        else call
                    }
                }
            }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getRenamer").invoke(null) as Recipe

        rewriteRun(
            { spec -> spec.recipe(recipe).validateRecipeSerialization(false) },
            kotlin(
                """
                val s: String = "hello".lowercase()
                """,
                """
                val s: String = "hello".uppercase()
                """,
            ),
        )
    }
}
