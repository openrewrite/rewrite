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
                    val test: Int  = 10
                    fun main() {
                      if (true) {
                        println("Hello, world!")
                      }
                    }
                  }
              """
          )
        );
    }
}
