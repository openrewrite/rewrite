/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.test.RewriteTest.toRecipe;

class SimplifyBooleanExpressionVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new SimplifyBooleanExpressionVisitor()));
    }

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/303")
    @Test
    void nullableReceiverEqualsTrue() {
        rewriteRun(
          kotlin(
            """
              fun Boolean?.toLegacyFlag() = if (this == true) "1" else "0"
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/303")
    @Test
    void nullableVariableEqualsTrue() {
        rewriteRun(
          kotlin(
            """
              data class Todo(val completed: Boolean?)
              fun main() {
                  val todo = Todo(null)
                  val isCompleted: Boolean = todo.completed == true
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/303")
    @Test
    void nullableVariableEqualsFalse() {
        rewriteRun(
          kotlin(
            """
              fun check(b: Boolean?) {
                  if (b == false) println("false or null")
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-static-analysis/issues/303")
    @Test
    void nullableVariableNotEqualTrue() {
        rewriteRun(
          kotlin(
            """
              fun check(b: Boolean?) {
                  if (b != true) println("not true")
              }
              """
          )
        );
    }

    @Test
    void simplifyBooleanLiteralOperations() {
        rewriteRun(
          kotlin(
            """
              fun check(b: Boolean) {
                  val a = !false
                  val c = b || true
                  val d = b && false
              }
              """,
            """
              fun check(b: Boolean) {
                  val a = true
                  val c = true
                  val d = false
              }
              """
          )
        );
    }
}
