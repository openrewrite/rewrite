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
package org.openrewrite.java.dataflow

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface FindLocalFlowPathsStringTest: RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            FindLocalFlowPaths(object : LocalFlowSpec<Expression, Expression>() {
                override fun isSource(expr: Expression, cursor: Cursor) =
                    when(expr) {
                        is J.Literal -> expr.value == "42"
                        is J.MethodInvocation -> expr.name.simpleName == "source"
                        else -> false
                    }

                override fun isSink(expr: Expression, cursor: Cursor) =
                    true
            })
        })
    }

    @Test
    fun `transitive assignment from literal`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        String o = /*~~>*/n;
                        System.out.println(/*~~>*/o);
                        String p = /*~~>*/o;
                    }
                }
            """
        )
    )

    @Test
    fun `transitive assignment from source method`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    String source() {
                        return null;
                    }

                    void test() {
                        String n = source();
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    String source() {
                        return null;
                    }

                    void test() {
                        String n = /*~~>*/source();
                        String o = /*~~>*/n;
                        System.out.println(/*~~>*/o);
                        String p = /*~~>*/o;
                    }
                }
            """
        )
    )

    @Test
    fun `taint flow via append is not data flow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        String o = /*~~>*/n + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
                """
        )
    )

    @Test
    fun `taint flow is not data flow but it is tracked to call site`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n.toString() + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        String o = /*~~>*//*~~>*/n.toString() + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
                """
        )
    )

    @Test
    fun `taint flow via constructor call is not data flow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        java.io.File o = new java.io.File(n);
                        System.out.println(o);
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        java.io.File o = new java.io.File(/*~~>*/n);
                        System.out.println(o);
                    }
                }
                """
        )
    )

    @Test
    fun `the source is also a sink simple`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    String source() {
                        return null;
                    }
                    void sink(Object any) {
                        // do nothing
                    }
                    void test() {
                        sink(source());
                    }
                }
            """,
            """
                class Test {
                    String source() {
                        return null;
                    }
                    void sink(Object any) {
                        // do nothing
                    }
                    void test() {
                        sink(/*~~>*/source());
                    }
                }
            """
        )
    )

    @Test
    fun `the source as a literal is also a sink`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void sink(Object any) {
                        // do nothing
                    }
                    void test() {
                        sink("42");
                    }
                }
            """,
            """
                class Test {
                    void sink(Object any) {
                        // do nothing
                    }
                    void test() {
                        sink(/*~~>*/"42");
                    }
                }
            """
        )
    )

    @Test
    fun `the source is also a sink`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        source();
                        source()
                            .toString();
                        source()
                            .toString()
                            .toString();
                        source()
                            .toLowerCase(Locale.ROOT);
                        source()
                            .toString()
                            .toLowerCase(Locale.ROOT);
                    }
                }
            """,
        """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        /*~~>*/source();
                        /*~~>*//*~~>*/source()
                            .toString();
                        /*~~>*//*~~>*//*~~>*/source()
                            .toString()
                            .toString();
                        /*~~>*/source()
                            .toLowerCase(Locale.ROOT);
                        /*~~>*//*~~>*/source()
                            .toString()
                            .toLowerCase(Locale.ROOT);
                    }
                }
                """
        )
    )

    @Test
    fun `the source is also a sink double call chain`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        source()
                            .toString()
                            .toString();
                    }
                }
            """,
            """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        /*~~>*//*~~>*//*~~>*/source()
                            .toString()
                            .toString();
                    }
                }
                """
        )
    )

    @Test
    fun `the source can be tracked through wrapped parentheses`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        (
                            source()
                        ).toLowerCase(Locale.ROOT);
                        (
                            (
                                source()
                            )
                        ).toLowerCase(Locale.ROOT);
                        (
                            (Object) source()
                        ).equals(null);
                    }
                }
            """,
            """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        /*~~>*/(
                            /*~~>*/source()
                        ).toLowerCase(Locale.ROOT);
                        /*~~>*/(
                            /*~~>*/(
                                /*~~>*/source()
                            )
                        ).toLowerCase(Locale.ROOT);
                        /*~~>*/(
                            /*~~>*/(Object) /*~~>*/source()
                        ).equals(null);
                    }
                }
                """
        )
    )

    @Test
    fun `the source can be tracked through wrapped parentheses through casting`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        (
                            (String)(
                                (Object) source()
                            )
                        ).toString();
                    }
                }
            """,
            """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        /*~~>*//*~~>*/(
                            /*~~>*/(String)/*~~>*/(
                                /*~~>*/(Object) /*~~>*/source()
                            )
                        ).toString();
                    }
                }
                """
        )
    )
}
