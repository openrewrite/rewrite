package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ChainStringBuilderAppendCallsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChainStringBuilderAppendCalls());
    }

    // todo, to be removed
    @Test
    void debugPurpose() {
        rewriteRun(
          java(
//            """
//              class A {
//                  void method1() {
//                      StringBuilder sb = new StringBuilder();
//                      String op = "+";
//                      str.append("A" + operator + "B");
//                  }
//              }
//              """,
            """
              class A {
                  void method1() {
                      StringBuilder sb = new StringBuilder();
                      String operator = "+";
                      sb.append("A").append(operator).append("B");
                  }
              }
              """
          )
        );
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

    /**
     * ----J.CompilationUnit
     *     \---J.ClassDeclaration
     *         |---J.Identifier | "A"
     *         \---J.Block
     *             \-------J.MethodDeclaration | "MethodDeclaration{A{name=method1,return=void,parameters=[]}}"
     *                     |---J.Primitive | "void"
     *                     |---J.Identifier | "method1"
     *                     |-----------J.Empty
     *                     \---J.Block
     *                         |-------J.VariableDeclarations | "StringBuilder sb = new StringBuilder()"
     *                         |       |---J.Identifier | "StringBuilder"
     *                         |       \-------J.VariableDeclarations.NamedVariable | "sb = new StringBuilder()"
     *                         |               |---J.Identifier | "sb"
     *                         |               \-------J.NewClass | "new StringBuilder()"
     *                         |                       |---J.Identifier | "StringBuilder"
     *                         |                       \-----------J.Empty
     *                         \-------J.MethodInvocation | "sb.append("A" + "B" + "C")"
     *                                 |-------J.Identifier | "sb"
     *                                 |---J.Identifier | "append"
     *                                 \-----------J.Binary | ""A" + "B" + "C""
     *                                             |---J.Binary | ""A" + "B""
     *                                             |   |---J.Literal | "A"
     *                                             |   \---J.Literal | "B"
     *                                             \---J.Literal | "C"
     */
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

    /**
     * ----J.CompilationUnit
     *     \---J.ClassDeclaration
     *         |---J.Identifier | "A"
     *         \---J.Block
     *             \-------J.MethodDeclaration | "MethodDeclaration{A{name=method1,return=void,parameters=[]}}"
     *                     |---J.Primitive | "void"
     *                     |---J.Identifier | "method1"
     *                     |-----------J.Empty
     *                     \---J.Block
     *                         |-------J.VariableDeclarations | "StringBuilder sb = new StringBuilder()"
     *                         |       |---J.Identifier | "StringBuilder"
     *                         |       \-------J.VariableDeclarations.NamedVariable | "sb = new StringBuilder()"
     *                         |               |---J.Identifier | "sb"
     *                         |               \-------J.NewClass | "new StringBuilder()"
     *                         |                       |---J.Identifier | "StringBuilder"
     *                         |                       \-----------J.Empty
     *                         |-------J.VariableDeclarations | "String op = "+""
     *                         |       |---J.Identifier | "String"
     *                         |       \-------J.VariableDeclarations.NamedVariable | "op = "+""
     *                         |               |---J.Identifier | "op"
     *                         |               \-------J.Literal | "+"
     *                         \-------J.MethodInvocation | "sb.append("A" + "B" + op + "C" + "D")"
     *                                 |-------J.Identifier | "sb"
     *                                 |---J.Identifier | "append"
     *                                 \-----------J.Binary | ""A" + "B" + op + "C" + "D""
     *                                             |---J.Binary | ""A" + "B" + op + "C""
     *                                             |   |---J.Binary | ""A" + "B" + op"
     *                                             |   |   |---J.Binary | ""A" + "B""
     *                                             |   |   |   |---J.Literal | "A"
     *                                             |   |   |   \---J.Literal | "B"
     *                                             |   |   \---J.Identifier | "op"
     *                                             |   \---J.Literal | "C"
     *                                             \---J.Literal | "D"
     */
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
                      StringBuilder sb = new StringBuilder().append("A" + operator()+ "B");
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

    /**
     *
     */
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
