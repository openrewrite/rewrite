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
import org.openrewrite.ExecutionContext
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.internal.BlockUtil
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.marker.SearchResult
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface ControlFlowTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {

                override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                    val methodDeclaration = cursor.firstEnclosing(J.MethodDeclaration::class.java)
                    val isTestMethod = methodDeclaration?.body == block && methodDeclaration.name.simpleName == "test"
                    val isStaticOrInitBlock = BlockUtil.isStaticOrInitBlock(cursor)
                    if (isTestMethod || isStaticOrInitBlock) {
                        return ControlFlow.startingAt(cursor).findControlFlow().map { controlFlow ->
                            val basicBlocks = controlFlow.basicBlocks
                            val leaders = basicBlocks.map { it.leader }.toSet()
                            doAfterVisit(object : JavaIsoVisitor<ExecutionContext>() {

                                override fun visitStatement(statement: Statement, p: ExecutionContext): Statement {
                                    return if (leaders.contains(statement)) {
                                        val searchResult =
                                            statement.markers.markers.filterIsInstance<SearchResult>().getOrNull(0)
                                        if (searchResult != null) {
                                            statement.withMarkers(
                                                statement.markers.removeByType(SearchResult::class.java).add(
                                                    searchResult.withDescription(searchResult.description?.plus(" | L"))
                                                )
                                            )
                                        } else {
                                            statement.withMarkers(statement.markers.searchResult("L"))
                                        }
                                    } else statement
                                }

                                override fun visitExpression(expression: Expression, p: ExecutionContext): Expression {
                                    return if (leaders.contains(expression))
                                        expression.withMarkers(expression.markers.searchResult(expression.leaderDescription()))
                                    else expression
                                }

                                fun Expression.leaderDescription(): String {
                                    return when (this) {
                                        is J.Binary -> {
                                            val tag = when (this.operator) {
                                                J.Binary.Type.And -> "&&"
                                                J.Binary.Type.Or -> "||"
                                                J.Binary.Type.Addition -> "+"
                                                J.Binary.Type.Subtraction -> "-"
                                                J.Binary.Type.Multiplication -> "*"
                                                J.Binary.Type.Division -> "/"
                                                J.Binary.Type.Modulo -> "%"
                                                J.Binary.Type.LessThan -> "<"
                                                J.Binary.Type.LessThanOrEqual -> "<="
                                                J.Binary.Type.GreaterThan -> ">"
                                                J.Binary.Type.GreaterThanOrEqual -> ">="
                                                J.Binary.Type.Equal -> "=="
                                                J.Binary.Type.NotEqual -> "!="
                                                J.Binary.Type.BitAnd -> "&"
                                                J.Binary.Type.BitOr -> "|"
                                                J.Binary.Type.BitXor -> "^"
                                                J.Binary.Type.LeftShift -> "<<"
                                                J.Binary.Type.RightShift -> ">>"
                                                J.Binary.Type.UnsignedRightShift -> ">>>"
                                                null -> "null"
                                            }
                                            "L ($tag)"
                                        }
                                        else -> "L"
                                    }
                                }
                            })
                            block.withMarkers(
                                block.markers.searchResult(
                                    "BB: ${basicBlocks.size} CN: ${controlFlow.conditionNodeCount} EX: ${controlFlow.exitCount}"
                                )
                            )
                        }.orElseGet { super.visitBlock(block, p) }
                    }
                    return super.visitBlock(block, p)
                }
            }
        })
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
                void test() /*~~(BB: 1 CN: 0 EX: 1 | L)~~>*/{
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
                void test() /*~~(BB: 1 CN: 0 EX: 1 | L)~~>*/{
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
                void test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x == 1) /*~~(L)~~>*/{
                        int y = 3;
                    } else /*~~(L)~~>*/{
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
                void test() /*~~(BB: 4 CN: 1 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x == 1) /*~~(L)~~>*/{
                        int y = 3;
                    } else /*~~(L)~~>*/{
                        int y = 5;
                    }
                    /*~~(L)~~>*/x++;
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
                void test() /*~~(BB: 6 CN: 2 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x == 1) /*~~(L)~~>*/{
                        if (x == 1) /*~~(L)~~>*/{
                            int y = 2;
                        } else /*~~(L)~~>*/{
                            int y = 5;
                        }
                    } else /*~~(L)~~>*/{
                        int y = 5;
                    }
                    /*~~(L)~~>*/x++;
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
                int test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x == 1) /*~~(L)~~>*/{
                        return 2;
                    } else /*~~(L)~~>*/{
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
                int test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x == 1) /*~~(L)~~>*/{
                        throw new RuntimeException();
                    }
                    return /*~~(L)~~>*/5;
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
                void test() /*~~(BB: 1 CN: 0 EX: 1 | L)~~>*/{
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
                int test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x == 1) /*~~(L)~~>*/{
                        return 2;
                    }
                    /*~~(L)~~>*/x++;
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
                int test() /*~~(BB: 4 CN: 2 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x >= 1 && /*~~(L)~~>*/x <= 2) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `if statement with || in control`() = rewriteRun(
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
                int test() /*~~(BB: 4 CN: 2 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x > 5 || /*~~(L)~~>*/x < 3) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
                }
            }
            """
        )
    )

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
                int test() /*~~(BB: 5 CN: 3 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (x >= 1 && /*~~(L)~~>*/x <= 5 && /*~~(L)~~>*/x == 3) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    boolean b = x >= 1;
                    if (b) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
                }
            }
            """
        )
    )

    @Test
    fun `if statement with && for boolean variable in control`() = rewriteRun(
        java(
            """
            abstract class Test {
                abstract int start();
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
                int test() /*~~(BB: 6 CN: 3 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    /*~~(L)~~>*/boolean b = x >= 1 && /*~~(L)~~>*/x <= 5 && /*~~(L)~~>*/x == 3;
                    if (b) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    boolean b = !(x >= 1);
                    if (b) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 4 CN: 2 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if ((x >= 1 && /*~~(L)~~>*/x <= 5)) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (theTest()) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    if (!theTest()) /*~~(L)~~>*/{
                        return 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 3 CN: 1 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    while (theTest()) /*~~(L)~~>*/{
                        x += 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 7 CN: 3 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    while (theTest()) /*~~(L)~~>*/{
                        if (theTest2()) /*~~(L)~~>*/{
                            continue;
                        }
                        /*~~(L)~~>*/if (theTest3()) /*~~(L)~~>*/{
                            break;
                        }
                        /*~~(L)~~>*/x += 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 3 CN: 1 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    do /*~~(L)~~>*/{
                        x += 2;
                    } while (theTest());
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 7 CN: 3 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    do /*~~(L)~~>*/{
                        if (theTest2())
                            /*~~(L)~~>*/continue;
                        /*~~(L)~~>*/if (theTest3())
                            /*~~(L)~~>*/break;
                        /*~~(L)~~>*/x += 2;
                    } while (theTest());
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 4 CN: 1 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    for (int i = 0; theTest(); /*~~(L)~~>*/i++) /*~~(L)~~>*/{
                        x += 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 8 CN: 3 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    for (int i = 0; theTest(); /*~~(L)~~>*/i++) /*~~(L)~~>*/{
                        if (theTest2())
                            /*~~(L)~~>*/continue;
                        /*~~(L)~~>*/if (theTest3())
                            /*~~(L)~~>*/break;
                        /*~~(L)~~>*/x += 2;
                    }
                    return /*~~(L)~~>*/5;
                }
            }
            """
        )
    )

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
                int test() /*~~(BB: 3 CN: 1 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    for (;;) /*~~(L)~~>*/{
                        x += 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 3 CN: 1 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    for (Integer i : iterable()) /*~~(L)~~>*/{
                        x += 2;
                    }
                    return /*~~(L)~~>*/5;
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
                int test() /*~~(BB: 6 CN: 3 EX: 1 | L)~~>*/{
                    int x = start();
                    x++;
                    for (Integer i : iterable()) /*~~(L)~~>*/{
                        if (theTest2())
                            /*~~(L)~~>*/continue;
                        /*~~(L)~~>*/if (theTest3())
                            /*~~(L)~~>*/break;
                    }
                    return /*~~(L)~~>*/5;
                }
            }
            """
        )
    )



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

                void test() /*~~(BB: 3 CN: 1 EX: 2 | L)~~>*/{
                    String n = "42";
                    int[] b = new int[1];
                    char c = (char) b[0];
                    if (1 == 1) /*~~(L)~~>*/{
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    } else /*~~(L)~~>*/{
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
                void test() /*~~(BB: 2 CN: 1 EX: 2 | L)~~>*/{
                    if (guard()) /*~~(L)~~>*/{
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
                void test() /*~~(BB: 2 CN: 1 EX: 2 | L)~~>*/{
                    System.out.println("Hello!");
                    if (guard()) /*~~(L)~~>*/{
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
                void test() /*~~(BB: 2 CN: 1 EX: 2 | L)~~>*/{
                    System.out.println("Hello!");
                    if (true) /*~~(L)~~>*/{
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
                    InputStream source() { return null; }
                    void test() /*~~(BB: 1 CN: 0 EX: 1 | L)~~>*/{
                        try (InputStream source = source()) {
                            System.out.println(source.read());
                        }
                    }
                }
                """
        )
    )

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
                    InputStream source() { return null; }
                    void test() /*~~(BB: 1 CN: 0 EX: 1 | L)~~>*/{
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
                    /*~~(BB: 2 CN: 1 EX: 2 | L)~~>*/{
                        if (compute()) /*~~(L)~~>*/{
                            System.out.println("Hello!");
                        }
                    }
                    static Boolean compute() {
                        return null;
                    }
                }
                """
        )
    )
}
