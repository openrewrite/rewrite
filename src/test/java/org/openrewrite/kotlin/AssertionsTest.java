package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

// This class may be removed after recipes with Assertions are added.
public class AssertionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                if ("a".equals(variable.getSimpleName())) {
                    return variable.withName(variable.getName().withSimpleName("b"));
                }
                return variable;
            }
        }));
    }

    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/30")
    @Test
    void isChanged() {
        rewriteRun(
          kotlin(
            """
                class A {
                    val a = 1
                }
            """,
            """
                class A {
                    val b = 1
                }
            """
          )
        );
    }
}
