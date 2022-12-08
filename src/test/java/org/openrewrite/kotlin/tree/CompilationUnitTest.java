package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class CompilationUnitTest implements RewriteTest {

    @Test
    void psvm() {
        rewriteRun(
          kotlin(
            """
                  class Test {
                    fun main() {
                      println("Hello, world!")
                    }
                  }
              """
          )
        );
    }
}
