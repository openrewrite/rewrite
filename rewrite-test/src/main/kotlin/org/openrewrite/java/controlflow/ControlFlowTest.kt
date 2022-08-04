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
import org.openrewrite.java.TypeValidation
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Statement
import org.openrewrite.marker.SearchResult
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName", "UnusedAssignment", "UnnecessaryLocalVariable", "ConstantConditions")
interface ControlFlowTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {


                @Suppress("unused")
                fun getPredecessors(
                    leadersToNodes: Map<J, ControlFlowNode>,
                    blockNumbers: Map<ControlFlowNode, Int>,
                    leader: J
                ): String {
                    if (leader is J.ControlParentheses<*>) {
                        val block = leadersToNodes[leader.tree] ?: error("No block for $leader")
                        val predecessors = block.predecessors.map { blockNumbers[it] }
                        return "Predecessors: ${predecessors.joinToString(", ")}"
                    }

                    val block = leadersToNodes[leader] ?: error("No block for $leader")
                    val predecessors = block.predecessors.map { blockNumbers[it] }
                    return "Predecessors: ${predecessors.joinToString(", ")}"

                }

                override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
                    val methodDeclaration = cursor.firstEnclosing(J.MethodDeclaration::class.java)
                    val isTestMethod = methodDeclaration?.body == block && methodDeclaration.name.simpleName == "test"
                    val isStaticOrInitBlock = J.Block.isStaticOrInitBlock(cursor)
                    if (isTestMethod || isStaticOrInitBlock) {
                        return ControlFlow.startingAt(cursor).findControlFlow().map { controlFlow ->
                            // maps basic block and condition nodes to the first statement in the node (the node leader)
                            val leadersToBlocks = controlFlow.basicBlocks.map { block ->
                                block.leader
                            }.zip(controlFlow.basicBlocks).toMap()
                            val conditionToConditionNodes = controlFlow.conditionNodes.map { node ->
                                node.condition
                            }.zip(controlFlow.conditionNodes).toMap()
                            val leadersToNodes = leadersToBlocks + conditionToConditionNodes

                            // get the key set of leadersToBlocks, which is the set of leaders
                            val leaders = leadersToNodes.keys

                            var nodeNumber = 0
                            val nodeNumbers = mutableMapOf<ControlFlowNode, Int>()
                            doAfterVisit(object : JavaIsoVisitor<ExecutionContext>() {

                                override fun visitStatement(statement: Statement, p: ExecutionContext): Statement {
                                    return if (leaders.contains(statement)) {
                                        val searchResult =
                                            statement.markers.markers.filterIsInstance<SearchResult>().getOrNull(0)
                                        if (searchResult != null) {
                                            // get the block from the leader
                                            val b = leadersToNodes[statement] ?: error("No block for $statement")
                                            val number = nodeNumbers.computeIfAbsent(b) { ++nodeNumber }
                                            statement.withMarkers(
                                                statement.markers.removeByType(SearchResult::class.java).add(
                                                    searchResult.withDescription(searchResult.description?.plus(" | " + number + "L"))
                                                )
                                            )
                                        } else {
                                            val b = leadersToNodes[statement] ?: error("No block for $statement")
                                            val number = nodeNumbers.computeIfAbsent(b) { ++nodeNumber }
                                            statement.withMarkers(statement.markers.searchResult("" + number + "L"))
                                        }
                                    } else statement
                                }

                                override fun visitExpression(expression: Expression, p: ExecutionContext): Expression {
                                    return if (leaders.contains(expression)) {
                                        val b = leadersToNodes[expression] ?: error("No block for $expression")
                                        val number = nodeNumbers.computeIfAbsent(b) { ++nodeNumber }
                                        expression.withMarkers(expression.markers.searchResult("" + number + expression.leaderDescription()))
                                    } else expression
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
//                            ControlFlowVisualizer.showCFG(controlFlow)

                            block.withMarkers(
                                block.markers.searchResult(
                                    "BB: ${controlFlow.basicBlocks.size} CN: ${controlFlow.conditionNodeCount} EX: ${controlFlow.exitCount}"
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
                    if (/*~~(2L (==))~~>*/x == 1) /*~~(3L)~~>*/{
                        int y = 3;
                    } else /*~~(4L)~~>*/{
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
                    if (/*~~(2L (==))~~>*/x == 1) /*~~(3L)~~>*/{
                        int y = 3;
                    } else /*~~(4L)~~>*/{
                        int y = 5;
                    }
                    /*~~(5L)~~>*/x++;
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
                    if (/*~~(2L (==))~~>*/x == 1) /*~~(3L)~~>*/{
                        if (/*~~(4L (==))~~>*/x == 1) /*~~(5L)~~>*/{
                            int y = 2;
                        } else /*~~(6L)~~>*/{
                            int y = 5;
                        }
                    } else /*~~(7L)~~>*/{
                        int y = 5;
                    }
                    /*~~(8L)~~>*/x++;
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
                    if (/*~~(2L (==))~~>*/x == 1) /*~~(3L)~~>*/{
                        return 2;
                    } else /*~~(4L)~~>*/{
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
                    if (/*~~(2L (==))~~>*/x == 1) /*~~(3L)~~>*/{
                        throw new RuntimeException();
                    }
                    return /*~~(4L)~~>*/5;
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
                    if (/*~~(2L (==))~~>*/x == 1) /*~~(3L)~~>*/{
                        return 2;
                    }
                    /*~~(4L)~~>*/x++;
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
                    if (/*~~(2L (>=))~~>*/x >= 1 && /*~~(3L (<=))~~>*//*~~(4L)~~>*/x <= 2) /*~~(5L)~~>*/{
                        return 2;
                    }
                    return /*~~(6L)~~>*/5;
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
                    if (/*~~(2L (>))~~>*/x > 5 || /*~~(3L (<))~~>*//*~~(4L)~~>*/x < 3) /*~~(5L)~~>*/{
                        return 2;
                    }
                    return /*~~(6L)~~>*/5;
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
                    if (/*~~(2L (>=))~~>*/x >= 1 && /*~~(3L (<=))~~>*//*~~(4L)~~>*/x <= 5 && /*~~(5L (==))~~>*//*~~(6L)~~>*/x == 3) /*~~(7L)~~>*/{
                        return 2;
                    }
                    return /*~~(8L)~~>*/5;
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
                    if (/*~~(2L)~~>*/b) /*~~(3L)~~>*/{
                        return 2;
                    }
                    return /*~~(4L)~~>*/5;
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
                    boolean /*~~(2L)~~>*/b = /*~~(3L (>=))~~>*/x >= 1 && /*~~(4L (<=))~~>*//*~~(5L)~~>*/x <= 5 && /*~~(6L)~~>*/x == 3;
                    if (/*~~(7L)~~>*/b) /*~~(8L)~~>*/{
                        return 2;
                    }
                    return /*~~(9L)~~>*/5;
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
                    if (/*~~(2L)~~>*/b) /*~~(3L)~~>*/{
                        return 2;
                    }
                    return /*~~(4L)~~>*/5;
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
                    if ((/*~~(2L (>=))~~>*/x >= 1 && /*~~(3L (<=))~~>*//*~~(4L)~~>*/x <= 5)) /*~~(5L)~~>*/{
                        return 2;
                    }
                    return /*~~(6L)~~>*/5;
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
                    if (/*~~(2L)~~>*/theTest()) /*~~(3L)~~>*/{
                        return 2;
                    }
                    return /*~~(4L)~~>*/5;
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
                    if (!/*~~(2L)~~>*/theTest()) /*~~(3L)~~>*/{
                        return 2;
                    }
                    return /*~~(4L)~~>*/5;
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
                    while (/*~~(2L)~~>*/theTest()) /*~~(3L)~~>*/{
                        x += 2;
                    }
                    return /*~~(4L)~~>*/5;
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
                    while (/*~~(2L)~~>*/theTest()) /*~~(3L)~~>*/{
                        if (/*~~(4L)~~>*/theTest2()) /*~~(5L)~~>*/{
                            continue;
                        }
                        /*~~(6L)~~>*/if (/*~~(7L)~~>*/theTest3()) /*~~(8L)~~>*/{
                            break;
                        }
                        /*~~(9L)~~>*/x += 2;
                    }
                    return /*~~(10L)~~>*/5;
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
                    do /*~~(3L)~~>*/{
                        x += 2;
                    } while (/*~~(2L)~~>*/theTest());
                    return /*~~(4L)~~>*/5;
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
                    do /*~~(3L)~~>*/{
                        if (/*~~(4L)~~>*/theTest2())
                            /*~~(5L)~~>*/continue;
                        /*~~(6L)~~>*/if (/*~~(7L)~~>*/theTest3())
                            /*~~(8L)~~>*/break;
                        /*~~(9L)~~>*/x += 2;
                    } while (/*~~(2L)~~>*/theTest());
                    return /*~~(10L)~~>*/5;
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
                    for (int i = 0; /*~~(2L)~~>*/theTest(); /*~~(3L)~~>*/i++) /*~~(4L)~~>*/{
                        x += 2;
                    }
                    return /*~~(5L)~~>*/5;
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
                    for (int i = 0; /*~~(2L)~~>*/theTest(); /*~~(3L)~~>*/i++) /*~~(4L)~~>*/{
                        if (/*~~(5L)~~>*/theTest2())
                            /*~~(6L)~~>*/continue;
                        /*~~(7L)~~>*/if (/*~~(8L)~~>*/theTest3())
                            /*~~(9L)~~>*/break;
                        /*~~(10L)~~>*/x += 2;
                    }
                    return /*~~(11L)~~>*/5;
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
                    /*~~(2L)~~>*/for (;;) /*~~(3L)~~>*/{
                        x += 2;
                    }
                    return /*~~(4L)~~>*/5;
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
                    for (int i = 0; /*~~(2L (<))~~>*/i < l1.size(); /*~~(3L)~~>*/i++)  /*~~(4L)~~>*/{
                        if (/*~~(5L (>))~~>*/i > 5) /*~~(6L)~~>*/{
                            if (/*~~(7L (<))~~>*/i * 2 < 50) /*~~(8L)~~>*/{
                                index += 1;
                            } else  /*~~(9L)~~>*/{
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
                public void test () /*~~(BB: 6 CN: 3 EX: 1 | L)~~>*/{
                    LinkedList<Integer> l1 = new LinkedList<>();
                    int index = 1;
                    for (int i = 0; i < l1.size(); /*~~(L)~~>*/i++)  /*~~(L)~~>*/{
                        if (i > 5) /*~~(L)~~>*/{
                            if (i * 2 < 50) /*~~(L)~~>*/{
                                index += 1;
                            } else  /*~~(L)~~>*/{
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
                        if (/*~~(3L)~~>*/theTest2())
                            /*~~(4L)~~>*/continue;
                        /*~~(5L)~~>*/if (/*~~(6L)~~>*/theTest3())
                            /*~~(7L)~~>*/break;
                    }
                    return /*~~(8L)~~>*/5;
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
                    if (/*~~(2L (==))~~>*/1 == 1) /*~~(3L)~~>*/{
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    } else /*~~(4L)~~>*/{
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
                    if (/*~~(2L)~~>*/guard()) /*~~(3L)~~>*/{
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
                    if (/*~~(2L)~~>*/guard()) /*~~(3L)~~>*/{
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
                    if (/*~~(2L)~~>*/true) /*~~(3L)~~>*/{
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
                    InputStream source() { return null; }
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
                    InputStream source() { return null; }
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
                    InputStream source() { return null; }
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
                        if (/*~~(2L)~~>*/compute()) /*~~(3L)~~>*/{
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
                        if (/*~~(2L (!=))~~>*/compute() != null) /*~~(3L)~~>*/{
                            System.out.println("Hello!");
                        }
                    }
                    static Object compute() {
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
                    if (/*~~(2L (!=))~~>*/url != null && /*~~(3L (>=))~~>*//*~~(4L)~~>*/url.indexOf('%') >= 0) /*~~(5L)~~>*/{
                        int n = url.length();
                        StringBuffer buffer = new StringBuffer();
                        ByteBuffer bytes = ByteBuffer.allocate(n);
                        for (int i = 0; /*~~(6L (<))~~>*/i < n;) /*~~(7L)~~>*/{
                            if (/*~~(8L (==))~~>*/url.charAt(i) == '%') /*~~(9L)~~>*/{
                                try {
                                    do /*~~(13L)~~>*/{
                                        byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                                        bytes.put(octet);
                                        i += 3;
                                    } while (/*~~(10L (<))~~>*/i < n && /*~~(11L (==))~~>*//*~~(12L)~~>*/url.charAt(i) == '%');
                                    /*~~(14L)~~>*/continue;
                                } catch (RuntimeException e) {
                                    // malformed percent-encoded octet, fall through and
                                    // append characters literally
                                } finally {
                                    if (/*~~(15L (>))~~>*/bytes.position() > 0) /*~~(16L)~~>*/{
                                        bytes.flip();
                                        buffer.append(utf8Decode(bytes));
                                        bytes.clear();
                                    }
                                }
                            }
                            /*~~(17L)~~>*/buffer.append(url.charAt(i++));
                        }
                        /*~~(18L)~~>*/decoded = buffer.toString();
                    }
                    return /*~~(19L)~~>*/decoded;
                }
                private static String utf8Decode(ByteBuffer buff) {
                    return null;
                }
            }
            """
        )
    )

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
                void test() /*~~(BB: 22 CN: 12 EX: 1 | L)~~>*/{
                    if (potato) /*~~(L)~~>*/{
                        // ...
                    }
                    /*~~(L)~~>*/if ((potato)) /*~~(L)~~>*/{
                        // ...
                    }
                    /*~~(L)~~>*/if (potato && /*~~(L)~~>*/turnip) /*~~(L)~~>*/{
                        // ...
                    }
                    /*~~(L)~~>*/if (potato && /*~~(L)~~>*/turnip || /*~~(L)~~>*/squash) /*~~(L)~~>*/{
                        // ...
                    }
                    int a = /*~~(L)~~>*/1, b = 2;
                    if ((a = turnip) == b) /*~~(L)~~>*/{
                        // ..
                    }
                    /*~~(L)~~>*/if (horse.equals(donkey)) /*~~(L)~~>*/{
                        // ..
                    }
                    /*~~(L)~~>*/if (horse.contains(hay)) /*~~(L)~~>*/{
                        // ..
                    }
                    boolean farmFresh = /*~~(L)~~>*/tomato;
                    boolean farmFreshAndFancyFree = (chicken);
                    boolean farmFreshEggs = true;
                    farmFreshEggs = chicken.layEggs();
                    while (farming) /*~~(L)~~>*/{
                        // ...
                    }
                    /*~~(L)~~>*/for (int i = 0; areMoreCabbages(); /*~~(L)~~>*/i++) /*~~(L)~~>*/{
                        // ...
                    }
                }
            }
            """.trimIndent()
        )
    )

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
                    if (!/*~~(2L)~~>*/f.toPath().startsWith(destination.toPath())) /*~~(3L)~~>*/{
                        throw new IOException("Bad Zip Entry!");
                    }
                    /*~~(4L)~~>*/IOUtils.copy(
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
            """
        )
    )
}
