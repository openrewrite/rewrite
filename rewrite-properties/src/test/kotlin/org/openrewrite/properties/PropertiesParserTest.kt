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

        assertThat(props.content.map { it as Properties.Entry }.map { it.key })
                .hasSize(1).containsExactly("key")
        assertThat(props.formatting.suffix).isEqualTo("\n")
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
