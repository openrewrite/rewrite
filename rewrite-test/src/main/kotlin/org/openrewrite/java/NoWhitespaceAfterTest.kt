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
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.cleanup.NoWhitespaceAfter
import org.openrewrite.java.format.AutoFormatVisitor
import org.openrewrite.java.style.Checkstyle
import org.openrewrite.java.style.NoWhitespaceAfterStyle
import org.openrewrite.style.NamedStyles

@Suppress(
    "CStyleArrayDeclaration",
    "StringConcatenationMissingWhitespace",
    "ConstantConditions",
    "UnusedAssignment",
    "UnaryPlus",
    "ReturnOfThis",
    "SimplifiableAnnotation"
)
interface NoWhitespaceAfterTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = NoWhitespaceAfter()

    fun noWhitespaceAfterStyle(with: NoWhitespaceAfterStyle.() -> NoWhitespaceAfterStyle = { this }) =
        listOf(
            NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                    Checkstyle.noWhitespaceAfterStyle().run { with(this) }
                )
            )
        )

    @Test
    fun arrayAccess(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                static void method(int[] n) {
                    int m = n [0];
                }
            }
        """,
        after = """
            class Test {
                static void method(int[] n) {
                    int m = n[0];
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun variableDeclaration(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                static void method() {
                    int [] [] a;
                    int [] b;
                    int c, d = 0;
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    int[][] a;
                    int[] b;
                    int c, d = 0;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun arrayVariableDeclaration(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                static void method() {
                    int[] n = { 1, 2};
                    int[] p = {1, 2 };
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    int[] n = {1, 2};
                    int[] p = {1, 2};
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun assignment(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                static void method(int m) {
                    long o = - m;
                }
            }
        """,
        after = """
            class Test {
                static void method(int m) {
                    long o = -m;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun unaryOperation(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                static void method(int m) {
                    ++ m;
                    -- m;
                    int o = + m;
                    o = ~ m;
                    boolean b = false;
                    b = ! b;
                }
            }
        """,
        after = """
            class Test {
                static void method(int m) {
                    ++m;
                    --m;
                    int o = +m;
                    o = ~m;
                    boolean b = false;
                    b = !b;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun typecastOperation(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle {
            withTypecast(true)
        }).build(),
        before = """
            class Test {
                static void method(int m) {
                    long o = - m;
                    m = (int) o;
                }
            }
        """,
        after = """
            class Test {
                static void method(int m) {
                    long o = -m;
                    m = (int)o;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun methodReference(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle {
            withMethodRef(true)
        }).build(),
        before = """
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<Object> stream) {
                    stream.forEach(System.out:: println);
                }
            }
        """,
        after = """
            import java.util.stream.Stream;

            class Test {
                static void method(Stream<Object> stream) {
                    stream.forEach(System.out::println);
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun methodReturnTypeSignatureAsArray(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                static int [] [] methodArr() { 
                    return null; 
                }
            }
        """,
        after = """
            class Test {
                static int[][] methodArr() { 
                    return null; 
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun fieldAccess(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle {
            withDot(true)
        }).build(),
        before = """
            class Test {
                int m = 0;

                void method0() {
                    int n = this. m;
                }

                static void method1() {
                    new Test()
                            .m = 2;
                }
            }
        """,
        after = """
            class Test {
                int m = 0;

                void method0() {
                    int n = this.m;
                }

                static void method1() {
                    new Test()
                            .m = 2;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun annotation(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                @ Override
                public boolean equals(Object o) {
                    return false;
                }
            }
        """,
        after = """
            class Test {
                @Override
                public boolean equals(Object o) {
                    return false;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun doNotAllowLinebreak(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceAfterStyle {
            withAllowLineBreaks(false)
                .withDot(true)
        }).build(),
        before = """
            class Test {
                int m;

                static void fieldAccess() {
                    new Test().
                            m = 2;
                }

                void methodInvocationChain() {
                    test().
                            test();
                }

                Test test() {
                    return this;
                }
            }
        """,
        after = """
            class Test {
                int m;

                static void fieldAccess() {
                    new Test().m = 2;
                }

                void methodInvocationChain() {
                    test().test();
                }

                Test test() {
                    return this;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun doNotChangeAnnotationValueInNewArray(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            @SuppressWarnings(value = {
                    "all",
                    "unchecked"
            })
            class Test {
            }
        """
    )

    @Test
    fun doNotChangeFirstAndLastValuesOfArrayInitializer(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceAfterStyle()).build(),
        before = """
            class Test {
                int[] ns = {
                        0,
                        1
                };
            }
        """
    )

}
