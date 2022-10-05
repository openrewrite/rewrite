/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.xml

import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

@Suppress("CheckTagEmptyBody")
class SemanticallyEqualTest {

    @Test
    fun `The order of attributes is irrelevant to semantic equality`() {
        assertSemanticEquality(
            """<foo fizz='fizz' buzz="buzz"></foo>""",
            """<foo buzz="buzz" fizz="fizz"></foo>"""
        ).isTrue
    }

    @Test
    fun `Tag contents are considered for semantic equality`() {
        assertSemanticEquality(
            """<foo>foo</foo>""",
            """<foo>bar</foo>"""
        ).isFalse
    }

    @Test
    fun `Attributes with different values are not equal`() {
        assertSemanticEquality(
            """<foo fizz='fizz' buzz="bang"></foo>""",
            """<foo fizz="fizz" buzz="buzz" ></foo>"""
        ).isFalse
    }

    @Test
    fun `A self-closing tag is equivalent to a tag with an empty body`() {
        assertSemanticEquality(
            """<foo></foo>""",
            """<foo/>"""
        ).isTrue
    }

    @Test
    fun `Nested tags are considered for equality, formatting doesn't matter`() {
        assertSemanticEquality(
            """
                <foo>
                    <bar>
                        <baz>hello</baz>
                        <bing/>
                    </bar>
                </foo>
            """,
            """
                <foo><bar>
                        <baz>hello</baz>
                        <bing/>
                    </bar></foo>
            """
        ).isTrue
    }

    @Test
    fun `Comments inside or around tags don't matter for semantic equality`() {
        assertSemanticEquality(
            """
                <foo>
                    <bar>bing</bar>
                </foo>
            """,
            """
                <!-- foo -->
                <foo>
                    <!-- bar -->
                    <bar><!-- bing -->bing<!-- bing --></bar>
                </foo>
            """
        ).isTrue
    }
    private fun assertSemanticEquality(@Language("xml") first: String, @Language("xml") second: String): AbstractBooleanAssert<*> {
        val xml = XmlParser().parse(first, second)
        return assertThat(SemanticallyEqual.areEqual(xml[0], xml[1]))
    }
}
