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
import org.openrewrite.yaml.tree.Yaml

class MappingTest : YamlParser() {
    @Test
    fun multipleEntries() {
        val yText = """
            type : specs.openrewrite.org/v1beta/visitor
            name : org.openrewrite.text.ChangeTextToJon
        """.trimIndent()
        val y = parse(yText)[0]

        assertThat((y.documents[0].blocks[0] as Yaml.Mapping).entries.map { it.key.value })
                .containsExactly("type", "name")
        assertThat(y.printTrimmed()).isEqualTo(yText)
    }

    @Test
    fun deep() {
        val yText = """
            type:
                name: org.openrewrite.text.ChangeTextToJon
        """.trimIndent()
        val y = parse(yText)[0]

        val mapping = y.documents[0].blocks[0] as Yaml.Mapping
        assertThat(mapping.entries.map { it.key.value }).containsExactly("type")
        assertThat(mapping.entries[0].value).isInstanceOf(Yaml.Mapping::class.java)
        assertThat(y.print()).isEqualTo(yText)
    }
}
