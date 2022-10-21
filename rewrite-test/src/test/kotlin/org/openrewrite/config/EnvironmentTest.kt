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
import org.openrewrite.Issue
import org.openrewrite.Tree.randomId
import org.openrewrite.marker.Markers
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpec
import org.openrewrite.test.SourceSpecs
import org.openrewrite.test.SourceSpecs.text
import org.openrewrite.text.PlainText
import java.net.URI
import java.nio.file.Paths
import java.util.*

class EnvironmentTest : RewriteTest {
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

        val results = recipe.run(listOf(PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null,false, null, null,"hello"))).results
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

        val results = recipe.run(listOf(PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY,null, false, null, null,"hello"))).results
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

        val results = recipe.run(listOf(PlainText(randomId(), Paths.get("test.txt"), Markers.EMPTY, null, false, null, null, "hello"))).results
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

    @Issue("https://github.com/openrewrite/rewrite/issues/343")
    @Test
    fun environmentActivatedRecipeUsableInTests() = rewriteRun(
        { spec ->
            spec.recipe(Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes("org.openrewrite.text.ChangeTextToJon"))
        },
        text(
            "some text that isn't jon",
            "Hello Jon!"
        )
    )

    @Test
    fun deserializesKotlinRecipe() = rewriteRun(
        { spec ->
            spec.recipe(Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes("org.openrewrite.text.HelloKotlin"))
        },
        text(
            "some text",
            "Hello Kotlin"
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/543")
    @Test
    fun recipeDescriptorsFromCrossResources() {
        val env = Environment.builder().scanRuntimeClasspath().build()
        val recipeDescriptors = env.listRecipeDescriptors()
        assertThat(recipeDescriptors).isNotNull.isNotEmpty
        val helloJon2 =
            recipeDescriptors.firstOrNull { it.name == "org.openrewrite.HelloJon2" }
        assertThat(helloJon2!!.recipeList)
            .hasSize(1)
        assertThat(helloJon2.recipeList.first().name).isEqualTo("org.openrewrite.HelloJon")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1789")
    @Test
    fun preserveRecipeListOrder() {
        val env = Environment.builder()
            .load(YamlResourceLoader(
                //language=yaml
                """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: test.FooOne
                    displayName: Test
                    recipeList:
                      - org.openrewrite.config.RecipeAcceptingParameters:
                            foo: "foo"
                            bar: 1
                """.trimIndent().byteInputStream(),
                URI.create("rewrite.yml"),
                Properties()
            ))
            .load(YamlResourceLoader(
                //language=yaml
                """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: test.BarTwo
                    displayName: Test
                    recipeList:
                      - org.openrewrite.config.RecipeAcceptingParameters:
                            foo: "bar"
                            bar: 2
                """.trimIndent().byteInputStream(),
                URI.create("rewrite.yml"),
                Properties()
            ))
            .load(YamlResourceLoader(
                //language=yaml
                """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: test.OrderPreserved
                    displayName: Test
                    recipeList:
                      - org.openrewrite.config.RecipeNoParameters
                      - test.FooOne
                      - org.openrewrite.config.RecipeAcceptingParameters:
                          foo: "bar"
                          bar: 2
                      - org.openrewrite.config.RecipeNoParameters
                      - test.BarTwo
                      - org.openrewrite.config.RecipeNoParameters
                """.trimIndent().byteInputStream(),
                URI.create("rewrite.yml"),
                Properties()
            ))
            .build()
        val recipeList = env.activateRecipes("test.OrderPreserved").recipeList[0].recipeList
        assertThat(recipeList[0].name).isEqualTo("org.openrewrite.config.RecipeNoParameters")
        assertThat(recipeList[1].name).isEqualTo("test.FooOne")
        assertThat(recipeList[2].name).isEqualTo("org.openrewrite.config.RecipeAcceptingParameters")
        assertThat(recipeList[3].name).isEqualTo("org.openrewrite.config.RecipeNoParameters")
        assertThat(recipeList[4].name).isEqualTo("test.BarTwo")
        assertThat(recipeList[5].name).isEqualTo("org.openrewrite.config.RecipeNoParameters")
    }

    @Test
    fun canCauseAnotherCycle() {
        val env = Environment.builder()
            .load(YamlResourceLoader(
                //language=yaml
                """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: test.Foo
                    displayName: Test
                    causesAnotherCycle: true
                    recipeList:
                      - org.openrewrite.config.RecipeNoParameters
                    
                """.trimIndent().byteInputStream(),
                URI.create("rewrite.yml"),
                Properties()
            )).build()
        val recipe = env.activateRecipes("test.Foo")
        assertThat(recipe.causesAnotherCycle()).isTrue
    }

    @Test
    fun willBeValidIfIncludesRecipesFromDependencies() {
        val env = Environment.builder()
            .load(YamlResourceLoader(
                //language=yaml
                """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: test.Foo
                    displayName: Test
                    recipeList:
                      - org.openrewrite.config.RecipeNoParameters
                    
                """.trimIndent().byteInputStream(),
                URI.create("rewrite.yml"),
                Properties()
            ),
            listOf(YamlResourceLoader(
                //language=yaml
                """
                    type: specs.openrewrite.org/v1beta/recipe
                    name: org.openrewrite.config.RecipeNoParameters
                    displayName: Test
                    recipeList:
                      - org.openrewrite.config.RecipeSomeParameters
                    
                """.trimIndent().byteInputStream(),
                URI.create("rewrite.yml"),
                Properties()
            ))).build()
        val recipe = env.activateRecipes("test.Foo")
        assertThat(recipe.validate().isValid).isTrue
    }
}
