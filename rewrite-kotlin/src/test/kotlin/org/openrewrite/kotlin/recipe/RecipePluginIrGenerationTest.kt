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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe

/**
 * End-to-end check that the recipe DSL compiler plugin's IR pass produces a real
 * `Recipe` instance instead of letting the runtime stub fire. Compiles a source
 * file containing `val Foo: Recipe = recipe(displayName = …, description = …) { }`,
 * loads the resulting bytecode, reads the val, and asserts its metadata.
 *
 * If the IR pass were not wired, evaluating the val initializer would throw
 * the "compiler plugin not loaded" `IllegalStateException` from
 * `RecipeDsl.kt`. A successful read therefore proves the IR pass replaced
 * the call site with a constructor invocation of a generated subclass.
 */
class RecipePluginIrGenerationTest {

    @Test
    fun `recipe val produces a Recipe instance with the declared metadata`() {
        val result = RecipePluginCompileFixture.compile(
            """
            import org.openrewrite.Recipe
            import org.openrewrite.recipe

            val Hello: Recipe = recipe(
                displayName = "Hello",
                description = "An empty illustrative recipe.",
            ) { }
            """.trimIndent(),
            fileName = "Recipes.kt",
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        // Top-level vals from `Recipes.kt` are reached through the generated `RecipesKt` facade.
        val facade = result.classLoader.loadClass("RecipesKt")
        val getter = facade.getMethod("getHello")
        val instance = getter.invoke(null)
        assertNotNull(instance, "Expected the IR pass to construct a non-null Recipe instance")
        assertTrue(
            instance is Recipe,
            "Expected the generated class to be a Recipe; got ${instance!!::class.java}",
        )
        val recipe = instance as Recipe
        assertEquals("Hello", recipe.displayName)
        assertEquals("An empty illustrative recipe.", recipe.description)

        // The generated class should carry a name that ties it back to the val,
        // so debugging stack traces / introspection are not opaque.
        val generatedClassName = recipe::class.java.name
        assertTrue(
            generatedClassName.contains("Generated") && generatedClassName.contains("Hello"),
            "Expected generated class name to reference the source val; got $generatedClassName",
        )
    }
}
