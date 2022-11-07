package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

public class LabelsTest implements RewriteTest {

    @Test
    void singleLabel() {
        rewriteRun(
          //language=groovy
          groovy("""
              given: "hello"
            """)
        );
    }

    @Test
    void multiLabel() {
        rewriteRun(
          //language=groovy
          groovy("""
              def foo() {}
              label1:
              label2:
              label3:
              foo()
            """)
        );
    }
}
