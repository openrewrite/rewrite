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
package org.openrewrite.javascript.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.javascript.Assertions.typescript;

class AutoFormatTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutoFormatBlocks());
    }

    /**
     * Exercises the service-dispatched {@link org.openrewrite.java.JavaVisitor#autoFormat} entry point
     * (the one recipes actually call), which resolves an {@link org.openrewrite.java.service.AutoFormatService}
     * off the enclosing compilation unit. For JavaScript/TypeScript this delegates over RPC to the
     * TypeScript {@code AutoformatVisitor}; for Java it runs the JVM-side {@code AutoFormatVisitor}.
     */
    static class AutoFormatBlocks extends Recipe {
        @Override
        public String getDisplayName() {
            return "Auto-format every block";
        }

        @Override
        public String getDescription() {
            return "Calls `autoFormat` on every block so the source file's `AutoFormatService` is exercised.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                    return autoFormat(super.visitBlock(block, ctx), ctx);
                }
            };
        }
    }

    @Test
    void reindentsJava() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                              int x = 1;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      int x = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void reindentsTypeScript() {
        rewriteRun(
          typescript(
            """
              function test() {
                          const x = 1;
              }
              """,
            """
              function test() {
                  const x = 1;
              }
              """
          )
        );
    }
}
