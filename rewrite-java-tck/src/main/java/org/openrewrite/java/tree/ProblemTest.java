package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ProblemTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/377")
    @Test
    void typeParameterAnnotations() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.lang.annotation.*;
              class TypeAnnotationTest {
                  List<@A ? extends @A String> list;
              
                  @Target({ ElementType.FIELD, ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
                  private @interface A {
                  }
              }
              """
          )
        );
    }
}
