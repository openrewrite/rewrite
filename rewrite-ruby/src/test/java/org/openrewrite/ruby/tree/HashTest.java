package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class HashTest implements RewriteTest {

    @Test
    void hash() {
        rewriteRun(
          ruby(
            """
              {a: 1}
              """
          )
        );
    }
}
