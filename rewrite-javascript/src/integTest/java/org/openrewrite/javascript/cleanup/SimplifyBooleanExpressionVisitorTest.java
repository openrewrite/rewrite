/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.cleanup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.javascript.rpc.JavaScriptRewriteRpc;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.javascript.Assertions.javascript;
import static org.openrewrite.javascript.Assertions.typescript;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("JSUnusedLocalSymbols")
@Timeout(60)
class SimplifyBooleanExpressionVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(toRecipe(() -> new SimplifyBooleanExpressionVisitor()));
    }

    @AfterEach
    void after() {
        JavaScriptRewriteRpc.shutdownCurrent();
    }

    @Test
    void doubleNegationPreservedForBooleanCoercion() {
        rewriteRun(
          javascript(
            """
              const x = "hello";
              const y = !!x;
              """
          )
        );
    }

    @Test
    void doubleNegationWithParensPreservedForBooleanCoercion() {
        rewriteRun(
          javascript(
            """
              const x = "hello";
              const y = !(!x);
              """
          )
        );
    }

    @Test
    void orFalsePreservedForNonBooleanOperand() {
        rewriteRun(
          javascript(
            """
              function f(o) {
                  o.withCredentials = o.value || false;
              }
              """
          )
        );
    }

    @Test
    void andTruePreservedForNonBooleanOperand() {
        rewriteRun(
          javascript(
            """
              function f(o) {
                  o.value = o.value && true;
              }
              """
          )
        );
    }

    @Test
    void orTruePreservedForNonBooleanOperand() {
        rewriteRun(
          javascript(
            """
              function f(o) {
                  o.value = o.value || true;
              }
              """
          )
        );
    }

    @Test
    void andFalsePreservedForNonBooleanOperand() {
        rewriteRun(
          javascript(
            """
              function f(o) {
                  o.value = o.value && false;
              }
              """
          )
        );
    }

    @Test
    void orFalseSimplifiedForBooleanTypedOperand() {
        rewriteRun(
          typescript(
            """
              function f(b: boolean) {
                  const c = b || false;
              }
              """,
            """
              function f(b: boolean) {
                  const c = b;
              }
              """
          )
        );
    }

    @Test
    void doubleNegationOfShortCircuitChainPreserved() {
        rewriteRun(
          javascript(
            """
              function isElement(sources) {
                  return !!(sources && sources.nodeName && sources.nodeType === 1);
              }
              """
          )
        );
    }

    @Test
    void simplifyBooleanLiterals() {
        rewriteRun(
          javascript(
            """
              const a = !false;
              const b = !true;
              """,
            """
              const a = true;
              const b = false;
              """
          )
        );
    }
}
