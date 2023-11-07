package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class StringTest implements RewriteTest {

    @Test
    void string() {
        rewriteRun(
          ruby(
            """
              "The programming language is"
              """
          )
        );
    }

    @Test
    void singleQuoteDelimiter() {
        rewriteRun(
          ruby(
            """
              'The programming language is'
              """
          )
        );
    }

    @Test
    void delimitedString() {
        rewriteRun(
          ruby(
            """
              "#{a1} The programming language is #{a1}"
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Q", "q", "x"})
    void percentDelimitedString(String delim) {
        rewriteRun(
          ruby(
            """
              %%%s!#{a1} The programming language is #{a1}!
              """.formatted(delim)
          )
        );
    }
}
