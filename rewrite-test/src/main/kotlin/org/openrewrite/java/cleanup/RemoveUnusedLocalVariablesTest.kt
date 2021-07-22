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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress(
    "ConstantConditions",
    "StatementWithEmptyBody",
    "EmptyTryBlock",
    "CatchMayIgnoreException",
    "UnusedAssignment",
    "ResultOfMethodCallIgnored"
)
interface RemoveUnusedLocalVariablesTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = RemoveUnusedLocalVariables()

    @Test
    fun removeUnusedLocalVariables() = assertChanged(
        before = """
            class Test {
                static int method(int x) {
                    int a = 0;
                    int b = 0;
                    return a + 1;
                }
            }
        """,
        after = """
            class Test {
                static int method(int x) {
                    int a = 0;
                    return a + 1;
                }
            }
        """
    )

    @Test
    fun removeUnusedLocalVariablesReassignedButNeverUsed() = assertChanged(
        before = """
            class Test {
                static int method() {
                    int isRead = -1;
                    int notRead = 0;
                    notRead = 1;
                    notRead += 1;
                    notRead = isRead + 1;
                    return isRead + 1;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int isRead = -1;
                    return isRead + 1;
                }
            }
        """
    )

    @Test
    fun removeUnusedLocalVariablesFromMultiVariablesDeclaration() = assertChanged(
        before = """
            class Test {
                static int method(int x) {
                    int a = 0, b = 0, c = 0, d = 0;
                    return b + c;
                }
            }
        """,
        after = """
            class Test {
                static int method(int x) {
                    int b = 0, c = 0;
                    return b + c;
                }
            }
        """
    )

    @Test
    fun ignoreLocalVariablesUsedAsMethodInvocationArguments() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    int a = 0;
                    System.out.println(a);
                }
            }
        """
    )

    @Test
    fun ignoreLocalVariablesHavingMethodInvocationsOnThem() = assertUnchanged(
        before = """
            class Test {
                void method() {
                    Worker worker = new Worker();
                    worker.doWork();
                }

                class Worker {
                    void doWork() {
                        //
                    }
                }
            }
        """
    )

    @Test
    fun ignoreUnusedClassFields() = assertUnchanged(
        before = """
            class Test {
                int a = 0;
                int b = 0;

                int method() {
                    return b + 1;
                }
            }
        """
    )

    @Test
    @Disabled
    @Issue("This still causes SonarQube to throw a warning, but there is not much that can be reasonably done in these cases. Maybe change to forEach?")
    fun enhancedForLoops() = assertChanged(
        before = """
            import java.util.List;

            class Test {
                static void method(List<String> list) {
                    for (String s : list) {
                        // do nothing
                    }
                }
            }
        """,
        after = """
            import java.util.List;

            class Test {
                static void method(List<String> list) {
                    list.forEach(s -> {
                        // do nothing
                    });
                }
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/apache/dubbo/blob/747282cdf851c144af562d3f92e10349cc315e36/dubbo-rpc/dubbo-rpc-api/src/main/java/org/apache/dubbo/rpc/RpcStatus.java#L108-L118")
    fun forLoopWithExternalIncrementLogic() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    for (int i; ; ) {
                        i = 41;
                        if (i == 42) {
                            break;
                        }
                    }
                }
            }
        """
    )

    @Test
    fun forLoopIncrementVariableReadInEvaluationCondition() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    for (int j = 0; j < 10; j++) {
                    }
                }
            }
        """
    )

    @Test
    @Disabled
    @Issue("this causes SonarQube to be angry, but there is not much that can be done in these cases. maybe change these to WHILE loops?")
    fun forLoopIncrementVariableNeverRead() = assertChanged(
        before = """
            class Test {
                static boolean isTrue() {
                    return true;
                }

                static void method() {
                    for (int j = 0; isTrue(); j++) {
                    }
                }
            }
        """,
        after = """
            class Test {
                static boolean isTrue() {
                    return true;
                }

                static void method() {
                    while (isTrue()) {
                    }
                }
            }
        """
    )

    @Test
    fun ignoreTryResource() = assertUnchanged(
        before = """
            import java.util.stream.Stream;

            class Test {
                static void method() {
                    try (Stream<Object> unused = Stream.of()) {
                    }
                }
            }
        """
    )

    @Test
    fun ignoreTryCatchException() = assertUnchanged(
        before = """
            class Test {
                static void method() {
                    try {
                    } catch (Exception e) {
                    }
                }
            }
        """
    )

    @Test
    fun ignoreUnusedLambdaExpressionParameters() = assertUnchanged(
        before = """
            import java.util.function.BinaryOperator;
            import java.util.function.UnaryOperator;

            class Test {
                static BinaryOperator<UnaryOperator<Object>> method() {
                    return (a, b) -> input -> {
                        Object o = a.apply(input);
                        o.toString();
                        return o;
                    };
                }
            }
        """
    )

    @Test
    fun ignoreUnusedLambdaExpressionParametersForEach() = assertUnchanged(
        before = """
            import java.util.List;

            class Test {
                static void method(List<Object> list) {
                    list.forEach(item -> {
                        // do nothing with "item"
                    });
                }
            }
        """
    )

}
