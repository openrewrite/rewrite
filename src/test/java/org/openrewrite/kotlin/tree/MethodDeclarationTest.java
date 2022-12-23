package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class MethodDeclarationTest implements RewriteTest {

    @Test
    void methodDeclaration() {
        rewriteRun(
          kotlin(
            """
                  fun method() {
                  }
              """,
            isFullyParsed()
          )
        );
    }

    @Disabled("Add method parameters")
    @Test
    void parameters() {
        rewriteRun(
          kotlin(
            """
                  fun method(l: List<Int>) {
                  }
              """,
            isFullyParsed()
          )
        );
    }

    @Test
    void methodDeclarationDeclaringType() {
        rewriteRun(
          kotlin(
            """
                  class A {
                    fun method() {
                    }
                  }
              """,
            isFullyParsed()
          )
        );
    }
}
