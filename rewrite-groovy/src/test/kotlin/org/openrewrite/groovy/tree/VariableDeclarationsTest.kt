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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest

@Suppress("GroovyUnusedAssignment")
class VariableDeclarationsTest : RewriteTest {

    @Test
    fun singleVariableDeclaration() = rewriteRun(
        groovy(
            "Integer a = 1"
        )
    )

    @Test
    fun singleVariableDeclarationStaticallyTyped() = rewriteRun(
        groovy(
            """
                int a = 1
                List<String> l
            """
        )
    )

    @Test
    fun wildcardWithUpperBound() = rewriteRun(
        groovy(
            """
            List<? extends String> l
        """
        ) { spec ->
            spec.beforeRecipe { cu ->
                val vd = cu.statements[0] as J.VariableDeclarations
                assertThat(vd.typeExpression).isInstanceOf(J.ParameterizedType::class.java)
                val typeExpression = (vd.typeExpression as J.ParameterizedType).typeParameters!![0]
                assertThat(typeExpression).isInstanceOf(J.Wildcard::class.java)
                val wildcard = typeExpression as J.Wildcard
                assertThat(wildcard.bound).isEqualTo(J.Wildcard.Bound.Extends)
            }
        }
    )

    @Test
    fun wildcardWithLowerBound() = rewriteRun(
        groovy(
            """
                List<? super String> l
            """
        )
    )

    @Test
    fun diamondOperator() = rewriteRun(
        groovy("List<String> l = new ArrayList< /* */ >()")
    )

    @Disabled
    @Test
    fun singleTypeMultipleVariableDeclaration() = rewriteRun(
        groovy("def a = 1, b = 1")
    )

    @Disabled
    @Test
    fun multipleTypeMultipleVariableDeclaration() = rewriteRun(
        groovy("def a = 1, b = 's'")
    )

    @Test
    fun genericVariableDeclaration() = rewriteRun(
        groovy("def a = new HashMap<String, String>()")
    )

    @Test
    fun anonymousClass() = rewriteRun(
        groovy(
            """
                def a = new Object( ) { 
                    def b = new Object() { }
                }
            """
        )
    )
}
