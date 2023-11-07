package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class BlockTest implements RewriteTest {

    @Test
    void topLevelBlock() {
        rewriteRun(
          ruby(
            """
              a = 42
              b = 42
              """
          )
        );
    }
}
