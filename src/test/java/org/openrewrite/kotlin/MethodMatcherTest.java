package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class MethodMatcherTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
            private final String pattern = "openRewriteFile0.kt function()";
            private final MethodMatcher methodMatcher = new MethodMatcher(pattern);

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                if (methodMatcher.matches(method.getMethodType())) {
                    assertThat(method.getName().getSimpleName()).isEqualTo("function");
                }
                return super.visitMethodDeclaration(method, p);
            }
        }));
    }

    @Test
    void matchesTopLevelFunction() {
        rewriteRun(
          kotlin(
            """
            fun function() {}
            fun usesFunction() {
                function()
            }
            """
          )
        );
    }
}