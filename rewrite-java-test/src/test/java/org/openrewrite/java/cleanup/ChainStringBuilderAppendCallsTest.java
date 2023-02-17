package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChainStringBuilderAppendCallsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChainStringBuilderAppendCalls());
    }

    @Test
    void objectsConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A" + op + "B");
                  }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A").append(op).append("B");
                  }
              }
              """
          )
        );
    }

    @Test
    void literalConcatenationIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      sb.append("A" + "B" + "C");
                  }
              }
              """
          )
        );
    }

    @Test
    void groupedObjectsConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A" + "B" + "C" + op + "D" + "E");
                  }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String op = "+";
                      sb.append("A" + "B" + "C").append(op).append("D" + "E");
                  }
              }
              """
          )
        );
    }

    @Test
    void appendMethods() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      sb.append(str1() + str2() + str3());
                  }

                  String str1() { return "A"; }
                  String str2() { return "B"; }
                  String str3() { return "C"; }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      sb.append(str1()).append(str2()).append(str3());
                  }

                  String str1() { return "A"; }
                  String str2() { return "B"; }
                  String str3() { return "C"; }
              }
              """
          )
        );
    }

    @Test
    void ChainedAppendWithConstructor() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder().append("A" + operator() + "B");
                  }

                  String operator() { return "+"; }
              }
              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder().append("A").append(operator()).append("B");
                  }

                  String operator() { return "+"; }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  void method1() {
                      String op = "+";
                      print(new StringBuilder().append("A" + op + "C").toString());
                  }

                  void print(String str) {
                  }
              }
              """
          )
        );
    }
}
