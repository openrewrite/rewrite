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
import org.openrewrite.Recipe
import org.openrewrite.text.ChangeText

internal class RecipeConfigurationTest {
    private val changeText = ChangeText()

    @Test
    fun includeWildcard() {
        val recipe = RecipeConfiguration().apply {
            name = "org.rewrite.test"
            setInclude(setOf("org.openrewrite.text.*"))
        }.build()

        assertThat(recipe.accept(ChangeText().apply {
            toText = "hi"
        })).isEqualTo(Recipe.FilterReply.ACCEPT)
    }

    @Test
    fun includeWildcardsDontIncludeNestedPackages() {
        val recipe = RecipeConfiguration().apply {
            name = "org.rewrite.test"
            setInclude(setOf("org.openrewrite.*"))
        }.build()

        assertThat(recipe.accept(ChangeText().apply {
            toText = "hi"
        })).isEqualTo(Recipe.FilterReply.NEUTRAL)
    }

    @Test
    fun configureSourceVisitor() {
        val recipe = RecipeConfiguration().apply {
            name = "org.rewrite.test"
            setConfigure(
                    mapOf("org.openrewrite.text.ChangeText" to
                            mapOf("toText" to "Hello Jon!")
                    )
            )
        }.build()

        assertThat(recipe.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun propertyNameCombinedWithVisitorName() {
        val recipe = RecipeConfiguration().apply {
            name = "org.rewrite.test"
            setConfigure(mapOf("org.openrewrite.text.ChangeText.toText" to "Hello Jon!"))
        }.build()

        assertThat(recipe.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun propertyNameCombinedWithWildcardVisitor() {
        val recipe = RecipeConfiguration().apply {
            name = "org.rewrite.test"
            setConfigure(mapOf("org.openrewrite.text.*.toText" to "Hello Jon!"))
        }.build()

        assertThat(recipe.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun splitPackageWildcard() {
        val recipe = RecipeConfiguration().apply {
            name = "org.rewrite.test"
            setConfigure(
                    mapOf("org.openrewrite" to
                            mapOf("text.*" to
                                    mapOf("toText" to "Hello Jon!")
                            )
                    )
            )
        }.build()

        assertThat(recipe.configure(changeText).toText).isEqualTo("Hello Jon!")
    }

    @Test
    fun splitPackage() {
        val recipe = RecipeConfiguration().apply {
            name = "org.rewrite.test"
            setConfigure(
                    mapOf("org.openrewrite" to
                            mapOf("text.ChangeText" to
                                    mapOf("toText" to "Hello Jon!")
                            )
                    )
            )
        }.build()

        assertThat(recipe.configure(changeText).toText).isEqualTo("Hello Jon!")
    }
}
