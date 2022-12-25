/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class DataflowFunctionalTest implements RewriteTest {

    static Stream<String> fileProvider() {
        try (ScanResult scanResult = new ClassGraph().acceptPaths("/dataflow-functional-tests").scan()) {
            return scanResult.getAllResources().getPaths().stream();
        }
    }

    @ParameterizedTest
    @MethodSource("fileProvider")
    void eachJavaFile(String input) {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  // Force a case where data flow occurs inside a doAfterVisit on a non-top-level visitor run.
                  new JavaIsoVisitor<ExecutionContext>() {
                      @Override
                      public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                          // The doAfterVisit
                          doAfterVisit(new JavaIsoVisitor<>() {
                              @Override
                              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                  doRunDataFlow();
                                  return super.visitMethodInvocation(method, ctx);
                              }
                          });
                          return method;
                      }
                  }.visitNonNull(classDecl, ctx, getCursor().getParentOrThrow());
                  return super.visitClassDeclaration(classDecl, ctx);
              }

              @Override
              public Expression visitExpression(Expression expression, ExecutionContext executionContext) {
                  doRunDataFlow();
                  return super.visitExpression(expression, executionContext);
              }

              private void doRunDataFlow() {
                  Dataflow.startingAt(getCursor()).findSinks(new LocalTaintFlowSpec<Expression, Expression>() {
                      @Override
                      public boolean isSource(Expression expression, Cursor cursor) {
                          return true;
                      }

                      @Override
                      public boolean isSink(Expression expression, Cursor cursor) {
                          return true;
                      }
                  });
              }
          })).cycles(1),
          java(
            StringUtils.readFully(requireNonNull(DataflowFunctionalTest.class
              .getResourceAsStream("/" + input)))
          )
        );
    }
}
