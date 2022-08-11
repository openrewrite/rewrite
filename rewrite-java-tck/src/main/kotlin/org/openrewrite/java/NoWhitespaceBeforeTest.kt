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
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.Tree
import org.openrewrite.java.cleanup.NoWhitespaceBefore
import org.openrewrite.java.format.AutoFormatVisitor
import org.openrewrite.java.style.Checkstyle
import org.openrewrite.java.style.NoWhitespaceBeforeStyle
import org.openrewrite.style.NamedStyles

@Suppress(
    "ConstantConditions",
    "UnusedAssignment",
    "ReturnOfThis",
    "InfiniteLoopStatement",
    "StatementWithEmptyBody"
)
interface NoWhitespaceBeforeTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = NoWhitespaceBefore()

    fun noWhitespaceBeforeStyle(with: NoWhitespaceBeforeStyle.() -> NoWhitespaceBeforeStyle = { this }) =
        listOf(
            NamedStyles(
                Tree.randomId(), "test", "test", "test", emptySet(), listOf(
                    Checkstyle.noWhitespaceBeforeStyle().run { with(this) }
                )
            )
        )

    @Test
    @DisabledOnOs(value = [OS.WINDOWS], disabledReason = "java.nio.file.Path does not allow leading or trailing spaces on Windows")
    fun packages(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            package org .openrewrite .example . cleanup;

            class Test {
            }
        """
    )

    @Test
    @DisabledOnOs(value = [OS.WINDOWS], disabledReason = "java.nio.file.Path does not allow leading or trailing spaces on Windows")
    fun imports(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            import java . util . function.*;

            class Test {
            }
        """
    )

    @Test
    fun fieldAccessDoNotAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withDot(true)
        }).build(),
        before = """
            class Test {
                int m;

                static void method() {
                    new Test()
                            .m = 2;
                    new Test() .m = 2;
                }
            }
        """,
        after = """
            class Test {
                int m;

                static void method() {
                    new Test().m = 2;
                    new Test().m = 2;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun fieldAccessAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withAllowLineBreaks(true)
                .withDot(true)
        }).build(),
        before = """
            class Test {
                int m;

                static void method() {
                    new Test()
                            .m = 2;
                    new Test() .m = 2;
                }
            }
        """,
        after = """
            class Test {
                int m;

                static void method() {
                    new Test()
                            .m = 2;
                    new Test().m = 2;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    @Disabled
    fun methodDeclarationParametersDoNotAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            class Test {
                static void method0(String
                                           ...params) {
                }
                
                static void method1(String ...params) {
                }
            }
        """,
        after = """
            class Test {
                static void method0(String...params) {
                }
                
                static void method1(String...params) {
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    @Disabled
    fun methodDeclarationParametersAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withAllowLineBreaks(true)
        }).build(),
        before = """
            class Test {
                static void method0(String
                                           ...params) {
                }
                
                static void method1(String ...params) {
                }
            }
        """,
        after = """
            class Test {
                static void method0(String
                                           ...params) {
                }
                
                static void method1(String...params) {
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun methodInvocationDoNotAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withDot(true)
        }).build(),
        before = """
            class Test {
                Test test(int... i) {
                    return this;
                }

                void method(Test t) {
                    test(1 , 2) .test(3 , 4) .test( );
                    t .test()
                        .test();
                }
            }
        """,
        after = """
            class Test {
                Test test(int... i) {
                    return this;
                }

                void method(Test t) {
                    test(1, 2).test(3, 4).test();
                    t.test().test();
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun methodInvocationAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withAllowLineBreaks(true)
                .withDot(true)
        }).build(),
        before = """
            class Test {
                Test test(int... i) {
                    return this;
                }

                void method(Test t) {
                    test(1 , 2) .test(3 , 4) .test( );
                    t .test()
                        .test();
                }
            }
        """,
        after = """
            class Test {
                Test test(int... i) {
                    return this;
                }

                void method(Test t) {
                    test(1, 2).test(3, 4).test();
                    t.test()
                        .test();
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun forLoop(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            class Test {
                static void method() {
                    for (int i = 0 , j = 0 ; i < 2 ; i++ , j++) {
                        // do nothing    
                    }
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    for (int i = 0, j = 0; i < 2; i++, j++) {
                        // do nothing    
                    }
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun forEachLoop(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            import java.util.List;

            class Test {
                static void method(List<String> list) {
                    for (String s : list) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    fun whileLoop(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            class Test {
                static void method0() {
                    while (true) ;
                }

                static void method1() {
                    while (true) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    fun doWhileLoop(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            class Test {
                static void method() {
                    do { } while (true);
                }
            }
        """
    )

    @Test
    fun variableDeclarationDoNotAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            class Test {
                static void method() {
                    int n , o = 0;
                    int x
                            , y = 0;
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    int n, o = 0;
                    int x, y = 0;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun variableDeclarationAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withAllowLineBreaks(true)
        }).build(),
        before = """
            class Test {
                static void method() {
                    int n , o = 0;
                    int x
                            , y = 0;
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    int n, o = 0;
                    int x
                            , y = 0;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun arrayDeclarations(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            class Test {
                static void method() {
                    int[][] array = {{1, 2}
                            , {3, 4}};
                }
            }
        """
    )

    @Test
    fun unaryDoNotAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            class Test {
                static void method(int n) {
                    n ++;
                    n --;
                    n
                            ++;
                }
            }
        """,
        after = """
            class Test {
                static void method(int n) {
                    n++;
                    n--;
                    n++;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun unaryAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withAllowLineBreaks(true)
        }).build(),
        before = """
            class Test {
                static void method(int n) {
                    n ++;
                    n --;
                    n
                            ++;
                }
            }
        """,
        after = """
            class Test {
                static void method(int n) {
                    n++;
                    n--;
                    n
                            ++;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun parameterizedTypeDoNotAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withGenericStart(true)
                .withGenericEnd(true)
        }).build(),
        before = """
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Function;

            class Test {
                static void method() {
                    List <String > list0 = new ArrayList <>();
                    List <Function <String, String > > list1 = new ArrayList <>();
                    List<String
                            > list2 = new ArrayList <>();
                }
            }
        """,
        after = """
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Function;

            class Test {
                static void method() {
                    List<String> list0 = new ArrayList<>();
                    List<Function<String, String>> list1 = new ArrayList<>();
                    List<String> list2 = new ArrayList<>();
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun memberReferenceDoNotAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withMethodRef(true)
        }).build(),
        before = """
            import java.util.function.Function;
            import java.util.function.Supplier;

            class Test {
                static void method() {
                    Supplier<Function<String, String>> a = Function ::identity;
                    Supplier<Function<String, String>> b = Function
                            ::identity;
                }
            }
        """,
        after = """
            import java.util.function.Function;
            import java.util.function.Supplier;

            class Test {
                static void method() {
                    Supplier<Function<String, String>> a = Function::identity;
                    Supplier<Function<String, String>> b = Function::identity;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun memberReferenceAllowLineBreaks(jp: JavaParser.Builder<*, *>) = assertChanged(
        parser = jp.styles(noWhitespaceBeforeStyle {
            withAllowLineBreaks(true)
                .withMethodRef(true)
        }).build(),
        before = """
            import java.util.function.Function;
            import java.util.function.Supplier;

            class Test {
                static void method() {
                    Supplier<Function<String, String>> a = Function ::identity;
                    Supplier<Function<String, String>> b = Function
                            ::identity;
                }
            }
        """,
        after = """
            import java.util.function.Function;
            import java.util.function.Supplier;

            class Test {
                static void method() {
                    Supplier<Function<String, String>> a = Function::identity;
                    Supplier<Function<String, String>> b = Function
                            ::identity;
                }
            }
        """,
        afterConditions = { cu ->
            val nucu = AutoFormatVisitor<ExecutionContext>().visit(cu, InMemoryExecutionContext {})
            assertThat(nucu).isEqualTo(cu)
        }
    )

    @Test
    fun doNotStripLastParameterSuffixInMethodDeclaration(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            package a;

            abstract class Test {
                abstract Test method(
                    int n,
                    int m
                );
            }
        """
    )

    @Test
    fun doNotStripLastArgumentSuffixInMethodInvocation(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            package a;

            class Test {
                static void method() {
                    int n = Math.min(
                            1,
                            2
                    );
                }
            }
        """
    )

    @Test
    fun doNotStripStatementSuffixInTernaryConditionAndTrue(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            import java.util.List;

            class Test {
                static void method(List<Object> l) {
                    int n = l.isEmpty() ? l.size() : 2;
                }
            }
        """
    )

    @Test
    fun doNotStripStatementSuffixPrecedingInstanceof(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            import java.util.ArrayList;
            import java.util.List;

            class Test {
                static void method(List<Object> l) {
                    boolean b = l.subList(0, 1) instanceof ArrayList;
                }
            }
        """
    )

    @Test
    fun doNotStripTryWithResourcesEndParentheses(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            import java.io.FileInputStream;
            import java.io.FileOutputStream;
            import java.io.InputStream;
            import java.io.OutputStream;
            import java.util.zip.GZIPInputStream;

            class Test {
                public static void main(String[] args) {
                    try (
                            InputStream source = new GZIPInputStream(new FileInputStream(args[0]));
                            OutputStream out = new FileOutputStream(args[1])
                    ) {
                        System.out.println("side effect");
                    } catch (Exception e) {
                        // do nothing
                    }
                }
            }
        """
    )

    @Test
    fun doNotStripAnnotationArguments(jp: JavaParser.Builder<*, *>) = assertUnchanged(
        parser = jp.styles(noWhitespaceBeforeStyle()).build(),
        before = """
            import org.graalvm.compiler.core.common.SuppressFBWarnings;

            class Test {
                @SuppressFBWarnings(
                        value = "SECPR",
                        justification = "Usages of this method are not meant for cryptographic purposes"
                )
                static void method() {
                }
            }
        """
    )

}
