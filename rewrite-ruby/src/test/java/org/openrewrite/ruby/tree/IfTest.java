package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class IfTest implements RewriteTest {

    @Test
    void singleIf() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              end
              """
          )
        );
    }

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

    @Test
    void ifElseIf() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              elsif n < 42
                  puts "less 42"
              end
              """
          )
        );
    }

    @Test
    void ifElse() {
        rewriteRun(
          ruby(
            """
              if n == 42 then
                  puts "42"
              else
                  puts "less 42"
              end
              """
          )
        );
    }
}
