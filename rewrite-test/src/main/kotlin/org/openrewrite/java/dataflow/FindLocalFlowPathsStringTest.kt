package org.openrewrite.java.dataflow

import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.java.tree.J
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface FindLocalFlowPathsStringTest: RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(RewriteTest.toRecipe {
            FindLocalFlowPaths(object : LocalFlowSpec<J.Literal, J.MethodInvocation>() {
                override fun isSource(expr: J.Literal, cursor: Cursor) = expr.value == "42"
                override fun isSink(expr: J.MethodInvocation, cursor: Cursor) = true
            })
        })
    }

    @Test
    fun transitiveAssignment() = rewriteRun(
        { spec -> spec.expectedCyclesThatMakeChanges(1).cycles(1) },
        java(
            """
                class Test {
                    void test() {
                        String n = "42";
                        String o = n;
                        System.out.println(o);
                        String p = o;
                    }
                }
            """,
            """
                class Test {
                    void test() {
                        String n = /*~~>*/"42";
                        String o = /*~~>*/n;
                        /*~~>*/System.out.println(/*~~>*/o);
                        String p = o;
                    }
                }
            """
        )
    )
}
