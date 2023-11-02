package org.openrewrite.ruby.tree;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class AssignmentTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"=", "+=", "-=", "*=", "/=", "%=", "**="})
    void assignmentOperators(String op) {
        rewriteRun(
          ruby(
            """
              a %s 1
              """.formatted(op)
          )
        );
    }
}
