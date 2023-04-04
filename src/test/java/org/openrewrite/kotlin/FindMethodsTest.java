package org.openrewrite.kotlin;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

public class FindMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new KotlinIsoVisitor<>() {
            @Override
            public K.CompilationUnit visitCompilationUnit(K.CompilationUnit cu, ExecutionContext executionContext) {
                String pattern = "openRewriteFile0.kt function()";
                assertThat(FindMethods.find(cu, pattern)).isNotEmpty();
                return super.visitCompilationUnit(cu, executionContext);
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