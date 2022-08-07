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
package org.openrewrite.java.controlflow

import org.junit.jupiter.api.Test
import org.openrewrite.java.TypeValidation
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName", "UnusedAssignment", "UnnecessaryLocalVariable", "ConstantConditions")
interface ControlFlowTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(ControlFlowVisualization(false))
        spec.expectedCyclesThatMakeChanges(1).cycles(1)
    }

    @Test
    fun `display control flow graph for single basic block`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                void test() {
                    int x = start();
                    x++;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                void test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                }
            }
            """
        )
    )

    @Test
    fun `control flow graph for synchronized block`() = rewriteRun(
        java(
            """
            abstract class Test {
                private final Object lock = new Object();
                abstract int start();
                void test() {
                    int x;
                    synchronized (lock) {
                        x = start();
                        x++;
                    }
                    x--;
                }
            }
            """,
            """
            abstract class Test {
                private final Object lock = new Object();
                abstract int start();
                void test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                    int x;
                    synchronized (lock) {
                        x = start();
                        x++;
                    }
                    x--;
                }
            }
            """
        )
    )

    @Test
    fun `display control flow graph with dual branch`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                void test() {
                    int x = start();
                    x++;
                    if (x == 1) {
                        int y = 3;
                    } else {
                        int y = 5;
                    }
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                void test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (==))~~>*/x == 1) /*~~(2L)~~>*/{
                        int y = 3;
                    } else /*~~(3L)~~>*/{
                        int y = 5;
                    }
                }
            }
            """
        )
    )

    @Test
    fun `display control flow graph with dual branch and statements afterwards`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                void test() {
                    int x = start();
                    x++;
                    if (x == 1) {
                        int y = 3;
                    } else {
                        int y = 5;
                    }
                    x++;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                void test() /*~~(BB: 4 CN: 1 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (==))~~>*/x == 1) /*~~(2L)~~>*/{
                        int y = 3;
                    } else /*~~(3L)~~>*/{
                        int y = 5;
                    }
                    /*~~(4L)~~>*/x++;
                }
            }
            """
        )
    )

    @Test
    fun `display control flow graph with nested branch and statements afterwards`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                void test() {
                    int x = start();
                    x++;
                    if (x == 1) {
                        if (x == 1) {
                            int y = 2;
                        } else {
                            int y = 5;
                        }
                    } else {
                        int y = 5;
                    }
                    x++;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                void test() /*~~(BB: 6 CN: 2 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (==))~~>*/x == 1) /*~~(2L)~~>*/{
                        if (/*~~(2C (==))~~>*/x == 1) /*~~(3L)~~>*/{
                            int y = 2;
                        } else /*~~(4L)~~>*/{
                            int y = 5;
                        }
                    } else /*~~(5L)~~>*/{
                        int y = 5;
                    }
                    /*~~(6L)~~>*/x++;
                }
            }
            """
        )
    )

    @Test
    fun `flow graph with branches with returns`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    if (x == 1) {
                        return 2;
                    } else {
                        return 5;
                    }
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (==))~~>*/x == 1) /*~~(2L)~~>*/{
                        return 2;
                    } else /*~~(3L)~~>*/{
                        return 5;
                    }
                }
            }
            """
        )
    )

    @Test
    fun `flow graph with branches with throws`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    if (x == 1) {
                        throw new RuntimeException();
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (==))~~>*/x == 1) /*~~(2L)~~>*/{
                        throw new RuntimeException();
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `display control flow graph with empty method signature`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    //.. nop
                }
            }
            """,
            """
            abstract class Test {
                void test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                    //.. nop
                }
            }
            """
        )
    )

    @Test
    fun `if statement with return ending basic block`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    if (x == 1) {
                        return 2;
                    }
                    x++;
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (==))~~>*/x == 1) /*~~(2L)~~>*/{
                        return 2;
                    }
                    /*~~(3L)~~>*/x++;
                    return 5;
                }
            }
            """
        )
    )

    @Test
    fun `if statement with && in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    if (x >= 1 && x <= 2) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 4 CN: 2 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (>=))~~>*/x >= 1 && /*~~(2C (<=))~~>*//*~~(2L)~~>*/x <= 2) /*~~(3L)~~>*/{
                        return 2;
                    }
                    return /*~~(4L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `if statement with OR in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    if (x > 5 || x < 3) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 4 CN: 2 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (>))~~>*/x > 5 || /*~~(2C (<))~~>*//*~~(2L)~~>*/x < 3) /*~~(3L)~~>*/{
                        return 2;
                    }
                    return /*~~(4L)~~>*/5;
                }
            }
            """
        )
    )

    @Suppress("ConditionCoveredByFurtherCondition", "ExcessiveRangeCheck")
    @Test
    fun `if statement with multiple && in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    if (x >= 1 && x <= 5 && x == 3) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 5 CN: 3 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C (>=))~~>*/x >= 1 && /*~~(2C (<=))~~>*//*~~(2L)~~>*/x <= 5 && /*~~(3C (==))~~>*//*~~(3L)~~>*/x == 3) /*~~(4L)~~>*/{
                        return 2;
                    }
                    return /*~~(5L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `a standalone boolean expression does not create a new basic block`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    boolean b = x >= 1;
                    if (b) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    boolean b = x >= 1;
                    if (/*~~(1C)~~>*/b) /*~~(2L)~~>*/{
                        return 2;
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Suppress("ConditionCoveredByFurtherCondition")
    @Test
    fun `if statement with && for boolean variable in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                @SuppressWarnings({"ExcessiveRangeCheck", "RedundantSuppression"})
                int test() {
                    int x = start();
                    x++;
                    boolean b = x >= 1 && x <= 5 && x == 3;
                    if (b) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                @SuppressWarnings({"ExcessiveRangeCheck", "RedundantSuppression"})
                int test() /*~~(BB: 6 CN: 3 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    boolean /*~~(2L)~~>*/b = /*~~(1C (>=))~~>*/x >= 1 && /*~~(2C (<=))~~>*//*~~(3L)~~>*/x <= 5 && /*~~(4L)~~>*/x == 3;
                    if (/*~~(3C)~~>*/b) /*~~(5L)~~>*/{
                        return 2;
                    }
                    return /*~~(6L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `if statement with negation for boolean variable in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    boolean b = !(x >= 1);
                    if (b) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    boolean b = !(x >= 1);
                    if (/*~~(1C)~~>*/b) /*~~(2L)~~>*/{
                        return 2;
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `if statement with wrapped parentheses in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    if ((x >= 1 && x <= 5)) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 4 CN: 2 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if ((/*~~(1C (>=))~~>*/x >= 1 && /*~~(2C (<=))~~>*//*~~(2L)~~>*/x <= 5)) /*~~(3L)~~>*/{
                        return 2;
                    }
                    return /*~~(4L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `if method access in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() {
                    int x = start();
                    x++;
                    if (theTest()) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (/*~~(1C)~~>*/theTest()) /*~~(2L)~~>*/{
                        return 2;
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `if statement with negation in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() {
                    int x = start();
                    x++;
                    if (!theTest()) {
                        return 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    int x = start();
                    x++;
                    if (!/*~~(1C)~~>*/theTest()) /*~~(2L)~~>*/{
                        return 2;
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `while loop`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() {
                    int x = start();
                    x++;
                    while (theTest()) {
                        x += 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() /*~~(BB: 3 CN: 1 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    while (/*~~(1C)~~>*/theTest()) /*~~(2L)~~>*/{
                        x += 2;
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `while loop with continue & break`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                int test() {
                    int x = start();
                    x++;
                    while (theTest()) {
                        if (theTest2()) {
                            continue;
                        }
                        if (theTest3()) {
                            break;
                        }
                        x += 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                int test() /*~~(BB: 7 CN: 3 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    while (/*~~(1C)~~>*/theTest()) /*~~(2L)~~>*/{
                        if (/*~~(2C)~~>*/theTest2()) /*~~(3L)~~>*/{
                            continue;
                        }
                        /*~~(4L)~~>*/if (/*~~(3C)~~>*/theTest3()) /*~~(5L)~~>*/{
                            break;
                        }
                        /*~~(6L)~~>*/x += 2;
                    }
                    return /*~~(7L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `do-while loop`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() {
                    int x = start();
                    x++;
                    do {
                        x += 2;
                    } while (theTest());
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() /*~~(BB: 3 CN: 1 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    do /*~~(2L)~~>*/{
                        x += 2;
                    } while (/*~~(1C)~~>*/theTest());
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `do-while loop with continue & break`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                int test() {
                    int x = start();
                    x++;
                    do {
                        if (theTest2())
                            continue;
                        if (theTest3())
                            break;
                        x += 2;
                    } while (theTest());
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                int test() /*~~(BB: 7 CN: 3 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    do /*~~(2L)~~>*/{
                        if (/*~~(2C)~~>*/theTest2())
                            /*~~(3L)~~>*/continue;
                        /*~~(4L)~~>*/if (/*~~(3C)~~>*/theTest3())
                            /*~~(5L)~~>*/break;
                        /*~~(6L)~~>*/x += 2;
                    } while (/*~~(1C)~~>*/theTest());
                    return /*~~(7L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `for i loop`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() {
                    int x = start();
                    x++;
                    for (int i = 0; theTest(); i++) {
                        x += 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() /*~~(BB: 4 CN: 1 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    for (int i = 0; /*~~(1C)~~>*/theTest(); /*~~(2L)~~>*/i++) /*~~(3L)~~>*/{
                        x += 2;
                    }
                    return /*~~(4L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `for i loop with continue and break`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                int test() {
                    int x = start();
                    x++;
                    for (int i = 0; theTest(); i++) {
                        if (theTest2())
                            continue;
                        if (theTest3())
                            break;
                        x += 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                int test() /*~~(BB: 8 CN: 3 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    for (int i = 0; /*~~(1C)~~>*/theTest(); /*~~(2L)~~>*/i++) /*~~(3L)~~>*/{
                        if (/*~~(2C)~~>*/theTest2())
                            /*~~(4L)~~>*/continue;
                        /*~~(5L)~~>*/if (/*~~(3C)~~>*/theTest3())
                            /*~~(6L)~~>*/break;
                        /*~~(7L)~~>*/x += 2;
                    }
                    return /*~~(8L)~~>*/5;
                }
            }
            """
        )
    )

    @Suppress("InfiniteLoopStatement")
    @Test
    fun `for i loop forever`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() {
                    int x = start();
                    x++;
                    for (;;) {
                        x += 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                int test() /*~~(BB: 3 CN: 1 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    for (;;) /*~~(2L)~~>*/{
                        x += 2;
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `for each loop`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract Iterable<Integer> iterable();
                int test() {
                    int x = start();
                    x++;
                    for (Integer i : iterable()) {
                        x += 2;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract Iterable<Integer> iterable();
                int test() /*~~(BB: 3 CN: 1 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    for (Integer i : iterable()) /*~~(2L)~~>*/{
                        x += 2;
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Suppress("UnnecessaryContinue")
    @Test
    fun `for loop nested branching with continue`() = rewriteRun(
        java(
            """
            import java.util.LinkedList;

            class Test {
                public void test () {
                    LinkedList<Integer> l1 = new LinkedList<>();
                    int index = 1;
                    for (int i = 0; i < l1.size(); i++)  {
                        if (i > 5) {
                            if (i * 2 < 50) {
                                index += 1;
                            } else  {
                                continue;
                            }
                        }
                    }
                }
            }
            """,
            """
            import java.util.LinkedList;
            class Test {
                public void test () /*~~(BB: 6 CN: 3 EX: 1 | 1L)~~>*/{
                    LinkedList<Integer> l1 = new LinkedList<>();
                    int index = 1;
                    for (int i = 0; /*~~(1C (<))~~>*/i < l1.size(); /*~~(2L)~~>*/i++)  /*~~(3L)~~>*/{
                        if (/*~~(2C (>))~~>*/i > 5) /*~~(4L)~~>*/{
                            if (/*~~(3C (<))~~>*/i * 2 < 50) /*~~(5L)~~>*/{
                                index += 1;
                            } else  /*~~(6L)~~>*/{
                                continue;
                            }
                        }
                    }
                }
            }
            """
        )
    )

    @Test
    fun `for each loop with continue and break`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                abstract Iterable<Integer> iterable();
                int test() {
                    int x = start();
                    x++;
                    for (Integer i : iterable()) {
                        if (theTest2())
                            continue;
                        if (theTest3())
                            break;
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                abstract boolean theTest();
                abstract boolean theTest2();
                abstract boolean theTest3();
                abstract Iterable<Integer> iterable();
                int test() /*~~(BB: 6 CN: 3 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    for (Integer i : iterable()) /*~~(2L)~~>*/{
                        if (/*~~(1C)~~>*/theTest2())
                            /*~~(3L)~~>*/continue;
                        /*~~(4L)~~>*/if (/*~~(2C)~~>*/theTest3())
                            /*~~(5L)~~>*/break;
                    }
                    return /*~~(6L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `for each loop over new array`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
                int test() {
                    int x = start();
                    x++;
                    for (int i : new int[]{1, 2, 3, 5}) {
                        System.out.println(i);
                    }
                    return 5;
                }
            }
            """,
            """
            abstract class Test {
                abstract int start();
                int test() /*~~(BB: 3 CN: 1 EX: 1 | 1L)~~>*/{
                    int x = start();
                    x++;
                    for (int i : new int[]{1, 2, 3, 5}) /*~~(2L)~~>*/{
                        System.out.println(i);
                    }
                    return /*~~(3L)~~>*/5;
                }
            }
            """
        )
    )

    @Suppress("ConstantConditions", "MismatchedReadAndWriteOfArray")
    @Test
    fun typecast() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();

                void test() {
                    String n = "42";
                    int[] b = new int[1];
                    char c = (char) b[0];
                    if (1 == 1) {
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
                void test() /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    String n = "42";
                    int[] b = new int[1];
                    char c = (char) b[0];
                    if (/*~~(1C (==))~~>*/1 == 1) /*~~(2L)~~>*/{
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    } else /*~~(3L)~~>*/{
                        System.out.println(n);
                    }
                }
            }
            """
        )
    )

    @Test
    fun `throw an exception as an exit condition`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();
                void test() {
                    if (guard()) {
                        throw new RuntimeException();
                    }
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean guard();
                void test() /*~~(BB: 2 CN: 1 EX: 2 | 1L)~~>*/{
                    if (/*~~(1C)~~>*/guard()) /*~~(2L)~~>*/{
                        throw new RuntimeException();
                    }
                }
            }
            """
        )
    )

    @Test
    fun `simple two branch exit condition`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean guard();
                void test() {
                    System.out.println("Hello!");
                    if (guard()) {
                        System.out.println("Goodbye!");
                    }
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean guard();
                void test() /*~~(BB: 2 CN: 1 EX: 2 | 1L)~~>*/{
                    System.out.println("Hello!");
                    if (/*~~(1C)~~>*/guard()) /*~~(2L)~~>*/{
                        System.out.println("Goodbye!");
                    }
                }
            }
            """
        )
    )

    /**
     * TODO: It may be beneficial in the future to represent this as a single basic block with no conditional nodes
     */
    @Test
    fun `literal true`() = rewriteRun(
        java(
            """
            abstract class Test {
                void test() {
                    System.out.println("Hello!");
                    if (true) {
                        System.out.println("Goodbye!");
                    }
                }
            }
            """,
            """
            abstract class Test {
                void test() /*~~(BB: 2 CN: 1 EX: 2 | 1L)~~>*/{
                    System.out.println("Hello!");
                    if (/*~~(1C)~~>*/true) /*~~(2L)~~>*/{
                        System.out.println("Goodbye!");
                    }
                }
            }
            """
        )
    )

    @Test
    fun `control flow for try with resources`() = rewriteRun(
        java(
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() { return null; }
                    void test() {
                        try (InputStream source = source()) {
                            System.out.println(source.read());
                        }
                    }
                }
                """,
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{ return null; }
                    void test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                        try (InputStream source = source()) {
                            System.out.println(source.read());
                        }
                    }
                }
                """
        )
    )

    /**
     * TODO: This is wrong, but we don't have control flow through try-catch modeled currently.
     * This test is just to make sure that we don't blow up when we hit this case.
     */
    @Test
    fun `control flow for try with resources with catch and additional return`() = rewriteRun(
        java(
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() { return null; }
                    int test() {
                        try (InputStream source = source()) {
                            return source.read();
                        } catch (RuntimeException ignored) {

                        }
                        return 0;
                    }
                }
                """,
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{ return null; }
                    int test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                        try (InputStream source = source()) {
                            return source.read();
                        } catch (RuntimeException ignored) {
                        }
                        return 0;
                    }
                }
                """
        )
    )

    @Suppress("TryFinallyCanBeTryWithResources")
    @Test
    fun `control flow for try`() = rewriteRun(
        java(
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() { return null; }
                    void test() {
                        InputStream source = source();
                        try {
                            System.out.println(source.read());
                        } finally {
                            source.close();
                        }
                    }
                }
                """,
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{ return null; }
                    void test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                        InputStream source = source();
                        try {
                            System.out.println(source.read());
                        } finally {
                            source.close();
                        }
                    }
                }
                """
        )
    )

    @Suppress("TryFinallyCanBeTryWithResources")
    @Test
    fun `control flow for try with return`() = rewriteRun(
        java(
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() { return null; }
                    int test() {
                        InputStream source = source();
                        try {
                            return source.read();
                        } finally {
                            source.close();
                        }
                    }
                }
                """,
            """
                import java.io.InputStream;
                class Test {
                    InputStream source() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{ return null; }
                    int test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                        InputStream source = source();
                        try {
                            return source.read();
                        } finally {
                            source.close();
                        }
                    }
                }
                """
        )
    )

    @Suppress("ClassInitializerMayBeStatic")
    @Test
    fun `control flow for init block`() = rewriteRun(
        java(
            """
                class Test {
                    {
                        if (compute()) {
                            System.out.println("Hello!");
                        }
                    }
                    static Boolean compute() {
                        return null;
                    }
                }
                """,
            """
                class Test {
                    /*~~(BB: 2 CN: 1 EX: 2 | 1L)~~>*/{
                        if (/*~~(1C)~~>*/compute()) /*~~(2L)~~>*/{
                            System.out.println("Hello!");
                        }
                    }
                    static Boolean compute() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                        return null;
                    }
                }
                """
        )
    )

    @Test
    fun `control flow for != null`() = rewriteRun(
        java(
            """
                class Test {
                    void test() {
                        if (compute() != null) {
                            System.out.println("Hello!");
                        }
                    }
                    static Object compute() {
                        return null;
                    }
                }
                """,
            """
                class Test {
                    void test() /*~~(BB: 2 CN: 1 EX: 2 | 1L)~~>*/{
                        if (/*~~(1C (!=))~~>*/compute() != null) /*~~(2L)~~>*/{
                            System.out.println("Hello!");
                        }
                    }
                    static Object compute() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                        return null;
                    }
                }
                """
        )
    )

    @Suppress("StringBufferMayBeStringBuilder")
    @Test
    fun `decode url`() = rewriteRun(
        java(
            """
                import java.lang.StringBuffer;
                import java.nio.ByteBuffer;

                class Test {
                    /**
                     * Decodes the specified URL as per RFC 3986, i.e. transforms
                     * percent-encoded octets to characters by decoding with the UTF-8 character
                     * set. This function is primarily intended for usage with
                     * {@link java.net.URL} which unfortunately does not enforce proper URLs. As
                     * such, this method will leniently accept invalid characters or malformed
                     * percent-encoded octets and simply pass them literally through to the
                     * result string. Except for rare edge cases, this will make unencoded URLs
                     * pass through unaltered.
                     *
                     * @param url  The URL to decode, may be <code>null</code>.
                     * @return The decoded URL or <code>null</code> if the input was
                     *         <code>null</code>.
                     */
                    static String test(String url) {
                        String decoded = url;
                        if (url != null && url.indexOf('%') >= 0) {
                            int n = url.length();
                            StringBuffer buffer = new StringBuffer();
                            ByteBuffer bytes = ByteBuffer.allocate(n);
                            for (int i = 0; i < n;) {
                                if (url.charAt(i) == '%') {
                                    try {
                                        do {
                                            byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                                            bytes.put(octet);
                                            i += 3;
                                        } while (i < n && url.charAt(i) == '%');
                                        continue;
                                    } catch (RuntimeException e) {
                                        // malformed percent-encoded octet, fall through and
                                        // append characters literally
                                    } finally {
                                        if (bytes.position() > 0) {
                                            bytes.flip();
                                            buffer.append(utf8Decode(bytes));
                                            bytes.clear();
                                        }
                                    }
                                }
                                buffer.append(url.charAt(i++));
                            }
                            decoded = buffer.toString();
                        }
                        return decoded;
                    }

                    private static String utf8Decode(ByteBuffer buff) {
                        return null;
                    }
                }
            """,
            """
            import java.lang.StringBuffer;
            import java.nio.ByteBuffer;
            class Test {
                /**
                 * Decodes the specified URL as per RFC 3986, i.e. transforms
                 * percent-encoded octets to characters by decoding with the UTF-8 character
                 * set. This function is primarily intended for usage with
                 * {@link java.net.URL} which unfortunately does not enforce proper URLs. As
                 * such, this method will leniently accept invalid characters or malformed
                 * percent-encoded octets and simply pass them literally through to the
                 * result string. Except for rare edge cases, this will make unencoded URLs
                 * pass through unaltered.
                 *
                 * @param url  The URL to decode, may be <code>null</code>.
                 * @return The decoded URL or <code>null</code> if the input was
                 *         <code>null</code>.
                 */
                static String test(String url) /*~~(BB: 12 CN: 7 EX: 1 | 1L)~~>*/{
                    String decoded = url;
                    if (/*~~(1C (!=))~~>*/url != null && /*~~(2C (>=))~~>*//*~~(2L)~~>*/url.indexOf('%') >= 0) /*~~(3L)~~>*/{
                        int n = url.length();
                        StringBuffer buffer = new StringBuffer();
                        ByteBuffer bytes = ByteBuffer.allocate(n);
                        for (int i = 0; /*~~(3C (<))~~>*/i < n;) /*~~(4L)~~>*/{
                            if (/*~~(4C (==))~~>*/url.charAt(i) == '%') /*~~(5L)~~>*/{
                                try {
                                    do /*~~(7L)~~>*/{
                                        byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                                        bytes.put(octet);
                                        i += 3;
                                    } while (/*~~(5C (<))~~>*/i < n && /*~~(6C (==))~~>*//*~~(6L)~~>*/url.charAt(i) == '%');
                                    /*~~(8L)~~>*/continue;
                                } catch (RuntimeException e) {
                                    // malformed percent-encoded octet, fall through and
                                    // append characters literally
                                } finally {
                                    if (/*~~(7C (>))~~>*/bytes.position() > 0) /*~~(9L)~~>*/{
                                        bytes.flip();
                                        buffer.append(utf8Decode(bytes));
                                        bytes.clear();
                                    }
                                }
                            }
                            /*~~(10L)~~>*/buffer.append(url.charAt(i++));
                        }
                        /*~~(11L)~~>*/decoded = buffer.toString();
                    }
                    return /*~~(12L)~~>*/decoded;
                }
                private static String utf8Decode(ByteBuffer buff) /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                    return null;
                }
            }
            """
        )
    )

    @Suppress("UnnecessaryBoxing", "CachedNumberConstructorCall", "deprecation", "KotlinRedundantDiagnosticSuppress")
    @Test
    fun `objects print`() = rewriteRun(
        java(
            """
                class Test {
                    void test() {
                        Integer i = new Integer(1);
                        System.out.println(i);
                    }
                }
            """,
            """
                class Test {
                    void test() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                        Integer i = new Integer(1);
                        System.out.println(i);
                    }
                }
            """
        )
    )

    @Suppress("StatementWithEmptyBody")
    @Test
    fun `identifies control flow with missing type information`() = rewriteRun(
        { spec -> spec.typeValidationOptions(TypeValidation.none()) },
        java(
            """
            class Test {
                void test() {
                    if (potato) {
                        // ...
                    }
                    if ((potato)) {
                        // ...
                    }
                    if (potato && turnip) {
                        // ...
                    }
                    if (potato && turnip || squash) {
                        // ...
                    }
                    int a = 1, b = 2;
                    if ((a = turnip) == b) {
                        // ..
                    }
                    if (horse.equals(donkey)) {
                        // ..
                    }
                    if (horse.contains(hay)) {
                        // ..
                    }
                    boolean farmFresh = tomato;
                    boolean farmFreshAndFancyFree = (chicken);
                    boolean farmFreshEggs = true;
                    farmFreshEggs = chicken.layEggs();
                    while (farming) {
                        // ...
                    }
                    for (int i = 0; areMoreCabbages(); i++) {
                        // ...
                    }
                }
            }
            """,
            """
            class Test {
                void test() /*~~(BB: 22 CN: 12 EX: 1 | 1L)~~>*/{
                    if (/*~~(1C)~~>*/potato) /*~~(2L)~~>*/{
                        // ...
                    }
                    /*~~(3L)~~>*/if ((/*~~(2C)~~>*/potato)) /*~~(4L)~~>*/{
                        // ...
                    }
                    /*~~(5L)~~>*/if (/*~~(3C)~~>*/potato && /*~~(6L)~~>*/turnip) /*~~(7L)~~>*/{
                        // ...
                    }
                    /*~~(8L)~~>*/if (/*~~(5C)~~>*/potato && /*~~(9L)~~>*/turnip || /*~~(10L)~~>*/squash) /*~~(11L)~~>*/{
                        // ...
                    }
                    int a = /*~~(12L)~~>*/1, b = 2;
                    if (/*~~(8C (==))~~>*/(a = turnip) == b) /*~~(13L)~~>*/{
                        // ..
                    }
                    /*~~(14L)~~>*/if (/*~~(9C)~~>*/horse.equals(donkey)) /*~~(15L)~~>*/{
                        // ..
                    }
                    /*~~(16L)~~>*/if (/*~~(10C)~~>*/horse.contains(hay)) /*~~(17L)~~>*/{
                        // ..
                    }
                    boolean farmFresh = /*~~(18L)~~>*/tomato;
                    boolean farmFreshAndFancyFree = (chicken);
                    boolean farmFreshEggs = true;
                    farmFreshEggs = chicken.layEggs();
                    while (/*~~(11C)~~>*/farming) /*~~(19L)~~>*/{
                        // ...
                    }
                    /*~~(20L)~~>*/for (int i = 0; /*~~(12C)~~>*/areMoreCabbages(); /*~~(21L)~~>*/i++) /*~~(22L)~~>*/{
                        // ...
                    }
                }
            }
            """
        )
    )

    @Suppress("IOStreamConstructor")
    @Test
    fun `example code`() = rewriteRun(
        java(
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.IOException;
            import java.io.InputStream;
            import java.util.Enumeration;
            import java.util.zip.ZipEntry;
            import java.util.zip.ZipFile;

            class Test {
                void test(File destination, ZipEntry e, ZipFile zip) {
                    File f = new File(destination, e.getName());
                    if (!f.toPath().startsWith(destination.toPath())) {
                        throw new IOException("Bad Zip Entry!");
                    }
                    IOUtils.copy(
                            zip.getInputStream(e),
                            new FileOutputStream(f)
                    );
                }
            }

            class IOUtils {
                static void copy(Object input, Object output) {
                    //.. nop
                }
            }
            """,
            """
            import java.io.File;
            import java.io.FileOutputStream;
            import java.io.IOException;
            import java.io.InputStream;
            import java.util.Enumeration;
            import java.util.zip.ZipEntry;
            import java.util.zip.ZipFile;
            class Test {
                void test(File destination, ZipEntry e, ZipFile zip) /*~~(BB: 3 CN: 1 EX: 2 | 1L)~~>*/{
                    File f = new File(destination, e.getName());
                    if (!/*~~(1C)~~>*/f.toPath().startsWith(destination.toPath())) /*~~(2L)~~>*/{
                        throw new IOException("Bad Zip Entry!");
                    }
                    /*~~(3L)~~>*/IOUtils.copy(
                            zip.getInputStream(e),
                            new FileOutputStream(f)
                    );
                }
            }
            class IOUtils {
                static void copy(Object input, Object output) /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                    //.. nop
                }
            }
            """
        )
    )

    @Suppress("SimplifyStreamApiCallChains")
    @Test
    fun `lambda foreach` () = rewriteRun(
        java(
            """
            import java.util.LinkedList;
            import java.util.stream.IntStream;
            class Test{
                int test() {
                    LinkedList<Integer> x = new LinkedList<>();
                    x.add(5);
                    x.add(1);
                    IntStream
                        .range(0, x.size())
                        .map( i -> {
                            int y = x.get(i);
                            return y++;
                        })
                        .forEach(ind -> {
                            x.set(ind, x.get(ind) + 5);
                            System.out.println(x.get(ind));
                        });
                    if (x.get(0) == 10) {
                        return -1;
                    }
                    return x.get(0);
                }
            }

            """,
            """
            import java.util.LinkedList;
            import java.util.stream.IntStream;
            class Test{
                int test() /*~~(BB: 7 CN: 1 EX: 2 | 1L)~~>*/{
                    LinkedList<Integer> x = new LinkedList<>();
                    x.add(5);
                    x.add(1);
                    /*~~(2L)~~>*//*~~(3L)~~>*/IntStream
                        .range(0, x.size())
                        .map( i -> /*~~(4L)~~>*/{
                            int y = x.get(i);
                            return y++;
                        })
                        .forEach(ind -> /*~~(5L)~~>*/{
                            x.set(ind, x.get(ind) + 5);
                            System.out.println(x.get(ind));
                        });
                    if (/*~~(1C (==))~~>*/x.get(0) == 10) /*~~(6L)~~>*/{
                        return -1;
                    }
                    return /*~~(7L)~~>*/x.get(0);
                }
            }

            """
        )
    )

    @Suppress("RedundantOperationOnEmptyContainer", "UnnecessaryContinue", "UnnecessaryLabelOnContinueStatement")
    @Test
    fun `byte-buddy minimal replica` () = rewriteRun(
        java(
            """
            import java.util.LinkedList;

            class Test {
                public void test () {
                    LinkedList<Integer> l2 = new LinkedList<>();
                    int index = 1;
                    top:
                    for (Integer j : l2) {
                        for (Integer i : l1)  {
                            if (i > 5) {
                                break;
                            }
                        }
                        if (i * 2 < 50) {
                            index += 1;
                        } else  {
                            continue top;
                        }
                    }
                }
            }
            """,
            """
            import java.util.LinkedList;
            class Test {
                public void test () /*~~(BB: 7 CN: 4 EX: 1 | 1L)~~>*/{
                    LinkedList<Integer> l2 = new LinkedList<>();
                    int index = 1;
                    top:
                    for (Integer j : l2) /*~~(2L)~~>*/{
                        for (Integer i : l1)  /*~~(3L)~~>*/{
                            if (/*~~(1C (>))~~>*/i > 5) /*~~(4L)~~>*/{
                                break;
                            }
                        }
                        /*~~(5L)~~>*/if (/*~~(2C (<))~~>*/i * 2 < 50) /*~~(6L)~~>*/{
                            index += 1;
                        } else  /*~~(7L)~~>*/{
                            continue top;
                        }
                    }
                }
            }
            """
        )
    )

    @Test
    @Suppress("StatementWithEmptyBody")
    fun `while loop with no body one conditional`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean condition();

                void test() {
                    while (condition());
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean condition();

                void test() /*~~(BB: 1 CN: 1 EX: 1 | 1L)~~>*/{
                    while (/*~~(1C)~~>*/condition());
                }
            }
            """
        )
    )

    @Test
    @Suppress("StatementWithEmptyBody")
    fun `while loop with no body two conditionals`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean condition();
                abstract boolean otherCondition();

                void test() {
                    while (condition() && otherCondition());
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean condition();
                abstract boolean otherCondition();

                void test() /*~~(BB: 2 CN: 2 EX: 2 | 1L)~~>*/{
                    while (/*~~(1C)~~>*/condition() && /*~~(2L | 2C)~~>*/otherCondition());
                }
            }
            """
        )
    )

    @Test
    @Suppress("StatementWithEmptyBody")
    fun `while loop with no body and three conditionals`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean condition();
                abstract boolean otherCondition();
                abstract boolean thirdCondition();

                void test() {
                    while (condition() && otherCondition() && thirdCondition());
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean condition();
                abstract boolean otherCondition();
                abstract boolean thirdCondition();
                void test() /*~~(BB: 3 CN: 3 EX: 2 | 1L)~~>*/{
                    while (/*~~(1C)~~>*/condition() && /*~~(2L | 2C)~~>*/otherCondition() && /*~~(3L | 3C)~~>*/thirdCondition());
                }
            }
            """
        )
    )

    @Test
    @Suppress("InfiniteLoopStatement")
    fun `for loop with strange conditional`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract String entry();
                void test() {
                    for (;;) {
                        if (("/" + entry()).endsWith("/pom.xml")) continue;
                        System.out.println("Hello!");
                    }
                }
            }
            """,
            """
            abstract class Test {
                abstract String entry();
                void test() /*~~(BB: 4 CN: 2 EX: 1 | 1L)~~>*/{
                    for (;;) /*~~(2L)~~>*/{
                        if (/*~~(1C)~~>*/("/" + entry()).endsWith("/pom.xml")) /*~~(3L)~~>*/continue;
                        /*~~(4L)~~>*/System.out.println("Hello!");
                    }
                }
            }
            """
        )
    )

    @Test
    fun `example imagej-ui-swing`() = rewriteRun(
        java(
            """
            import java.io.IOException;
            import java.net.URL;
            import java.util.jar.Attributes;
            import java.util.jar.JarEntry;
            import java.util.jar.JarInputStream;
            import java.util.jar.Manifest;

            class Test {
                private String getCommit(final URL jarURL) {
                    try {
                        final JarInputStream in = new JarInputStream(jarURL.openStream());
                        in.close();
                        Manifest manifest = in.getManifest();
                        if (manifest == null)
                            for (;;) {
                                final JarEntry entry = in.getNextJarEntry();
                                if (entry == null) return null;
                                if (entry.getName().equals("META-INF/MANIFEST.MF")) {
                                    manifest = new Manifest(in);
                                    break;
                                }
                            }
                        final Attributes attributes = manifest.getMainAttributes();
                        return attributes.getValue(new Attributes.Name("Implementation-Build"));
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
            """,
            """
            import java.io.IOException;
            import java.net.URL;
            import java.util.jar.Attributes;
            import java.util.jar.JarEntry;
            import java.util.jar.JarInputStream;
            import java.util.jar.Manifest;
            class Test {
                private String getCommit(final URL jarURL) /*~~(BB: 7 CN: 4 EX: 2 | 1L)~~>*/{
                    try {
                        final JarInputStream in = new JarInputStream(jarURL.openStream());
                        in.close();
                        Manifest manifest = in.getManifest();
                        if (/*~~(1C (==))~~>*/manifest == null)
                            /*~~(2L)~~>*/for (;;) /*~~(3L)~~>*/{
                                final JarEntry entry = in.getNextJarEntry();
                                if (/*~~(2C (==))~~>*/entry == null) return /*~~(4L)~~>*/null;
                                /*~~(5L)~~>*/if (/*~~(3C)~~>*/entry.getName().equals("META-INF/MANIFEST.MF")) /*~~(6L)~~>*/{
                                    manifest = new Manifest(in);
                                    break;
                                }
                            }
                        final /*~~(7L)~~>*/Attributes attributes = manifest.getMainAttributes();
                        return attributes.getValue(new Attributes.Name("Implementation-Build"));
                    } catch (IOException e) {
                        return null;
                    }
                }
            }
            """
        )
    )

    @Test
    fun `a class declared inside a function`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean conditional();

                void testDeclareAClass() {
                    if (conditional()) {
                        class A implements ExampleInterface {

                            @Override
                            public int doSomething() {
                                System.out.println("Hello");
                                return 1;
                            }

                            @Override
                            public int doSomethingElse() {
                                System.out.println("Hello");
                                return 2;
                            }

                            @Override
                            public int doAThirdThing() {
                                System.out.println("I don't know why you say goodbye, I say Hello!");
                                return 3;
                            }
                        }
                        new A().doAThirdThing();
                    }
                }

                interface ExampleInterface {
                    int doSomething();
                    int doSomethingElse();
                    int doAThirdThing();
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean conditional();
                void testDeclareAClass() {
                    if (conditional()) {
                        class A implements ExampleInterface {
                            @Override
                            public int doSomething() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                                System.out.println("Hello");
                                return 1;
                            }
                            @Override
                            public int doSomethingElse() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                                System.out.println("Hello");
                                return 2;
                            }
                            @Override
                            public int doAThirdThing() /*~~(BB: 1 CN: 0 EX: 1 | 1L)~~>*/{
                                System.out.println("I don't know why you say goodbye, I say Hello!");
                                return 3;
                            }
                        }
                        new A().doAThirdThing();
                    }
                }
                interface ExampleInterface {
                    int doSomething();
                    int doSomethingElse();
                    int doAThirdThing();
                }
            }
            """
        )
    )

    @Test
    @Suppress("InfiniteLoopStatement")
    fun `for loop with continue then another conditional`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract boolean conditional();
                abstract String entry();
                void test() {
                    for (;;) {
                        if (("/" + entry()).endsWith("/pom.xml")) continue;
                        if (conditional()) {
                            System.out.println("Hello!");
                        }
                    }
                }
            }
            """,
            """
            abstract class Test {
                abstract boolean conditional();
                abstract String entry();
                void test() /*~~(BB: 5 CN: 3 EX: 1 | 1L)~~>*/{
                    for (;;) /*~~(2L)~~>*/{
                        if (/*~~(1C)~~>*/("/" + entry()).endsWith("/pom.xml")) /*~~(3L)~~>*/continue;
                        /*~~(4L)~~>*/if (/*~~(2C)~~>*/conditional()) /*~~(5L)~~>*/{
                            System.out.println("Hello!");
                        }
                    }
                }
            }
            """
        )
    )
}
