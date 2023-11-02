package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class MethodInvocationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"\"test\"", "(\"test\")"})
    void print(String args) {
        rewriteRun(
          ruby(
            """
              print %s
              """.formatted(args)
          )
        );
    }

}
