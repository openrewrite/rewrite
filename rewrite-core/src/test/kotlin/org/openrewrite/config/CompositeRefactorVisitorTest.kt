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
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting
import org.openrewrite.SourceVisitor
import org.openrewrite.Tree.randomId
import org.openrewrite.text.PlainText

class CompositeRefactorVisitorTest {
    @Test
    fun delegateToVisitorsInOrder() {
        val yaml = """
            type: beta.openrewrite.org/v1/visitor
            name: org.openrewrite.text.ChangeTextTwice
            visitors:
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jon
              - org.openrewrite.text.ChangeText:
                  toText: Hello Jonathan!
        """.trimIndent()

        val loader = YamlResourceLoader(yaml.byteInputStream())

        val visitors = loader.loadVisitors()

        val a = PlainText(randomId(), "Hi Jon", Formatting.EMPTY)

        val fixed = a.refactor().visit(visitors.map {
            @Suppress("UNCHECKED_CAST")
            it as SourceVisitor<PlainText>
        }).fix().fixed

        assertThat(fixed.printTrimmed()).isEqualTo("Hello Jonathan!")
    }
}
