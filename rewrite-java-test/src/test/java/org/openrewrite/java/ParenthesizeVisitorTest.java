/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"StatementWi«thEmptyBody", "ConstantConditions"})
class ParenthesizeVisitorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.cycles(1).expectedCyclesThatMakeChanges(1);
    }

    @DocumentExample
    @Test
    void parenthesizeUnaryWithBinary() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
                  J.Unary u = super.visitUnary(unary, ctx);
                  if (u.getExpression() instanceof J.Parentheses &&
                      ((J.Parentheses<?>) u.getExpression()).getTree() instanceof J.Binary) {
                      // Remove parentheses from binary expression inside unary
                      u = u.withExpression((Expression) ((J.Parentheses<?>) u.getExpression()).getTree());
                  }
                  return (J.Unary) new ParenthesizeVisitor().visit(u, ctx);
              }
          })),
          java(
            """
              import java.util.Set;

              public class A {
                  static Set<String> set;
                  static boolean notEmpty = !(set == null || set.isEmpty());
              }
              """,
            """
              import java.util.Set;

              public class A {
                  static Set<String> set;
                  static boolean notEmpty = !(set == null || set.isEmpty());
              }
              """
          )
        );
    }

    @Test
    void parenthesizeBinaryForPrecedence() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
                  J.Binary b = super.visitBinary(binary, ctx);
                  if (b.getOperator() == J.Binary.Type.Multiplication && 
                      b.getLeft() instanceof J.Parentheses &&
                      ((J.Parentheses<?>) b.getLeft()).getTree() instanceof J.Binary) {
                      // Remove parentheses from binary expression inside multiplication
                      J.Binary leftBinary = (J.Binary) ((J.Parentheses<?>) b.getLeft()).getTree();
                      b = b.withLeft(leftBinary);
                  }
                  return (J.Binary) new ParenthesizeVisitor().visit(b, ctx);
              }
          })),
          java(
            """
              public class A {
                  int result = (1 + 2) * 3;
              }
              """,
            """
              public class A {
                  int result = (1 + 2) * 3;
              }
              """
          )
        );
    }

    @Test
    void parenthesizeTernary() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Ternary visitTernary(J.Ternary ternary, ExecutionContext ctx) {
                  J.Ternary t = super.visitTernary(ternary, ctx);
                  if (t.getTruePart() instanceof J.Parentheses) {
                      // Remove parentheses from true part of ternary
                      J.Parentheses<?> parentheses = (J.Parentheses<?>) t.getTruePart();
                      t = t.withTruePart((Expression) parentheses.getTree());
                  }
                  return (J.Ternary) new ParenthesizeVisitor().visit(t, ctx);
              }
          })),
          java(
            """
              public class A {
                  boolean condition = true;
                  int result = condition ? (1 + 2) : 3;
              }
              """,
            """
              public class A {
                  boolean condition = true;
                  int result = condition ?1 + 2 : 3;
              }
              """
          )
        );
    }

    @Test
    void parenthesizeInstanceOf() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
                  J.Unary u = super.visitUnary(unary, ctx);
                  if (u.getExpression() instanceof J.Parentheses &&
                      ((J.Parentheses<?>) u.getExpression()).getTree() instanceof J.InstanceOf) {
                      // Remove parentheses from instanceof expression inside unary
                      J.InstanceOf instanceOf = (J.InstanceOf) ((J.Parentheses<?>) u.getExpression()).getTree();
                      u = u.withExpression(instanceOf);
                  }
                  return (J.Unary) new ParenthesizeVisitor().visit(u, ctx);
              }
          })),
          java(
            """
              public class A {
                  Object obj = new Object();
                  boolean check = !(obj instanceof String);
              }
              """,
            """
              public class A {
                  Object obj = new Object();
                  boolean check = !(obj instanceof String);
              }
              """
          )
        );
    }

    @Test
    void parenthesizeAssignment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
                  J.Unary u = super.visitUnary(unary, ctx);
                  if (u.getExpression() instanceof J.Parentheses &&
                      ((J.Parentheses<?>) u.getExpression()).getTree() instanceof J.Assignment) {
                      // Remove parentheses from assignment expression inside unary
                      J.Assignment assignment = (J.Assignment) ((J.Parentheses<?>) u.getExpression()).getTree();
                      u = u.withExpression(assignment);
                  }
                  return (J.Unary) new ParenthesizeVisitor().visit(u, ctx);
              }
          })),
          java(
            """
              public class A {
                  boolean x;
                  boolean result = !(x = true);
              }
              """,
            """
              public class A {
                  boolean x;
                  boolean result = !(x = true);
              }
              """
          )
        );
    }
}
