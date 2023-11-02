package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class ContinueTest implements RewriteTest {

    @Test
    void nextStatement() {
        rewriteRun(
          ruby(
            """
              for i in 0..5
                 if i > 2 then
                    next
                 end
              end
              """
          )
        );
    }
}
