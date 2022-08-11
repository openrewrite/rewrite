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
import org.openrewrite.java.cleanup.OperatorWrap
import org.openrewrite.java.format.AutoFormatVisitor
import org.openrewrite.java.style.Checkstyle
import org.openrewrite.java.style.OperatorWrapStyle
import org.openrewrite.style.NamedStyles

@Suppress(
    "CStyleArrayDeclaration",
    "StringConcatenationMissingWhitespace",
    "ConstantConditions",
)
interface OperatorWrapTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = OperatorWrap()

    fun operatorWrap(with: OperatorWrapStyle.() -> OperatorWrapStyle = { this }) =
        listOf(
            NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                    Checkstyle.operatorWrapStyle().run { with(this) }
                )
            )
        )

    @Test
    fun binaryOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap()).build(),
        before = """
            class Test {
                static void method() {
                    String s = "aaa" +
                            "b" + "c";
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    String s = "aaa"
                            + "b" + "c";
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun binaryOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
        }).build(),
        before = """
            class Test {
                static void method() {
                    String s = "aaa"
                            + "b" + "c";
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    String s = "aaa" +
                            "b" + "c";
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun typeParameterOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap()).build(),
        before = """
            import java.io.Serializable;

            class Test {
                static <T extends Serializable &
                        Comparable<T>> T method0() {
                    return null;
                }

                static <T extends Serializable> T method1() {
                    return null;
                }
            }
        """,
        after = """
            import java.io.Serializable;

            class Test {
                static <T extends Serializable
                        & Comparable<T>> T method0() {
                    return null;
                }

                static <T extends Serializable> T method1() {
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
    fun typeParameterOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
        }).build(),
        before = """
            import java.io.Serializable;

            class Test {
                static <T extends Serializable
                        & Comparable<T>> T method0() {
                    return null;
                }

                static <T extends Serializable> T method1() {
                    return null;
                }
            }
        """,
        after = """
            import java.io.Serializable;

            class Test {
                static <T extends Serializable &
                        Comparable<T>> T method0() {
                    return null;
                }

                static <T extends Serializable> T method1() {
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
    fun instanceOfOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap()).build(),
        before = """
            class Test {
                static Object method(Object s) {
                    if (s instanceof
                            String) {
                        return null;
                    }
                    return s;
                }
            }
        """,
        after = """
            class Test {
                static Object method(Object s) {
                    if (s
                            instanceof String) {
                        return null;
                    }
                    return s;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun instanceOfOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
        }).build(),
        before = """
            class Test {
                static Object method(Object s) {
                    if (s
                            instanceof String) {
                        return null;
                    }
                    return s;
                }
            }
        """,
        after = """
            class Test {
                static Object method(Object s) {
                    if (s instanceof
                            String) {
                        return null;
                    }
                    return s;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun ternaryOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap()).build(),
        before = """
            class Test {
                static String method(String s) {
                    return s.contains("a") ?
                            "truePart" :
                            "falsePart";
                }
            }
        """,
        after = """
            class Test {
                static String method(String s) {
                    return s.contains("a")
                            ? "truePart"
                            : "falsePart";
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun ternaryOnNewlineIgnoringColon(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withColon(false)
        }).build(),
        before = """
            class Test {
                static String method(String s) {
                    return s.contains("a") ?
                            "truePart" :
                            "falsePart";
                }
            }
        """,
        after = """
            class Test {
                static String method(String s) {
                    return s.contains("a")
                            ? "truePart" :
                            "falsePart";
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun ternaryOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
        }).build(),
        before = """
            class Test {
                static String method(String s) {
                    return s.contains("a")
                            ? "truePart"
                            : "falsePart";
                }
            }
        """,
        after = """
            class Test {
                static String method(String s) {
                    return s.contains("a") ?
                            "truePart" :
                            "falsePart";
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun assignmentOperatorOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withAssign(true)
                .withDivAssign(true)
                .withPlusAssign(true)
                .withMinusAssign(true)
                .withStarAssign(true)
                .withModAssign(true)
                .withSrAssign(true)
                .withBsrAssign(true)
                .withSlAssign(true)
                .withBxorAssign(true)
                .withBorAssign(true)
                .withBandAssign(true)
        }).build(),
        before = """
            class Test {
                static int method() {
                    int a = 0;
                    a /=
                            1;
                    a +=
                            1;
                    return a;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int a = 0;
                    a
                            /= 1;
                    a
                            += 1;
                    return a;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun assignmentOperatorOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
                .withAssign(true)
                .withDivAssign(true)
                .withPlusAssign(true)
                .withMinusAssign(true)
                .withStarAssign(true)
                .withModAssign(true)
                .withSrAssign(true)
                .withBsrAssign(true)
                .withSlAssign(true)
                .withBxorAssign(true)
                .withBorAssign(true)
                .withBandAssign(true)
        }).build(),
        before = """
            class Test {
                static int method() {
                    int a = 0;
                    a
                            /= 1;
                    a
                            += 1;
                    return a;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int a = 0;
                    a /=
                            1;
                    a +=
                            1;
                    return a;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun memberReferenceOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withMethodRef(true)
        }).build(),
        before = """
            import java.util.stream.Stream;

            class Test {
                static void methodStream(Stream<Object> stream) {
                    stream.forEach(System.out::
                            println);
                }
            }
        """,
        after = """
            import java.util.stream.Stream;

            class Test {
                static void methodStream(Stream<Object> stream) {
                    stream.forEach(System.out
                            ::println);
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun memberReferenceOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
                .withMethodRef(true)
        }).build(),
        before = """
            import java.util.stream.Stream;

            class Test {
                static void methodStream(Stream<Object> stream) {
                    stream.forEach(System.out
                            ::println);
                }
            }
        """,
        after = """
            import java.util.stream.Stream;

            class Test {
                static void methodStream(Stream<Object> stream) {
                    stream.forEach(System.out::
                            println);
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun assignmentOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withAssign(true)
        }).build(),
        before = """
            class Test {
                static int method() {
                    int n;
                    n =
                            1;
                    return n;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int n;
                    n
                            = 1;
                    return n;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun assignmentOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
                .withAssign(true)
        }).build(),
        before = """
            class Test {
                static int method() {
                    int n;
                    n
                            = 1;
                    return n;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int n;
                    n =
                            1;
                    return n;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun variableOnNewline(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withAssign(true)
        }).build(),
        before = """
            class Test {
                static void method() {
                    int n =
                            1;
                    int nArr[] =
                            new int[0];
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    int n
                            = 1;
                    int nArr[]
                            = new int[0];
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun variableOnEndOfLine(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(operatorWrap {
            withWrapOption(OperatorWrapStyle.WrapOption.EOL)
                .withAssign(true)
        }).build(),
        before = """
            class Test {
                static void method() {
                    int n
                            = 1;
                    int nArr[]
                            = new int[0];
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    int n =
                            1;
                    int nArr[] =
                            new int[0];
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

}
