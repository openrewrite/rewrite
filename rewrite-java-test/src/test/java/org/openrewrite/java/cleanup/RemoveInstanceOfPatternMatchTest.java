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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class RemoveInstanceOfPatternMatchTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveInstanceOfPatternMatch());
    }

    @Test
    void blockOfStatements() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str) {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String) {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void singleStatement() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str)
                          System.out.println(str);
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String) {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void emptyStatement() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str);
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String);
                  }
              }
              """),
            14));
    }

    @Test
    void elseStatement() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str) {
                          System.out.println(str);
                      } else {
                          System.out.println();
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String) {
                          String str = (String) obj;
                          System.out.println(str);
                      } else {
                          System.out.println();
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void qualifiedTypeName() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof java.lang.String str) {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof java.lang.String) {
                          java.lang.String str = (java.lang.String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void genericType() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              import java.util.Collection;
              import java.util.List;

              class Example {
                  public void test(Collection<String> collection) {
                      if (collection instanceof List<String> list) {
                          System.out.println(list.size());
                      }
                  }
              }
              """,
              """
              package com.example;

              import java.util.Collection;
              import java.util.List;

              class Example {
                  public void test(Collection<String> collection) {
                      if (collection instanceof List<String>) {
                          List<String> list = (List<String>) collection;
                          System.out.println(list.size());
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void expression() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj.toString() instanceof String str) {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj.toString() instanceof String) {
                          String str = (String) obj.toString();
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void multipleVariableUsage() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str) {
                          System.out.println(str + str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String) {
                          String str = (String) obj;
                          System.out.println(str + str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void multipleInstanceOf() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str && obj instanceof Integer num) {
                          System.out.println(str);
                          System.out.println(num);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String && obj instanceof Integer) {
                          String str = (String) obj;
                          Integer num = (Integer) obj;
                          System.out.println(str);
                          System.out.println(num);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void multipleInstanceOfWithOppositeOrder() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str && obj instanceof Integer num) {
                          System.out.println(num);
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String && obj instanceof Integer) {
                          String str = (String) obj;
                          Integer num = (Integer) obj;
                          System.out.println(num);
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void complexCondition() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str && str.length() > 10) {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String && ((String) obj).length() > 10) {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void noExtraParentheses() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str && str != null) {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String && (String) obj != null) {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void unusedVariable() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str) {
                          System.out.println();
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String) {
                          System.out.println();
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void disjunction() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str && str.isEmpty() || false) {
                          System.out.println();
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String && ((String) obj).isEmpty() || false) {
                          System.out.println();
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void localVariable() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str || true) {
                          String str = null;
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String || true) {
                          String str = null;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    @Disabled("Not supported")
    void negationLocalVariable() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String str)) {
                          Integer str = null;
                          System.out.println(str);
                      } else {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String)) {
                          String str = null;
                          System.out.println(str);
                      } else {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    @Disabled("Not supported")
    void negationWithoutElse() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String str)) {
                          throw new IllegalArgumentException();
                      }
                      System.out.println(str.length());
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String)) {
                          throw new IllegalArgumentException();
                      }
                      String str = (String) obj;
                      System.out.println(str);
                  }
              }
              """),
            14));
    }

    @Test
    void nestedComplexCondition() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if ((1 != 2 && obj instanceof String str && str.isEmpty()) && (str.length() > 10 || 1 == 2)) {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if ((1 != 2 && obj instanceof String && ((String) obj).isEmpty()) && (((String) obj).length() > 10 || 1 == 2)) {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void expressionAndComplexCondition() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj.toString() instanceof String str && str.length() > 10) {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj.toString() instanceof String && ((String) obj.toString()).length() > 10) {
                          String str = (String) obj.toString();
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void sequentialIfs() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String v) {
                          System.out.println(v);
                      }
                      if (obj instanceof Integer v) {
                          System.out.println(v);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String) {
                          String v = (String) obj;
                          System.out.println(v);
                      }
                      if (obj instanceof Integer) {
                          Integer v = (Integer) obj;
                          System.out.println(v);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void nestedIfs() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              import java.time.LocalDate;
              import java.time.temporal.Temporal;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof Temporal t) {
                          if (t instanceof LocalDate d) {
                              System.out.println(d);
                          }
                      }
                  }
              }
              """,
              """
              package com.example;

              import java.time.LocalDate;
              import java.time.temporal.Temporal;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof Temporal) {
                          Temporal t = (Temporal) obj;
                          if (t instanceof LocalDate) {
                              LocalDate d = (LocalDate) t;
                              System.out.println(d);
                          }
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void declaredInThenUsedInElse() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String str) {
                          if (1 == 2) {
                              System.out.println();
                          } else {
                              System.out.println(str);
                          }
                      } else {
                          System.out.println();
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof String) {
                          String str = (String) obj;
                          if (1 == 2) {
                              System.out.println();
                          } else {
                              System.out.println(str);
                          }
                      } else {
                          System.out.println();
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void instanceOfPattern() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              import java.time.LocalDate;
              import java.time.temporal.Temporal;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof Temporal t && t instanceof LocalDate d) {
                          System.out.println(d);
                      }
                  }
              }
              """,
              """
              package com.example;

              import java.time.LocalDate;
              import java.time.temporal.Temporal;

              class Example {
                  public void test(Object obj) {
                      if (obj instanceof Temporal && (Temporal) obj instanceof LocalDate) {
                          LocalDate d = (LocalDate) (Temporal) obj;
                          System.out.println(d);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void negation() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String str)) {
                          System.out.println(obj);
                      } else {
                          System.out.println(str);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String)) {
                          System.out.println(obj);
                      } else {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void emptyElseStatement() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String str)) {
                          System.out.println(obj);
                      } else;
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String)) {
                          System.out.println(obj);
                      } else;
                  }
              }
              """),
            14));
    }

    @Test
    void singleElseStatement() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String str)) {
                          System.out.println(obj);
                      } else
                          System.out.println(str);
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String)) {
                          System.out.println(obj);
                      } else {
                          String str = (String) obj;
                          System.out.println(str);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void doubleNegation() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!!(obj instanceof String str)) {
                          System.out.println(str);
                      } else {
                          System.out.println(obj);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!!(obj instanceof String)) {
                          String str = (String) obj;
                          System.out.println(str);
                      } else {
                          System.out.println(obj);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void negationCancellation() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String str) && obj instanceof String str) {
                          System.out.println(str);
                      } else {
                          System.out.println(obj);
                      }
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      if (!(obj instanceof String) && obj instanceof String) {
                          String str = (String) obj;
                          System.out.println(str);
                      } else {
                          System.out.println(obj);
                      }
                  }
              }
              """),
            14));
    }

    @Test
    void ternary() {
        rewriteRun(
          version(
            java(
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      System.out.println(obj instanceof String str ? str : null);
                  }
              }
              """,
              """
              package com.example;

              class Example {
                  public void test(Object obj) {
                      System.out.println(obj instanceof String ? (String) obj : null);
                  }
              }
              """),
            14));
    }

}
