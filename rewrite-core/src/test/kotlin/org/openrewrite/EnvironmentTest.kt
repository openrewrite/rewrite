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

import io.micrometer.core.instrument.Tags
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.config.CompositeSourceVisitor
import org.openrewrite.text.PlainText

class EnvironmentTest {
    @Test
    fun profileExtends() {
        val parent = Profile().apply {
            name = "parent"
            setDefine(
                    mapOf(
                            "custom.Declarative1" to
                                    listOf(mapOf("org.openrewrite.text.ChangeText" to mapOf("toText" to "Hello Jon"))),
                            "custom.Declarative2" to
                                    listOf(mapOf("text.ChangeText" to mapOf("toText" to "Hello Jonathan!")))
                    )
            )
        }

        val child = Profile().apply {
            name = "child"
            extend = setOf("parent")
            setInclude(setOf("custom.*"))
        }

        val env = Environment.builder()
                .loadProfile { listOf(parent, child) }
                .scan("org.openrewrite.text")

        assertThat(env.getProfile("child")
                .getVisitorsForSourceType(PlainText::class.java)
                .map { it as CompositeSourceVisitor }
                .map { it.tags }
        ).containsExactlyInAnyOrder(Tags.of("name", "custom.Declarative1"), Tags.of("name", "custom.Declarative2"))
    }
}
