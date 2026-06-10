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
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaVisitor
import org.openrewrite.kotlin.KotlinVisitor

/**
 * v2 dispatch coverage for the IR pass's helper selection. The
 * LST-structural classifier promotes a recipe to the Kotlin helper when
 * either a value-parameter type references a `K.*` tree node OR a call
 * expression resolves into the `kotlin.*` namespace (Kotlin stdlib
 * function / extension that `JavaTemplate` can't parse). All-Java bodies
 * — no K.* tree nodes, no `kotlin.*` calls — dispatch to the Java helper
 * (`JavaVisitor` + `JavaTemplate`), which still matches both Java and
 * Kotlin sources via `TreeVisitorAdapter` but parses the after-template
 * as Java.
 *
 * Property-access and not-null shapes stay Kotlin-only — `J.FieldAccess`
 * is a different LST node than Kotlin property access, and `!!` has no
 * Java analogue.
 *
 * Test strategy: compile each recipe via the K2 plugin, load the synthesized
 * `<Name>$KtRecipe` class through the compilation result's classloader, and
 * `instanceof`-check the visitor returned by `getVisitor()`.
 */
class JavaKotlinClassifierTest {

    @Test
    fun `kotlin lowercase uppercase stdlib extensions dispatch to KotlinVisitor`() {
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
        assertThat(unwrap(r.getVisitor())).isInstanceOf(KotlinVisitor::class.java)
    }

    @Test
    fun `kotlin appendLine extension dispatches to KotlinVisitor`() {
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
        assertThat(unwrap(r.getVisitor())).isInstanceOf(KotlinVisitor::class.java)
    }

    @Test
    fun `reified-generic Kotlin builtin dispatches to KotlinVisitor`() {
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
        assertThat(unwrap(r.getVisitor())).isInstanceOf(KotlinVisitor::class.java)
    }

    @Test
    fun `all-Java body dispatches to JavaVisitor in v2`() {
        // Pure-Java callees on both sides: `StringBuilder.appendCodePoint(I)`
        // matcher and `StringBuilder.append(String)` after-template. Both
        // resolve to `java.lang.{AbstractStringBuilder,StringBuilder}` members
        // — no `K.*` LST node references, no `kotlin.*` callees in the
        // lambdas. JavaTemplate parses the after; JavaVisitor matches the
        // call sites against both Java and Kotlin sources (TreeVisitorAdapter
        // glue). This is the dispatch path that unlocks the Kotlin DSL for
        // Java-source migrations like adopt-jdk-9plus-idioms.
        val r = compileRecipe(
            """
            import org.openrewrite.recipe
            val ReplaceCodePoint = recipe("d", "desc") {
                edit {
                    rewrite { sb: StringBuilder -> sb.appendCodePoint(65) } to { sb -> sb.append("A") }
                }
            }
            """.trimIndent(),
            propertyName = "ReplaceCodePoint",
        )
        assertThat(unwrap(r.getVisitor())).isInstanceOf(JavaVisitor::class.java)
    }

    /**
     * Compile the snippet, load the named property from the synthesized
     * top-level file, and cast its value to Recipe.
     *
     * The synthesized class lives at `<file-package>.<propertyName>$KtRecipe`;
     * the property's getter returns an instance of it.
     */
    /**
     * Peel off the [Preconditions.Check] wrapper added by
     * [org.openrewrite.kotlin.recipe.GeneratedRecipeSupport] so the
     * classifier dispatch assertion sees the inner walker. The wrapper is
     * present for every rewrite/to recipe (UsesMethod / UsesField gating)
     * but isn't what the classifier picks — `KotlinVisitor` vs `JavaVisitor`
     * lives inside.
     */
    private fun unwrap(v: TreeVisitor<*, *>): TreeVisitor<*, *> =
        if (v is Preconditions.Check) {
            val field = Preconditions.Check::class.java.getDeclaredField("v")
            field.isAccessible = true
            field.get(v) as TreeVisitor<*, *>
        } else v

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
