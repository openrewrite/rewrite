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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ALL")
class SimplifyBooleanExpressionVisitorTest implements RewriteTest {

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
                      boolean a1 = 1 != 1
                      boolean a2 = 1 == 1
                      boolean a3 = 1 >= 1
                      boolean a4 = 1 > 1
                      boolean a5 = 1 <= 1
                      boolean a6 = 1 < 1
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

    @ParameterizedTest
    @Issue("https://github.com/openrewrite/rewrite-templating/issues/28")
    // Mimic what would be inserted by a Refaster template using two nullable parameters, with the second one a literal
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
            rewriteRun(groovy(beforeJava));
        } else {
            rewriteRun(groovy(beforeJava, template.formatted(after)));
        }
    }
}
