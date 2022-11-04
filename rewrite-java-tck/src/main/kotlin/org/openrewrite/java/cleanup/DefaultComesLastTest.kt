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
@file:Suppress("ConstantConditions", "SwitchStatementWithTooFewBranches", "DuplicateBranchesInSwitch")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Tree
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.java.style.DefaultComesLastStyle
import org.openrewrite.style.NamedStyles
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface DefaultComesLastTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(DefaultComesLast())
    }

    @Test
    fun moveDefaultToLastAlongWithItsStatementsAndAddBreakIfNecessary(jp: JavaParser) = rewriteRun(
        java(
            """
            class Test {
                int n;
                {
                    switch (n) {
                        case 1:
                            break;
                        case 2:
                            break;
                        default:
                            System.out.println("default");
                            break;
                        case 3:
                            System.out.println("case3");
                    }
                }
            }
        """,
            """
            class Test {
                int n;
                {
                    switch (n) {
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            System.out.println("case3");
                            break;
                        default:
                            System.out.println("default");
                    }
                }
            }
        """
        )
    )

    @Test
    fun moveDefaultToLastWhenSharedWithAnotherCaseStatement(jp: JavaParser) = rewriteRun(
        java(
            """
            class Test {
                int n;
                {
                    switch (n) {
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                        default:
                            break;
                        case 4:
                        case 5:
                    }
                }
            }
        """,
            """
            class Test {
                int n;
                {
                    switch (n) {
                        case 1:
                            break;
                        case 2:
                            break;
                        case 4:
                        case 5:
                            break;
                        case 3:
                        default:
                    }
                }
            }
        """
        )
    )

    @Test
    fun skipIfLastAndSharedWithCase(jp: JavaParser.Builder<*, *>) = rewriteRun(
        { spec ->
            spec.parser(
                jp.styles(
                    listOf(
                        NamedStyles(
                            Tree.randomId(), "test", "test", "test", emptySet(),
                            listOf(DefaultComesLastStyle(true))
                        )
                    )
                )
            )
        },
        java(
            """
            class Test {
                int n;
                {
                    switch (n) {
                        case 1:
                            break;
                        case 2:
                        default:
                            break;
                        case 3:
                            break;
                    }
                }
            }
        """
        )
    )

    @Test
    fun defaultIsLastAndThrows(jp: JavaParser) = rewriteRun(
        java(
            """
            class Test {
                int n;
                {
                    switch (n) {
                        case 1:
                            break;
                        default:
                            throw new RuntimeException("unexpected value");
                    }
                }
            }
        """
        )
    )

    @Test
    fun defaultIsLastAndReturnsNonVoid(jp: JavaParser) = rewriteRun(
        java(
            """
            class Test {
                public int foo(int n) {
                    switch (n) {
                        case 1:
                            return 1;
                        default:
                            return 2;
                    }
                }
            }
        """
        )
    )

    @Test
    fun dontAddBreaksIfCasesArentMoving(jp: JavaParser) = rewriteRun(
        java(
            """
            class Test {
                int n;
                boolean foo() {
                    switch (n) {
                        case 1:
                        case 2:
                            System.out.println("side effect");
                        default:
                            return true;
                    }
                }
            }
        """
        )
    )

    @Test
    fun dontRemoveExtraneousDefaultCaseBreaks(jp: JavaParser) = rewriteRun(
        java(
            """
            class Test {
                int n;
                void foo() {
                    switch (n) {
                        default:
                            break;
                    }
                }
            }
        """
        )
    )

    @Test
    fun allCasesGroupedWithDefault(jp: JavaParser) = rewriteRun(
        java(
            """
            class Test {
                int n;
                boolean foo() {
                    switch (n) {
                        case 1:
                        case 2:
                        default:
                            return true;
                    }
                }
            }
        """
        )
    )

}
