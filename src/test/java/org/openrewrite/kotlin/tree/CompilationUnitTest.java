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
                  package kotlin
                  
                  import kotlin.collections.List
                  fun method(list: List<Int>): Int {
                    return 0
                  }
              """
          )
        );
    }
}
