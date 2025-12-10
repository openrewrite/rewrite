package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.SimplifyMethodChain;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.groovy.Assertions.groovy;

class SimplifyMethodChainTest implements RewriteTest {

    @DocumentExample
    @Test
    void simplify() {
        rewriteRun(
          spec -> spec.recipe(new SimplifyMethodChain(
            List.of("A b()", "B c()"), "c2", false)),
          groovy(
            """
              class A {
                  static B b() { return new B() }
                  static C c2() { return new C() }
              }

              class B {
                  C c() { return new C() }
              }

              class C {
              }

              class Test {
                  C c = A.b().c()
              }
              """,
            """
              class A {
                  static B b() { return new B() }
                  static C c2() { return new C() }
              }

              class B {
                  C c() { return new C() }
              }

              class C {
              }

              class Test {
                  C c = A.c2()
              }
              """
          )
        );
    }
}
