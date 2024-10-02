package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavadocPrinterTest implements RewriteTest {

    @Test
    void findInJavadoc() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                  return space.withComments(ListUtils.map(space.getComments(), comment -> {
                      if (comment instanceof Javadoc.DocComment) {
                          return comment.withMarkers(comment.getMarkers().
                            computeByType(new SearchResult(randomId(), null), (s1, s2) -> s1 == null ? s2 : s1));
                      }
                      return comment;
                  }));
              }
          })),
          java(
            """
              /** this is a doc comment*/
              class Test {
              }
              """,
            """
              /*~~>*//** this is a doc comment*/
              class Test {
              }
              """
          )
        );
    }
}