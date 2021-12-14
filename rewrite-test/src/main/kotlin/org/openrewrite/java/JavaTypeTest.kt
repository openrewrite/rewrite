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

interface JavaTypeTest {
    val executionContext: ExecutionContext
        get() {
            val ctx = InMemoryExecutionContext { t: Throwable? -> fail<Any>("Failed to parse", t) }
            ctx.putMessage(JavaParser.SKIP_SOURCE_SET_MARKER, true)
            return ctx
        }

    @Test
    fun multipleTypeParameterBounds(jp: JavaParser) {
        val cu = jp.parse(
            executionContext,
            """
                class C<Multiple extends I1 & I2> {
                }
                
                interface I1 {}
                interface I2 {}
            """.trimIndent()
        )[0]

        val type = cu.classes[0].type
        println(type)
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
            .contains(JavaType.Class.build("java.lang.FunctionalInterface"))
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
}
