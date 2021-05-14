/*
 * Copyright 2020 the original author or authors.
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

import com.google.common.io.CharSink
import com.google.common.io.CharSource
import com.google.googlejavaformat.java.Formatter
import com.google.googlejavaformat.java.JavaFormatterOptions
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.tree.J
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

interface JavaTemplateSubstitutionsTest : JavaRecipeTest {

    @Test
    fun any(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val cycle = AtomicInteger(0)
            val t = template("test(#{any()})").build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (cycle.getAndIncrement() == 0) {
                    val s = method.body!!.statements[0]
                    return method.withTemplate(
                        t,
                        s.coordinates.replace(),
                        s
                    )
                }
                return method;
            }
        }.toRecipe(),
        before = """
            class Test {
                void test(int n) {
                    value();
                }
                
                int value() {
                    return 0;
                }
            }
        """,
        after = """
            class Test {
                void test(int n) {
                    test(value());
                }
                
                int value() {
                    return 0;
                }
            }
        """
    )

    @Test
    fun array(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val cycle = AtomicInteger(0)
            val t = template("test(#{anyArray()})").build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (cycle.getAndIncrement() == 0) {
                    val s = method.body!!.statements[0]
                    return method.withTemplate(
                        t,
                        s.coordinates.replace(),
                        s
                    )
                }
                return method;
            }
        }.toRecipe(),
        before = """
            class Test {
                void test(int n[][]) {
                    array();
                }
                
                int[][] array() {
                    return new int[0][0];
                }
            }
        """,
        after = """
            class Test {
                void test(int n[][]) {
                    test(array());
                }
                
                int[][] array() {
                    return new int[0][0];
                }
            }
        """
    )

    @Test
    fun annotation(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("#{} void test2() {}").build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (method.simpleName == "test") {
                    return method.withTemplate(
                        t,
                        method.coordinates.replace(),
                        method.leadingAnnotations[0]
                    )
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                @SuppressWarnings("ALL") void test() {
                }
            }
        """,
        after = """
            class Test {
            
                @SuppressWarnings("ALL")
                void test2() {
                }
            }
        """
    )

    @Test
    fun methodInvocation(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val cycle = AtomicInteger(0)
            val t = template("test(#{any(java.util.Collection)}, #{any(int)})").build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (cycle.getAndIncrement() == 0) {
                    val s = method.body!!.statements[0]
                    return method.withTemplate(
                        t,
                        s.coordinates.replace(),
                        s,
                        (method.parameters[1] as J.VariableDeclarations).variables[0].name
                    )
                }
                return method;
            }
        }.toRecipe(),
        before = """
            import java.util.*;
            class Test {
                void test(Collection<?> c, Integer n) {
                    Collections.emptyList();
                }
            }
        """,
        after = """
            import java.util.*;
            class Test {
                void test(Collection<?> c, Integer n) {
                    test(Collections.emptyList(), n);
                }
            }
        """
    )

    @Test
    fun block(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            val t = template("if(true) #{}")
                .doBeforeParseTemplate(print)
                .build()

            override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                if (method.body!!.statements[0] !is J.If) {
                    return method.withTemplate(t,
                        method.body!!.statements[0].coordinates.replace(),
                        method.body
                    )
                }
                return super.visitMethodDeclaration(method, p)
            }
        }.toRecipe(),
        before = """
            class Test {
                void test() {
                    int n;
                }
            }
        """,
        after = """
            class Test {
                void test() {
                    if (true) {
                        int n;
                    }
                }
            }
        """
    )

    companion object {
        val print = Consumer<String> { s: String ->
            try {
                val bos = ByteArrayOutputStream()
                Formatter(JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.AOSP).build())
                    .formatSource(CharSource.wrap(s), object : CharSink() {
                        override fun openStream() = OutputStreamWriter(bos)
                    })

                println(bos.toString(Charsets.UTF_8).trim())
            } catch (_: Throwable) {
                println("Unable to format:")
                println(s)
            }
        }
    }
}
