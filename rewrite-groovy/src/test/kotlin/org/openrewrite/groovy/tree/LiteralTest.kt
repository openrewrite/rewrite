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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.search.FindAnnotationsTest.Companion.foo

class LiteralTest : GroovyTreeTest {

    @Test
    fun string() = assertParsePrintAndProcess(
        "def a = 'hello'"
    )

    @Test
    fun nullValue() = assertParsePrintAndProcess(
        "def a = null"
    )

    @Test
    fun boxedInt() = assertParsePrintAndProcess(
        "Integer a = 1"
    )

    @Test
    fun tripleQuotedString() = assertParsePrintAndProcess(
        """
            def template = ""${'"'}
                Hi
            ""${'"'}
        """.trimIndent()
    )

    @Test
    fun slashyString() = assertParsePrintAndProcess(
        """
            def fooPattern = /.*foo.*/
        """.trimIndent()
    )

    @Test
    fun gstring() = assertParsePrintAndProcess(
        """
           def s = "uid: ${'$'}{UUID.randomUUID()}"
        """.trimIndent()
    )

    @Test
    fun gStringNoCurlyBraces() = assertParsePrintAndProcess(
        """
            def foo = 1
            def s = "foo: ${'$'}foo"
        """
    )

    @Test
    fun gStringPropertyAccessNoCurlyBraces() = assertParsePrintAndProcess(
        """
            def person = [name: 'sam']
            def s = "name: ${'$'}person.name"
        """
    )

    @Test
    fun gStringInterpolationFollowedByForwardSlash() = assertParsePrintAndProcess("""
        String s = "${"$"}{ARTIFACTORY_URL}/plugins-release"
    """)

    @Test
    fun mapLiteral() = assertParsePrintAndProcess(
        """
            def person = [ name: 'sam' , age: 9000 ]
        """
    )

    @Test
    fun numericLiterals() = assertParsePrintAndProcess(
        """
            float a = 0.1
            def b = 0.1f
            double c = 1.0d
            long d = 1L
        """
    )

    @Test
    fun emptyListLiteral() = assertParsePrintAndProcess(
        """
            def a = []
            def b = [   ]
        """
    )

    @Test
    fun multilineStringWithApostrophes() = assertParsePrintAndProcess(
        """
            def s = '''
              multiline
              string
              with apostrophes
            '''
        """.trimIndent()
    )

    @Test
    fun mapLiteralTrailingComma() = assertParsePrintAndProcess("""
        def a = [ foo : "bar" , ]
    """)

    @Test
    fun listLiteralTrailingComma() = assertParsePrintAndProcess("""
        def a = [ "foo" /* "foo" suffix */ , /* "]" prefix */ ]
    """)

    @Test
    fun gStringThatHasEmptyValueExpressionForUnknownReason() = assertParsePrintAndProcess("""
        def a = "${'$'}{foo.bar}"
        def b = "${'$'}{foo.bar}baz"
    """)

}
