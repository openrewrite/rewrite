package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class IfTest implements RewriteTest {

    @Test
    void noElse() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val n: Int = 0
                  if (n == 0) {
                  }
              }
              """,
              isFullyParsed()
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val n: Int = 0
                  if (n == 0) {
                      val x = 0
                  } else if (n == 1) {
                      val x = 1
                  } else {
                      val x = 2
                  }
              }
              """,
            isFullyParsed()
          )
        );
    }
}
