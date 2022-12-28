package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.kotlin.tree.ParserAsserts.isFullyParsed;

public class EnumTest implements RewriteTest {

    @Disabled("add enum modifier as annotation.")
    @Test
    void enumEmptyBody() {
        rewriteRun(
          kotlin("""
              enum class A
              """,
            isFullyParsed()
          )
        );
    }

    @Disabled
    @Test
    void enumDefinition() {
        rewriteRun(
          kotlin("""
              enum class A {
                  B, C,
                  D
              }
              """,
            isFullyParsed()
          )
        );
    }

    @Disabled
    @Test
    void innerEnum() {
        rewriteRun(
          kotlin("""
              class A {
                  enum class B {
                      C
                  }
              }
              """,
            isFullyParsed()
          )
        );
    }
}
