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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantConditions")
class RemoveObjectsIsNullTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveObjectsIsNull());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    @Test
    void transformCallToIsNull() {
        rewriteRun(
          java(
            """
              import static java.util.Objects.isNull;
              public class A {
                  public void test() {
                      boolean a = true;
                      if (java.util.Objects.isNull(a)) {
                          System.out.println("a is null");
                      }
                  }
              }
              """,
            """
              import static java.util.Objects.isNull;
              public class A {
                  public void test() {
                      boolean a = true;
                      if (a == null) {
                          System.out.println("a is null");
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    @Test
    void transformCallToIsNullNeedsParentheses() {
        rewriteRun(
          java(
            """
              import static java.util.Objects.isNull;
              public class A {
                  public void test() {
                      boolean a = true, b = false;
                      if (isNull(a || b)) {
                          System.out.println("a || b is null");
                      }
                  }
              }
              """,
            """
              import static java.util.Objects.isNull;
              public class A {
                  public void test() {
                      boolean a = true, b = false;
                      if ((a || b) == null) {
                          System.out.println("a || b is null");
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    @Test
    void transformCallToNonNull() {
        rewriteRun(
          java(
            """
              import static java.util.Objects.nonNull;
              public class A {
                  public void test() {
                      boolean a = true;
                      if (java.util.Objects.nonNull(a)) {
                          System.out.println("a is non-null");
                      }
                  }
              }
              """,
            """
              import static java.util.Objects.nonNull;
              public class A {
                  public void test() {
                      boolean a = true;
                      if (a != null) {
                          System.out.println("a is non-null");
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1547")
    @Test
    void transformCallToNonNullNeedsParentheses() {
        rewriteRun(
          java(
            """
              import static java.util.Objects.nonNull;
              public class A {
                  public void test() {
                      boolean a = true, b = false;
                      if (nonNull(a || b)) {
                          System.out.println("a || b is non-null");
                      }
                  }
              }
              """,
            """
              import static java.util.Objects.nonNull;
              public class A {
                  public void test() {
                      boolean a = true, b = false;
                      if ((a || b) != null) {
                          System.out.println("a || b is non-null");
                      }
                  }
              }
              """
          )
        );
    }
}
