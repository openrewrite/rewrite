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
package org.openrewrite.java

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils

interface JavaTypeTest {
    val executionContext: ExecutionContext
        get() {
            val ctx = InMemoryExecutionContext { t: Throwable? -> fail<Any>("Failed to parse", t) }
            ctx.putMessage(JavaParser.SKIP_SOURCE_SET_TYPE_GENERATION, true)
            return ctx
        }

    @Test
    fun resolvedSignatureOfGenericMethodDeclarations(jp: JavaParser.Builder<*, *>) {
        val cu = jp
            .logCompilationWarningsAndErrors(true)
            .build()
            .parse(
                executionContext,
                """
                    import java.util.ListIterator;
                    import static java.util.Collections.singletonList;
                    
                    interface MyList<E> {
                        ListIterator<E> listIterator();
                    }
                    
                    class Test {
                        ListIterator<Integer> s = singletonList(1).listIterator();
                    }
                """.trimIndent()
            )[0]

        val declaredMethod = cu.classes[0].type!!.methods[0]
        assertThat(declaredMethod.returnType).isInstanceOf(JavaType.Parameterized::class.java)

        val inv = ((cu.classes[1].body.statements[0] as J.VariableDeclarations).variables[0].initializer as J.MethodInvocation)
        val rt = inv.methodType!!.returnType
        assertThat(TypeUtils.asParameterized(rt)!!.typeParameters[0].asFullyQualified()!!.fullyQualifiedName)
            .isEqualTo("java.lang.Integer")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/762")
    @Test
    fun annotationsOnTypeAttribution(jp: JavaParser) {
        val cu = jp.parse(
            executionContext,
            """
            import java.util.function.Consumer;
            class Test {
                Consumer<String> c;
            }
        """
        )[0]

        val field = cu.classes[0].body.statements[0]
        val annotations = (field as J.VariableDeclarations).typeAsFullyQualified?.annotations

        assertThat(annotations)
            .hasSize(1)
        assertThat(annotations!![0].asFullyQualified()?.fullyQualifiedName).isEqualTo("java.lang.FunctionalInterface")
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1267")
    @Test
    fun noStackOverflow(jp: JavaParser.Builder<*, *>) {
        val cu = jp.build().parse(
            executionContext,
            """
            import java.util.HashMap;
            import java.util.Map;
            class A {
                Map<String, Map<String, Map<Integer, String>>> overflowMap = new HashMap<>();
            }
            """
        )
        val foo = cu.find { it.classes[0].name.simpleName == "A" }!!
        val foundTypes = foo.typesInUse.typesInUse
        assertThat(foundTypes.isNotEmpty())
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2493")
    @Test
    fun noNewMethodType(jp: JavaParser.Builder<*, *>) {
        val cu = jp.build().parse(
            executionContext,
            """
                public class Test {
                }
            """,
            """
            public class A {
                void method() {
                    Test a = test(null);
                }
                
                Test test(Test test) {
                    return test;
                }
            }
            """
        )
        val cu2 = object : JavaIsoVisitor<ExecutionContext>() {
            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation {
                var m = super.visitMethodInvocation(method, p)
                if (m.name.prefix.whitespace.isEmpty()) {
                    m = m.withName(m.name.withPrefix(m.name.prefix.withWhitespace("  ")))
                }
                return m
            }
        }.visit(cu[1], InMemoryExecutionContext()) as J.CompilationUnit

        val mi = ((((cu2.classes[0].body.statements[0] as J.MethodDeclaration)
            .body!!.statements[0] as J.VariableDeclarations)
            .variables[0] as J.VariableDeclarations.NamedVariable)
            .initializer as J.MethodInvocation)
        assertThat(mi.name.type === mi.methodType).isTrue
    }
}
