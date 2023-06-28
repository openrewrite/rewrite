package org.openrewrite.kotlin.recipe;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.*;


public class ReplaceCharToIntWithCodeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceCharToIntWithCode());
    }

    @DocumentExample
    @Test
    void kotlinFile() {
        rewriteRun(
          kotlin(
            """
              fun decimalDigitalNumber(c : Char) : Int? {
                  if (c in '0'..'9') {
                      return c.toInt() - '0'.toInt()
                  }
                  return null
              }
              """,
            """
              fun decimalDigitalNumber(c : Char) : Int? {
                  if (c in '0'..'9') {
                      return c.code - '0'.code
                  }
                  return null
              }
              """
          )
        );
    }
}
