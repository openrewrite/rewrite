package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class MethodDeclarationTest implements RewriteTest {

    @Test
    void noArgs() {
        rewriteRun(
          ruby(
            """
              def test
                 i = 42
              end
              """
          )
        );
    }

    @Test
    void singleArg() {
        rewriteRun(
          ruby(
            """
              def test(a1 = "Ruby")
                  puts "The programming language is #{a1}"
              end
              """
          )
        );
    }

    @Test
    void twoArgs() {
        rewriteRun(
          ruby(
            """
              def test(a1 = "Ruby", a2 = "Perl")
                  puts "The programming language is #{a1}"
              end
              """
          )
        );
    }
}
