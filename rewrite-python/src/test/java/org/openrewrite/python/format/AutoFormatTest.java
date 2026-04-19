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
package org.openrewrite.python.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class AutoFormatTest implements RewriteTest {

    /**
     * Verifies that auto-formatting a Python file with imports doesn't crash.
     * Previously, the Java-side BlankLinesVisitor would throw
     * IndexOutOfBoundsException because Python stores imports as statements,
     * so cu.getImports() returns an empty list.
     */
    @Test
    void autoFormatDoesNotCrashOnPythonWithImports() {
        rewriteRun(
          spec -> spec.recipe(new AutoFormatSubtreeRecipe()),
          python(
            """
              import os


              def foo():
                  if True:
                      return 1
                  else:
                      return 2
              """
          )
        );
    }

    /**
     * Verifies that subtree auto-formatting dispatches to the Python-side
     * formatter via RPC and produces observable formatting changes.
     * <p>
     * Both methods have bad spacing (space before comma in parameters).
     * Only {@code bar} is auto-formatted, so its spacing is fixed while
     * {@code foo}'s bad spacing is left untouched.
     */
    @Test
    void autoFormatFixesSpacingOnlyInSubtree() {
        rewriteRun(
          spec -> spec.recipe(new AutoFormatBarRecipe()),
          python(
            """
              def foo(a , b):
                  pass

              def bar(a , b):
                  pass
              """,
            """
              def foo(a , b):
                  pass


              def bar(a, b):
                  pass
              """
          )
        );
    }

    private static class AutoFormatSubtreeRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Auto-format subtree test";
        }

        @Override
        public String getDescription() {
            return "Calls autoFormat on if-else statements to exercise the auto-format service.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<>() {
                @Override
                public J.If visitIf(J.If iff, ExecutionContext ctx) {
                    J.If i = super.visitIf(iff, ctx);
                    return autoFormat(i, ctx);
                }
            };
        }
    }

    /**
     * A recipe that calls autoFormat only on the method named "bar",
     * leaving "foo" untouched.
     */
    private static class AutoFormatBarRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Auto-format bar method";
        }

        @Override
        public String getDescription() {
            return "Calls autoFormat on the method named 'bar' to verify subtree-scoped formatting.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                    if (m.getSimpleName().equals("bar")) {
                        return autoFormat(m, ctx);
                    }
                    return m;
                }
            };
        }
    }
}
