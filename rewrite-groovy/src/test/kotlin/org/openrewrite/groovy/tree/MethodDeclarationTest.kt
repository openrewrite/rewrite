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
import org.openrewrite.groovy.Assertions.groovy
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest

class MethodDeclarationTest : RewriteTest {

    @Test
    fun methodDeclarationDeclaringType() = rewriteRun(
        groovy(
            """
                class A {
                    void method() {}
                }
            """
        ) { spec ->
            spec.beforeRecipe { cu ->
                val method = (cu.classes[0].body.statements[0] as J.MethodDeclaration)
                val methodType = method.methodType!!
                assertThat(methodType.name).isEqualTo("method")
                val declaring = method.methodType!!.declaringType
                assertThat(declaring.fullyQualifiedName).isEqualTo("A")
                assertThat(declaring.methods.find { it == methodType }).isNotNull
            }
        }
    )

    @Test
    fun methodDeclaration() = rewriteRun(
        groovy(
            """
                def accept(Map m) {
                }
            """
        )
    )

    @Test
    fun primitiveReturn() = rewriteRun(
        groovy(
            """
                static int accept(Map m) {
                    List l
                    return 0
                }
            """
        )
    )

    @Test
    fun emptyArguments() = rewriteRun(
        groovy("def foo( ) {}")
    )

    @Test
    fun methodThrows() = rewriteRun(
        groovy(
            """
                def foo(int a) throws Exception , RuntimeException {
                }
            """
        )
    )

    @Test
    fun dynamicTypedArguments() = rewriteRun(
        groovy(
            """
               def foo(bar, baz) {
               }
            """
        )
    )

    @Test
    fun defaultArgumentValues() = rewriteRun(
        groovy(
            """
                def confirmNextStepWithCredentials(String message /* = prefix */ = /* hello prefix */ "Hello" ) {
                }
            """
        )
    )

    @Test
    fun returnNull() = rewriteRun(
        groovy(
            """
                static def foo() {
                    return null
                }
            """
        )
    )
}
