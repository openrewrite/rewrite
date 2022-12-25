/*
 * Copyright 2021 the original author or authors.
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

@SuppressWarnings("StatementWithEmptyBody")
class ForLoopControlVariablePostfixOperatorsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ForLoopControlVariablePostfixOperators());
    }

    @Test
    void forLoopPostfixInductionVariableCounter() {
        rewriteRun(
          java(
            """
              class Test {
                  static void method(int n) {
                      for (int i = 0; i < n; ++i) {
                          // empty
                      }
                  }
              }
              """,
            """
              class Test {
                  static void method(int n) {
                      for (int i = 0; i < n; i++) {
                          // empty
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleInductionVariables() {
        rewriteRun(
          java(
            """
              class Test {
                  static void method(int n) {
                      for (int a = 0, b = 0, c = 0; a < n; ++a, --c) {
                          // empty
                      }
                  }
              }
              """,
            """
              class Test {
                  static void method(int n) {
                      for (int a = 0, b = 0, c = 0; a < n; a++, c--) {
                          // empty
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void forLoopAlreadyPostfix() {
        rewriteRun(
          java(
            """
              class Test {
                  static void method(int n) {
                      for (int i = 0; i < n; i++) {
                          // empty
                      }
                  }
              }
              """
          )
        );
    }
}
