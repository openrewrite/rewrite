package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class AvoidBoxedBooleanExpressionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AvoidBoxedBooleanExpressions());
    }

    @Test
    void guardAgainstNpe() {
        rewriteRun(
          java(
            """
              class Test {
                  Boolean b;
                  int test() {
                      if (b) {
                        return 1;
                      } else {
                        return 2;
                      }
                  }
              }
              """,
            """
              class Test {
                  Boolean b;
                  int test() {
                      if (Boolean.TRUE.equals(b)) {
                        return 1;
                      } else {
                        return 2;
                      }
                  }
              }
              """
          )
        );
    }
}
