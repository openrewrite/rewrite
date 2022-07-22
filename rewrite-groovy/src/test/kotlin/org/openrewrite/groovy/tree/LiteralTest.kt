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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.test.RewriteTest

@Suppress("GroovyUnusedAssignment")
class LiteralTest : RewriteTest {

    @Test
    fun string() = rewriteRun(
        groovy("def a = 'hello'")
    )

    @Test
    fun nullValue() = rewriteRun(
        groovy("def a = null")
    )

    @Test
    fun boxedInt() = rewriteRun(
        groovy("Integer a = 1")
    )

    @Test
    fun tripleQuotedString() = rewriteRun(
        groovy(
            """
                def template = ""${'"'}
                    Hi
                ""${'"'}
            """
        )
    )

    @Test
    fun slashString() = rewriteRun(
        groovy(
            """
                def fooPattern = /.*foo.*/
            """
        )
    )

    @Test
    fun gString() = rewriteRun(
        groovy(
            """
               def s = "uid: ${'$'}{UUID.randomUUID()}"
            """
        )
    )

    @Test
    fun gStringNoCurlyBraces() = rewriteRun(
        groovy(
            """
                def foo = 1
                def s = "foo: ${'$'}foo"
            """
        )
    )

    @Test
    fun gStringPropertyAccessNoCurlyBraces() = rewriteRun(
        groovy(
            """
                def person = [name: 'sam']
                def s = "name: ${'$'}person.name"
            """
        )
    )

    @Test
    fun gStringInterpolationFollowedByForwardSlash() = rewriteRun(
        groovy(
            """
                String s = "${"$"}{ARTIFACTORY_URL}/plugins-release"
            """
        )
    )

    @Test
    fun mapLiteral() = rewriteRun(
        groovy(
            """
                def person = [ name: 'sam' , age: 9000 ]
            """
        )
    )

    @Test
    fun numericLiterals() = rewriteRun(
        groovy(
            """
                float a = 0.1
                def b = 0.1f
                double c = 1.0d
                long d = 1L
            """
        )
    )

    @Test
    fun literalValueAndTypeAgree() = rewriteRun(groovy(
        """
            def a = 1.8
        """
    ) { spec ->
        spec.beforeRecipe { cu ->
            // Groovy AST represents 1.8 as a BigDecimal
            // Java AST would represent it as Double
            // Our AST could reasonably make either choice
            val initializer =
                (cu.statements[0] as J.VariableDeclarations).variables[0].initializer!! as J.Literal
            if (initializer.type == JavaType.Primitive.Double) {
                assertThat(initializer.value).isEqualTo(1.8)
            } else if (TypeUtils.isOfClassType(
                    initializer.type,
                    "java.math.BigDecimal"
                )
            ) {
                assertThat(initializer.value).isInstanceOf(java.math.BigDecimal::class.java)
            }
        }
    })

    @Test
    fun emptyListLiteral() = rewriteRun(
        groovy(
            """
                def a = []
                def b = [   ]
            """
        )
    )

    @Test
    fun multilineStringWithApostrophes() = rewriteRun(
        groovy(
            """
                def s = '''
                  multiline
                  string
                  with apostrophes
                '''
            """
        )
    )

    @Test
    fun mapLiteralTrailingComma() = rewriteRun(
        groovy(
            """
                def a = [ foo : "bar" , ]
            """
        )
    )

    @Test
    fun listLiteralTrailingComma() = rewriteRun(
        groovy(
            """
                def a = [ "foo" /* "foo" suffix */ , /* "]" prefix */ ]
            """
        )
    )

    @Test
    fun gStringThatHasEmptyValueExpressionForUnknownReason() = rewriteRun(
        groovy(
            """
                def a = "${'$'}{foo.bar}"
                def b = "${'$'}{foo.bar}baz"
            """
        )
    )
}
