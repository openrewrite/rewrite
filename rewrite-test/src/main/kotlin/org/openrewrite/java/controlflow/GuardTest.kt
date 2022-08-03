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
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("FunctionName")
interface GuardTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitExpression(expression: Expression, p: ExecutionContext): Expression =
                        Guard.from(cursor)
                                .map { expression.withMarkers<Expression>(expression.markers.searchResult()) }
                                .orElse(expression)
            }
        })
    }

    @Test
    fun `identifies guards`() = rewriteRun(
            java(
            """
            abstract class Test {
                abstract boolean boolLiteral();
                abstract Boolean boolObject();

                void test() {
                    if (boolLiteral()) {
                        // ...
                    }
                    if (boolObject()) {
                        // ...
                    }
                }
            }
            """,
                    """
            abstract class Test {
                abstract /*~~>*/boolean boolLiteral();
                abstract /*~~>*/Boolean boolObject();

                void test() {
                    if (/*~~>*/boolLiteral()) {
                        // ...
                    }
                    if (/*~~>*/boolObject()) {
                        // ...
                    }
                }
            }
            """
            )
    )

    @Test
    fun `identifies guards with binary expressions`() = rewriteRun(
            java(
                    """
            abstract class Test {
                abstract boolean boolPrim();
                abstract Boolean boolObject();

                void test() {
                    if (boolPrim()) {
                        // ...
                    }
                    if (boolObject() || boolPrim()) {
                        // ...
                    }
                }
            }
            """,
                    """
            abstract class Test {
                abstract /*~~>*/boolean boolPrim();
                abstract /*~~>*/Boolean boolObject();

                void test() {
                    if (/*~~>*/boolPrim()) {
                        // ...
                    }
                    if (/*~~>*//*~~>*/boolObject() || /*~~>*/boolPrim()) {
                        // ...
                    }
                }
            }
            """
            )
    )


    @Test
    fun `identifies guards with methods with parameters`() = rewriteRun(
            java(
                    """
            abstract class Test {

                void test(boolean x, Boolean y) {
                    if (x) {
                        // ...
                    }
                    if (y) {
                        // ...
                    }
                }
            }
            """,
                    """
            abstract class Test {
                void test(/*~~>*/boolean /*~~>*/x, /*~~>*/Boolean /*~~>*/y) {
                    if (/*~~>*/x) {
                        // ...
                    }
                    if (/*~~>*/y) {
                        // ...
                    }
                }
            }
            """
            )
    )


    // field accesses
    @Test
    fun `identifies guards with field accesses`() = rewriteRun(
            java(
                    """
            abstract class Test {
                private boolean x;
                private Boolean y;

                void test() {
                    if (x) {
                        // ...
                    }
                    if (y) {
                        // ...
                    }
                }
            }
            """,
                    """
            abstract class Test {
                private /*~~>*/boolean /*~~>*/x;
                private /*~~>*/Boolean /*~~>*/y;

                void test() {
                    if (/*~~>*/x) {
                        // ...
                    }
                    if (/*~~>*/y) {
                        // ...
                    }
                }
            }
            """
            )
    )

    @Test
    fun `identifies guards with missing type information`() = rewriteRun(
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
                    horse.equals(donkey);
                    boolean farmFresh = tomato;
                    boolean farmFreshAndFancyFree = (chicken);
                    boolean farmFreshEggs = true;
                    farmFreshEggs = chicken.layEggs();
                }
            }
            """.trimIndent(),
            """
            class Test {
                void test() {
                    if (/*~~>*/potato) {
                        // ...
                    }
                    if (/*~~>*/(/*~~>*/potato)) {
                        // ...
                    }
                    if (/*~~>*//*~~>*/potato && /*~~>*/turnip) {
                        // ...
                    }
                    if (/*~~>*//*~~>*//*~~>*/potato && /*~~>*/turnip || /*~~>*/squash) {
                        // ...
                    }
                    int a = 1, b = 2;
                    if (/*~~>*/(a = turnip) == b) {
                        // ..
                    }
                    /*~~>*/horse.equals(donkey);
                    /*~~>*/boolean /*~~>*/farmFresh = /*~~>*/tomato;
                    /*~~>*/boolean /*~~>*/farmFreshAndFancyFree = /*~~>*/(/*~~>*/chicken);
                    /*~~>*/boolean /*~~>*/farmFreshEggs = /*~~>*/true;
                    /*~~>*//*~~>*/farmFreshEggs = /*~~>*/chicken.layEggs();
                }
            }
            """.trimIndent()
        )
    )

    @Test
    fun `identifies guards for control parentheses with missing type information`() = rewriteRun(
        java(
            """
            class Test {
                void test() {
                    if ((potato)) {
                        // ...
                    }
                }
            }
            """.trimIndent(),
            """
            class Test {
                void test() {
                    if (/*~~>*/(/*~~>*/potato)) {
                        // ...
                    }
                }
            }
            """.trimIndent()
        )
    )

    @Test
    fun `does not flag arbitrary parentheses as guards`() = rewriteRun(
        java(
            """
            class Test {
                void test() {
                    int a = (potato);
                    int b = (a = turnip);
                }
            }
            """.trimIndent()
        )
    )
}
