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
                abstract boolean boolLiteral();
                abstract Boolean boolObject();

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
}
