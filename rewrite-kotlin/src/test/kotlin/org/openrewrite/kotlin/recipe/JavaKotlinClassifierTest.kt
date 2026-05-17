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
import org.openrewrite.kotlin.KotlinVisitor

/**
 * v1 dispatch coverage for the IR pass's helper selection. The
 * LST-structural classifier still runs (it computes `usesKotlinTreeNode`)
 * but v1 always dispatches to the Kotlin helper — see
 * `RecipeIrGenerationExtension.pickHelperSymbol` for the reasoning. These
 * tests pin that v1 behavior so a future change that re-enables the
 * Java-template path doesn't accidentally regress recipes authored in
 * Kotlin (every recipe in Kotlin1To2.kt today).
 *
 * Test strategy: compile each recipe via the K2 plugin, load the synthesized
 * `<Name>$KtRecipe` class through the compilation result's classloader, and
 * `instanceof`-check the visitor returned by `getVisitor()`.
 */
class JavaKotlinClassifierTest {

    @Test
    fun `all-Java body still dispatches to KotlinVisitor in v1`() {
        val r = compileRecipe(
            """
            import org.openrewrite.recipe
            val UseUpper = recipe("d", "desc") {
                edit {
                    rewrite { s: String -> s.lowercase() } to { s -> s.uppercase() }
                }
            }
            """.trimIndent(),
            propertyName = "UseUpper",
        )
        assertThat(r.getVisitor()).isInstanceOf(KotlinVisitor::class.java)
    }

    @Test
    fun `kotlin stdlib extension call dispatches to KotlinVisitor`() {
        val r = compileRecipe(
            """
            import org.openrewrite.recipe
            val UseAppendLine = recipe("d", "desc") {
                edit {
                    rewrite { sb: StringBuilder -> sb.append("x") } to { sb -> sb.appendLine("x") }
                }
            }
            """.trimIndent(),
            propertyName = "UseAppendLine",
        )
        assertThat(r.getVisitor()).isInstanceOf(KotlinVisitor::class.java)
    }

    @Test
    fun `reified-generic stdlib call dispatches to KotlinVisitor`() {
        val r = compileRecipe(
            """
            import org.openrewrite.recipe
            enum class E { A, B }
            val EnumEntriesRename = recipe("d", "desc") {
                edit {
                    rewrite<Array<E>> { enumValues<E>() } to { enumValues<E>() }
                }
            }
            """.trimIndent(),
            propertyName = "EnumEntriesRename",
        )
        assertThat(r.getVisitor()).isInstanceOf(KotlinVisitor::class.java)
    }

    /**
     * Compile the snippet, load the named property from the synthesized
     * top-level file, and cast its value to Recipe.
     *
     * The synthesized class lives at `<file-package>.<propertyName>$KtRecipe`;
     * the property's getter returns an instance of it.
     */
    private fun compileRecipe(source: String, propertyName: String): Recipe {
        val result = RecipePluginCompileFixture.compile(source)
        check(result.exitOk()) { "compile failed:\n${result.messages}" }
        val classLoader = result.classLoader
        // kotlinc names the top-level file class after the source file basename
        // + "Kt" suffix. `RecipePluginCompileFixture.compile` uses "Recipes.kt"
        // by default, producing class `RecipesKt`.
        val topLevelClass = classLoader.loadClass("RecipesKt")
        val getter = topLevelClass.getDeclaredMethod("get" + propertyName.replaceFirstChar { it.uppercase() })
        getter.isAccessible = true
        return getter.invoke(null) as Recipe
    }
}
