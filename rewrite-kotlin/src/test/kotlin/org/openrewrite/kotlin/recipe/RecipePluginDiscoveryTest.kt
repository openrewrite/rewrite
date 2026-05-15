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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import java.lang.reflect.Modifier

/**
 * Recipes authored via the Kotlin DSL need to be discoverable by the rewrite
 * framework so they appear in `rewrite` / `mod build` / IDE recipe pickers
 * without per-recipe configuration. OpenRewrite's `ClasspathScanningLoader`
 * walks the classpath via ASM and surfaces every public, non-abstract
 * `Recipe` subclass — no manifest registration required.
 *
 * These tests verify that the K2 compiler plugin's generated subclass meets
 * that shape (`generated recipe class meets ClasspathScanningLoader's
 * discovery shape`) and that `Environment.scanClassLoader` actually finds it
 * end-to-end (`Environment scanClassLoader discovers the generated recipe`).
 */
class RecipePluginDiscoveryTest {

    @Test
    fun `generated recipe class meets ClasspathScanningLoader's discovery shape`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val Discoverable: Recipe = recipe(
                displayName = "Discoverable",
                description = "Empty recipe exercised by the discovery test.",
            ) { }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getDiscoverable").invoke(null) as Recipe
        val cls = recipe::class.java

        assertTrue(
            Modifier.isPublic(cls.modifiers),
            "Generated class $cls must be public for ClasspathScanningLoader to surface it",
        )
        assertFalse(
            Modifier.isAbstract(cls.modifiers),
            "Generated class $cls must be concrete (non-abstract)",
        )

        // RecipeLoader instantiates via the no-arg constructor; without a
        // public one the framework can find the class but can't run it.
        val ctor = cls.getDeclaredConstructor()
        assertTrue(
            Modifier.isPublic(ctor.modifiers),
            "Generated class $cls must expose a public no-arg constructor",
        )
        val viaReflection = ctor.newInstance() as Recipe
        assertEquals(recipe.displayName, viaReflection.displayName)
    }

    @Test
    fun `Environment scanClassLoader discovers the generated recipe`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val DiscoveredByEnv: Recipe = recipe(
                displayName = "Discovered by Environment",
                description = "Verifies end-to-end framework discovery.",
            ) { }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val facade = result.classLoader.loadClass("RecipesKt")
        val recipe = facade.getMethod("getDiscoveredByEnv").invoke(null) as Recipe
        val expectedFqn = recipe::class.java.name

        // `scanClassLoader` walks the URLs of the compile-test result loader
        // (a URLClassLoader over the temp output dir), discovering every
        // public concrete Recipe subclass on it.
        val env = Environment.builder().scanClassLoader(result.classLoader).build()
        val descriptors = env.listRecipeDescriptors()
        val recipeNames = descriptors.map { it.name }
        assertTrue(
            recipeNames.contains(expectedFqn),
            "Expected Environment to discover $expectedFqn; got ${recipeNames.size} " +
                "recipes, the first 20: ${recipeNames.take(20)}",
        )
    }
}
