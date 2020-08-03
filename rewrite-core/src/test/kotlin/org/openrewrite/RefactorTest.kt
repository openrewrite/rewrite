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
import org.openrewrite.text.PlainText

class RefactorTest {
    @Test
    fun scanAutoConfigurableRules() {
        val plan = RefactorPlan.builder()
                .scanVisitors("org.openrewrite.text")
                .loadProfile(ProfileConfiguration().apply {
                    name = "hello-jon"
                    setInclude(setOf("org.openrewrite.text.*"))
                    setConfigure(mapOf("org.openrewrite.text.ChangeText.toText" to "Hello Jon!"))
                })
                .build()

        val visitors = plan.visitors("hello-jon")

        val fixed = Refactor()
                .visit(visitors)
                .fixed(PlainText(Tree.randomId(), "Hello World!", Formatting.EMPTY, emptyList()))

        assertThat(fixed!!.print()).isEqualTo("Hello Jon!")
    }

    @Test
    fun scanProfileAndDeclarativeRule() {
        val plan = RefactorPlan.builder()
                .scanResources()
                .build()

        val visitors = plan.visitors("hello-jon")

        assertThat(visitors).hasSize(1)
    }
}
