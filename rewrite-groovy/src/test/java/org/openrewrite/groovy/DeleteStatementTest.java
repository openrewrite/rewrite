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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

class DeleteStatementTest implements RewriteTest {
    @Test
    void deleteStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
              @Override
              public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                  G.CompilationUnit g = super.visitCompilationUnit(cu, ctx);
                  List<Statement> statements = g.getStatements();
                  for (int i = 0; i < statements.size(); i++) {
                      Statement s = statements.get(i);
                      if (i == 1) {
                          g = (G.CompilationUnit) new DeleteStatement<>(s).visit(cu, ctx);
                      }
                  }
                  return g;
              }
          })),
          groovy(
            """
              int i = 0
              i = 1
              """,
            """
              int i = 0
              """
          )
        );
    }
}
