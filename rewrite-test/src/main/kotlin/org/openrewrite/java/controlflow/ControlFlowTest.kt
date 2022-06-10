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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.JavaIsoVisitor
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
                    if (methodDeclaration?.body == block && methodDeclaration.name.simpleName == "test") {
                        val controlFlow = ControlFlow.startingAt(cursor).findControlFlow()
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
                                    expression.withMarkers(expression.markers.searchResult("L"))
                                else expression
                            }
                        })
                        return block.withMarkers(
                            block.markers.searchResult(
                                "BB: ${basicBlocks.size} CN: ${controlFlow.conditionNodeCount} EX: ${controlFlow.exitCount}"
                            )
                        )
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
    fun `display control flow graph with branches with returns`() = rewriteRun(
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
                    /*~~(L)~~>*/return 5;
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
                    /*~~(L)~~>*/return 5;
                }
            }
            """
        )
    )

    @Test
    @Disabled("TODO: fix this test")
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
                int test() /*~~(BB: 6 CN: 4 EX: 2 | L)~~>*/{
                    int x = start();
                    x++;
                    boolean b = x >= 1 && /*~~(L)~~>*/x <= 5;
                    if (b) /*~~(L)~~>*/{
                        return 2;
                    }
                    /*~~(L)~~>*/return 5;
                }
            }
            """
        )
    )
}
