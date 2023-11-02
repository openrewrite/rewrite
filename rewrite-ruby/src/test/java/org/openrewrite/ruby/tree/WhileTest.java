package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class WhileTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"while", "until"})
    void explicitDo(String whileOrUntil) {
        rewriteRun(
          ruby(
            """
              %s true do
                  puts "Hello"
              end
              """.formatted(whileOrUntil)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"while", "until"})
    void noDo(String whileOrUntil) {
        rewriteRun(
          ruby(
            """
              %s true
                  puts "Hello"
              end
              """.formatted(whileOrUntil)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"while", "until"})
    void whileModifier(String whileOrUntil) {
        rewriteRun(
          ruby(
            """
              puts "Hello" %s true
              """.formatted(whileOrUntil)
          )
        );
    }

    @Disabled
    @Test
    void beginEndWhileModifier() {
        rewriteRun(
          ruby(
            """
              begin
                  puts "Hello"
              end while true
              """
          )
        );
    }
}
