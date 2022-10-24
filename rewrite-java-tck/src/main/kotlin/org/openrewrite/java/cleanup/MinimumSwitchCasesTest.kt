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
@file:Suppress("DuplicateBranchesInSwitch", "IfStatementWithIdenticalBranches")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.java.search.SemanticallyEqualTest.Companion.jp
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation

@Suppress("SwitchStatementWithTooFewBranches", "ConstantConditions")
interface MinimumSwitchCasesTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(MinimumSwitchCases())
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun primitiveAndDefault() = rewriteRun(
        java(
            """
                class Test {
                    int variable;
                    void test() {
                        switch (variable) {
                          case 0:
                              doSomething();
                              break;
                          default:
                              doSomethingElse();
                              break;
                        }
                    }
                    void doSomething() {}
                    void doSomethingElse() {}
                }
            """,
            """
                class Test {
                    int variable;
                    void test() {
                        if (variable == 0) {
                            doSomething();
                        } else {
                            doSomethingElse();
                        }
                    }
                    void doSomething() {}
                    void doSomethingElse() {}
                }
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun twoPrimitives() = rewriteRun(
        java(
            """
            class Test {
                int variable;
                void test() {
                    switch (variable) {
                      case 0:
                          doSomething();
                          break;
                      case 1:
                          doSomethingElse();
                          break;
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """,
            """
            class Test {
                int variable;
                void test() {
                    if (variable == 0) {
                        doSomething();
                    } else if (variable == 1) {
                        doSomethingElse();
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun stringAndDefault() = rewriteRun(
        java("""
            class Test {
                String name;
                void test() {
                    switch (name) {
                      case "jonathan":
                          doSomething();
                          break;
                      default:
                          doSomethingElse();
                          break;
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """,
        """
            class Test {
                String name;
                void test() {
                    if ("jonathan".equals(name)) {
                        doSomething();
                    } else {
                        doSomethingElse();
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun twoStrings() = rewriteRun(
        java("""
            class Test {
                String name;
                void test() {
                    switch (name) {
                      case "jonathan":
                          doSomething();
                          break;
                      case "jon":
                          doSomethingElse();
                          break;
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """,
        """
            class Test {
                String name;
                void test() {
                    if ("jonathan".equals(name)) {
                        doSomething();
                    } else if ("jon".equals(name)) {
                        doSomethingElse();
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun onePrimitive() = rewriteRun(
        java("""
            class Test {
                int variable;
                void test() {
                    switch (variable) {
                      case 0:
                          doSomething();
                          break;
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """,
        """
            class Test {
                int variable;
                void test() {
                    if (variable == 0) {
                        doSomething();
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun oneString() = rewriteRun(
        java("""
            class Test {
                String name;
                void test() {
                    switch (name) {
                      case "jonathan":
                          doSomething();
                          break;
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """,
        """
            class Test {
                String name;
                void test() {
                    if ("jonathan".equals(name)) {
                        doSomething();
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """)
    )

    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun noCases() = rewriteRun(
        java("""
            class Test {
                int variable;
                void test() {
                    switch (variable) {
                    }
                }
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1212")
    fun importsOnEnum() = rewriteRun(
        java("""
            import java.time.DayOfWeek;

            class Test {
                DayOfWeek day;

                void test() {
                    switch(day) {
                        case MONDAY:
                            someMethod();
                            break;
                    }
                    switch(day) {
                        case MONDAY:
                            someMethod();
                        default:
                            someMethod();
                            break;
                    }
                    switch (day) {
                        case MONDAY:
                            someMethod();
                            break;
                        case TUESDAY:
                            someMethod();
                            break;
                    }
                }

                void someMethod() {
                }
            }
        """,
        """
            import java.time.DayOfWeek;

            class Test {
                DayOfWeek day;

                void test() {
                    if (day == DayOfWeek.MONDAY) {
                        someMethod();
                    }
                    if (day == DayOfWeek.MONDAY) {
                        someMethod();
                    } else {
                        someMethod();
                    }
                    if (day == DayOfWeek.MONDAY) {
                        someMethod();
                    } else if (day == DayOfWeek.TUESDAY) {
                        someMethod();
                    }
                }

                void someMethod() {
                }
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1701")
    @Test
    fun removeBreaksFromCaseBody()  = rewriteRun(
        java("""
            class Test {
                String name;
                void test() {
                    switch (name) {
                      case "jonathan": {
                          doSomething();
                          break;
                      }
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """,
        """
            class Test {
                String name;
                void test() {
                    if ("jonathan".equals(name)) {
                        doSomething();
                    }
                }
                void doSomething() {}
                void doSomethingElse() {}
            }
        """)
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/2258")
    @Test
    fun defaultOnly() = rewriteRun(
        java("""
            enum Test {
                A, B, C;
        
                @Override
                public String toString() {
                    String s;
                    switch (this) {
                        default:
                            s = this.name();
                            break;
                    }
                    switch(this) {
                        default:
                            return s;
                    }
                }
            }
        """,
        """
            enum Test {
                A, B, C;
        
                @Override
                public String toString() {
                    String s;
                    s = this.name();
                    return s;
                }
            }
        """)
    )
}
