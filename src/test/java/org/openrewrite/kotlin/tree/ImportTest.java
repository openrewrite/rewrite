package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

public class ImportTest implements RewriteTest {

    @Test
    void jdkImport() {
        rewriteRun(
          kotlin(
            """
                  import java.util.ArrayList
              """
          )
        );
    }

    @Test
    void kotlinImport() {
        rewriteRun(
          kotlin(
            """
                  import kotlin.collections.List
              """
          )
        );
    }
}
