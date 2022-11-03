package org.openrewrite.groovy.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.groovy.Assertions.groovy;

public class OmitParenthesesForLastArgumentLambdaTest implements RewriteTest {
    final SourceSpecs closureApi = groovy(
      """
        public class Test {
          public static void test(Closure closure) {
          }
        }
        """
    );

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OmitParenthesesForLastArgumentLambda());
    }

    @Test
    void lastClosureArgument() {
        rewriteRun(
          closureApi,
          groovy(
            "Test.test({ it })",
            "Test.test { it }"
          )
        );
    }
}
