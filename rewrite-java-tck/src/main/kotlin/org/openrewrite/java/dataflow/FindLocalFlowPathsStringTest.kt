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

import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.controlflow.Guard
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface FindLocalFlowPathsStringTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            FindLocalFlowPaths(object : LocalFlowSpec<Expression, Expression>() {
                override fun isSource(expr: Expression, cursor: Cursor) = when (expr) {
                    is J.Literal -> expr.value == "42"
                    is J.MethodInvocation -> expr.name.simpleName == "source"
                    else -> false
                }

                override fun isSink(expr: Expression, cursor: Cursor) = true

                override fun isBarrierGuard(guard: Guard, branch: Boolean): Boolean = guard.expression.run {
                    when (this) {
                        is J.MethodInvocation -> this.name.simpleName == "guard" && branch
                        else -> false
                    }
                }
            })
        })
        spec.expectedCyclesThatMakeChanges(1).cycles(1)
    }

    @Test
    fun `transitive assignment from literal`() = rewriteRun(
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
            """, """
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
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n.toString() + '/';
                        System.out.println(o);
                        String p = o;
                    }
                }
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        java.io.File o = new java.io.File(n);
                        System.out.println(o);
                    }
                }
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
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
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    void sink(Object any) {
                        // do nothing
                    }
                    void test() {
                        sink("42");
                    }
                }
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
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
                            .toLowerCase(Locale.ROOT);
                        source()
                            .toString()
                            .toLowerCase(Locale.ROOT);
                    }
                }
            """, """
                import java.util.Locale;
                class Test {
                    String source() {
                        return null;
                    }
                    void test() {
                        /*~~>*/source();
                        /*~~>*//*~~>*/source()
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
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
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
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
            """, """
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
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
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
            """, """
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

    @Test
    fun `source is tracked when assigned in while loop control parentheses`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    String source() {
                        return null;
                    }
                    @SuppressWarnings("SillyAssignment")
                    void test() {
                        String a;
                        a = a;
                        while ((a = source()) != null) {
                            System.out.println(a);
                        }
                    }
                }
            """, """
            class Test {
                String source() {
                    return null;
                }
                @SuppressWarnings("SillyAssignment")
                void test() {
                    String a;
                    a = a;
                    while ((a = /*~~>*/source()) != null) {
                        System.out.println(/*~~>*/a);
                    }
                }
            }
                """
        )
    )

    @Test
    fun `source is tracked when assigned in do while loop control parentheses`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    String source() {
                        return null;
                    }
                    @SuppressWarnings("SillyAssignment")
                    void test() {
                        String a = null;
                        a = a;
                        do {
                            System.out.println(a);
                        } while ((a = source()) != null);
                    }
                }
            """, """
            class Test {
                String source() {
                    return null;
                }
                @SuppressWarnings("SillyAssignment")
                void test() {
                    String a = null;
                    a = a;
                    do {
                        System.out.println(/*~~>*/a);
                    } while ((a = /*~~>*/source()) != null);
                }
            }
                """
        )
    )

    @Test
    fun `source is tracked when assigned in for loop`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    String source(int i) {
                        return null;
                    }
                    @SuppressWarnings("SillyAssignment")
                    void test() {
                        String a = null;
                        a = a;
                        for (int i = 0; i < 10 && (a = source(i)) != null; i++) {
                            System.out.println(a);
                        }
                    }
                }
            """, """
            class Test {
                String source(int i) {
                    return null;
                }
                @SuppressWarnings("SillyAssignment")
                void test() {
                    String a = null;
                    a = a;
                    for (int i = 0; i < 10 && (a = /*~~>*/source(i)) != null; i++) {
                        System.out.println(/*~~>*/a);
                    }
                }
            }
                """
        )
    )

    @Test
    fun `assignment of value inside if block`() = rewriteRun(
        java(
            """
            class Test {
                String source() {
                    return null;
                }
                void test(boolean condition) {
                    String a = null;
                    if (condition) {
                        a = source();
                        System.out.println(a);
                    }
                }
            }
            """,
            """
            class Test {
                String source() {
                    return null;
                }
                void test(boolean condition) {
                    String a = null;
                    if (condition) {
                        a = /*~~>*/source();
                        System.out.println(/*~~>*/a);
                    }
                }
            }
            """
        )
    )

    @Test
    fun `reassignment of a variable breaks flow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        System.out.println(n);
                        n = "100";
                        System.out.println(n);
                    }
                }
            """, """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        System.out.println(/*~~>*/n);
                        n = "100";
                        System.out.println(n);
                    }
                }
            """
        )
    )

    @Test
    fun `reassignment of a variable with existing value preserves flow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        System.out.println(n);
                        n = n;
                        System.out.println(n);
                    }
                }
            """, """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        System.out.println(/*~~>*/n);
                        n = /*~~>*/n;
                        System.out.println(/*~~>*/n);
                    }
                }
            """
        )
    )

    @Test
    fun `reassignment of a variable with existing value wrapped in parentheses preserves flow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        System.out.println(n);
                        (n) = n;
                        System.out.println(n);
                    }
                }
            """, """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        System.out.println(/*~~>*/n);
                        (n) = /*~~>*/n;
                        System.out.println(/*~~>*/n);
                    }
                }
            """
        )
    )

    @Test
    fun `a class name in a constructor call is not considered as a part of dataflow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    class n {}

                    void test() {
                        String n = "42";
                        System.out.println(n);
                        n = new n().toString();
                        System.out.println(n);
                    }
                }
            """, """
                class Test {
                    class n {}

                    void test() {
                        String n = /*~~>*/"42";
                        System.out.println(/*~~>*/n);
                        n = new n().toString();
                        System.out.println(n);
                    }
                }
            """
        )
    )

    @Test
    fun `a class name in a constructor call on parent type is not considered as a part of dataflow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    class n {}

                    void test() {
                        String n = "42";
                        System.out.println(n);
                        n = new Test.n().toString();
                        System.out.println(n);
                    }
                }
            """, """
                class Test {
                    class n {}

                    void test() {
                        String n = /*~~>*/"42";
                        System.out.println(/*~~>*/n);
                        n = new Test.n().toString();
                        System.out.println(n);
                    }
                }
            """
        )
    )

    @Test
    fun `a method name is not considered as a part of dataflow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    String n() {
                        return null;
                    }

                    void test() {
                        String n = "42";
                        System.out.println(n);
                        n = n();
                        System.out.println(n);
                    }
                }
            """, """
                class Test {
                    String n() {
                        return null;
                    }

                    void test() {
                        String n = /*~~>*/"42";
                        System.out.println(/*~~>*/n);
                        n = n();
                        System.out.println(n);
                    }
                }
            """
        )
    )

    @Test
    fun `a class variable access is not considered as a part of dataflow`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {
                    String n = "100";

                    void test() {
                        String n = "42";
                        System.out.println(n);
                        System.out.println(this.n);
                    }
                }
            """, """
                class Test {
                    String n = "100";

                    void test() {
                        String n = /*~~>*/"42";
                        System.out.println(/*~~>*/n);
                        System.out.println(this.n);
                    }
                }
            """
        )
    )

    @Test
    fun `a ternary operator is considered a data flow step`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {

                    void test(boolean conditional) {
                        String n = conditional ? "42" : "100";
                        System.out.println(n);
                    }
                }
            """, """
                class Test {

                    void test(boolean conditional) {
                        String n = /*~~>*/conditional ? /*~~>*/"42" : "100";
                        System.out.println(/*~~>*/n);
                    }
                }
            """
        )
    )

    @Test
    fun `a ternary operator is considered a data flow step 2`() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) }, java(
            """
                class Test {

                    void test(boolean conditional) {
                        String n = "42";
                        String m = conditional ? "100" : n;
                        System.out.println(m);
                    }
                }
            """, """
                class Test {

                    void test(boolean conditional) {
                        String n = /*~~>*/"42";
                        String m = /*~~>*/conditional ? "100" : /*~~>*/n;
                        System.out.println(/*~~>*/m);
                    }
                }
            """
        )
    )

    @Test
    fun `a ternary condition is not considered a data flow step`() = rewriteRun(
        java(
            """
                class Test {

                    Boolean source() {
                        return null;
                    }

                    void test(String other) {
                        String n = source() ? "102" : "100";
                        System.out.println(n);
                    }
                }
            """, """
                class Test {

                    Boolean source() {
                        return null;
                    }

                    void test(String other) {
                        String n = /*~~>*/source() ? "102" : "100";
                        System.out.println(n);
                    }
                }
            """
        )
    )

    @Test
    fun `Objects requireNotNull is a valid flow step`() = rewriteRun(
        java(
            """
                import java.util.Objects;
                @SuppressWarnings({"ObviousNullCheck", "RedundantSuppression"})
                class Test {
                    void test() {
                        String n = Objects.requireNonNull("42");
                        String o = n;
                        System.out.println(Objects.requireNonNull(o));
                        String p = o;
                    }
                }
            """, """
                import java.util.Objects;
                @SuppressWarnings({"ObviousNullCheck", "RedundantSuppression"})
                class Test {
                    void test() {
                        String n = /*~~>*/Objects.requireNonNull(/*~~>*/"42");
                        String o = /*~~>*/n;
                        System.out.println(/*~~>*/Objects.requireNonNull(/*~~>*/o));
                        String p = /*~~>*/o;
                    }
                }
            """
        )
    )

    @Test
    fun `transitive assignment from literal with with a guard`() = rewriteRun(
        java(
            """
                abstract class Test {
                    abstract boolean guard();

                    void test() {
                        String n = "42";
                        if (guard()) {
                            String o = n;
                            System.out.println(o);
                            String p = o;
                        } else {
                            System.out.println(n);
                        }
                    }
                }
            """, """
                abstract class Test {
                    abstract boolean guard();

                    void test() {
                        String n = /*~~>*/"42";
                        if (guard()) {
                            String o = n;
                            System.out.println(o);
                            String p = o;
                        } else {
                            System.out.println(/*~~>*/n);
                        }
                    }
                }
            """
        )
    )

    @Test
    fun `transitive assignment from literal with with a || guard`() = rewriteRun(
        java(
            """
                abstract class Test {
                    abstract boolean guard();

                    void test() {
                        String n = "42";
                        if (guard() || guard()) {
                            String o = n;
                            System.out.println(o);
                            String p = o;
                        } else {
                            System.out.println(n);
                        }
                    }
                }
            """, """
                abstract class Test {
                    abstract boolean guard();

                    void test() {
                        String n = /*~~>*/"42";
                        if (guard() || guard()) {
                            String o = n;
                            System.out.println(o);
                            String p = o;
                        } else {
                            System.out.println(/*~~>*/n);
                        }
                    }
                }
            """
        )
    )

    @Test
    fun `transitive assignment from literal with with a negated guard`() = rewriteRun(
        java(
            """
                abstract class Test {
                    abstract boolean guard();

                    void test() {
                        String n = "42";
                        if (!guard()) {
                            String o = n;
                            System.out.println(o);
                            String p = o;
                        } else {
                            System.out.println(n);
                        }
                    }
                }
            """, """
                abstract class Test {
                    abstract boolean guard();

                    void test() {
                        String n = /*~~>*/"42";
                        if (!guard()) {
                            String o = /*~~>*/n;
                            System.out.println(/*~~>*/o);
                            String p = /*~~>*/o;
                        } else {
                            System.out.println(n);
                        }
                    }
                }
            """
        )
    )

    @Test
    fun `a thrown exception is a guard`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    if (!guard()) {
                        throw new RuntimeException();
                    }
                    String o = n;
                    System.out.println(o);
                    String p = o;
                }
            }
            """, """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    if (!guard()) {
                        throw new RuntimeException();
                    }
                    String o = n;
                    System.out.println(o);
                    String p = o;
                }
            }
            """
        )
    )

    @Test
    fun `a thrown exception is a guard when included in an boolean expression`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    if (!guard() && !guard()) {
                        throw new RuntimeException();
                    }
                    String o = n;
                    System.out.println(o);
                    String p = o;
                }
            }
            """, """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    if (!guard() && !guard()) {
                        throw new RuntimeException();
                    }
                    String o = n;
                    System.out.println(o);
                    String p = o;
                }
            }
            """
        )
    )

    @Test
    fun `a thrown exception is a guard when included in an boolean expression De Morgan's`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    if (!(guard() || guard())) {
                        throw new RuntimeException();
                    }
                    String o = n;
                    System.out.println(o);
                    String p = o;
                }
            }
            """, """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    if (!(guard() || guard())) {
                        throw new RuntimeException();
                    }
                    String o = n;
                    System.out.println(o);
                    String p = o;
                }
            }
            """
        )
    )

    @Test
    fun `try-catch block`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    try {
                        System.out.println(n);
                    } catch (Exception e) {
                        System.out.println(n);
                    }
                    String o = n;
                    System.out.println(o);
                    String p = o;
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    try {
                        System.out.println(/*~~>*/n);
                    } catch (Exception e) {
                        System.out.println(n);
                    }
                    String o = /*~~>*/n;
                    System.out.println(/*~~>*/o);
                    String p = /*~~>*/o;
                }
            }
            """
        )
    )

    @Test
    fun `if-statement with boolean array index conditional`() = rewriteRun(
        java(
            """
            abstract class Test {

                void test() {
                    String n = "42";
                    Boolean[] b = new Boolean[1];
                    if (b[0] && (b.length == 1)) {
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    } else {
                        System.out.println(n);
                    }
                }
            }
            """, """
            abstract class Test {

                void test() {
                    String n = /*~~>*/"42";
                    Boolean[] b = new Boolean[1];
                    if (b[0] && (b.length == 1)) {
                        String o = /*~~>*/n;
                        System.out.println(/*~~>*/o);
                        String p = /*~~>*/o;
                    } else {
                        System.out.println(/*~~>*/n);
                    }
                }
            }
            """
        )
    )

    // currently giving 'java.lang.AssertionError: The recipe must make changes'
    @Test
    fun `switch with multiple cases`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    switch (n) {
                        case "1":
                            System.out.println(n);
                            break;
                        case "42":
                            System.out.println("Correct");
                            break;
                        default:
                            break;
                    }
                    String o = n + "";
                    System.out.println(o);
                    String p = o;
                }
            }
            """, """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    switch (n) {
                        case "1":
                            System.out.println(n);
                            break;
                        case "42":
                            System.out.println("Correct");
                            break;
                        default:
                            break;
                    }
                    String o = n + "";
                    System.out.println(o);
                    String p = o;
                }
            }
            """
        )
    )



    @Test
    fun `for-each loop`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    for (char c : n.toCharArray()) {
                        System.out.println(c);
                        System.out.println(n);
                    }
                }
            }
            """, """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    for (char c : /*~~>*/n.toCharArray()) {
                        System.out.println(c);
                        System.out.println(/*~~>*/n);
                    }
                }
            }
            """
        )
    )


    @Test
    fun `generic object instantiation`() = rewriteRun(
        java(
            """
            import java.util.LinkedList;
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    LinkedList<Integer> ll = new LinkedList<>();
                    ll.add(1);
                    for (int i : ll) {
                        System.out.println(i);
                        System.out.println(n);
                    }
                }
            }
            """,
            """
            import java.util.LinkedList;
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    LinkedList<Integer> ll = new LinkedList<>();
                    ll.add(1);
                    for (int i : ll) {
                        System.out.println(i);
                        System.out.println(/*~~>*/n);
                    }
                }
            }
            """
        )
    )

    @Test
    fun `assert expression`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();
                void test() {
                    String n = "42";
                    assert n.contains("4");
                }
            }
            """, """
            abstract class Test {
                abstract boolean guard();
                void test() {
                    String n = /*~~>*/"42";
                    assert /*~~>*/n.contains("4");
                }
            }
            """
        )
    )


    @Test
    fun `lambda expression`() = rewriteRun(
        java(
            """
            import java.util.ArrayList; abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    ArrayList<Integer> numbers = new ArrayList<Integer>();
                    numbers.forEach( (i) -> { System.out.println(n); } );
                }
            }
            """, """
            import java.util.ArrayList; abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = /*~~>*/"42";
                    ArrayList<Integer> numbers = new ArrayList<Integer>();
                    numbers.forEach( (i) -> { System.out.println(/*~~>*/n); } );
                }
            }
            """
        )
    )

    @Test
    fun `true literal guard`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    String n = "42";
                    if (true) {
                        System.out.println(n);
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test() {
                    String n = /*~~>*/"42";
                    if (true) {
                        System.out.println(/*~~>*/n);
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `false literal guard`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    String n = "42";
                    if (false) {
                        System.out.println(n);
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test() {
                    String n = /*~~>*/"42";
                    if (false) {
                        System.out.println(n);
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a higher block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    String n;
                    {
                        n = "42";
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test() {
                    String n;
                    {
                        n = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a doubly higher block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    String n;
                    {
                        {
                            n = "42";
                        }
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test() {
                    String n;
                    {
                        {
                            n = /*~~>*/"42";
                        }
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a triply higher block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    String n;
                    {
                        {
                            {
                                n = "42";
                            }
                        }
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test() {
                    String n;
                    {
                        {
                            {
                                n = /*~~>*/"42";
                            }
                        }
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in an if block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    if (condition) {
                        n = "42";
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    if (condition) {
                        n = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a while block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    while (condition) {
                        n = "42";
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    while (condition) {
                        n = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a do-while block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    do {
                        n = "42";
                    } while (condition);
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    do {
                        n = /*~~>*/"42";
                    } while (condition);
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a for i block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    for(int i = 0; i < 42; i++) {
                        n = "42";
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    for(int i = 0; i < 42; i++) {
                        n = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a for each block`() = rewriteRun(
        java(
            """
            import java.util.List;
            abstract class Test {
                void test(boolean condition, List<Integer> integerList) {
                    String n;
                    for(Integer i : integerList) {
                        n = "42";
                    }
                    System.out.println(n);
                }
            }
            """,
            """
            import java.util.List;
            abstract class Test {
                void test(boolean condition, List<Integer> integerList) {
                    String n;
                    for(Integer i : integerList) {
                        n = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a try catch block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    try {
                        n = "42";
                    } catch (Exception e) {
                        // No-op
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    try {
                        n = /*~~>*/"42";
                    } catch (Exception e) {
                        // No-op
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a try catch finally block`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    String o;
                    try {
                        n = "42";
                    } catch (Exception e) {
                        // No-op
                    } finally {
                        o = "42";
                    }
                    System.out.println(n);
                    System.out.println(o);
                }
            }
            """, """
            abstract class Test {
                void test(boolean condition) {
                    String n;
                    String o;
                    try {
                        n = /*~~>*/"42";
                    } catch (Exception e) {
                        // No-op
                    } finally {
                        o = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                    System.out.println(/*~~>*/o);
                }
            }
            """
        )
    )

    @Test
    fun `data flow should not cross scope boundaries`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    {
                        String n;
                        {
                            n = "42";
                        }
                        System.out.println(n);
                    }
                    {
                        String n = "hello";
                        System.out.println(n);
                    }
                }
            }
            """,
            """
            abstract class Test {
                void test() {
                    {
                        String n;
                        {
                            n = /*~~>*/"42";
                        }
                        System.out.println(/*~~>*/n);
                    }
                    {
                        String n = "hello";
                        System.out.println(n);
                    }
                }
            }
            """
        )
    )

    @Test
    fun `data flow should not cross scope boundaries with class scope variable conflicts`() = rewriteRun(
        java(
            """
            abstract class Test {
                String n;
                void test() {
                    {
                        String n;
                        {
                            n = "42";
                        }
                        System.out.println(n);
                    }
                    {
                        System.out.println(n);
                    }
                }
            }
            """,
            """
            abstract class Test {
                String n;
                void test() {
                    {
                        String n;
                        {
                            n = /*~~>*/"42";
                        }
                        System.out.println(/*~~>*/n);
                    }
                    {
                        System.out.println(n);
                    }
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a higher block in static block`() = rewriteRun(
        java(
            """
            abstract class Test {
                static {
                    String n;
                    {
                        n = "42";
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                static {
                    String n;
                    {
                        n = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in an init`() = rewriteRun(
        java(
            """
            abstract class Test {
                {
                    String n = "42";
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                {
                    String n = /*~~>*/"42";
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )

    @Test
    fun `data flow for a source in a higher block in init block`() = rewriteRun(
        java(
            """
            abstract class Test {
                {
                    String n;
                    {
                        n = "42";
                    }
                    System.out.println(n);
                }
            }
            """, """
            abstract class Test {
                {
                    String n;
                    {
                        n = /*~~>*/"42";
                    }
                    System.out.println(/*~~>*/n);
                }
            }
            """
        )
    )
}
