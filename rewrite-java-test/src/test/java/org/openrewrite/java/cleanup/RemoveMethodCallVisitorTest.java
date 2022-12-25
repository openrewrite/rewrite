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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("FunctionName")
class RemoveMethodCallVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new RemoveMethodCallVisitor<>(new MethodMatcher("* assertTrue(..)"),
          (arg, expr) -> arg == 0 && J.Literal.isLiteralValue(expr, true))));
    }

    @Test
    void assertTrueIsRemoved() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract void assertTrue(boolean condition);

                  void test() {
                      System.out.println("Hello");
                      assertTrue(true);
                      System.out.println("World");
                  }
              }
              """, """
              abstract class Test {
                  abstract void assertTrue(boolean condition);

                  void test() {
                      System.out.println("Hello");
                      System.out.println("World");
                  }
              }
              """
          )
        );
    }

    @Test
    void assertTrueFalseIsNotremoved() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract void assertTrue(boolean condition);

                  void test() {
                      System.out.println("Hello");
                      assertTrue(false);
                      System.out.println("World");
                  }
              }
              """
          )
        );
    }

    @Test
    void assertTrueTwoArgIsRemoved() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new RemoveMethodCallVisitor<>(
            new MethodMatcher("* assertTrue(..)"), (arg, expr) -> arg != 1 || J.Literal.isLiteralValue(expr, true)
          ))),
          java(
            """
              abstract class Test {
                  abstract void assertTrue(String message, boolean condition);

                  void test() {
                      System.out.println("Hello");
                      assertTrue("message", true);
                      System.out.println("World");
                  }
              }
              """,
            """
              abstract class Test {
                  abstract void assertTrue(String message, boolean condition);

                  void test() {
                      System.out.println("Hello");
                      System.out.println("World");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotRemoveAssertTrueIfReturnValueIsUsed() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract int assertTrue(boolean condition);

                  void test() {
                      System.out.println("Hello");
                      int value = assertTrue(true);
                      System.out.println("World");
                  }
              }
              """
          )
        );
    }
}
