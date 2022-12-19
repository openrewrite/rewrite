package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class CompilationUnitTest implements RewriteTest {

    @Test
    void compilationUnit() {
        rewriteRun(
          kotlin(
            """
                  import kotlin.collections.List
                  import java.util.ArrayList
                  
                  fun method(list: List<Int>): Int {
                    val l: ArrayList<Int> = ArrayList()
                    l.add(0)
                    return 0
                  }
              """
          )
        );
    }
}
