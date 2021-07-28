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
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress(
    "StatementWithEmptyBody",
    "ConstantConditions",
    "InfiniteLoopStatement",
    "AssignmentToForLoopParameter",
    "EmptyClassInitializer",
    "AccessStaticViaInstance",
    "ClassInitializerMayBeStatic",
    "ParameterCanBeLocal",
    "PointlessArithmeticExpression",
    "EmptyTryBlock",
    "AssignmentReplaceableWithOperatorAssignment",
    "AnonymousInnerClassMayBeStatic"
)
interface RemoveUnusedAssignmentsTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = RemoveUnusedAssignments()

    @Test
    fun localVariableUnusedAssignment() = assertChanged(
        before = """
            class Test {
                static int method() {
                    int a;
                    a = 0;
                    a = 99;
                    return a;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int a;
                    a = 99;
                    return a;
                }
            }
        """
    )

    @Test
    fun localVariableReadAfterReassignment() = assertUnchanged(
        before = """
            class Test {
                static int method() {
                    int a;
                    a = 0;
                    int b = a;
                    a = 99;
                    return a;
                }
            }
        """
    )

    @Test
    fun localVariableUnusedAfterReassignment() = assertChanged(
        before = """
            class Test {
                static int method() {
                    int a;
                    a = 0;
                    int b = a;
                    a = 99;
                    return b;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int a;
                    a = 0;
                    int b = a;
                    return b;
                }
            }
        """
    )

    @Test
    fun multipleLocalVariableUnusedAfterReassignment() = assertChanged(
        before = """
            class Test {
                static int method() {
                    int a;
                    a = 0;
                    int b = a;
                    b = 1;
                    b = 2;
                    b = 3;
                    a = 99;
                    System.out.println(b);
                    b = 2;
                    return b;
                }
            }
        """,
        after = """
            class Test {
                static int method() {
                    int a;
                    a = 0;
                    int b = a;
                    b = 3;
                    System.out.println(b);
                    b = 2;
                    return b;
                }
            }
        """
    )

    @Test
    fun localVariableReadAfterReassignmentMethodInvocation() = assertChanged(
        before = """
            class Test {
                static void method() {
                    Object a;
                    a = new Object();
                    System.out.println(a);
                    a = null;
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    Object a;
                    a = new Object();
                    System.out.println(a);
                }
            }
        """
    )

    @Test
    fun localVariableNameScopeBetweenMethods() = assertChanged(
        before = """
            class Test {
                static void method0() {
                    int a = 1;
                    a = 2;
                }

                static void method1() {
                    int a = 3;
                    a = 4;
                    System.out.println(a);
                }
            }
        """,
        after = """
            class Test {
                static void method0() {
                    int a = 1;
                }

                static void method1() {
                    int a = 3;
                    a = 4;
                    System.out.println(a);
                }
            }
        """
    )

    @Test
    @Disabled
    fun localVariableAssignmentPathsWithinIfStatements() = assertUnchanged(
        before = """
            class Test {
                static String method(int a) {
                    String str;
                    if (a == 0) {
                        str = "zero";
                    } else {
                        str = "not zero";
                    }
                    return str;
                }
            }
        """
    )

    @Test
    @Disabled
    fun localVariableIdentifierEnclosedInParentheses() = assertChanged(
        before = """
            class Test {
                static void method() {
                    int i = 0;
                    System.out.println(i);
                    (i) = 99;
                }
            }
        """,
        after = """
            class Test {
                static void method() {
                    int i = 0;
                    System.out.println(i);
                }
            }
        """
    )

    @Test
    fun localVariableReadInAssertStatement() = assertUnchanged(
        before = """
            class Test {
                static void method(boolean x) {
                    boolean y = !x;
                    assert y;
                }
            }
        """
    )

    @Test
    fun localVariableSelfAssignmentOperation() = assertUnchanged(
        before = """
            class Test {
                static int method() {
                    int a;
                    a = 0;
                    a += 1;
                    return a;
                }
            }
        """
    )

    @Test
    fun shadowedNameScope() = assertChanged(
        before = """
            class Test {
                int a = 0;

                static Object method() {
                    int a = 1;
                    a = 2;

                    class InnerTest {
                        int innerMethod() {
                            int a = 0;
                            a = 3;
                            a = 4;
                            return a;
                        }
                    }
                    return new InnerTest();
                }
            }
        """,
        after = """
            class Test {
                int a = 0;

                static Object method() {
                    int a = 1;

                    class InnerTest {
                        int innerMethod() {
                            int a = 0;
                            a = 4;
                            return a;
                        }
                    }
                    return new InnerTest();
                }
            }
        """
    )

    @Test
    fun recognizeReadsFromInnerClass() = assertUnchanged(
        before = """
            class Test {
                int a = 0;

                Object method() {
                    class InnerTest {
                        int innerMethod() {
                            return a;
                        }
                    }
                    return new InnerTest();
                }
            }
        """
    )

    @Test
    fun ignoreFields0() = assertUnchanged(
        before = """
            class Test {
                int a;

                void method() {
                    this.a = 0;
                    a = 1;
                    a = 2;
                    System.out.println(a);
                    a = 3;
                    a = 4;
                }
            }
        """
    )

    @Test
    @Disabled
    // reformation of noticing an issue where "builder" is being removed in:
    // https://github.com/apache/drill/blob/958d849144a662a781e4d7d59adbf3300ad3bdea/contrib/storage-http/src/main/java/org/apache/drill/exec/store/http/HttpCSVBatchReader.java#L93
    fun ignoreFields1() = assertUnchanged(
        before = """
            class Test {
                Test builder;

                void addContext(Test builder) {
                    this.builder = builder;
                }

                void doWork() {
                    // nothing
                }

                void someInitialization() {
                    Test anotherTest = new Test() {
                        @Override
                        void addContext(Test builder) {
                            this.builder = builder;
                        }
                    };

                    builder = new Test();
                }

                void someUsage() {
                    builder.doWork();
                }
            }
        """
    )

    @Test
    fun parameterReassignment() = assertChanged(
        before = """
            class Test {
                static int method(int a) {
                    a = 99;
                    a = 0;
                    return a;
                }
            }
        """,
        after = """
            class Test {
                static int method(int a) {
                    a = 0;
                    return a;
                }
            }
        """
    )

    @Test
    fun forLoop() = assertUnchanged(
        before = """
            class Test {
                static void method(int j) {
                    int k = 0;
                    for (int i = 0; j < 10; j++) {
                        k += j;
                    }
                }
            }
        """
    )

    @Test
    fun ignoreUnusedForEachInitialization() = assertUnchanged(
        before = """
            import java.util.List;

            class Test {
                static void method(List<String> list) {
                    int a = 0;
                    for (String elem : list) {
                        System.out.println(a);
                        a = 2;
                    }
                    System.out.println(a);
                }
            }
        """
    )

    @Test
    // This shows how there may be unwanted removal of unused assignments in situations
    // where the assignment is technically "unused", but it's an intentional choice.
    // https://github.com/apache/drill/blob/958d849144a662a781e4d7d59adbf3300ad3bdea/contrib/format-esri/src/main/java/org/apache/drill/exec/store/esri/ShpBatchReader.java#L298-L316
    fun removesUnusedAssignmentToNull() = assertChanged(
        before = """
            import java.io.IOException;
            import java.io.InputStream;

            class Test {
                static void closeStream(InputStream inputStream, String name) {
                    if (inputStream == null) {
                        return;
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        System.out.println(String.format("Error when closing {}: {}", name, e.getMessage()));
                    }
                    inputStream = null;
                }
            }
        """,
        after = """
            import java.io.IOException;
            import java.io.InputStream;

            class Test {
                static void closeStream(InputStream inputStream, String name) {
                    if (inputStream == null) {
                        return;
                    }
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        System.out.println(String.format("Error when closing {}: {}", name, e.getMessage()));
                    }
                }
            }
        """
    )


}
