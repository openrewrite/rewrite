package org.openrewrite.ruby.tree;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class UnaryTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"!", "not"})
    void logical(String op) {
        rewriteRun(
          ruby(
            """
              %s 42
              """.formatted(op)
          )
        );
    }
}
