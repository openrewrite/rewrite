package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class BreakTest implements RewriteTest {

    @Test
    void breakStatement() {
        rewriteRun(
          ruby(
            """
              for i in 0..5
                 if i > 2 then
                    break
                 end
              end
              """
          )
        );
    }
}
