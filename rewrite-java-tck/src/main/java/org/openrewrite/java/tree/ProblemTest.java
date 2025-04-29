package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class ProblemTest implements RewriteTest {

    @MinimumJava17
    @Test
    void string() {
        rewriteRun(
          java(
            """
              public class Test {
                  static {
                      var a = "";
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    assertThat(requireNonNull(multiVariable.getTypeAsFullyQualified()).getFullyQualifiedName())
                      .isEqualTo("java.lang.String");
                    return multiVariable;
                }
            })
          )
        );
    }
}
