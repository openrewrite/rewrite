/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Verifies that the public {@code withX(...)} mutators on {@link J.MethodDeclaration},
 * {@link J.ClassDeclaration}, {@link J.TypeParameter}, and {@link J.Try} produce well-formed
 * source when adding a keyword-prefixed container (such as {@code throws}, {@code implements},
 * {@code permits}, {@code extends} type bounds, and {@code try (...)} resources) to a node that
 * previously did not have one. Each container's leading {@link Space} must default to
 * {@link Space#SINGLE_SPACE} so the keyword is separated from the preceding token.
 */
class AddKeywordContainerSpacingTest implements RewriteTest {

    @Test
    void withThrowsAddsLeadingSpaceWhenMethodHasNoExistingThrowsClause() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (!"target".equals(method.getSimpleName()) || method.getThrows() != null) {
                      return method;
                  }
                  maybeAddImport("java.io.IOException");
                  J.Identifier ioException = new J.Identifier(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                          Collections.emptyList(), "IOException",
                          JavaType.ShallowClass.build("java.io.IOException"), null);
                  return method.withThrows(Collections.singletonList(ioException));
              }
          })),
          //language=java
          java(
            """
              class Test {
                  void target() {
                  }
              }
              """,
            """
              import java.io.IOException;

              class Test {
                  void target() throws IOException {
                  }
              }
              """
          )
        );
    }

    @Test
    void withImplementsAddsLeadingSpaceWhenClassHasNoExistingImplementsClause() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
                  if (!"Target".equals(cd.getSimpleName()) || cd.getImplements() != null) {
                      return cd;
                  }
                  maybeAddImport("java.io.Serializable");
                  J.Identifier serializable = new J.Identifier(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                          Collections.emptyList(), "Serializable",
                          JavaType.ShallowClass.build("java.io.Serializable"), null);
                  return cd.withImplements(Collections.singletonList(serializable));
              }
          })),
          //language=java
          java(
            """
              class Target {
              }
              """,
            """
              import java.io.Serializable;

              class Target implements Serializable {
              }
              """
          )
        );
    }

    @Test
    void withPermitsAddsLeadingSpaceWhenSealedClassHasNoExistingPermitsClause() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
                  if (!"Shape".equals(cd.getSimpleName()) || cd.getPermits() != null) {
                      return cd;
                  }
                  J.Identifier circle = new J.Identifier(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                          Collections.emptyList(), "Circle",
                          JavaType.ShallowClass.build("Circle"), null);
                  return cd.withPermits(Collections.singletonList(circle));
              }
          })),
          //language=java
          java(
            """
              sealed class Shape {
              }

              final class Circle extends Shape {
              }
              """,
            """
              sealed class Shape permits Circle {
              }

              final class Circle extends Shape {
              }
              """
          )
        );
    }

    @Test
    void withResourcesAddsLeadingSpaceWhenTryHasNoExistingResources() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
              @Override
              public J.Try visitTry(J.Try tryStmt, ExecutionContext ctx) {
                  if (tryStmt.getResources() != null) {
                      return tryStmt;
                  }
                  // Build a single resource: AutoCloseable r = null
                  J.Identifier acType = new J.Identifier(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                          Collections.emptyList(), "AutoCloseable",
                          JavaType.ShallowClass.build("java.lang.AutoCloseable"), null);
                  J.Identifier varName = new J.Identifier(
                          Tree.randomId(), Space.format(" "), Markers.EMPTY,
                          Collections.emptyList(), "r", null, null);
                  J.Literal nullLit = new J.Literal(
                          Tree.randomId(), Space.format(" "), Markers.EMPTY,
                          null, "null", null, JavaType.Primitive.Null);
                  J.VariableDeclarations.NamedVariable namedVar = new J.VariableDeclarations.NamedVariable(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                          varName, Collections.emptyList(),
                          new JLeftPadded<>(Space.format(" "), nullLit, Markers.EMPTY),
                          null);
                  J.VariableDeclarations decl = new J.VariableDeclarations(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                          Collections.emptyList(), Collections.emptyList(),
                          acType, null, Collections.emptyList(),
                          Collections.singletonList(new JRightPadded<>(namedVar, Space.EMPTY, Markers.EMPTY)));
                  J.Try.Resource resource = new J.Try.Resource(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY, decl, false);
                  return tryStmt.withResources(Collections.singletonList(resource));
              }
          })),
          //language=java
          java(
            """
              class Test {
                  void run() {
                      try {
                      } finally {
                      }
                  }
              }
              """,
            """
              class Test {
                  void run() {
                      try (AutoCloseable r = null) {
                      } finally {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void withBoundsAddsLeadingSpaceWhenTypeParameterHasNoExistingBounds() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
              @Override
              public J.TypeParameter visitTypeParameter(J.TypeParameter typeParam, ExecutionContext ctx) {
                  if (typeParam.getBounds() != null) {
                      return typeParam;
                  }
                  maybeAddImport("java.lang.Comparable");
                  J.Identifier comparable = new J.Identifier(
                          Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                          Collections.emptyList(), "Comparable",
                          JavaType.ShallowClass.build("java.lang.Comparable"), null);
                  return typeParam.withBounds(Collections.singletonList(comparable));
              }
          })),
          //language=java
          java(
            """
              class Test<T> {
              }
              """,
            """
              class Test<T extends Comparable> {
              }
              """
          )
        );
    }
}
