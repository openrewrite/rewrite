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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceMethodInvocationWithConstantTest implements RewriteTest {

    private final Recipe REPLACE_WITH_NULL = new ReplaceMethodInvocationWithConstant("java.lang.System#getSecurityManager()", "null");
    private final Recipe REPLACE_WITH_INTEGER = new ReplaceMethodInvocationWithConstant("Test#getValue()", "4711");

    @Test
    void replaceNothing() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(REPLACE_WITH_INTEGER),
          java(
            """
              class Test {
                  void test() {
                      int value = 42;
                  }
              }
              """
          )
        );
    }

    @Nested
    class InMethod {
        @DocumentExample
        @SuppressWarnings("removal")
        @Test
        void withNull() {
            rewriteRun(
              recipeSpec -> recipeSpec.recipe(REPLACE_WITH_NULL),
              java(
                """
                  class Test {
                      void test() {
                          SecurityManager securityManager = System.getSecurityManager();
                      }
                  }
                  """,
                """
                  class Test {
                      void test() {
                          SecurityManager securityManager = null;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void withInteger() {
            rewriteRun(
              recipeSpec -> recipeSpec.recipe(REPLACE_WITH_INTEGER),
              java(
                """
                  class Test {
                      void test() {
                          int value = getValue();
                      }
                      int getValue() {
                          return 42;
                      }
                  }
                  """,
                """
                  class Test {
                      void test() {
                          int value = 4711;
                      }
                      int getValue() {
                          return 42;
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class InField {
        @SuppressWarnings("removal")
        @Test
        void withNull() {
            rewriteRun(
              recipeSpec -> recipeSpec.recipe(REPLACE_WITH_NULL),
              java(
                """
                  class Test {
                      SecurityManager securityManager = System.getSecurityManager();
                  }
                  """,
                """
                  class Test {
                      SecurityManager securityManager = null;
                  }
                  """
              )
            );
        }

        @Test
        void withInteger() {
            rewriteRun(
              recipeSpec -> recipeSpec.recipe(REPLACE_WITH_INTEGER),
              java(
                """
                  class Test {
                      int value = getValue();
                      static int getValue() {
                          return 42;
                      }
                  }
                  """,
                """
                  class Test {
                      int value = 4711;
                      static int getValue() {
                          return 42;
                      }
                  }
                  """
              )
            );
        }
    }
}
