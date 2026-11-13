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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.DeleteStatement;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.test.RewriteTest.toRecipe;

class DeleteStatementTest implements RewriteTest {
    @ParameterizedTest
    @ValueSource(strings = {"if (keep) {}", "if (keep) { println 'remove' } else { println 'other' }"})
    void deleteTopLevelIf(String statement) {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new GroovyIsoVisitor<>() {
              @Override
              public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                  for (Statement current : cu.getStatements()) {
                      if (current instanceof J.If) {
                          return (G.CompilationUnit) new DeleteStatement<>(current).visitNonNull(cu, ctx);
                      }
                  }
                  return cu;
              }
          })),
          groovy("def keep = true\n" + statement + "\nprintln keep\n", "def keep = true\nprintln keep\n")
        );
    }

    @DocumentExample
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
