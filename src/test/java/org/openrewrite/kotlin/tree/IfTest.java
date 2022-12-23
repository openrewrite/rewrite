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
}
