package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

public class GroovyVisitorTest implements RewriteTest {

    @Test
    void autoFormatIncludesOmitParentheses() {
        rewriteRun(
          spec -> spec
            .cycles(1)
            .expectedCyclesThatMakeChanges(1)
            .recipeExecutionContext(new InMemoryExecutionContext().addObserver(new TreeObserver.Subscription(new TreeObserver() {
                @Override
                public Tree treeChanged(Cursor cursor, Tree newTree) {
                    return newTree;
                }
            }).subscribeToType(J.Lambda.class)))
            .recipe(RewriteTest.toRecipe(() -> new GroovyVisitor<>() {
                @Override
                public J visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    return autoFormat(super.visitCompilationUnit(cu, ctx), ctx);
                }
            })),
          groovy(
            """
              Test.test({ it })
              """,
            """
              Test.test {
                  it
              }
              """
          )
        );
    }
}
