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
package org.openrewrite.yaml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DocumentTest {
    @Test
    fun explicitStart() {
        val yText = """
            ---
            type: specs.openrewrite.org/v1beta/visitor
            ---
            
            type: specs.openrewrite.org/v1beta/recipe
        """.trimIndent()

        val y = YamlParser().parse(yText)[0]

        assertThat(y.documents).hasSize(2)
        assertThat(y.documents[0].isExplicit).isTrue()
        assertThat(y.printTrimmed()).isEqualTo(yText)
    }

    @Test
    fun explicitEnd() {
        val yText = """
            type: specs.openrewrite.org/v1beta/visitor
            ...
            ---
            type: specs.openrewrite.org/v1beta/recipe
            
            ...
        """.trimIndent()

        val y = YamlParser().parse(yText)[0]

        assertThat(y.documents).hasSize(2)
        assertThat(y.documents[0].end).isNotNull()
        assertThat(y.printTrimmed()).isEqualTo(yText)
    }

    @Test
    fun implicitStart() {
        val yText = "type: specs.openrewrite.org/v1beta/visitor"
        val y = YamlParser().parse(yText)[0]

        assertThat(y.documents).hasSize(1)
        assertThat(y.documents[0].isExplicit).isFalse()
        assertThat(y.printTrimmed()).isEqualTo(yText)
    }

    @Test
    fun implicitEnd() {
        val yText = "type: specs.openrewrite.org/v1beta/visitor"
        val y = YamlParser().parse(yText)[0]

        assertThat(y.documents).hasSize(1)
        assertThat(y.documents[0].end).isNull()
        assertThat(y.printTrimmed()).isEqualTo(yText)
    }
}
