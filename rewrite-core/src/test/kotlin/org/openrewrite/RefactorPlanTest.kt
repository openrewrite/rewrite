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
import org.openrewrite.config.ProfileConfiguration
import org.openrewrite.text.ChangeText
import org.openrewrite.text.PlainText

class RefactorPlanTest {
    private val parent = ProfileConfiguration().apply {
        name = "parent"
        setInclude(setOf("org.openrewrite.text.*"))
        setConfigure(mapOf("org.openrewrite.text.ChangeText.toText" to "hi"));
    }

    private val child = ProfileConfiguration().apply {
        name = "child"
        setExtend(setOf("parent"))
        setConfigure(mapOf("org.openrewrite.text.ChangeText.toText" to "overridden"))
    }

    private val planBuilder = RefactorPlan.builder()
            .loadProfile(parent)
            .loadProfile(child)
            .visitor(ChangeText())

    @Test
    fun nearestConfigurationTakesPrecedence() {
        val visitors = planBuilder.build()
                .visitors(PlainText::class.java, "child")

        val fixed = PlainText(Tree.randomId(), "Hello World!", Formatting.EMPTY)
                .refactor()
                .visit(visitors)
                .fix().fixed

        assertThat(fixed.print()).isEqualTo("overridden")
    }

    @Test
    fun excludes() {
        child.apply {
            setExclude(setOf("org.openrewrite.text.*"))
        }

        val visitors = planBuilder.build()
                .visitors(PlainText::class.java, "child")

        assertThat(visitors).isEmpty()
    }
}
