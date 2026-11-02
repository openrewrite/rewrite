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
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.python.Assertions.python;
import static org.openrewrite.test.RewriteTest.toRecipe;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "No remote client/server available")
class ContextSensitiveJavaTemplateTest implements RewriteTest {

    @Test
    void contextSensitiveTemplateInsidePythonClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                  if (md.getBody() == null || md.getBody().getStatements().size() != 1) {
                      return md;
                  }
                  return JavaTemplate.builder("self.close()")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), md.getBody().getCoordinates().lastStatement());
              }
          })),
          python(
            """
              class Base(object):
                  def close_all(self):
                      pass
              """,
            """
              class Base(object):
                  def close_all(self):
                      pass
                      self.close()
              """
          )
        );
    }
}
