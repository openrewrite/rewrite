package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ProblemTest implements RewriteTest {

    @Test
    void anonymousClass() {
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;
              
              class Test {
                  List<Integer> l = new ArrayList<Integer>() {
                      /** Javadoc */
                      @Override
                      public boolean isEmpty() {
                          return false;
                      }
                  };
              }
              """
          )
        );
    }
}
