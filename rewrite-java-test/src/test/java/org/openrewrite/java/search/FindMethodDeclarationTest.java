package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("InfiniteRecursion")
public class FindMethodDeclarationTest implements RewriteTest {
    @Test
    void find() {
        rewriteRun(
          spec -> spec.recipe(new FindMethodDeclaration("A a(int)", false)),
          java(
            """
              class A {
                  void a(int n) {
                    a(1);
                  }
                  void a(String s) {
                  }
              }
              """,
            """
              class A {
                  /*~~>*/void a(int n) {
                    a(1);
                  }
                  void a(String s) {
                  }
              }
              """)
        );
    }
}
