package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class ForTest implements RewriteTest {

    @Test
    void forIn() {
        rewriteRun(
          ruby(
            """
              for i in 0..10
                  puts i
              end
              """
          )
        );
    }
}
