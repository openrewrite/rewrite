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
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Parser
import org.openrewrite.RecipeTest
import org.openrewrite.Tree.randomId
import org.openrewrite.marker.Markers
import org.openrewrite.text.PlainText
import java.net.URI
import java.nio.file.Path
import java.util.*

class EnvironmentTest : RecipeTest {
    @Test
    fun listRecipes() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.ChangeTextToHello
                        displayName: Change text to hello
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
                        displayName: Change text to hello
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
                        displayName: Text migration
                        recipeList:
                            - test.ChangeTextToHello
                        ---
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.ChangeTextToHello
                        displayName: Change text to hello
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
                        displayName: Text migration
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
                        displayName: Change text to hello
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
                        displayName: Text migration
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
    fun declarativeRecipeListClassCastException() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.LicenseHeader
                        displayName: License header.
                        recipeList:
                          - org.openrewrite.java.AddLicenseHeader: |-
                              LicenseHeader
                    """.trimIndent().byteInputStream(),
                    URI.create("rewrite.yml"),
                    Properties()
                )
            ).build()

        val recipe = env.activateRecipes("test.LicenseHeader")
        assertThat(recipe.validateAll()).anyMatch { v -> v.isInvalid }
    }

    @Test
    fun declarativeRecipeWrongPackage() {
        val env = Environment.builder()
            .load(
                YamlResourceLoader(
                    """
                        type: specs.openrewrite.org/v1beta/recipe
                        name: test.ResultOfFileMkdirsIgnored
                        displayName: Test
                        recipeList:
                          - org.openrewrite.java.ResultOfMethodCallIgnored:
                                methodPattern: 'java.io.File mkdir*()'
                    """.trimIndent().byteInputStream(),
                    URI.create("rewrite.yml"),
                    Properties()
                )
            ).build()

        val recipe = env.activateRecipes("test.ResultOfFileMkdirsIgnored")
        val validateAll = recipe.validateAll()
        assertThat(validateAll).anyMatch { v -> v.isInvalid }
    }

    @Test
    fun scanClasspath() {
        val env = Environment.builder().scanRuntimeClasspath().build()

        assertThat(env.listRecipes()).hasSizeGreaterThanOrEqualTo(2)
            .extracting("name")
            .contains("org.openrewrite.text.ChangeTextToJon", "org.openrewrite.HelloJon")

        assertThat(env.listStyles()).hasSizeGreaterThanOrEqualTo(1)
            .extracting("name")
            .contains("org.openrewrite.SampleStyle")
    }

    @Test
    fun listRecipeDescriptors() {
        val env = Environment.builder().scanRuntimeClasspath().build()
        val recipeDescriptors = env.listRecipeDescriptors()
        assertThat(recipeDescriptors).isNotNull.isNotEmpty
        val changeTextDescriptor =
            recipeDescriptors.firstOrNull { it.name == "org.openrewrite.text.ChangeText" }
        assertThat(changeTextDescriptor).isNotNull
        assertThat(changeTextDescriptor!!.options).hasSize(1)
        assertThat(changeTextDescriptor.options[0].name).isEqualTo("toText")
        assertThat(changeTextDescriptor.options[0].type).isEqualTo("String")
    }

    @Test
    fun listStyles() {
        val env = Environment.builder().scanRuntimeClasspath().build()
        val styles = env.listStyles()
        assertThat(styles).isNotNull.isNotEmpty
        val sampleStyle = styles.firstOrNull { it.name == "org.openrewrite.SampleStyle" }
        assertThat(sampleStyle).isNotNull
        assertThat(sampleStyle!!.displayName).isEqualTo("Sample style")
        assertThat(sampleStyle.description).isEqualTo("Sample test style")
        assertThat(sampleStyle.tags).containsExactly("testing")
    }

    private val plainTextParser = object : Parser<PlainText> {
        override fun parse(vararg sources: String?): MutableList<PlainText> {
            return sources.asSequence()
                .filterNotNull()
                .map { PlainText(randomId(), Markers.EMPTY, it) }
                .toMutableList()
        }

        override fun parseInputs(
            sources: MutableIterable<Parser.Input>,
            relativeTo: Path?,
            ctx: ExecutionContext
        ): MutableList<PlainText> {
            return mutableListOf()
        }

        override fun accept(path: Path): Boolean {
            return true
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/343")
    @Test
    fun environmentActivatedRecipeUsableInTests() = assertChanged(
        parser = plainTextParser,
        recipe = Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.text.ChangeTextToJon"),
        before = "some text that isn't jon",
        after = "Hello Jon!"
    )

    @Test
    fun deserializesKotlinRecipe() = assertChanged(
        parser = plainTextParser,
        recipe = Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.text.HelloKotlin"),
        before = "some text",
        after = "Hello Kotlin"
    )
}
