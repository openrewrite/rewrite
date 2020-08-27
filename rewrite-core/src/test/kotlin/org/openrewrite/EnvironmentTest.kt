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
package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.config.RecipeConfiguration
import org.openrewrite.text.ChangeText
import org.openrewrite.text.PlainText

class EnvironmentTest {
    private val parent = RecipeConfiguration().apply {
        name = "org.openrewrite.example"
        setInclude(setOf("org.openrewrite.text.*"))
        setConfigure(mapOf("org.openrewrite.text.ChangeText.toText" to "Hello World!"))
    }

    private val planBuilder = Environment.builder()
            .loadRecipe(parent)
            .loadVisitors(listOf(ChangeText()))

    @Test
    fun basicExample() {
        val visitors = planBuilder.build().visitors("org.openrewrite.example")

        val fixed: PlainText = Refactor()
                .visit(visitors)
                .fixed(PlainText(Tree.randomId(), "Goodbye World.", Formatting.EMPTY, emptyList()))!!

        assertThat(fixed.print()).isEqualTo("Hello World!")
    }

}
