/*
 * Copyright 2022 the original author or authors.
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
@file:Suppress("GroovyUnusedAssignment", "GrPackage")

package org.openrewrite.groovy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.asFullyQualified
import org.openrewrite.java.search.FindAnnotationsTest.Companion.foo
import org.openrewrite.java.tree.J
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.SourceSpec
import java.util.function.Consumer

class GroovyTypeAttributionTest : RewriteTest {

    @Test
    fun defTypeAttributed() = rewriteRun(
        groovy(
            """
                class Test {
                    static void test() {
                        def l = new ArrayList()
                    }
                }
            """,
            isAttributed(true)
        )
    )

    @Test
    fun defFieldNotTypeAttributed() = rewriteRun(
        groovy(
            """
                class Test {
                    def l = new ArrayList()
                }
            """,
            isAttributed(false)
        )
    )

    @Test
    fun globalTypeAttributed() = rewriteRun(
        groovy(
            """
                def l = new ArrayList()
            """,
            isAttributed(true)
        )
    )

    @Test
    fun closureImplicitParameterAttributed() = rewriteRun(
        groovy("""
            public <T> T register(String name, Class<T> type, Closure<T> configurationAction) {
                return null
            }
            register("testTask", String) {
                it.substring(0, 0)
            }
        """) { spec ->
            spec.afterRecipe { cu ->
                val m = cu.statements[1] as J.MethodInvocation
                assertThat(m.arguments).hasSize(3)
                assertThat(m.arguments[2]).isInstanceOf(J.Lambda::class.java)
                val it = (((m.arguments[2] as J.Lambda).body as J.Block).statements[0] as J.Return).expression as J.MethodInvocation
                assertThat(it.select!!.type.asFullyQualified()!!.fullyQualifiedName).isEqualTo("java.lang.String")
                assertThat(it.methodType!!.name).isEqualTo("substring")
                assertThat(it.methodType!!.declaringType.fullyQualifiedName).isEqualTo("java.lang.String")
            }
        }
    )

    @Test
    fun closureImplicitParameterAttributedZeroArgMethod() = rewriteRun(
        groovy("""
            public <T> T register(String name, Class<T> type, Closure<T> configurationAction) {
                return null
            }
            register("testTask", Integer) {
                it.byteValue()
            }
        """) { spec ->
            spec.afterRecipe { cu ->
                val m = cu.statements[1] as J.MethodInvocation
                assertThat(m.arguments).hasSize(3)
                assertThat(m.arguments[2]).isInstanceOf(J.Lambda::class.java)
                val it = (((m.arguments[2] as J.Lambda).body as J.Block).statements[0] as J.Return).expression as J.MethodInvocation
                assertThat(it.select!!.type.asFullyQualified()!!.fullyQualifiedName).isEqualTo("java.lang.Integer")
                assertThat(it.methodType!!.name).isEqualTo("byteValue")
                assertThat(it.methodType!!.declaringType.fullyQualifiedName).isEqualTo("java.lang.Integer")
            }
        }
    )

    @Test
    fun closureNamedParameterAttributed() = rewriteRun(
        groovy("""
            public <T> T register(String name, Class<T> type, Closure<T> configurationAction) {
                return null
            }
            register("testTask", String) { foo ->
                foo
            }
        """) { spec ->
            spec.afterRecipe { cu ->
                val m = cu.statements[1] as J.MethodInvocation
                assertThat(m.arguments).hasSize(3)
                assertThat(m.arguments[2]).isInstanceOf(J.Lambda::class.java)
                val foo = ((((m.arguments[2] as J.Lambda).body as J.Block).statements[0] as J.Return).expression as G.ExpressionStatement).expression as J.Identifier
                assertThat(foo.type.asFullyQualified()!!.fullyQualifiedName).isEqualTo("java.lang.String")
            }
        }
    )

    @Test
    fun closureWithDelegate() = rewriteRun(
        groovy("""
            public String register(@DelegatesTo(String) Closure stringAction) {
                return null
            }
            register {
                substring(0, 0)
            }
        """) { spec ->
            spec.afterRecipe { cu ->
                val m = cu.statements[1] as J.MethodInvocation
                assertThat(m.arguments).hasSize(1)
                assertThat(m.arguments[0]).isInstanceOf(J.Lambda::class.java)
                val substring = (((m.arguments[0] as J.Lambda).body as J.Block).statements[0] as J.Return).expression as J.MethodInvocation
                assertThat(substring.methodType!!.name).isEqualTo("substring")
                assertThat(substring.methodType!!.declaringType.fullyQualifiedName).isEqualTo("java.lang.String")
            }
        }
    )

    @Disabled
    @Test
    fun infersDelegateViaSimilarGradleApi() = rewriteRun(
        groovy("""
            package org.gradle.api
            
            interface Action<T> {
                void execute(T t);
            }
            void register(Action<String> stringAction) {
            }
            void register(Closure stringAction) {
            }
            register {
                substring(0, 0)
            }
        """) { spec ->
            spec.afterRecipe { cu ->
                val m = cu.statements.filterIsInstance(J.MethodInvocation::class.java).first()
                assertThat(m.arguments).hasSize(1)
                assertThat(m.arguments[0]).isInstanceOf(J.Lambda::class.java)
                val substring = (((m.arguments[0] as J.Lambda).body as J.Block).statements[0] as J.Return).expression as J.MethodInvocation
                assertThat(substring.methodType!!.name).isEqualTo("substring")
                assertThat(substring.methodType!!.declaringType.fullyQualifiedName).isEqualTo("java.lang.String")
            }
        }
    )

    private fun isAttributed(attributed: Boolean) = Consumer<SourceSpec<G.CompilationUnit>> { spec ->
        spec.afterRecipe { cu ->
            object : JavaVisitor<Int>() {
                override fun visitVariable(variable: J.VariableDeclarations.NamedVariable, p: Int): J {
                    assertThat(variable.variableType!!.type.asFullyQualified()!!.fullyQualifiedName)
                        .isEqualTo(if (attributed) "java.util.ArrayList" else "java.lang.Object")
                    return variable
                }
            }.visit(cu, 0)
        }
    }
}
