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
package org.openrewrite.kotlin.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;

class ImplicitParameterInLambdaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ImplicitParameterInLambda());
    }

    @DocumentExample
    @Test
    void removeIt() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  listOf(1, 2, 3).forEach { it -> it.and(6) }
                  val a: (Int) -> Int = { it -> it + 5 }
              }
              """,
            """
              fun method() {
                  listOf(1, 2, 3).forEach { it.and(6) }
                  val a: (Int) -> Int = { it + 5 }
              }
              """
          )
        );
    }

    @Test
    void noChangeWithType() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  val d = {it: Int -> it + 5 } // Compliant, need to know the type
              }
              """
          )
        );
    }

    @Test
    void noChangeIfAlreadyImplicit() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  listOf(1, 2, 3).forEach { it.and(6) }
              }
              """
          )
        );
    }

    @Test
    void noChangeWithMultiParameters() {
        rewriteRun(
          kotlin(
            """
              fun method() {
                  listOf(1, 2, 3).forEachIndexed { it, index ->
                      val result = it * index
                      println(result)
                  }
              }
              """
          )
        );
    }
}
