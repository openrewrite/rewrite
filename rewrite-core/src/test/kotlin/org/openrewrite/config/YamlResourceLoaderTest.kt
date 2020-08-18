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

import io.micrometer.core.instrument.Tag
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.ValidationException
import org.openrewrite.text.ChangeText

class YamlResourceLoaderTest {
    @Test
    fun loadVisitorFromYaml() {
        val yaml = """
            type: specs.openrewrite.org/v1beta/visitor
            name: org.openrewrite.text.ChangeTextTwice
            visitors:
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jon
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jonathan!
        """.trimIndent()

        val loader = YamlResourceLoader(yaml.byteInputStream())

        val visitors = loader.loadVisitors()

        assertThat(visitors).hasSize(1)
        assertThat(visitors.first().name)
                .isEqualTo("org.openrewrite.text.ChangeTextTwice")
        assertThat(visitors.first().tags)
                .contains(Tag.of("name", "org.openrewrite.text.ChangeTextTwice"))
    }

    @Test
    fun loadRecipeYaml() {
        val recipe = YamlResourceLoader("""
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.test
            include:
              - 'org.openrewrite.text.*'
            exclude:
              - org.openrewrite.text.DoesNotExist
            configure:
              org.openrewrite.text.ChangeText:
                toText: 'Hello Jon!'
        """.trimIndent().byteInputStream()).loadRecipes().first().build(emptyList())

        val changeText = ChangeText()

        assertThat(recipe.configure(changeText).toText).isEqualTo("Hello Jon!")
        assertThat(recipe.accept(changeText)).isEqualTo(Recipe.FilterReply.ACCEPT)
    }

    @Test
    fun loadMultiYaml() {
        val resources = YamlResourceLoader("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.checkstyle
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.spring
            ---
            type: specs.openrewrite.org/v1beta/visitor
            name: org.openrewrite.text.ChangeTextToJon
            visitors:
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jon!
        """.trimIndent().byteInputStream())

        assertThat(resources.loadRecipes().map { it.name }).containsOnly("org.openrewrite.checkstyle", "org.openrewrite.spring")
        assertThat(resources.loadVisitors().map { it.name }).containsExactly("org.openrewrite.text.ChangeTextToJon")
    }

    @Test
    fun rejectsDuplicateNames() {
        assertThatExceptionOfType(ValidationException::class.java)
                .isThrownBy {
                    YamlResourceLoader("""
                        ---
                        type: specs.openrewrite.org/v1beta/recipe
                        name: org.openrewrite.spring
                        ---
                        type: specs.openrewrite.org/v1beta/recipe
                        name: org.openrewrite.spring
                        ---
                        type: specs.openrewrite.org/v1beta/visitor
                        name: org.openrewrite.text.ChangeTextToJon
                        visitors:
                          - org.openrewrite.text.ChangeText:
                              toText: Hello Jon!
                    """.trimIndent().byteInputStream())
                }
    }

    @Test
    fun rejectsUnqualifiedNames() {
        assertThatExceptionOfType(ValidationException::class.java)
                .isThrownBy {
                    YamlResourceLoader("""
                        ---
                        type: specs.openrewrite.org/v1beta/recipe
                        name: spring
                        ---
                        type: specs.openrewrite.org/v1beta/recipe
                        name: rewrite
                        ---
                        type: specs.openrewrite.org/v1beta/visitor
                        name: org.openrewrite.text.ChangeTextToJon
                        visitors:
                          - org.openrewrite.text.ChangeText:
                              toText: Hello Jon!
                    """.trimIndent().byteInputStream())
                }
    }
}
