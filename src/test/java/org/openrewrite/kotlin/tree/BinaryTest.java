package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class BinaryTest implements RewriteTest {

    @Test
    void equals() {
        rewriteRun(
          kotlin("""
              fun method() {
                val n = 0
                val b = n == 0
              }
              """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Requires function call and Convert PSI.")
    @Test
    void minusEquals() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                var n = 0
                n -= 5
              }
              """,
            isFullyParsed()
          )
        );
    }
}
