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
package org.openrewrite.kotlin.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.kotlin.Assertions.kotlin;

class RemoveLambdaArgumentParenthesesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MoveLambdaArgumentParentheses())
                .typeValidationOptions(TypeValidation.none())
                .expectedCyclesThatMakeChanges(1)
                .cycles(1);
    }

    @DocumentExample
    @Test
    void removeParenthesesFromLet() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val foo = 1.let({ it + 1 })
              }
              """,
            """
              fun method() {
                  val foo = 1.let { it + 1 }
              }
              """
          )
        );
    }

    @Test
    void removeParenthesesFromFilter() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val list = listOf(1, 2, 3)
                  list.filter({ it > 1 })
              }
              """,
            """
              fun method() {
                  val list = listOf(1, 2, 3)
                  list.filter { it > 1 }
              }
              """
          )
        );
    }

    @Test
    void removeParenthesesFromRun() {
        rewriteRun(
            kotlin(
            """
              fun method() {
                  run({ print("Hello world") })
              }
              """,
            """
              fun method() {
                  run { print("Hello world") }
              }
              """
            )
        );
    }

    @Test
    void removeParentheses() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val list = listOf(1, 2, 3)
                  list.filterIndexed({ index, value -> index > 1 && value > 1 })
              }
              """,
                """
              fun method() {
                  val list = listOf(1, 2, 3)
                  list.filterIndexed { index, value -> index > 1 && value > 1 }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenNotLambda() {
        rewriteRun(
                spec -> spec.recipe(new MoveLambdaArgumentParentheses())
                        .expectedCyclesThatMakeChanges(0),
            kotlin(
            """
              fun method() {
                  val list = listOf(1, 2, 3)
                  list.filter(::isEven)
              }
              """
            ),
            kotlin(
            """
              fun method() {
                  run(::print)
              }
              """
            )
        );
    }
}
