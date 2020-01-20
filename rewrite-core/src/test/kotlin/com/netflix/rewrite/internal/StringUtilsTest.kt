/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.rewrite.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

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

        assertEquals(input.trimIndent(), StringUtils.trimIndent(input))
    }

    @Test
    fun trimIndentMinimalIndent() {
        val input = """
        class {
                A field;
            }
        """

        assertEquals(input.trimIndent(), StringUtils.trimIndent(input))
    }

    @Test
    fun trimIndentNoIndent() {
        val input = "class{\n   A field;\n}"

        assertEquals(input.trimIndent(), StringUtils.trimIndent(input))
    }
}