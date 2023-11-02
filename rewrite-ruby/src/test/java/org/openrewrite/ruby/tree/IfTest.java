package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class IfTest implements RewriteTest {

    @Test
    void ifElseIfElse() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              elsif n > 42 then
                  puts "greater 42"
              elsif n < 42
                  puts "less 42"
              else
                  puts "something else"
              end
              """
          )
        );
    }
}
