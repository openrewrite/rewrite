package org.openrewrite.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class StringUtilsTest {
    @Test
    fun detectIndentLevel() {
        assertThat(StringUtils.indentLevel("""
            |<
            |   <
            |   <
            |   <
            |<
        """.trimMargin())).isEqualTo(0)

        assertThat(StringUtils.indentLevel("""
            |<
            |   <
        """.trimMargin())).isEqualTo(3)

        assertThat(StringUtils.indentLevel("""
            |<
            |    <
            |      <
        """.trimMargin())).isEqualTo(4)

        // ignores the last line if it is all blank
        assertThat(StringUtils.indentLevel("""
            class {
                A field;
            }
        """)).isEqualTo(12)

        assertThat(StringUtils.indentLevel("""
            | <
            |  <
            |   <
            |    <
        """.trimMargin())).isEqualTo(1)

        assertThat(StringUtils.indentLevel("""
            | <
            |  <
            |    <
            |    <
            |      <
        """.trimMargin())).isEqualTo(1)

        assertThat(StringUtils.indentLevel("""
            |<
            |<
        """.trimMargin())).isEqualTo(0)

        // doesn't consider newlines that occur as the first character on the first line or terminating newlines
        assertThat(StringUtils.indentLevel("""
            |
            |  <
            |    <
            |    <
            |
        """.trimMargin())).isEqualTo(2)
    }

    @Test
    fun trimIndent() {
        val input = """
            class {
                A field;
            }
        """

        assertThat(StringUtils.trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentMinimalIndent() {
        val input = """
        class {
                A field;
            }
        """

        assertThat(StringUtils.trimIndent(input)).isEqualTo(input.trimIndent())
    }

    @Test
    fun trimIndentNoIndent() {
        val input = "class{\n   A field;\n}"

        assertThat(StringUtils.trimIndent(input)).isEqualTo(input.trimIndent())
    }
}