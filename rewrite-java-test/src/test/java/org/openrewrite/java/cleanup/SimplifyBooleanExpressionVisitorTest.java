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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
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
                      boolean a1 = 1 != 1;
                      boolean a2 = 1 == 1;
                      boolean a3 = 1 >= 1;
                      boolean a4 = 1 > 1;
                      boolean a5 = 1 <= 1;
                      boolean a6 = 1 < 1;
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
    void negatedTernary() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean m1(boolean a) {
                      return !(a ? true : false);
                  }
                  boolean m2(boolean a) {
                      return !(!a ? true : false);
                  }
                  boolean m3(boolean a) {
                      return !a ? true : false;
                  }
                  boolean m4(boolean a) {
                      return a ? true : false;
                  }
              }
              """,
            """
              public class A {
                  boolean m1(boolean a) {
                      return a ? false : true;
                  }
                  boolean m2(boolean a) {
                      return a ? true : false;
                  }
                  boolean m3(boolean a) {
                      return a ? false : true;
                  }
                  boolean m4(boolean a) {
                      return a ? true : false;
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
            rewriteRun(java(beforeJava));
        } else {
            rewriteRun(java(beforeJava, template.formatted(after)));
        }
    }
}
