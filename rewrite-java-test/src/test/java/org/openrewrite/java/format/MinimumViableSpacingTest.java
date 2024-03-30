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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Space;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("StatementWithEmptyBody")
class MinimumViableSpacingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(
          toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                  if (ctx.getMessage("cyclesThatResultedInChanges", 0) == 0) {
                      return space.withWhitespace("");
                  }
                  return space;
              }
          }),
          toRecipe(() -> new MinimumViableSpacingVisitor<>(null))
        );
    }

    @DocumentExample
    @Test
    void method() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              class A {
                  public <T> void foo() {
                  }
              }
              """,
            """
              class A{public <T> void foo(){}}
              """
          )
        );
    }

    @Test
    void returnExpression() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              class A {
                  public String foo() {
                      return"foo";
                  }
              }
              """,
            """
              class A{public String foo(){return "foo";}}
              """
          )
        );
    }

    @Test
    void variableDeclarationsInClass() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              class A {
                  int unassigned;
                  int zero = 0;
                  final int one = 1;
                  public static final int ONE = 1;
                  public static final int TWO = 1, THREE = 3;
              }
              """,
            """
              class A{int unassigned;int zero=0;final int one=1;public static final int ONE=1;public static final int TWO=1,THREE=3;}
              """
          )
        );
    }

    @Test
    void variableDeclarationsInMethod() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              class A {
                  public void foo(int paramA, final int paramB) {
                      int unassigned;
                      int a = 1;
                      int b, c = 5;
                      final int d = 10;
                      final int e, f = 20;
                  }
              }
              """,
            """
              class A{public void foo(int paramA,final int paramB){int unassigned;int a=1;int b,c=5;final int d=10;final int e,f=20;}}
              """
          )
        );
    }

    @Test
    void variableDeclarationsInForLoops() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              class Test {
                  void foo(final int[] arr) {
                      for (int n = 0, x = 0; n < 100; n++, x++) {
                      }
                      
                      for (int i: arr) {
                      }
                      
                      for (final int i: arr) {
                      }
                  }
              }
              """,
            """
              class Test{void foo(final int[] arr){for(int n=0,x=0;n<100;n++,x++){}for(int i:arr){}for(final int i:arr){}}}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/758")
    @Test
    void spacesBetweenModifiers() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              public final class A {
                  public static String foo() {
                      return "foo";
                  }
              }
              """,
            """
              public final class A{public static String foo(){return "foo";}}
              """
          )
        );
    }

    // This test can not be reproduced by running MinimumViableSpacingVisitor only, so using `AutoFormat` recipe here.
    @Issue("https://github.com/openrewrite/rewrite/issues/3346")
    @Test
    void annotatedReturnTypeExpression() {
        rewriteRun(
          spec -> spec.recipe(new AutoFormat()),
          java(
            """
              class A {
                  public @Deprecated String method() {
                      return "name";
                  }

                  public    @Deprecated String method2() {
                      return "name";
                  }
              }
              """,
            """
              class A {
                  public @Deprecated String method() {
                      return "name";
                  }

                  public @Deprecated String method2() {
                      return "name";
                  }
              }
              """
          )
        );
    }

    @Test
    void annotatedFinalParameter() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new MinimumViableSpacingVisitor<>(null))
          ),
          java(
            """
              class A {
                  public String method(final @Deprecated int a) {
                      return "name";
                  }
              }
              """
          )
        );
    }
}
