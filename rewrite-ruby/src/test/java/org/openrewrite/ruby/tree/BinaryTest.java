package org.openrewrite.ruby.tree;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class BinaryTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"+", "-", "*", "/", "%", "**"})
    void arithmetic(String op) {
        rewriteRun(
          ruby(
            """
              42 %s 1
              """.formatted(op)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"<<", ">>", "&", "|", "^", "~"})
    void bitwise(String op) {
        rewriteRun(
          ruby(
            """
              42 %s 1
              """.formatted(op)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"==", "!=", "===", "<", "<=", ">", ">=", "<=>"})
    void comparison(String op) {
        rewriteRun(
          ruby(
            """
              42 %s 1
              """.formatted(op)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"&&", "and", "or", "||"})
    void logical(String op) {
        rewriteRun(
          ruby(
            """
              42 %s 1
              """.formatted(op)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "..."})
    void range(String op) {
        rewriteRun(
          ruby(
            """
              42 %s 1
              """.formatted(op)
          )
        );
    }
}
