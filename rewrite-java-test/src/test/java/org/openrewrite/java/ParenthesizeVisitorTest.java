/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class ParenthesizeVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(toRecipe(Reparenthesize::new))
          .cycles(1)
          .expectedCyclesThatMakeChanges(1);
    }

    @DocumentExample
    @Test
    void binaryOperatorPrecedence() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 1 + (2 * 3);
                      int b = (1 * 2) + 3;
                      int c = (1 * 2) + (3 * 4);
                      int d = 7 - (5 - 2);
                      int e = -(-(1));

                      boolean k = a > 5 && b <= 10;
                      boolean l = a > 5 || (b <= 10 && c == 9);
                      boolean m = !(!k);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1 + 2 * 3;
                      int b = 1 * 2 + 3;
                      int c = 1 * 2 + 3 * 4;
                      int d = 7 - (5 - 2);
                      int e = -(-1);

                      boolean k = a > 5 && b <= 10;
                      boolean l = a > 5 || b <= 10 && c == 9;
                      boolean m = !!k;
                  }
              }
              """
          )
        );
    }

    @Test
    void binaryOperatorsSamePrecedence() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = (1 + 2) + 3;
                      int b = (1 * 2) * 3;
                      int c = (1 - 2) + 3;
                      int d = (1 / 2) * 3;

                      boolean e = (a > b && c > d) && a != d;
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1 + 2 + 3;
                      int b = 1 * 2 * 3;
                      int c = 1 - 2 + 3;
                      int d = 1 / 2 * 3;

                      boolean e = a > b && c > d && a != d;
                  }
              }
              """
          )
        );
    }

    @Test
    void unaryOperatorsWithBinaries() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      boolean a = (!true) && false;
                      int b = (-1) + 2;
                      int c = (~1) & 2;
                      int d = (++c) + 1;
                      boolean e = !(a && b > 0);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      boolean a = !true && false;
                      int b = -1 + 2;
                      int c = ~1 & 2;
                      int d = ++c + 1;
                      boolean e = !(a && b > 0);
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryConditions() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 1;
                      int b = 2;
                      int c = a > b ? (a + b) : (a - b);
                      int d = a > b ? (a > 0 ? a : 0) : b;
                      boolean e = (a > b || a < 0) ? true : false;
                      int f = (-a) > 0 ? (-a) : a;
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1;
                      int b = 2;
                      int c = a > b ? a + b : a - b;
                      int d = a > b ? a > 0 ? a : 0 : b;
                      boolean e = a > b || a < 0 ? true : false;
                      int f = -a > 0 ? -a : a;
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceofExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                  void method(Object obj) {
                      boolean a = (obj instanceof String) && obj != null;
                      boolean b = obj != null && (obj instanceof String);
                      boolean c = (!obj) instanceof String;
                      boolean d = (obj instanceof String) ? true : false;
                  }
              }
              """,
            """
              class Test {
                  void method(Object obj) {
                      boolean a = obj instanceof String && obj != null;
                      boolean b = obj != null && obj instanceof String;
                      boolean c = !obj instanceof String;
                      boolean d = obj instanceof String ? true : false;
                  }
              }
              """
          )
        );
    }

    @Test
    void stringConcatenationWithAddition() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      String s = "Value: " + (1 + 2);
                      String t = "Value: " + 1 + 2;
                      String u = 1 + 2 + " is the result";
                      String v = 1 + (2 + " is the result");

                      // String concatenation with mixed types
                      String a = "Calculation: " + (1.5 * 2);
                      String b = ("Item " + 1) + ": detail";
                      String c = "Item " + (1 + ": detail");

                      // String concatenation in conditions
                      boolean test1 = ("a" + "b").equals("ab");
                      boolean test2 = "a" + "b" == "ab";
                      boolean test3 = "x" + 1 == "x" + 1;

                      // With method calls
                      String m1 = "Length: " + (String.valueOf(123).length());
                      String m2 = String.valueOf(1 + 2) + "0";
                      String m3 = String.valueOf(1) + 2 + "0";
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      String s = "Value: " + (1 + 2);
                      String t = "Value: " + 1 + 2;
                      String u = 1 + 2 + " is the result";
                      String v = 1 + (2 + " is the result");

                      // String concatenation with mixed types
                      String a = "Calculation: " + 1.5 * 2;
                      String b = "Item " + 1 + ": detail";
                      String c = "Item " + 1 + ": detail";

                      // String concatenation in conditions
                      boolean test1 = ("a" + "b").equals("ab");
                      boolean test2 = "a" + "b" == "ab";
                      boolean test3 = "x" + 1 == "x" + 1;

                      // With method calls
                      String m1 = "Length: " + String.valueOf(123).length();
                      String m2 = String.valueOf(1 + 2) + "0";
                      String m3 = String.valueOf(1) + 2 + "0";
                  }
              }
              """
          )
        );
    }

    @Test
    void assignmentExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a, b, c;
                      a = b = c = 0;
                      int d = (a = 1) + 1;
                      boolean e = (b = 2) > 0;
                      c = a > b ? (a = 3) : (b = 4);
                      int f = (a += 2);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a, b, c;
                      a = b = c = 0;
                      int d = (a = 1) + 1;
                      boolean e = (b = 2) > 0;
                      c = a > b ? (a = 3) : (b = 4);
                      int f = a += 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void complexExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 1, b = 2, c = 3;
                      int d = (a + (b * c)) - (a / b);
                      boolean e = (a > b && b > c) || (a < c && c < 10);
                      int f = ((a++) + (++b)) + (c--);
                      int g = a > b ? (a > c ? a : c) : (b > c ? b : c);
                      boolean h = a < b ? (a < c && b < c) : (a > c || b > c);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1, b = 2, c = 3;
                      int d = a + b * c - a / b;
                      boolean e = a > b && b > c || a < c && c < 10;
                      int f = a++ + ++b + c--;
                      int g = a > b ? a > c ? a : c : b > c ? b : c;
                      boolean h = a < b ? a < c && b < c : a > c || b > c;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodInvocationArgsAndNestedExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 1, b = 2;
                      System.out.println((a + (b * 3)));
                      System.out.println((a > b && a < 10));
                      System.out.println((a > b ? a : b));
                      System.out.println((a += b));
                      doSomething((a > b && a < 10) ? (a++) : (b--));
                  }

                  void doSomething(int x) {}
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1, b = 2;
                      System.out.println(a + b * 3);
                      System.out.println(a > b && a < 10);
                      System.out.println(a > b ? a : b);
                      System.out.println(a += b);
                      doSomething(a > b && a < 10 ? a++ : b--);
                  }

                  void doSomething(int x) {}
              }
              """
          )
        );
    }

    @Test
    void bitOperations() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 1, b = 2, c = 3;
                      int d = (a & b) | c;
                      int e = a | (b & c);
                      int f = (a ^ (b & c)) | a;
                      int g = (~a) & b;
                      int h = a << (1 + b);
                      int i = (a + b) >> c;
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1, b = 2, c = 3;
                      int d = a & b | c;
                      int e = a | b & c;
                      int f = a ^ b & c | a;
                      int g = ~a & b;
                      int h = a << 1 + b;
                      int i = a + b >> c;
                  }
              }
              """
          )
        );
    }

    @Test
    void cast() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = ((Integer) 1).intValue();
                      int b = (((Integer) 1).intValue());
                      int c = ((Integer) 1) + 2;
                      int d = ~((Integer) 1);

                      boolean e = ((Integer) 1) instanceof Integer;
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = ((Integer) 1).intValue();
                      int b = ((Integer) 1).intValue();
                      int c = ((Integer) 1) + 2;
                      int d = ~((Integer) 1);

                      boolean e = ((Integer) 1) instanceof Integer;
                  }
              }
              """
          )
        );
    }

    @Test
    void alreadyParenthesized() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ParenthesizeVisitor()))
            .expectedCyclesThatMakeChanges(0),
          java(
            """
              class Test {
                  void method() {
                      int a = 1, b = 2, c = 3;
                      int d = (a + b) * c;
                      boolean e = (a > b) && (c > a);
                      int f = (a + b) * (c - a);
                      boolean g = !(a > b);
                      int h = (a > b) ? (a + c) : (b - c);
                  }
              }
              """
          )
        );
    }

    @Test
    void mixedOperatorTypes() {
        rewriteRun(
          java(
            """
              class Test {
                  void method() {
                      int a = 1, b = 2;
                      boolean c = ((a + b) > (a * b)) && ((a - b) < (a / b));
                      boolean d = ((a + b) > 0) || (((a * b) > 0) && ((a / b) > 0));
                      boolean e = ((a < b) && ((a + b) > 0)) ? ((a > 0) || (b > 0)) : ((a < 0) && (b < 0));
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1, b = 2;
                      boolean c = a + b > a * b && a - b < a / b;
                      boolean d = a + b > 0 || a * b > 0 && a / b > 0;
                      boolean e = a < b && a + b > 0 ? a > 0 || b > 0 : a < 0 && b < 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void maybeParenthesize() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<ExecutionContext>() {
              @Override
              public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
                  return ParenthesizeVisitor.maybeParenthesize((Expression) parens.getTree(), getCursor()).withPrefix(parens.getPrefix());
              }
          })),
          java(
            """
              class Test {
                  void method() {
                      int a = 1;
                      int b = 2;
                      int c = 3;

                      // Binary operations
                      int result1 = a + (b * c);
                      int result2 = (a + b) * c;

                      // Non-associative operations
                      int result3 = a - (b - c);

                      // Unary context
                      boolean result4 = !(a > b);

                      // Method invocation context
                      String s = ((String) "value").substring(0);

                      // Ternary expression
                      int result5 = (a > b) ? c : 0;

                      // Assignment inside other expressions
                      int result6 = (a = b) + c;

                      // Switch expression
                      int result7 = (switch(a) { default -> "one";}).length();
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      int a = 1;
                      int b = 2;
                      int c = 3;

                      // Binary operations
                      int result1 = a + b * c;
                      int result2 = (a + b) * c;

                      // Non-associative operations
                      int result3 = a - (b - c);

                      // Unary context
                      boolean result4 = !(a > b);

                      // Method invocation context
                      String s = ((String) "value").substring(0);

                      // Ternary expression
                      int result5 = a > b ? c : 0;

                      // Assignment inside other expressions
                      int result6 = (a = b) + c;

                      // Switch expression
                      int result7 = (switch(a) { default -> "one";}).length();
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("SameParameterValue")
    private SourceSpecs javaIdenticalTo(@Language("java") String source) {
        return java(source, source);
    }

    static class Reparenthesize extends JavaVisitor<ExecutionContext> {
        @Override
        public @Nullable J postVisit(J tree, ExecutionContext ctx) {
            return tree instanceof JavaSourceFile ? new ParenthesizeVisitor<>().visit(tree, ctx) : super.postVisit(tree, ctx);
        }

        @Override
        public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
            J j = super.visitParentheses(parens, ctx);
            return j instanceof J.Parentheses<?> p ? p.getTree().withPrefix(p.getPrefix()) : j;
        }
    }
}
