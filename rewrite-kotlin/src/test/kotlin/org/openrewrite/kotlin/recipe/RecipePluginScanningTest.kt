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

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.ScanningRecipe
import org.openrewrite.test.RewriteTest
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.SourceSpecs.text
import org.assertj.core.api.Assertions.assertThat

/**
 * End-to-end coverage for imperative DSL recipes that declare `scan { }` /
 * `generate { }` phases. These shapes are NOT handled by the declarative
 * `rewrite { } to { }` IR path — they route through the imperative-recipe
 * synthesis, which must preserve every phase (`getInitialValue` / `getScanner`
 * / `getVisitor` / `generate`) for the recipe to have any effect.
 *
 * Companion to `RecipePluginRewriteTest` (declarative shapes) and
 * `RecipeDslSurfaceTest` (pure-runtime, no plugin).
 */
class RecipePluginScanningTest : RewriteTest {

    @Test
    fun `scan then generate emits a source file`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import org.openrewrite.Tree
                import org.openrewrite.text.PlainText
                import java.nio.file.Paths

                val InventoryKotlinClasses = recipe(
                    displayName = "Inventory Kotlin class declarations",
                    description = "..."
                ) {
                    scan(mutableSetOf<String>()) { classNames ->
                        kotlin {
                            visitClassDeclaration { cd ->
                                classNames.add(cd.name.simpleName)
                                cd
                            }
                        }
                    }.generate { classNames ->
                        if (classNames.isEmpty()) return@generate emptyList()
                        val inventory = classNames.sorted().joinToString("\n")
                        listOf(
                            PlainText.builder()
                                .id(Tree.randomId())
                                .sourcePath(Paths.get("kotlin-classes.txt"))
                                .text(inventory)
                                .build(),
                        )
                    }
                }
            """.trimIndent(),
            propertyName = "InventoryKotlinClasses",
        )
        rewriteRun(
            // Noop-scanner generator re-emits each cycle, so cap at one cycle.
            { spec -> spec.recipe(r).validateRecipeSerialization(false).cycles(1).expectedCyclesThatMakeChanges(1) },
            kotlin(
                """
                class Alpha
                class Beta
                """.trimIndent(),
            ),
            text(
                null,
                """
                Alpha
                Beta
                """.trimIndent(),
                { spec -> spec.path("kotlin-classes.txt") },
            ),
        )
    }

    @Test
    fun `scan then edit consumes the populated accumulator`() {
        // The edit must see what the scanner populated, not a fresh empty acc.
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe

                val TagWhenMultipleClasses = recipe(
                    displayName = "Suffix ! when the file has >1 class",
                    description = "..."
                ) {
                    scan(mutableSetOf<String>()) { classNames ->
                        kotlin {
                            visitClassDeclaration { cd ->
                                classNames.add(cd.name.simpleName)
                                cd
                            }
                        }
                    }.edit { classNames ->
                        kotlin {
                            visitClassDeclaration { cd ->
                                if (classNames.size > 1 && !cd.simpleName.endsWith("!"))
                                    cd.withName(cd.name.withSimpleName(cd.simpleName + "!"))
                                else cd
                            }
                        }
                    }
                }
            """.trimIndent(),
            propertyName = "TagWhenMultipleClasses",
        )
        rewriteRun(
            { spec -> spec.recipe(r).validateRecipeSerialization(false) },
            kotlin(
                """
                class Alpha
                class Beta
                """.trimIndent(),
                """
                class Alpha!
                class Beta!
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `bare generate emits a source file`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import org.openrewrite.Tree
                import org.openrewrite.text.PlainText
                import java.nio.file.Paths

                val EmitMarker = recipe(
                    displayName = "Emit a marker file",
                    description = "..."
                ) {
                    generate {
                        listOf(
                            PlainText.builder()
                                .id(Tree.randomId())
                                .sourcePath(Paths.get("marker.txt"))
                                .text("generated")
                                .build(),
                        )
                    }
                }
            """.trimIndent(),
            propertyName = "EmitMarker",
        )
        rewriteRun(
            { spec -> spec.recipe(r).validateRecipeSerialization(false).cycles(1).expectedCyclesThatMakeChanges(1) },
            text(
                null,
                "generated",
                { spec -> spec.path("marker.txt") },
            ),
        )
    }

    @Test
    fun `scanning recipe synthesizes a ScanningRecipe subclass`() {
        val r = loadCompiledRecipe(
            source = """
                import org.openrewrite.recipe
                import org.openrewrite.Tree
                import org.openrewrite.text.PlainText
                import java.nio.file.Paths

                val EmitMarker = recipe("Emit", "...") {
                    generate {
                        listOf(PlainText.builder().id(Tree.randomId())
                            .sourcePath(Paths.get("marker.txt")).text("x").build())
                    }
                }
            """.trimIndent(),
            propertyName = "EmitMarker",
        )
        assertThat(r).isInstanceOf(ScanningRecipe::class.java)
        assertThat(r::class.java.declaredFields).isEmpty()
    }

    private fun loadCompiledRecipe(source: String, propertyName: String, packageName: String = ""): Recipe {
        val result = RecipePluginCompileFixture.compile(source)
        check(result.exitOk()) { "compile failed:\n${result.messages}" }
        val topLevelClass = result.classLoader.loadClass(
            if (packageName.isEmpty()) "RecipesKt" else "$packageName.RecipesKt"
        )
        val getter = topLevelClass.getDeclaredMethod("get" + propertyName.replaceFirstChar { it.uppercase() })
        getter.isAccessible = true
        return getter.invoke(null) as Recipe
    }
}
