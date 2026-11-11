/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class SimplifyBooleanExpressionVisitorTest implements RewriteTest {

    @Test
    void preserveNestedNegationPrecedence() {
        rewriteRun(
          java(
            """
              class Test {
                  boolean same(String left, String right) {
                      return !!(left == right);
                  }
              }
              """,
            """
              class Test {
                  boolean same(String left, String right) {
                      return left == right;
                  }
              }
              """
          )
        );
    }

    @Test
    void stringLiteralEqualsAsStatement() {
        rewriteRun(java("class A{void f(){\"foo\".equals(\"bar\");}}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"foo\".equals(\"bar\")", "\"\".isEmpty()", "value.equals(value)"})
    void preserveStringCallsInStatementPositions(String invocation) {
        rewriteRun(java(
          """
            class A {
                void f(String value, boolean flag, String[] values) {
                    %1$s;
                    if (flag) %1$s; else %1$s;
                    while (flag) %1$s;
                    do %1$s; while (flag);
                    for (; flag;) %1$s;
                    for (String item : values) %1$s;
                    for (%1$s; flag; %1$s) {}
                    label: %1$s;
                    switch (value) { case "foo": %1$s; break; }
                }
            }
            """.formatted(invocation),
          spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
              @Override
              public J.Block visitBlock(J.Block block, Integer p) {
                  assertThat((List<?>) block.getStatements())
                    .allMatch(org.openrewrite.java.tree.Statement.class::isInstance);
                  return super.visitBlock(block, p);
              }
          }.visit(cu, 0))
        ));
    }

    @Test
    void foldStringExpressionsInsideStatementArguments() {
        rewriteRun(java(
          """
            class A {
                void accept(boolean a, boolean b) {}
                boolean f(boolean flag) {
                    accept("foo".equals("bar"), "".isEmpty());
                    if (flag) accept("foo".equals("foo"), "foo".isEmpty());
                    for (; "".isEmpty(); accept("foo".equals("bar"), "".isEmpty())) {}
                    return "foo".equals("bar");
                }
            }
            """,
          """
            class A {
                void accept(boolean a, boolean b) {}
                boolean f(boolean flag) {
                    accept(false, true);
                    if (flag) accept(true, false);
                    for (; true; accept(false, true)) {}
                    return false;
                }
            }
            """
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"value.equals(value)", "next().equals(next())", "holder.value.equals(holder.value)", "values[index].equals(values[index])"})
    void preserveStringSelfEqualsEvaluation(String expression) {
        rewriteRun(java(
          """
            class A {
                String value;
                String next() { return value; }
                boolean f(String value, A holder, String[] values, int index) {
                    return %s;
                }
            }
            """.formatted(expression)
        ));
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new SimplifyBooleanExpressionVisitor()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"call() && false", "call() || true", "holder.flag && false",
      "values[index] || true", "(1 / divisor > 0) && false", "(flag = true) && false",
      "(call()) && (call())", "(call()) || (call())"})
    void preservePrimitiveOperandEvaluation(String expression) {
        rewriteRun(java("class A { boolean flag; int calls; boolean call() { calls++; return true; } " +
          "boolean f(A holder, boolean[] values, int index, int divisor) { return " + expression + "; } }"));
    }

    @Test
    void preserveShortCircuitedPatternBindings() {
        rewriteRun(java(
          """
            class A {
                void f(Object value) {
                    if (false && value instanceof String text) {
                        System.out.println(text.length());
                    }
                    if (true || !(value instanceof String text)) {
                    } else {
                        System.out.println(text.length());
                    }
                }
            }
            """
        ));
    }

    @Test
    void preserveVolatileBooleanRead() {
        rewriteRun(java("class A { volatile boolean flag; boolean f() { return flag && false; } }"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"value == true", "value != false", "!!value", "true && value", "value && true",
      "false || value", "value || false", "value && false", "value || true", "value && value", "value || value",
      "true ? value : false", "false ? false : value"})
    void preserveBoxedBooleanUnboxing(String expression) {
        rewriteRun(java("class A { Object f(Boolean value) { return " + expression + "; } }"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"value == true", "value != false", "!!value", "true && value", "value && true",
      "false || value", "value || false", "value && false", "value || true", "value && value", "value || value"})
    void preserveBooleanBoundUnboxing(String expression) {
        rewriteRun(java("class A { <T extends Boolean> Object f(T value) { return " + expression + "; } }"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true ? value : false", "false ? false : value"})
    void simplifyBooleanBoundReferenceConditional(String expression) {
        rewriteRun(java(
          "class A { <T extends Boolean> Object f(T value) { return " + expression + "; } }",
          "class A { <T extends Boolean> Object f(T value) { return value; } }"
        ));
    }

    @Test
    void preserveBooleanBoundOverloadSelection() {
        rewriteRun(java(
          """
            class A {
                <T extends Boolean> String f(T value) { return select(value == true); }
                String select(boolean value) { return "primitive"; }
                String select(Boolean value) { return "boxed"; }
            }
            """
        ));
    }

    @Test
    void preservePrimitiveOverloadSelection() {
        rewriteRun(java(
          """
            class A {
                String f(Boolean value) { return select(value == true); }
                String select(boolean value) { return "primitive"; }
                String select(Boolean value) { return "boxed"; }
            }
            """
        ));
    }

    @ParameterizedTest
    @CsvSource({"double,<", "double,>", "double,<=", "double,>=", "float,<", "Float,>=", "Double,>"})
    void preserveNegatedFloatingComparisons(String type, String operator) {
        rewriteRun(java("class A { boolean f(%s x, %s y) { return !(x %s y); } }"
          .formatted(type, type, operator)));
    }

    @Test
    void preserveMixedFloatingComparison() {
        rewriteRun(java("class A { boolean f(int x, double y) { return !(x < y); } }"));
    }

    @ParameterizedTest
    @CsvSource({"int,<,>=", "Integer,>,<=", "long,<=,>", "Long,>=,<", "char,<,>=", "Byte,>,<="})
    void stillComplementsIntegralComparisons(String type, String operator, String complement) {
        rewriteRun(java(
          "class A { boolean f(%s x, %s y) { return !(x %s y); } }".formatted(type, type, operator),
          "class A { boolean f(%s x, %s y) { return x %s y; } }".formatted(type, type, complement)
        ));
    }

    @Test
    void preserveFloatingComparisonsInNegatedTernary() {
        rewriteRun(java(
          "class A { boolean f(boolean b, double x, double y) { return !(b ? x < y : x >= y); } }",
          "class A { boolean f(boolean b, double x, double y) { return b ? !(x < y) : !(x >= y); } }"
        ));
    }

    @DocumentExample
    @Test
    void simplifyEqualsLiteralTrueIf() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean a;
                  {
                      if (true == a) {}
                      if (a == true) {}
                  }
              }
              """,
            """
              public class A {
                  boolean a;
                  {
                      if (a) {}
                      if (a) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void skipMissingTypeAttribution() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().identifiers(false).build()),
          java(
            """
              public class A {
                  boolean a = null instanceof Unknown || null instanceof java.lang.Unknown;
              }
              """
          )
        );
    }

    @Test
    void simplifyEqualsLiteralFalseIf() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean a;
                  boolean b;
                  {
                      if (false == a) {}
                      if (a == false) {}
                      if ((a && b) == false) {}
                      if (false == (a && b)) {}
                  }
              }
              """,
            """
              public class A {
                  boolean a;
                  boolean b;
                  {
                      if (!a) {}
                      if (!a) {}
                      if (!(a && b)) {}
                      if (!(a && b)) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyBooleanExpressionComprehensive() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean a = !false;
                      boolean b = (a == true);
                      boolean c = b || true;
                      boolean d = c || c;
                      boolean e = d && d;
                      boolean f = (e == true) || e;
                      boolean g = f && false;
                      boolean h = g;
                      boolean i = a == false;
                      boolean j = a != false;
                      boolean k = a != true;
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a = true;
                      boolean b = a;
                      boolean c = true;
                      boolean d = c;
                      boolean e = d;
                      boolean f = e;
                      boolean g = false;
                      boolean h = g;
                      boolean i = !a;
                      boolean j = a;
                      boolean k = !a;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyInvertedBooleanLiteral() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean a = !false;
                      boolean b = !true;
                      boolean c = !(false);
                      boolean d = !(true);
                      boolean e = !((false));
                      boolean f = !((true));
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a = true;
                      boolean b = false;
                      boolean c = true;
                      boolean d = false;
                      boolean e = true;
                      boolean f = false;
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleNegationWithParentheses() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean a = !!(!(!true));
                      boolean b = !(!a);
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a = true;
                      boolean b = a;
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleNegatedBinaryWithParentheses() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean a1 = !(1 == 1);
                      boolean a2 = !(1 != 1);
                      boolean a3 = !(1 < 1);
                      boolean a4 = !(1 <= 1);
                      boolean a5 = !(1 > 1);
                      boolean a6 = !(1 >= 1);
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a1 = false;
                      boolean a2 = true;
                      boolean a3 = true;
                      boolean a4 = false;
                      boolean a5 = true;
                      boolean a6 = false;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyEqualsLiteralTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean a = true;
                      boolean b = (a == true);
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a = true;
                      boolean b = a;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyOrLiteralTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean b = true;
                      boolean c = b || true;
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean b = true;
                      boolean c = true;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyOrAlwaysTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean c = true;
                      boolean d = c || c;
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean c = true;
                      boolean d = c;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyAndAlwaysTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean d = true;
                      boolean e = d && d;
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean d = true;
                      boolean e = d;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyEqualsLiteralTrueAlwaysTrue() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean e = true;
                      boolean f = (e == true) || e;
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean e = true;
                      boolean f = e;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralFalseAlwaysFalse() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean f = true;
                      boolean g = f && false;
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean f = true;
                      boolean g = false;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyDoubleNegation() {
        rewriteRun(
          java(
            """
              public class A {
                  public void doubleNegation(boolean g) {
                      boolean h = g;
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyNotEqualsFalse() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean a = true;
                      boolean i = (a != false);
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a = true;
                      boolean i = a;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/502")
    @Test
    void autoFormatIsConditionallyApplied() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      boolean a=true;
                      boolean i=a!=true;
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a=true;
                      boolean i=!a;
                  }
              }
              """
          )
        );
    }

    @Test
    void binaryOrBothFalse() {
        rewriteRun(
          java(
            """
              public class A {
                  {
                      if (!true || !true) {
                          System.out.println("");
                      }
                  }
              }
              """,
            """
              public class A {
                  {
                      if (false) {
                          System.out.println("");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryConstant() {
        rewriteRun(
          java(
            """
              class A {
                  String alwaysA() {
                      return true ? "a" : "b";
                  }
                  String alwaysA2() {
                      return (true) ? "a" : "b";
                  }
                  String alwaysA3() {
                      return (true ? "a" : "b");
                  }
                  String alwaysA4() {
                      return !false ? "a" : "b";
                  }
                  String alwaysA5() {
                      return (!false) ? "a" : "b";
                  }
                  String alwaysA6() {
                      return !(false) ? "a" : "b";
                  }
                  String alwaysB() {
                      return false ? "a" : "b";
                  }
              }
              """,
            """
              class A {
                  String alwaysA() {
                      return "a";
                  }
                  String alwaysA2() {
                      return "a";
                  }
                  String alwaysA3() {
                      return "a";
                  }
                  String alwaysA4() {
                      return "a";
                  }
                  String alwaysA5() {
                      return "a";
                  }
                  String alwaysA6() {
                      return "a";
                  }
                  String alwaysB() {
                      return "b";
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryDoubleNegation() {
        rewriteRun(
          java(
            """
              class A {
                  boolean orElse(Boolean nullable, boolean nonnull) {
                      return !(nullable != null ? nullable : nonnull);
                  }
              }
              """,
            """
              class A {
                  boolean orElse(Boolean nullable, boolean nonnull) {
                      return nullable != null ? !nullable : !nonnull;
                  }
              }
              """
          )
        );
    }

    @Test
    void ternayNegation() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean m1(boolean a, boolean b, boolean c) {
                      return !(a ? b : c);
                  }
                  boolean m2(boolean a, boolean b, boolean c) {
                      return !(!a ? b : c);
                  }
                  boolean m3(boolean a, boolean b, boolean c) {
                      return !a ? b : c;
                  }
                  boolean m4(boolean a, boolean b, boolean c) {
                      return a ? b : c;
                  }
              }
              """,
            """
              public class A {
                  boolean m1(boolean a, boolean b, boolean c) {
                      return a ? !b : !c;
                  }
                  boolean m2(boolean a, boolean b, boolean c) {
                      return a ? !c : !b;
                  }
                  boolean m3(boolean a, boolean b, boolean c) {
                      return a ? c : b;
                  }
                  boolean m4(boolean a, boolean b, boolean c) {
                      return a ? b : c;
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedTernaryNegation() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean m1(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return !(a ? b ? x : y : z);
                  }
                  boolean m2(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return !(a ? x : b ? y : z);
                  }
                  boolean m3(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return !(a ? !b ? x : y : z);
                  }
                  boolean m4(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return a ? b ? x : y : z;
                  }
              }
              """,
            """
              public class A {
                  boolean m1(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return a ? b ? !x : !y : !z;
                  }
                  boolean m2(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return a ? !x : b ? !y : !z;
                  }
                  boolean m3(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return a ? b ? !y : !x : !z;
                  }
                  boolean m4(boolean a, boolean b, boolean x, boolean y, boolean z) {
                      return a ? b ? x : y : z;
                  }
              }
              """
          )
        );
    }

    @Test
    void negatedTernaryBranchWrappedInParentheses() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean m1(boolean a, Object o, boolean c) {
                      return !(a ? o instanceof String : c);
                  }
              }
              """,
            """
              public class A {
                  boolean m1(boolean a, Object o, boolean c) {
                      return a ? !(o instanceof String) : !c;
                  }
              }
              """
          )
        );
    }

    @Test
    void negatedTernaryAsOperandKeepsParentheses() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean m1(boolean a, boolean b, boolean c, boolean d) {
                      return !(a ? b : c) && d;
                  }
                  boolean m2(boolean a, boolean b, boolean c, boolean d) {
                      return !(a ? b : c) ? c : d;
                  }
                  boolean m3(boolean a, boolean b, boolean c, boolean d) {
                      return d ? !(a ? b : c) : d;
                  }
                  boolean m4(boolean a, boolean b, boolean c) {
                      return !!(a ? b : c);
                  }
              }
              """,
            """
              public class A {
                  boolean m1(boolean a, boolean b, boolean c, boolean d) {
                      return (a ? !b : !c) && d;
                  }
                  boolean m2(boolean a, boolean b, boolean c, boolean d) {
                      return (a ? !b : !c) ? c : d;
                  }
                  boolean m3(boolean a, boolean b, boolean c, boolean d) {
                      return d ? a ? !b : !c : d;
                  }
                  boolean m4(boolean a, boolean b, boolean c) {
                      return a ? b : c;
                  }
              }
              """
          )
        );
    }

    @Test
    void differentFieldAccesses() {
        rewriteRun(
          java(
            """
              public class A {
                  Object f = null;
                  class B extends A {
                      boolean m(Object o) {
                          B other = (B) o;
                          if (this.f == null || other.f == null) {
                              return true;
                          }
                          return false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveComments() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean m(boolean a) {
                      if (/*a*/!!a) {
                          return true;
                      }
                      return /*a*/!true || !true;
                  }
              }
              """,
            """
              public class A {
                  boolean m(boolean a) {
                      if (/*a*/a) {
                          return true;
                      }
                      return /*a*/false;
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleMethodInvocationNotSimplified() {
        rewriteRun(
          java(
            """
              public class A {
                  void foo() {
                      boolean a = booleanExpression() || booleanExpression();
                      boolean b = !(booleanExpression() && booleanExpression());
                      boolean c = booleanExpression() == booleanExpression();
                      boolean d = booleanExpression() != booleanExpression();
                  }
                  boolean booleanExpression() {
                    return true;
                  }
              }
              """
          )
        );
    }

    @CsvSource(delimiterString = "//", textBlock = """
      a == null || a.isEmpty()                                 // a == null || a.isEmpty()
      a == null || !a.isEmpty()                                // a == null || !a.isEmpty()
      a != null && a.isEmpty()                                 // a != null && a.isEmpty()
      a != null && !a.isEmpty()                                // a != null && !a.isEmpty()

      "" == null || "".isEmpty()                               // true
      "" == null || !"".isEmpty()                              // false
      "" != null && "".isEmpty()                               // true
      "" != null && !"".isEmpty()                              // false

      "b" == null || "b".isEmpty()                             // false
      "b" == null || !"b".isEmpty()                            // true
      "b" != null && "b".isEmpty()                             // false
      "b" != null && !"b".isEmpty()                            // true

      a == null || a.isEmpty() || "" == null || "".isEmpty()   // a == null || a.isEmpty() || true
      a == null || a.isEmpty() || "" == null || !"".isEmpty()  // a == null || a.isEmpty()
      a == null || a.isEmpty() || "" != null && "".isEmpty()   // a == null || a.isEmpty() || true
      a == null || a.isEmpty() || "" != null && !"".isEmpty()  // a == null || a.isEmpty()
      a == null || a.isEmpty() && "" == null || "".isEmpty()   // a == null || a.isEmpty() && false || true
      a == null || a.isEmpty() && "" == null || !"".isEmpty()  // a == null || a.isEmpty() && false
      a == null || a.isEmpty() && "" != null && "".isEmpty()   // a == null || a.isEmpty()
      a == null || a.isEmpty() && "" != null && !"".isEmpty()  // a == null || a.isEmpty() && false
      a == null || !a.isEmpty() || "" == null || "".isEmpty()  // a == null || !a.isEmpty() || true
      a == null || !a.isEmpty() || "" == null || !"".isEmpty() // a == null || !a.isEmpty()
      a == null || !a.isEmpty() || "" != null && "".isEmpty()  // a == null || !a.isEmpty() || true
      a == null || !a.isEmpty() || "" != null && !"".isEmpty() // a == null || !a.isEmpty()
      a == null || !a.isEmpty() && "" == null || "".isEmpty()  // a == null || !a.isEmpty() && false || true
      a == null || !a.isEmpty() && "" == null || !"".isEmpty() // a == null || !a.isEmpty() && false
      a == null || !a.isEmpty() && "" != null && "".isEmpty()  // a == null || !a.isEmpty()
      a == null || !a.isEmpty() && "" != null && !"".isEmpty() // a == null || !a.isEmpty() && false

      a == null || a.isEmpty() || "b" == null || "b".isEmpty()   // a == null || a.isEmpty()
      a == null || a.isEmpty() || "b" == null || !"b".isEmpty()  // a == null || a.isEmpty() || true
      a == null || a.isEmpty() || "b" != null && "b".isEmpty()   // a == null || a.isEmpty()
      a == null || a.isEmpty() || "b" != null && !"b".isEmpty()  // a == null || a.isEmpty() || true
      a == null || a.isEmpty() && "b" == null || "b".isEmpty()   // a == null || a.isEmpty() && false
      a == null || a.isEmpty() && "b" == null || !"b".isEmpty()  // a == null || a.isEmpty() && false || true
      a == null || a.isEmpty() && "b" != null && "b".isEmpty()   // a == null || a.isEmpty() && false
      a == null || a.isEmpty() && "b" != null && !"b".isEmpty()  // a == null || a.isEmpty()
      a == null || !a.isEmpty() || "b" == null || "b".isEmpty()  // a == null || !a.isEmpty()
      a == null || !a.isEmpty() || "b" == null || !"b".isEmpty() // a == null || !a.isEmpty() || true
      a == null || !a.isEmpty() || "b" != null && "b".isEmpty()  // a == null || !a.isEmpty()
      a == null || !a.isEmpty() || "b" != null && !"b".isEmpty() // a == null || !a.isEmpty() || true
      a == null || !a.isEmpty() && "b" == null || "b".isEmpty()  // a == null || !a.isEmpty() && false
      a == null || !a.isEmpty() && "b" == null || !"b".isEmpty() // a == null || !a.isEmpty() && false || true
      a == null || !a.isEmpty() && "b" != null && "b".isEmpty()  // a == null || !a.isEmpty() && false
      a == null || !a.isEmpty() && "b" != null && !"b".isEmpty() // a == null || !a.isEmpty()
      """)
    @Issue("https://github.com/openrewrite/rewrite-templating/issues/28")
    // Retain variable receiver calls conservatively, including their possible exceptions.
    // Mimic what would be inserted by a Refaster template using two nullable parameters, with the second one a literal
    @ParameterizedTest
    void simplifyLiteralNull(String before, String after) {
        //language=java
        String template = """
          class A {
              void foo(String a) {
                  boolean c = %s;
              }
          }
          """;
        String beforeJava = template.formatted(before);
        if (before.equals(after)) {
            rewriteRun(java(beforeJava));
        } else {
            rewriteRun(java(beforeJava, template.formatted(after)));
        }
    }

    @Issue("https://github.com/openrewrite/rewrite-feature-flags/issues/40")
    @Test
    void simplifyStringLiteralEqualsStringLiteral() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      String foo = "foo";
                      if ("foo".equals("foo")) {}
                      if (foo.equals(foo)) {}
                      if (foo.equals("foo")) {}
                      if ("foo".equals(foo)) {}
                      if ("foo".equals("bar")) {}
                  }
              }
              """,
            """
              class A {
                  {
                      String foo = "foo";
                      if (true) {}
                      if (foo.equals(foo)) {}
                      if (foo.equals("foo")) {}
                      if ("foo".equals(foo)) {}
                      if (false) {}
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4821")
    @Test
    void stringComparisonInBinary() {
        rewriteRun(
          java(
            """
              class A {
                  private boolean notNullAndNotEqual(String one, String other) {
                      return one != null && !one.equals(other);
                  }
              }
              """
          )
        );
    }

    @Test
    void defensiveSetter() {
        rewriteRun(
          java(
            """
              class A {
                  String a;
                  void setA(String a) {
                      if (!a.equals(this.a)) {
                          this.a = a;
                      }
                  }
              }
              """
          ),
          java(
            """
              class B {
                  String b;
                  void setB(String b) {
                      if (b.equals(this.b)) {
                          return;
                      }
                      this.b = b;
                  }
              }
              """
          )
        );
    }

    @Test
    void correctlySimplifyNegatedTernaryEqualsNull() {
        rewriteRun(
          java(
            """
              class A {
                  void doSome(String o1, String o2) {
                      if (!(o1 == null ? o2 == null : o1.equals(o2))) {
                      }
                  }
              }
              """,
            """
              class A {
                  void doSome(String o1, String o2) {
                      if (o1 == null ? o2 != null : !o1.equals(o2)) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyNumericLiteralComparisons() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      boolean a = 1 < 2;
                      boolean b = 3 == 4;
                      boolean c = 5 > 3;
                      boolean d = 2 <= 2;
                      boolean e = 10 >= 20;
                      boolean f = 7 != 7;
                      boolean g = 1.5 < 2.5;
                      boolean h = 3.0 == 3.0;
                      boolean i = 100L > 50L;
                      boolean j = 1 < 1;
                      boolean k = 5 == 5;
                  }
              }
              """,
            """
              class A {
                  {
                      boolean a = true;
                      boolean b = false;
                      boolean c = true;
                      boolean d = true;
                      boolean e = false;
                      boolean f = false;
                      boolean g = true;
                      boolean h = true;
                      boolean i = true;
                      boolean j = false;
                      boolean k = true;
                  }
              }
              """
          )
        );
    }

    @Test
    void numericLiteralComparisonsWithEdgeCases() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      // Edge cases with floating point - these are not simplified
                      // because they involve non-literal expressions (field accesses)
                      boolean a = Double.NaN == Double.NaN;
                      boolean b = Double.NaN != Double.NaN;
                      boolean c = -0.0 == 0.0;
                      boolean d = -0.0 < 0.0;
                      boolean e = Double.POSITIVE_INFINITY > 1.0;
                      boolean f = Double.NEGATIVE_INFINITY < 1.0;
                      boolean g = Float.NaN == Float.NaN;
                      boolean h = Float.POSITIVE_INFINITY > Float.NEGATIVE_INFINITY;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/6164")
    @Test
    void largeLongValuesLosePrecisionInDouble() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      // Large long values that would lose precision if cast to double
                      // Now correctly handled by comparing as long values
                      boolean i = 9007199254740992L < 9007199254740993L;
                      boolean j = 9007199254740992L == 9007199254740993L;
                  }
              }
              """,
            """
              class A {
                  {
                      // Large long values that would lose precision if cast to double
                      // Now correctly handled by comparing as long values
                      boolean i = true;
                      boolean j = false;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotSimplifyNonLiteralComparisons() {
        rewriteRun(
          java(
            """
              class A {
                  {
                      int i = 1;
                      boolean a = i == 1;
                      boolean b = i < 2;
                      boolean c = 1 == i;

                      int j = 2;
                      boolean d = i == j;
                      boolean e = i < j;

                      long l = 100L;
                      boolean f = l > 50L;
                      boolean g = l == 100L;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotFailOnNumericComparisonWhenLiteralTypeMissing() {
        rewriteRun(
          spec -> spec.recipe(new StripLiteralTypesThenSimplify())
            .expectedCyclesThatMakeChanges(1)
            .typeValidationOptions(TypeValidation.none()),
          java(
            """
              class A {
                  boolean a = 1906 >= 8521;
              }
              """
          )
        );
    }

    private static class StripLiteralTypesThenSimplify extends Recipe {
        @Override
        public String getDisplayName() {
            return "Strip numeric literal types, then simplify boolean expressions";
        }

        @Override
        public String getDescription() {
            return "Mimics a non-Java LST (e.g. an untyped Go constant) where a numeric literal carries no type.";
        }

        @Override
        public List<Recipe> getRecipeList() {
            return List.of(
              toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
                  @Override
                  public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                      if (literal.getValue() instanceof Integer && literal.getType() != null) {
                          return new J.Literal(literal.getId(), literal.getPrefix(), literal.getMarkers(),
                            literal.getValue(), literal.getValueSource(), null, null);
                      }
                      return literal;
                  }
              }),
              toRecipe(SimplifyBooleanExpressionVisitor::new)
            );
        }
    }
}
