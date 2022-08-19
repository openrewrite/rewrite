package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openrewrite.java.JavaParserResolver;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@ExtendWith(JavaParserResolver.class)
class UseDiamondOperatorWithVarTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseDiamondOperator());
    }

    @SuppressWarnings("Convert2Diamond")
    @Test
    void doNotConvertVar() {
        rewriteRun(
          java(
            """
                  import java.util.*;
                  class Test {
                      void test() {
                          var ls1 = new ArrayList<String>();
                          List<String> ls2 = new ArrayList<String>();
                      }
                  }
              """,
            """
                  import java.util.*;
                  class Test {
                      void test() {
                          var ls1 = new ArrayList<String>();
                          List<String> ls2 = new ArrayList<>();
                      }
                  }
              """
          )
        );
    }
}
