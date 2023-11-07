package org.openrewrite.ruby.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.ruby.Assertions.ruby;

public class ClassDeclarationTest implements RewriteTest {

    @Test
    void classDeclaration() {
        rewriteRun(
          ruby(
            """
              class Customer
              end
              """
          )
        );
    }

    @Test
    void classExtends() {
        rewriteRun(
          ruby(
            """
              class Box
              end
              
              class BigBox < Box
              end
              """
          )
        );
    }
}
