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
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.yaml.tree.Yaml

class SequenceTest {

    @Test
    fun blockSequence() {
        val yText = """
            - apples
            - oranges
        """.trimIndent()

        val y = YamlParser().parse(yText)[0]

        assertThat((y.documents[0].blocks[0] as Yaml.Sequence).entries.map { it.block }.map { it as Yaml.Scalar }.map { it.value })
            .containsExactly("apples", "oranges")

        assertThat(y.printTrimmed()).isEqualTo(yText)
    }

    @Test
    fun blockSequenceOfMappings() {
        val yamlText = """
            - name: Fred
              age: 45
            - name: Barney
              age: 25
        """.trimIndent()
        val y = YamlParser().parse(
            InMemoryExecutionContext { t -> t.printStackTrace() },
            yamlText
        )[0]
        assertThat(y.printTrimmed()).isEqualTo(yamlText)
    }
}
