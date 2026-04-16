/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScalaTemplateMatchTest implements RewriteTest {

    @DocumentExample
    @Test
    void matchBinary() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return ScalaTemplate.matches("1 == #{any(int)}", getCursor()) ?
                    SearchResult.found(binary) : super.visitBinary(binary, ctx);
              }
          })),
          scala(
            """
              class Test {
                  val b1 = 1 == 2
                  val b2 = 1 == 3

                  val b3 = 2 == 1
              }
              """,
            """
              class Test {
                  val b1 = /*~~>*/1 == 2
                  val b2 = /*~~>*/1 == 3

                  val b3 = 2 == 1
              }
              """
          ));
    }

    @Test
    void staticConvenienceMatch() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  if (ScalaTemplate.matches("#{any(int)} == 1", getCursor())) {
                      return SearchResult.found(binary);
                  }
                  return super.visitBinary(binary, ctx);
              }
          })),
          scala(
            """
              class Test {
                  val a = 5 == 1
                  val b = 1 == 5
              }
              """,
            """
              class Test {
                  val a = /*~~>*/5 == 1
                  val b = 1 == 5
              }
              """
          ));
    }
}
