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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ALL")
class SimplifyBooleanExpressionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SimplifyBooleanExpression());
    }

    @Test
    void simplifyEqualsLiteralTrueIf() {
        rewriteRun(
          java(
            """
              public class A {
                  boolean a;
                  {
                      if(true == a) {
                      }
                  }
              }
              """,
            """
              public class A {
                  boolean a;
                  {
                      if(a) {
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
                      boolean i = (a != false);
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
                      boolean i = a;
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
                  }
              }
              """,
            """
              public class A {
                  {
                      boolean a = true;
                      boolean b = false;
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
                      boolean i=(a!=true);
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
}
