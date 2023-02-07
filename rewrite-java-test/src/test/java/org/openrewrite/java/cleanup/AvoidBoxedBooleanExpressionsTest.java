/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class AvoidBoxedBooleanExpressionsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AvoidBoxedBooleanExpressions());
    }

    @Test
    void guardAgainstNpe() {
        rewriteRun(
          java(
            """
              class Test {
                  Boolean b;
                  int test() {
                      if (b) {
                          return 1;
                      } else {
                          return 2;
                      }
                  }
              }
              """,
            """
              class Test {
                  Boolean b;
                  int test() {
                      if (Boolean.TRUE.equals(b)) {
                          return 1;
                      } else {
                          return 2;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void unaryNotInIfCondition() {
        rewriteRun(
          java(
            """
              class Test {
                  boolean test(Boolean b) {
                      if (!b) return false;
                      return true;
                  }
              }
              """,
            """
              class Test {
                  boolean test(Boolean b) {
                      if (Boolean.FALSE.equals(b)) return false;
                      return true;
                  }
              }
              """
          )
        );
    }

    @Test
    void guardAgainstNpeUnaryExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                  int test(Boolean b) {
                      if (!b) {
                          return 0;
                      } else if (isTrue(b)) {
                          return 1;
                      }
                  }
                  Boolean isTrue(Boolean b) {
                      return b != null && b.equals(true);
                  }
              }
              """,
            """
              class Test {
                  int test(Boolean b) {
                      if (Boolean.FALSE.equals(b)) {
                          return 0;
                      } else if (Boolean.TRUE.equals(isTrue(b))) {
                          return 1;
                      }
                  }
                  Boolean isTrue(Boolean b) {
                      return b != null && b.equals(true);
                  }
              }
              """
          )
        );
    }

    @Test
    void boxedBooleansInTernaryExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                  String whatToGet(Boolean forThing1) {
                      return forThing1 ? "a fish" : "a bowl";
                  }
              }
              """,
            """
              class Test {
                  String whatToGet(Boolean forThing1) {
                      return Boolean.TRUE.equals(forThing1) ? "a fish" : "a bowl";
                  }
              }
              """
          )
        );
    }
}
