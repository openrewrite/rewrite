package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class YieldTest implements RewriteTest {

    @Test
    void yield() {
        rewriteRun(
          ruby(
            """
              yield 'test'
              """
          )
        );
    }

    @Test
    void yieldParentheses() {
        rewriteRun(
          ruby(
            """
              yield('test', 'test2')
              """
          )
        );
    }

    @Test
    void yieldNoParentheses() {
        rewriteRun(
          ruby(
            """
              yield 'test', 'test2'
              """
          )
        );
    }
}
