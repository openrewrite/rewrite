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
package org.openrewrite.java

import com.fasterxml.jackson.annotation.JsonCreator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.style.IntelliJ

class EnvironmentTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/616")
    @Test
    fun canLoadRecipeWithZeroArgsConstructorAndPrimaryConstructor() {
        val env = Environment.builder().scanRuntimeClasspath().build()
        val recipe = env.activateRecipes(MixedConstructorRecipe::class.java.canonicalName)
        assertThat(recipe).isNotNull
    }

    @Test
    fun listCategoryDescriptors() {
        val env = Environment.builder().scanRuntimeClasspath().build()
        val categoryDescriptors = env.listCategoryDescriptors()
        assertThat(categoryDescriptors).isNotEmpty
    }

    @Test
    fun listStyles() {
        val env = Environment.builder().scanRuntimeClasspath().build()
        val styles = env.listStyles()
        val intelliJStyle = styles.find { it.name.equals(IntelliJ.defaults().name) }
        assertThat(intelliJStyle)
            .`as`("Environment should be able to find and activate the IntelliJ style")
            .isNotNull
    }
}

@Suppress("unused")
class MixedConstructorRecipe @JsonCreator constructor(val opt: Boolean) : Recipe() {
    constructor() : this(true)

    override fun getDisplayName(): String = "Mixed constructor"
}
