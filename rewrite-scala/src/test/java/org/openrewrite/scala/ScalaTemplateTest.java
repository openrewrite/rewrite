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
package org.openrewrite.scala;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.scala.Assertions.scala;
import static org.openrewrite.test.RewriteTest.toRecipe;

class ScalaTemplateTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return ScalaTemplate.builder("println(\"foo\")")
                    .build()
                    .apply(getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          scala(
            """
              class Test {
                  val b1 = 1 == 2
              }
              """,
            """
              class Test {
                  println("foo")
              }
              """
          ));
    }

    @Test
    void replaceExpression() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  if (literal.getValue() instanceof Integer && (Integer) literal.getValue() == 42) {
                      return ScalaTemplate.builder("99")
                        .build()
                        .apply(getCursor(), literal.getCoordinates().replace());
                  }
                  return super.visitLiteral(literal, ctx);
              }
          })),
          scala(
            """
              class Test {
                  val x = 42
              }
              """,
            """
              class Test {
                  val x = 99
              }
              """
          ));
    }

    @Test
    void addStatementToBlockEnd() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  J.ClassDeclaration c = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
                  List<Statement> statements = c.getBody().getStatements();
                  if (statements.stream().noneMatch(s -> s.toString().contains("added"))) {
                      return ScalaTemplate.builder("val added = true")
                        .build()
                        .apply(getCursor(), c.getBody().getCoordinates().lastStatement());
                  }
                  return c;
              }
          })),
          scala(
            """
              class Test {
                  val x = 1
              }
              """,
            """
              class Test {
                  val x = 1
                  val  added = true
              }
              """
          ));
    }

    @Test
    void staticConvenienceApply() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new ScalaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  return ScalaTemplate.apply("println(\"static\")", getCursor(), multiVariable.getCoordinates().replace());
              }
          })),
          scala(
            """
              class Test {
                  val b1 = 1
              }
              """,
            """
              class Test {
                  println("static")
              }
              """
          ));
    }
}
