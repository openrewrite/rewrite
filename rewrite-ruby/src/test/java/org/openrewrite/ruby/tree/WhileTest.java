package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class WhileTest implements RewriteTest {

    @Test
    void explicitDo() {
        rewriteRun(
          ruby(
            """
              while true do
                  puts "Hello"
              end
              """
          )
        );
    }

    @Test
    void noDo() {
        rewriteRun(
          ruby(
            """
              while true
                  puts "Hello"
              end
              """
          )
        );
    }
}
