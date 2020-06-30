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
package org.openrewrite.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.properties.tree.Properties

class PropertiesParserTest: PropertiesParser() {
    @Test
    fun noEndOfLine() {
        val props = parse("""
            key=value
        """.trimIndent())

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
                .hasSize(1).containsExactly("key")
        assertThat(props.formatting.suffix).isEmpty()
    }

    @Test
    fun endOfLine() {
        val props = parse("""
            key=value
            
        """.trimIndent())

        val entries = props.content.map { it as Properties.Entry }
        assertThat(entries.map { it.key }).containsExactly("key")
        assertThat(entries[0].formatting.suffix).isEqualTo("\n")
    }

    @Test
    fun comment() {
        val props = parse("""
            # this is a comment
            key=value
        """.trimIndent())

        assertThat(props.content[0].let { it as Properties.Comment }.message).isEqualTo(" this is a comment")
        assertThat(props.content[1].let { it as Properties.Entry }.key).isEqualTo("key")
    }

    @Test
    fun multipleEntries() {
        val props = parse("""
            key=value
            key2 = value2
        """.trimIndent())

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
                .hasSize(2).containsExactly("key", "key2")
        assertThat(props.content.map { it as Properties.Entry }.map { it.value })
                .hasSize(2).containsExactly("value", "value2")
    }
}
