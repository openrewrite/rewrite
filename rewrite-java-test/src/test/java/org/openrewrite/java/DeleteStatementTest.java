/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("rawtypes")
class DeleteStatementTest implements RewriteTest {

    @Test
    void deleteField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  doAfterVisit(new DeleteStatement<>(multiVariable));
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              public class A {
                 List collection = null;
              }
              """,
            """
              public class A {
              }
              """
          )
        );
    }

    @SuppressWarnings("ALL")
    @Test
    void deleteSecondStatement() {
        //noinspection UnusedAssignment
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (b.getStatements().size() != 4) {
                      return b;
                  }
                  List<Statement> statements = b.getStatements();
                  for (int i = 0; i < statements.size(); i++) {
                      Statement s = statements.get(i);
                      if (i == 1) {
                          doAfterVisit(new DeleteStatement<>(s));
                      }
                  }
                  return b;
              }
          })),
          java(
            """
              public class A {
                 {
                    String s = "";
                    s.toString();
                    s = "hello";
                    s.toString();
                 }
              }
              """,
            """
              public class A {
                 {
                    String s = "";
                    s = "hello";
                    s.toString();
                 }
              }
              """
          )
        );
    }

    @SuppressWarnings("ALL")
    @Test
    void deleteSecondAndFourthStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (b.getStatements().size() != 4) {
                      return b;
                  }
                  List<Statement> statements = b.getStatements();
                  for (int i = 0; i < statements.size(); i++) {
                      Statement s = statements.get(i);
                      if (i == 1 || i == 3) {
                          doAfterVisit(new DeleteStatement<>(s));
                      }
                  }
                  return b;
              }
          })),
          java(
            """
              public class A {
                 {
                    String s = "";
                    s.toString();
                    s = "hello";
                    s.toString();
                 }
              }
              """,
            """
              public class A {
                 {
                    String s = "";
                    s = "hello";
                 }
              }
              """
          )
        );
    }
}
