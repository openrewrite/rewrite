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

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J
import java.util.concurrent.atomic.AtomicInteger

interface JavaTemplateSubstitutionsTest : JavaRecipeTest {

    @Suppress("InfiniteRecursion")
    @Test
    fun any(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : Recipe() {
            val cycle = AtomicInteger(0)

            override fun getDisplayName(): String {
                return ""
            }

            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaIsoVisitor<ExecutionContext>() {
                    val t = JavaTemplate.builder({ cursor }, "test(#{any()})").build()

                    override fun visitMethodDeclaration(
                        method: J.MethodDeclaration,
                        p: ExecutionContext,
                    ): J.MethodDeclaration {
                        if (cycle.getAndIncrement() == 0) {
                            val s = method.body!!.statements[0]
                            return method.withTemplate(
                                t,
                                s.coordinates.replace(),
                                s
                            )
                        }
                        return method
                    }
                }
            }

        },
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

    @Suppress("InfiniteRecursion")
    @Test
    fun array(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : Recipe() {
            val cycle = AtomicInteger(0)

            override fun getDisplayName(): String {
                return ""
            }

            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaIsoVisitor<ExecutionContext>() {
                    val t = JavaTemplate.builder({ cursor }, "test(#{anyArray()})").build()

                    override fun visitMethodDeclaration(
                        method: J.MethodDeclaration,
                        p: ExecutionContext,
                    ): J.MethodDeclaration {
                        if (cycle.getAndIncrement() == 0) {
                            val s = method.body!!.statements[0]
                            return method.withTemplate(
                                t,
                                s.coordinates.replace(),
                                s
                            )
                        }
                        return method
                    }
                }
            }
        },
        before = """
            class Test {
                void test(int[][] n) {
                    array();
                }
                
                int[][] array() {
                    return new int[0][0];
                }
            }
        """,
        after = """
            class Test {
                void test(int[][] n) {
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
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "#{} void test2() {}")
                    .javaParser { JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true).build() }
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext,
                ): J.MethodDeclaration {
                    if (method.simpleName == "test") {
                        return method.withTemplate(
                            t,
                            method.coordinates.replace(),
                            method.leadingAnnotations[0]
                        )
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
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

    @Suppress("ResultOfMethodCallIgnored", "InfiniteRecursion")
    @Test
    fun methodInvocation(jp: JavaParser) = assertChanged(
        jp,
        recipe = object : Recipe() {
            val cycle = AtomicInteger(0)

            override fun getDisplayName(): String {
                return ""
            }

            override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
                return object : JavaIsoVisitor<ExecutionContext>() {
                    val t = JavaTemplate.builder({ cursor }, "test(#{any(java.util.Collection)}, #{any(int)})").build()

                    override fun visitMethodDeclaration(
                        method: J.MethodDeclaration,
                        p: ExecutionContext,
                    ): J.MethodDeclaration {
                        if (cycle.getAndIncrement() == 0) {
                            val s = method.body!!.statements[0]
                            return method.withTemplate(
                                t,
                                s.coordinates.replace(),
                                s,
                                (method.parameters[1] as J.VariableDeclarations).variables[0].name
                            )
                        }
                        return method
                    }
                }
            }
        },
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

    @Suppress("ConstantConditions")
    @Test
    fun block(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                val t = JavaTemplate.builder({ cursor }, "if(true) #{}")
                    .build()

                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext,
                ): J.MethodDeclaration {
                    if (method.body!!.statements[0] !is J.If) {
                        return method.withTemplate(t,
                            method.body!!.statements[0].coordinates.replace(),
                            method.body
                        )
                    }
                    return super.visitMethodDeclaration(method, p)
                }
            }
        },
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1985")
    @Test
    fun newArray(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {

                override fun visitNewArray(newArray: J.NewArray, p: ExecutionContext): J {
                    if ((newArray.dimensions[0].index as J.Literal).value == 1) {
                        val t = JavaTemplate.builder({ cursor }, "Some.method()")
                            .javaParser {
                                JavaParser.fromJavaVersion()
                                    .logCompilationWarningsAndErrors(true)
                                    .dependsOn("""
                                        public class Some {
                                            public static int[] method() {
                                                return new int[0];
                                            }
                                        }
                                    """.trimIndent()).build()
                            }.build()
                        return newArray.withTemplate(t, newArray.coordinates.replace())
                    }
                    return super.visitNewArray(newArray, p)
                }
            }
        },
        before = """
            public class Test {
                int[] array = new int[1];
            }
        """,
        after = """
            public class Test {
                int[] array = Some.method();
            }
        """
    )

    @Suppress("ConstantConditions")
    @Test
    fun binary(jp: JavaParser) = assertChanged(
        jp,
        recipe = toRecipe {
            object : JavaVisitor<ExecutionContext>() {

                override fun visitBinary(binary: J.Binary, p: ExecutionContext): J {
                    if (binary.operator == J.Binary.Type.Equal) {
                        val t = JavaTemplate.builder({ cursor }, "Some.method()")
                            .javaParser {
                                JavaParser.fromJavaVersion()
                                    .logCompilationWarningsAndErrors(true)
                                    .dependsOn("""
                                        public class Some {
                                            public static boolean method() {
                                                return true;
                                            }
                                        }
                                    """.trimIndent()).build()
                            }.build()
                        return binary.withTemplate(t, binary.coordinates.replace())
                    }
                    return super.visitBinary(binary, p)
                }
            }
        },
        before = """
            public class Test {
                boolean binary = 1 == 1;
            }
        """,
        after = """
            public class Test {
                boolean binary = Some.method();
            }
        """
    )
}
