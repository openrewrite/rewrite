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
import org.openrewrite.java.JavaVisitor
import org.openrewrite.kotlin.KotlinVisitor

/**
 * Coverage for the LST-structural Java vs Kotlin classifier in
 * `RecipeIrGenerationExtension`. The classifier picks a visitor type per
 * plan §Design.4: default JavaVisitor; promote to KotlinVisitor only when the
 * before/after lambdas structurally reference a `K.*` LST node.
 *
 * Method-name / callee-package / API-level signals are DELIBERATELY NOT used,
 * so a recipe whose before-lambda calls a Kotlin extension function like
 * `appendln()` still ends up on a JavaVisitor — the MethodMatcher spec is
 * resolved at FIR time pre-inline and encodes the Kotlin-extension target
 * correctly regardless.
 *
 * Test strategy: compile each recipe via the K2 plugin, load the synthesized
 * `Generated$<Name>` class through the compilation result's classloader, and
 * `instanceof`-check the visitor returned by `getVisitor()`. Because
 * KotlinVisitor extends JavaVisitor, the KotlinVisitor check has to come
 * first.
 */
class JavaKotlinClassifierTest {

    @Test
    fun `all-Java body defaults to JavaVisitor`() {
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
        val v = r.getVisitor()
        assertThat(v).isNotInstanceOf(KotlinVisitor::class.java)
        assertThat(v).isInstanceOf(JavaVisitor::class.java)
    }

    @Test
    fun `kotlin stdlib extension call still defaults to JavaVisitor`() {
        // Per plan §Design.4 the classifier ignores method-name / package
        // signals. `appendLine()` is a Kotlin extension on Appendable but
        // doesn't reference a K.* tree node, so the recipe defaults to
        // JavaVisitor. The MethodMatcher spec was built at FIR time and
        // encodes the resolved Kotlin facade — it works regardless of visitor
        // type.
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
        val v = r.getVisitor()
        assertThat(v).isNotInstanceOf(KotlinVisitor::class.java)
        assertThat(v).isInstanceOf(JavaVisitor::class.java)
    }

    @Test
    fun `reified-generic stdlib call defaults to JavaVisitor`() {
        // `enumValues<...>()` is a reified-generic Kotlin stdlib call; the
        // classifier doesn't care.
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
        val v = r.getVisitor()
        assertThat(v).isNotInstanceOf(KotlinVisitor::class.java)
        assertThat(v).isInstanceOf(JavaVisitor::class.java)
    }

    /**
     * Compile the snippet, load the named property from the synthesized
     * top-level file, and cast its value to Recipe.
     *
     * The synthesized class lives at `<file-package>.Generated$<propertyName>`;
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
