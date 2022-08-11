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
@file:Suppress("StatementWithEmptyBody", "UnusedAssignment", "ConstantConditions")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

interface FinalizeLocalVariablesTest : JavaRecipeTest {
    override val recipe: Recipe?
        get() = FinalizeLocalVariables()

    @Issue("https://github.com/openrewrite/rewrite/issues/1478")
    @Test
    fun initializedInWhileLoop(jp: JavaParser) = assertUnchanged(
        before = """
            import java.io.BufferedReader;
            class T {
                public void doSomething(StringBuilder sb, BufferedReader br) {
                    String line;
                    try {
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                    } catch (Exception e) {
                        logger.error("Exception", e);
                    }
                }
            }
        """
    )

    @Test
    fun localVariablesAreMadeFinal(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public void test() {
                    int n = 1;
                    for(int i = 0; i < n; i++) {
                    }
                }
            }
        """,
        after = """
            class A {
                public void test() {
                    final int n = 1;
                    for(int i = 0; i < n; i++) {
                    }
                }
            }
        """
    )

    @Test
    fun identifyReassignedLocalVariables(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public void test() {
                    int a = 0;
                    int b = 0;
                    int c = 10;
                    for(int i = 0; i < c; i++) {
                        a = i + c;
                        b++;
                    }
                }
            }
        """,
        after = """
            class A {
                public void test() {
                    int a = 0;
                    int b = 0;
                    final int c = 10;
                    for(int i = 0; i < c; i++) {
                        a = i + c;
                        b++;
                    }
                }
            }
        """
    )

    @Disabled("consider uninitialized local variables non final")
    @Test
    fun multipleVariablesDeclarationOnSingleLine(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public void multiVariables() {
                    int a, b = 1;
                    a = 0;
                }
            }
        """,
        // the final only applies to any initialized variables (b in this case)
        after = """
            class A {
                public void multiVariables() {
                    final int a, b = 1;
                    a = 0;
                }
            }
        """
    )

    @Disabled("consider uninitialized local variables non final")
    @Test
    fun calculateLocalVariablesInitializerOffset(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public void testOne() {
                    int a;
                    a = 0;
                    System.out.println(a);
                }

                public void testTwo() {
                    int a;
                    a = 0;
                    a = 0;
                    System.out.println(a);
                }

                public void testThree() {
                    int a;
                    a = 0;
                    a++;
                    System.out.println(a);
                }
            }
        """,
        after = """
            class A {
                public void testOne() {
                    final int a;
                    a = 0;
                    System.out.println(a);
                }

                public void testTwo() {
                    int a;
                    a = 0;
                    a = 0;
                    System.out.println(a);
                }

                public void testThree() {
                    int a;
                    a = 0;
                    a++;
                    System.out.println(a);
                }
            }
        """
    )

    @Test
    @Disabled
    fun calculateLocalVariablesInitializerBranching(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class A {
                public void test(boolean hasThing) {
                    int a;
                    if (hasThing) {
                        a = 0;
                    } else {
                        a = 1;
                    }
                    System.out.println(a);
                }
            }
        """,
        after = """
            class A {
                public void test(boolean hasThing) {
                    final int a;
                    if (hasThing) {
                        a = 0;
                    } else {
                        a = 1;
                    }
                    System.out.println(a);
                }
            }
        """
    )

    @Disabled("consider uninitialized local variables non final")
    @Test
    fun forEachLoopAssignmentMadeFinal(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                public static void testForEach(String[] args) {
                    for (String a : args) {
                        System.out.println(a);
                    }

                    for (String b : args) {
                        b = b.toUpperCase();
                        System.out.println(b);
                    }
                }
            }
        """,
        after = """
            class Test {
                public static void testForEach(String[] args) {
                    for (final String a : args) {
                        System.out.println(a);
                    }

                    for (String b : args) {
                        b = b.toUpperCase();
                        System.out.println(b);
                    }
                }
            }
        """
    )

    @Test
    fun localVariableScopeAwareness(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                public static void testA() {
                    int a = 0;
                    a = 1;
                }

                public static void testB() {
                    int a = 0;
                }
            }
        """,
        after = """
            class Test {
                public static void testA() {
                    int a = 0;
                    a = 1;
                }

                public static void testB() {
                    final int a = 0;
                }
            }
        """
    )

    @Test
    @Disabled("consider uninitialized local variables non final")
    fun forEachLoopScopeAwareness(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                public static void testForEach(String[] args) {
                    for (String i : args) {
                        System.out.println(i);
                    }

                    for (String i : args) {
                        i = i.toUpperCase();
                        System.out.println(i);
                    }
                }
            }
        """,
        after = """
            class Test {
                public static void testForEach(String[] args) {
                    for (final String i : args) {
                        System.out.println(i);
                    }

                    for (String i : args) {
                        i = i.toUpperCase();
                        System.out.println(i);
                    }
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/549")
    fun catchBlocksIgnored(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.io.IOException;
            
            class Test {
                static {
                    try {
                        null;
                    } catch (RuntimeException | IOException e) {
                        null;
                    }
                }
            }
        """
    )

    @Test // aka "non-static-fields"
    fun instanceVariablesIgnored(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                int instanceVariableUninitialized;
                int instanceVariableInitialized = 0;
            }
        """
    )

    @Test // aka "static fields"
    fun classVariablesIgnored(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                static int classVariableInitialized = 0;
            }
        """
    )

    @Test
    fun classInitializersIgnored(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                static {
                    int n = 1;
                    for(int i = 0; i < n; i++) {
                    }
                }
            }
        """
    )

    @Test
    fun methodParameterVariablesIgnored(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                private static int testMath(int x, int y) {
                    y = y + y;
                    return x + y;
                }

                public static void main(String[] args) {
                }
            }
        """
    )

    @Test
    fun lambdaVariablesIgnored(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.stream.Stream;
            class A {
                public boolean hasFoo(Stream<String> input) {
                    return input.anyMatch(word -> word.equalsIgnoreCase("foo"));
                }
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1357")
    @Test
    fun forLoopVariablesIgnored(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            import java.util.concurrent.FutureTask;
            
            class A {
                void f() {
                    for(FutureTask<?> future; (future = new FutureTask<>(() -> "hello world")) != null;) { }
                }
            }
        """
    )
}
