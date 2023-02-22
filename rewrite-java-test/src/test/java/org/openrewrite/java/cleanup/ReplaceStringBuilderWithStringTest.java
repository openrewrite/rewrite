package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceStringBuilderWithStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceStringBuilderWithString());
    }

    @Test
    void replaceLiteralConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  void foo() {
                      String s = new StringBuilder().append("A").append("B").toString();
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      String s = "A" + "B";
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceLiteralConcatenationWithReturn() {
        rewriteRun(
          java(
            """
              class A {
                  String foo() {
                      return new StringBuilder().append("A").append("B").toString();
                  }
              }
              """,
            """
              class A {
                  String foo() {
                      return "A" + "B";
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceCombinedConcatenation() {
        rewriteRun(
          java(
            """
              class A {
                  String bar() {
                      String str1 = "Hello";
                      String str2 = name();
                      return new StringBuilder().append(str1).append(str2).append(getSuffix()).toString();
                  }
                  String name() {
                      return "world";
                  }
                  String getSuffix() {
                      return "!";
                  }
              }
              """,
            """
              class A {
                  String bar() {
                      String str1 = "Hello";
                      String str2 = name();
                      return str1 + str2 + getSuffix();
                  }
                  String name() {
                      return "world";
                  }
                  String getSuffix() {
                      return "!";
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceInChainedMethods() {
        rewriteRun(
          java(
            """
              class A {
                  void foo() {
                      int len = new StringBuilder().append("A").append("B").toString().length();
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      int len = ("A" + "B").length();
                  }
              }
              """
          )
        );
    }
}
