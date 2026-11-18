/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.groovy.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class SimplifyBooleanExpressionVisitorTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"\"foo\" == null", "null == \"foo\"", "1 == 2", "3 == 4",
      "!(1 == 2)", "(1 == 2) || (3 == 4)", "!false", "\"foo\".equals(\"bar\")"})
    void preserveAssertDiagnosticInput(String condition) {
        rewriteRun(
          groovy(
            """
              class Conditions {
                  void check() {
                      assert %1$s
                      assert %1$s : "diagnostic message"
                  }
              }
              """.formatted(condition)
          )
        );
    }

    @Test
    void preserveNestedNegationPrecedence() {
        rewriteRun(
          groovy(
            """
              class Test {
                  boolean same(String left, String right) {
                      return !!(left == right)
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveNonBooleanDuplicateOperands() {
        rewriteRun(
          groovy(
            """
              class Test {
                  def nullOr() { return null || null }
                  def nullAnd() { return null && null }
                  def stringOr(String value) { return value || value }
                  def stringAnd(String value) { return value && value }
              }
              """
          )
        );
    }

    @Test
    void stringLiteralEqualsAsStatement() {
        rewriteRun(groovy("class A{void f(){\"foo\".equals(\"bar\");}}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"foo\".equals(\"bar\")", "\"\".isEmpty()", "value.equals(value)"})
    void preserveStringCallsInStatementPositions(String invocation) {
        rewriteRun(groovy(
          """
            class A {
                void f(String value, boolean flag, String[] values) {
                    %1$s;
                    if (flag) %1$s; else %1$s;
                    while (flag) %1$s;
                    do %1$s while (flag);
                    for (; flag;) %1$s;
                    for (String item : values) %1$s;
                    for (%1$s; flag; %1$s) {}
                    label: %1$s;
                    switch (value) { case "foo": %1$s; break; }
                }
            }
            """.formatted(invocation)
        ));
    }

    @Test
    void foldStringExpressionsInsideStatementArguments() {
        rewriteRun(groovy(
          """
            class A {
                void accept(boolean a, boolean b) {}
                boolean f() {
                    accept("foo".equals("bar"), "".isEmpty())
                    return "foo".equals("bar")
                }
            }
            """,
          """
            class A {
                void accept(boolean a, boolean b) {}
                boolean f() {
                    accept(false, true)
                    return false
                }
            }
            """
        ));
    }

    @ParameterizedTest
    @ValueSource(strings = {"next().equals(next())"})
    void preserveStringSelfEqualsEvaluation(String expression) {
        rewriteRun(groovy(
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

    @DocumentExample
    @Test
    void simplifyEqualsLiteralTrueIf() {
        rewriteRun(
          groovy(
            """
              class A {
                  boolean a
                  def m() {
                      if (true == a) {
                      }
                  }
              }
              """,
            """
              class A {
                  boolean a
                  def m() {
                      if (a) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyBooleanExpressionComprehensive() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean a = !false
                      boolean b = (a == true)
                      boolean c = b || true
                      boolean d = c || c
                      boolean e = d && d
                      boolean f = (e == true) || e
                      boolean g = f && false
                      boolean h = g
                      boolean i = (a != false)
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean a = true
                      boolean b = a
                      boolean c = true
                      boolean d = c
                      boolean e = d
                      boolean f = e
                      boolean g = false
                      boolean h = g
                      boolean i = a
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyInvertedBooleanLiteral() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean a = !false
                      boolean b = !true
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean a = true
                      boolean b = false
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveSpockDoubleNotOptOut() {
        rewriteRun(
          groovy(
            """
              class Conditions {
                  void check(String foo, String bar, Boolean boxed) {
                      assert !!(foo == bar)
                      assert !!(foo != bar)
                      assert !!(foo.length() > bar.length())
                      assert !!boxed
                      assert !!true
                      assert !!(foo == bar) : "foobar"
                      assert !!(foo.isEmpty() || bar.isEmpty())
                      assert !!(foo.length() == bar.length())
                  }
              }
              """
          )
        );
    }

    @Test
    void preserveDoubleNotByLanguageRatherThanFileExtension() {
        rewriteRun(
          groovy(
            """
              assert !!(1 == 2)
              boolean simplified = !false
              """,
            """
              assert !!(1 == 2)
              boolean simplified = true
              """,
            spec -> spec.path("conditions.java")
          )
        );
    }

    @Test
    void doubleNegationWithParentheses() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean a = !!(!(!true))
                      boolean b = !(!a)
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleNegationPreservedForTruthinessCoercion() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m(String s) {
                      boolean a = !!s
                      boolean b = !(!s)
                  }
              }
              """
          )
        );
    }

    @Test
    void doubleNegatedBinaryWithParentheses() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean a1 = !(1 == 1)
                      boolean a2 = !(1 != 1)
                      boolean a3 = !(1 < 1)
                      boolean a4 = !(1 <= 1)
                      boolean a5 = !(1 > 1)
                      boolean a6 = !(1 >= 1)
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean a1 = false
                      boolean a2 = true
                      boolean a3 = true
                      boolean a4 = false
                      boolean a5 = true
                      boolean a6 = false
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyEqualsLiteralTrue() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean a = true
                      boolean b = (a == true)
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean a = true
                      boolean b = a
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyOrLiteralTrue() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean b = true
                      boolean c = b || true
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean b = true
                      boolean c = true
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyOrAlwaysTrue() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean c = true
                      boolean d = c || c
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean c = true
                      boolean d = c
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyAndAlwaysTrue() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean d = true
                      boolean e = d && d
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean d = true
                      boolean e = d
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyEqualsLiteralTrueAlwaysTrue() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean e = true
                      boolean f = (e == true) || e
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean e = true
                      boolean f = e
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyLiteralFalseAlwaysFalse() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean f = true
                      boolean g = f && false
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean f = true
                      boolean g = false
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyDoubleNegation() {
        rewriteRun(
          groovy(
            """
              class A {
                  def doubleNegation(boolean g) {
                      boolean h = g
                  }
              }
              """
          )
        );
    }

    @Test
    void simplifyNotEqualsFalse() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      boolean a = true
                      boolean i = (a != false)
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean a = true
                      boolean i = a
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
          groovy(
            """
              class A {
                  def m() {
                      boolean a=true
                      boolean i=a!=true
                  }
              }
              """,
            """
              class A {
                  def m() {
                      boolean a=true
                      boolean i=!a
                  }
              }
              """
          )
        );
    }

    @Test
    void binaryOrBothFalse() {
        rewriteRun(
          groovy(
            """
              class A {
                  def m() {
                      if (!true || !true) {
                          System.out.println("")
                      }
                  }
              }
              """,
            """
              class A {
                  def m() {
                      if (false) {
                          System.out.println("")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void mapFields() {
        rewriteRun(
          groovy(
            """
              class A {
                  def foo(boolean a, Map m) {
                      if (a || m.foo || m.bar) {
                          System.out.println("")
                      }
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

      a == null || a.isEmpty() || "" == null || "".isEmpty()   // true
      a == null || a.isEmpty() || "" == null || !"".isEmpty()  // a == null || a.isEmpty()
      a == null || a.isEmpty() || "" != null && "".isEmpty()   // true
      a == null || a.isEmpty() || "" != null && !"".isEmpty()  // a == null || a.isEmpty()
      a == null || a.isEmpty() && "" == null || "".isEmpty()   // true
      a == null || a.isEmpty() && "" == null || !"".isEmpty()  // a == null
      a == null || a.isEmpty() && "" != null && "".isEmpty()   // a == null || a.isEmpty()
      a == null || a.isEmpty() && "" != null && !"".isEmpty()  // a == null
      a == null || !a.isEmpty() || "" == null || "".isEmpty()  // true
      a == null || !a.isEmpty() || "" == null || !"".isEmpty() // a == null || !a.isEmpty()
      a == null || !a.isEmpty() || "" != null && "".isEmpty()  // true
      a == null || !a.isEmpty() || "" != null && !"".isEmpty() // a == null || !a.isEmpty()
      a == null || !a.isEmpty() && "" == null || "".isEmpty()  // true
      a == null || !a.isEmpty() && "" == null || !"".isEmpty() // a == null
      a == null || !a.isEmpty() && "" != null && "".isEmpty()  // a == null || !a.isEmpty()
      a == null || !a.isEmpty() && "" != null && !"".isEmpty() // a == null

      a == null || a.isEmpty() || "b" == null || "b".isEmpty()   // a == null || a.isEmpty()
      a == null || a.isEmpty() || "b" == null || !"b".isEmpty()  // true
      a == null || a.isEmpty() || "b" != null && "b".isEmpty()   // a == null || a.isEmpty()
      a == null || a.isEmpty() || "b" != null && !"b".isEmpty()  // true
      a == null || a.isEmpty() && "b" == null || "b".isEmpty()   // a == null
      a == null || a.isEmpty() && "b" == null || !"b".isEmpty()  // true
      a == null || a.isEmpty() && "b" != null && "b".isEmpty()   // a == null
      a == null || a.isEmpty() && "b" != null && !"b".isEmpty()  // a == null || a.isEmpty()
      a == null || !a.isEmpty() || "b" == null || "b".isEmpty()  // a == null || !a.isEmpty()
      a == null || !a.isEmpty() || "b" == null || !"b".isEmpty() // true
      a == null || !a.isEmpty() || "b" != null && "b".isEmpty()  // a == null || !a.isEmpty()
      a == null || !a.isEmpty() || "b" != null && !"b".isEmpty() // true
      a == null || !a.isEmpty() && "b" == null || "b".isEmpty()  // a == null
      a == null || !a.isEmpty() && "b" == null || !"b".isEmpty() // true
      a == null || !a.isEmpty() && "b" != null && "b".isEmpty()  // a == null
      a == null || !a.isEmpty() && "b" != null && !"b".isEmpty() // a == null || !a.isEmpty()
      """)
    @Issue("https://github.com/openrewrite/rewrite-templating/issues/28")
    // Mimic what would be inserted by a Refaster template using two nullable parameters, with the second one a literal
    @ParameterizedTest
    void simplifyLiteralNull(String before, String after) {
        String template = """
          class A {
              void foo(String a) {
                  boolean c = %s;
              }
          }
          """;
        String beforeJava = template.formatted(before);
        if (before.equals(after)) {
            rewriteRun(groovy(beforeJava));
        } else {
            rewriteRun(groovy(beforeJava, template.formatted(after)));
        }
    }
}
