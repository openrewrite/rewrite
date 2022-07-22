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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.MethodMatcher
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface RemoveMethodCallVisitorTest : RewriteTest {

    fun RecipeSpec.testVisitor(
        methodMatcher: MethodMatcher,
        argumentPredicate: (Int, Expression) -> Boolean
    ): RecipeSpec =
        recipe(RewriteTest.toRecipe {
            RemoveMethodCallVisitor(
                methodMatcher,
                argumentPredicate
            )
        })

    fun RecipeSpec.asserTrueTestVisitor() =
        testVisitor(MethodMatcher("* assertTrue(..)")) { arg, expr ->
            arg == 0 && (expr as? J.Literal)?.value == true
        }

    @Test
    fun `asertTrue(true) is removed`() = rewriteRun(
        { spec -> spec.asserTrueTestVisitor() },
        java(
            """
            abstract class Test {
                abstract void assertTrue(boolean condition);

                void test() {
                    System.out.println("Hello");
                    assertTrue(true);
                    System.out.println("World");
                }
            }
            """, """
            abstract class Test {
                abstract void assertTrue(boolean condition);

                void test() {
                    System.out.println("Hello");
                    System.out.println("World");
                }
            }
            """
        )
    )

    @Test
    fun `asertTrue(false) is not removed`() = rewriteRun(
        { spec -> spec.asserTrueTestVisitor() },
        java(
            """
            abstract class Test {
                abstract void assertTrue(boolean condition);

                void test() {
                    System.out.println("Hello");
                    assertTrue(false);
                    System.out.println("World");
                }
            }
            """
        )
    )

    @Test
    fun `asertTrue("message", true) is removed`() = rewriteRun(
        { spec ->
            spec.testVisitor(MethodMatcher("* assertTrue(..)")) { arg, expr ->
                (arg == 1 && (expr as? J.Literal)?.value == true) || arg != 1
            }
        },
        java(
            """
            abstract class Test {
                abstract void assertTrue(String message, boolean condition);

                void test() {
                    System.out.println("Hello");
                    assertTrue("message", true);
                    System.out.println("World");
                }
            }
            """, """
            abstract class Test {
                abstract void assertTrue(String message, boolean condition);

                void test() {
                    System.out.println("Hello");
                    System.out.println("World");
                }
            }
            """
        )
    )

    @Test
    fun `does not remove assertTrue(true) if return value is used`() = rewriteRun(
        { spec -> spec.asserTrueTestVisitor() },
        java(
            """
            abstract class Test {
                abstract int assertTrue(boolean condition);

                void test() {
                    System.out.println("Hello");
                    int value = assertTrue(true);
                    System.out.println("World");
                }
            }
            """
        )
    )
}
