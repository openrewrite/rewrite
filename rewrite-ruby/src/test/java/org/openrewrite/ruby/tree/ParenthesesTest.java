package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class ParenthesesTest implements RewriteTest {

    @Disabled
    @Test
    void parentheses() {
        rewriteRun(
          ruby(
            """
              (42)
              """
          )
        );
    }
}
