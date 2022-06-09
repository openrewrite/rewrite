package org.openrewrite.java.dataflow.guard

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.JavaIsoVisitor
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
}
