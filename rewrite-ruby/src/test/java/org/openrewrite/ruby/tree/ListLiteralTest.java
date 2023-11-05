package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class ListLiteralTest implements RewriteTest {

    @Test
    void list() {
        rewriteRun(
          ruby(
            """
              [1, 2]
              """
          )
        );
    }
}
