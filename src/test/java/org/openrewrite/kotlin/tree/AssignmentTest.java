package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class AssignmentTest implements RewriteTest {

    @Disabled("Requires function call and Convert PSI.")
    @Test
    void unaryMinus() {
        rewriteRun(
          kotlin(
            """
              val i = -1
              val l = -2L
              val f = -3.0f
              val d = -4.0
            """,
            isFullyParsed()
          )
        );
    }
}
