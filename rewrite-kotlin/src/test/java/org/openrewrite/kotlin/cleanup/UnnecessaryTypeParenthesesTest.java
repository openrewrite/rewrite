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

class UnnecessaryTypeParenthesesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UnnecessaryTypeParentheses());
    }

    @DocumentExample
    @Test
    void variableDeclarations() {
        rewriteRun(
          kotlin(
            """
              val x : (Int) = 42
              val y : (((Int))) = 42
              """,
            """
              val x : Int = 42
              val y : Int = 42
              """
          )
        );
    }

    @Test
    void lambdaParentheses() {
        rewriteRun(
          kotlin(
            """
              val sum : (Int, Int) -> (Int) = { a, b -> a + b }
              val double : ((Int)) -> ((Int)) = { a -> a * 2 }
              """,
            """
              val sum : (Int, Int) -> Int = { a, b -> a + b }
              val double : (Int) -> Int = { a -> a * 2 }
              """
          )
        );
    }

    @Test
    void methodDeclarationArgsTypeInParentheses() {
        rewriteRun(
          kotlin(
            """
              @Suppress("UNUSED_PARAMETER")
              fun <T> method(
                  arg1: (String),
                  arg2: (String.(isGenericParam: Boolean) -> T),
                  arg3: ((current: Pair<T, MutableList<T>>) -> T)
              ): Int {
                  return 42
              }
                            """,
            """
              @Suppress("UNUSED_PARAMETER")
              fun <T> method(
                  arg1: String,
                  arg2: String.(isGenericParam: Boolean) -> T,
                  arg3: (current: Pair<T, MutableList<T>>) -> T
              ): Int {
                  return 42
              }
              """
          )
        );
    }

    @Test
    void withKeyword() {
        rewriteRun(
          kotlin(
            """
              val v: (suspend (param: (Int)) -> Unit) = { }
              """,
            """
              val v: suspend (param: Int) -> Unit = { }
              """
          )
        );
    }
}
