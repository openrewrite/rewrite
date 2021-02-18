/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Tree.randomId
import org.openrewrite.marker.Markers
import org.openrewrite.text.PlainText
import java.net.URI
import java.util.*

class EnvironmentTest {
    @Test
    fun listRecipes() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.ChangeTextToHello
                        recipeList:
                            - org.openrewrite.text.ChangeText:
                                toText: Hello
                    """.trimIndent().byteInputStream(),
                    URI.create("rewrite.yml"),
                    Properties()
                )
            )
            .build()

        assertThat(env.listRecipes().map { it.name })
            .containsExactly("test.ChangeTextToHello")

        val recipe = env.activateRecipes("test.ChangeTextToHello")
        assertThat(recipe.validateAll()).allMatch { v -> v.isValid }

        val results = recipe.run(listOf(PlainText(randomId(), Markers.EMPTY, "hello")))
        assertThat(results).hasSize(1)
    }

    @Test
    fun recipeWithoutRequiredConfiguration() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.ChangeTextToHello
                        recipeList:
                            - org.openrewrite.text.ChangeText
                    """.trimIndent().byteInputStream(),
                    URI.create("rewrite.yml"),
                    Properties()
                )
            )
            .build()

        val recipe = env.activateRecipes("test.ChangeTextToHello")
        assertThat(recipe.validateAll()).anyMatch { v -> v.isInvalid }
    }

    @Test
    fun recipeDependsOnOtherDeclarativeRecipe() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.TextMigration
                        recipeList:
                            - test.ChangeTextToHello
                        ---
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.ChangeTextToHello
                        recipeList:
                            - org.openrewrite.text.ChangeText:
                                toText: Hello
                    """.trimIndent().byteInputStream(),
                    URI.create("rewrite.yml"),
                    Properties()
                )
            )
            .build()

        val recipe = env.activateRecipes("test.TextMigration")
        assertThat(recipe.validateAll()).allMatch { v -> v.isValid }

        val results = recipe.run(listOf(PlainText(randomId(), Markers.EMPTY, "hello")))
        assertThat(results).hasSize(1)
    }

    @Test
    fun recipeDependsOnOtherDeclarativeRecipeSpecifiedInAnotherFile() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.TextMigration
                        recipeList:
                            - test.ChangeTextToHello
                    """.trimIndent().byteInputStream(),
                    URI.create("rewrite.yml"),
                    Properties()
                )
            )
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.ChangeTextToHello
                        recipeList:
                            - org.openrewrite.text.ChangeText:
                                toText: Hello
                    """.trimIndent().byteInputStream(),
                    URI.create("text.yml"),
                    Properties()
                )
            )
            .build()

        val recipe = env.activateRecipes("test.TextMigration")
        assertThat(recipe.validateAll()).allMatch { v -> v.isValid }

        val results = recipe.run(listOf(PlainText(randomId(), Markers.EMPTY, "hello")))
        assertThat(results).hasSize(1)
    }

    @Test
    fun recipeDependsOnNonExistentRecipe() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.TextMigration
                        recipeList:
                            - test.DoesNotExist
                    """.trimIndent().byteInputStream(),
                    URI.create("rewrite.yml"),
                    Properties()
                )
            )
            .build()

        val recipe = env.activateRecipes("test.TextMigration")
        assertThat(recipe.validateAll()).anyMatch { v -> v.isInvalid }
    }

    @Test
    fun scanClasspath() {
        val env = Environment.builder().scanClasspath(Collections.emptySet()).build()

        assertThat(env.listRecipes()).hasSize(2)
                .extracting("name")
                .contains("org.openrewrite.text.ChangeTextToJon", "org.openrewrite.HelloJon")

        assertThat(env.listStyles()).hasSize(1)
                .extracting("name")
                .contains("org.openrewrite.SampleStyle")
    }
}
