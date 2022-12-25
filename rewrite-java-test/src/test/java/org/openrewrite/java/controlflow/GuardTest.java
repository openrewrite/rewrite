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
package org.openrewrite.java.controlflow;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"FunctionName", "StatementWithEmptyBody", "UnusedAssignment"})
class GuardTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public Expression visitExpression(Expression expression, ExecutionContext p) {
                return Guard.from(getCursor())
                  .map(e -> SearchResult.found(expression))
                  .orElse(expression);
            }
        }));
    }

    @Test
    void identifiesGuards() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean boolLiteral();
                  abstract Boolean boolObject();

                  void test() {
                      if (boolLiteral()) {
                          // ...
                      }
                      if (boolObject()) {
                          // ...
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  abstract /*~~>*/boolean boolLiteral();
                  abstract /*~~>*/Boolean boolObject();

                  void test() {
                      if (/*~~>*/boolLiteral()) {
                          // ...
                      }
                      if (/*~~>*/boolObject()) {
                          // ...
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void identifiesGuardsWithBinaryExpressions() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  abstract boolean boolPrim();
                  abstract Boolean boolObject();

                  void test() {
                      if (boolPrim()) {
                          // ...
                      }
                      if (boolObject() || boolPrim()) {
                          // ...
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  abstract /*~~>*/boolean boolPrim();
                  abstract /*~~>*/Boolean boolObject();

                  void test() {
                      if (/*~~>*/boolPrim()) {
                          // ...
                      }
                      if (/*~~>*//*~~>*/boolObject() || /*~~>*/boolPrim()) {
                          // ...
                      }
                  }
              }
              """
          )
        );
    }


    @Test
    void identifiesGuardWithMethodsWithParameters() {
        rewriteRun(
          java(
            """
              abstract class Test {

                  void test(boolean x, Boolean y) {
                      if (x) {
                          // ...
                      }
                      if (y) {
                          // ...
                      }
                  }
              }
              """,
            """
              abstract class Test {

                  void test(/*~~>*/boolean /*~~>*/x, /*~~>*/Boolean /*~~>*/y) {
                      if (/*~~>*/x) {
                          // ...
                      }
                      if (/*~~>*/y) {
                          // ...
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void identifiesGuardWithFieldAccesses() {
        rewriteRun(
          java(
            """
              abstract class Test {
                  private boolean x;
                  private Boolean y;

                  void test() {
                      if (x) {
                          // ...
                      }
                      if (y) {
                          // ...
                      }
                  }
              }
              """,
            """
              abstract class Test {
                  private /*~~>*/boolean /*~~>*/x;
                  private /*~~>*/Boolean /*~~>*/y;

                  void test() {
                      if (/*~~>*/x) {
                          // ...
                      }
                      if (/*~~>*/y) {
                          // ...
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void identifiesGuardWithMissingTypeInformation() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              class Test {
                  void test() {
                      if (potato) {
                          // ...
                      }
                      if ((potato)) {
                          // ...
                      }
                      if (potato && turnip) {
                          // ...
                      }
                      if (potato && turnip || squash) {
                          // ...
                      }
                      int a = 1, b = 2;
                      if ((a = turnip) == b) {
                          // ..
                      }
                      horse.equals(donkey);
                      boolean farmFresh = tomato;
                      boolean farmFreshAndFancyFree = (chicken);
                      boolean farmFreshEggs = true;
                      farmFreshEggs = chicken.layEggs();
                      while (farming) {
                          // ...
                      }
                      for (int i = 0; areMoreCabbages(); i++) {
                          // ...
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      if (/*~~>*/potato) {
                          // ...
                      }
                      if (/*~~>*/(/*~~>*/potato)) {
                          // ...
                      }
                      if (/*~~>*//*~~>*/potato && /*~~>*/turnip) {
                          // ...
                      }
                      if (/*~~>*//*~~>*//*~~>*/potato && /*~~>*/turnip || /*~~>*/squash) {
                          // ...
                      }
                      int a = 1, b = 2;
                      if (/*~~>*/(a = turnip) == b) {
                          // ..
                      }
                      /*~~>*/horse.equals(donkey);
                      /*~~>*/boolean /*~~>*/farmFresh = /*~~>*/tomato;
                      /*~~>*/boolean /*~~>*/farmFreshAndFancyFree = /*~~>*/(/*~~>*/chicken);
                      /*~~>*/boolean /*~~>*/farmFreshEggs = /*~~>*/true;
                      /*~~>*//*~~>*/farmFreshEggs = /*~~>*/chicken.layEggs();
                      while (/*~~>*/farming) {
                          // ...
                      }
                      for (int i = 0; /*~~>*/areMoreCabbages(); i++) {
                          // ...
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void identifiesGuardsForControlParenthesesWithMissingTypeInformation() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      if ((potato)) {
                          // ...
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      if (/*~~>*/(/*~~>*/potato)) {
                          // ...
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotFlagArbitraryParenthesesAsGuards() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      int a = (potato);
                      int b = (a = turnip);
                  }
              }
              """
          )
        );
    }
}
