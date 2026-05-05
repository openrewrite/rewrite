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
package org.openrewrite.javascript;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.tree.J;
import org.openrewrite.javascript.tree.JS;
import org.openrewrite.test.RewriteTest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.Assertions.javascript;

/**
 * Prove that {@link ChangeType}, written for Java, does not fail on JavaScript source files
 * containing nodes like {@link JS.FunctionCall} or {@link JS.ComputedPropertyMethodDeclaration}
 * whose {@code withType(..)} throws {@link UnsupportedOperationException} by design.
 */
class ChangeTypeAdaptabilityTest implements RewriteTest {

    @Test
    void functionCallDoesNotThrow() {
        AtomicBoolean hasFunctionCall = new AtomicBoolean(false);
        rewriteRun(
          spec -> spec.recipe(new ChangeType("Foo", "Bar", false)),
          javascript(
            """
              class Foo {
                  getHandler() {
                      return function() { return 1; };
                  }
              }
              var result = new Foo().getHandler()();
              """,
            """
              class Bar {
                  getHandler() {
                      return function() { return 1; };
                  }
              }
              var result = new Bar().getHandler()();
              """,
            spec -> spec.afterRecipe(cu -> {
                new JavaScriptVisitor<Integer>() {
                    @Override
                    public JS.FunctionCall visitFunctionCall(JS.FunctionCall functionCall, Integer p) {
                        hasFunctionCall.set(true);
                        return functionCall;
                    }
                }.visit(cu, 0);
                assertThat(hasFunctionCall.get())
                  .as("Expected parsed JavaScript to contain a JS.FunctionCall node")
                  .isTrue();
                assertThat(cu.getClasses().getFirst().getType())
                  .as("Expected JS ClassDeclaration to have type attribution")
                  .isNotNull();
            })
          )
        );
    }

    @Test
    void computedPropertyMethodDeclarationDoesNotThrow() {
        AtomicBoolean hasComputedPropertyMethodDeclaration = new AtomicBoolean(false);
        rewriteRun(
          spec -> spec.recipe(new ChangeType("Foo", "Bar", false)),
          javascript(
            """
              class Foo {
                  ["bar"](): number { return 1; }
              }
              """,
            """
              class Bar {
                  ["bar"](): number { return 1; }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                new JavaScriptVisitor<Integer>() {
                    @Override
                    public J visitComputedPropertyMethodDeclaration(JS.ComputedPropertyMethodDeclaration m, Integer p) {
                        hasComputedPropertyMethodDeclaration.set(true);
                        return super.visitComputedPropertyMethodDeclaration(m, p);
                    }
                }.visit(cu, 0);
                assertThat(hasComputedPropertyMethodDeclaration.get())
                  .as("Expected parsed JavaScript to contain a JS.ComputedPropertyMethodDeclaration node")
                  .isTrue();
            })
          )
        );
    }
}
