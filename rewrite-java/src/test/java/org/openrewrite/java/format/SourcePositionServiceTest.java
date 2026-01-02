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
package org.openrewrite.java.format;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.service.Span;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.java.Assertions.java;

class SourcePositionServiceTest implements RewriteTest {

    @DocumentExample
    @Test
    void correctlyCalculatesPosition() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

              @Nullable
              SourcePositionService service;

              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  service = cu.service(SourcePositionService.class);
                  assertResult(1, 1, 33, 2, 153, 0); //entire file
                  assertResult(cu.getClasses().getFirst(), 4, 1, 33, 2, 153, 0); //entire Test class declaration
                  return super.visitCompilationUnit(cu, ctx);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if ("Test".equals(classDecl.getSimpleName())) {
                      assertResult(4, 1, 33, 2, 153, 0); //entire Test class declaration
                      assertResult(classDecl.getBody().getStatements().get(4), 17, 5, 20, 6, 77, 4); //example2 method
                  }
                  if ("Inner".equals(classDecl.getSimpleName())) {
                      assertResult(27, 5, 30, 6, 85, 4); //entire Inner class declaration
                  }
                  if ("RecordDeclaration".equals(classDecl.getSimpleName())) {
                      assertResult(32, 5, 32, 153, 153, 4); //entire Inner class declaration
                      assertResult(classDecl.getPadding().getPrimaryConstructor(), 32, 30, 32, 35, 35, 4);
                  }
                  return super.visitClassDeclaration(classDecl, ctx);
              }

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  switch (method.getSimpleName()) {
                      case "example1":
                          assertResult(10, 5, 15, 6, 61, 4); // calculate the span as is.
                          assertMinimizedResult(10, 5, 13, 6, 77, 4); // collapsing the method args to a single line using minimized (correctly calculate a span after changing an element)
                          assertResult(method.getPadding().getParameters(), 10, 55, 12, 18, 61, 4); // calculate the span of the parameters within the declaration
                          assertMinimizedResult(m -> ((J.MethodDeclaration) m).getPadding().getParameters(), 10, 55, 10, 74, 74, 4); // calculate the span of the parameters after minimization (correctly calculate a nested span after changing an element)
                          break;
                      case "example2":
                          assertResult(17, 5, 20, 6, 77, 4);
                          assertMinimizedResult(17, 5, 20, 6, 77, 4);
                          assertResult(method.getPadding().getParameters(), 17, 55, 17, 74, 74, 4);
                          assertMinimizedResult(m -> ((J.MethodDeclaration) m).getPadding().getParameters(), 17, 55, 17, 74, 74, 4);
                          break;
                      case "someVeryLongMethodNameThatIsAsLongAsTheMethodsAbove":
                          assertResult(22, 5, 25, 6, 75, 4);
                          assertMinimizedResult(22, 5, 23, 78, 78, 4); // minimization collapses the block to start/end on same line
                          assertResult(method.getPadding().getParameters(), 23, 69, 23, 74, 74, 4);
                          assertMinimizedResult(m -> ((J.MethodDeclaration) m).getPadding().getParameters(), 23, 69, 23, 74, 74, 4);
                          break;
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().stream().anyMatch(v -> "abc".contains(v.getSimpleName()))) {
                      assertResult(6, 5, 6, 38, 38, 4);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "d".equals(v.getSimpleName()))) {
                      assertResult(7, 5, 7, 14, 14, 4);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "e".equals(v.getSimpleName()))) {
                      assertResult(8, 5, 8, 10, 10, 4);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "f".equals(v.getSimpleName()))) {
                      assertResult(32, 30, 32, 35, 35, 4);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "sum1".equals(v.getSimpleName()))) {
                      assertResult(13, 9, 13, 25, 25, 8);
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }

              private void assertResult(int line, int column, int endLine, int endColumn, int maxColumn, int rowIndent) {
                  assertThat(service.positionOf(getCursor()))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).rowIndent(rowIndent).build());
              }

              private void assertResult(J j, int line, int column, int endLine, int endColumn, int maxColumn, int rowIndent) {
                  assertThat(service.positionOf(getCursor(), j))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).rowIndent(rowIndent).build());
              }

              private void assertResult(JContainer<Statement> j, int line, int column, int endLine, int endColumn, int maxColumn, int rowIndent) {
                  assertThat(service.positionOf(getCursor(), j))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).rowIndent(rowIndent).build());
              }

              private void assertMinimizedResult(int line, int column, int endLine, int endColumn, int maxColumn, int rowIndent) {
                  assertThat(service.positionOf(new Cursor(getCursor().getParent(), minimize(getCursor().getValue()))))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).rowIndent(rowIndent).build());
              }

              private void assertMinimizedResult(Function<J, JContainer<Statement>> find, int line, int column, int endLine, int endColumn, int maxColumn, int rowIndent) {
                  J minimized = minimize(getCursor().getValue());
                  assertThat(service.positionOf(new Cursor(getCursor().getParent(), minimized), find.apply(minimized)))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).rowIndent(rowIndent).build());
              }
          })),
          java(
            """
              package com.example;
              
              // Own-line comments are not considered the start of a line
              @Deprecated
              public class Test {
                  private final int a = 1, b, c = 3;
                  int d = 4;
                  int e;
              
                  public int /* multiline comment impact*/ example1(int g,
                          int h,
                          int i) {
                      int sum1 = g + h;
                      return sum1;
                  }
              
                  public int /* multiline comment impact*/ example2(int g, int h, int i) {
                      int sum2 = a + c;
                      return sum2;
                  }
              
                  @Deprecated // eol comments do not impact the col of the element
                  public void someVeryLongMethodNameThatIsAsLongAsTheMethodsAbove(int x)
                  {
                  }
              
                  @Deprecated /* same line annotations do impact declaration length */ class Inner
                  {
                      // Inner class to test nested structures
                  }
              
                  record RecordDeclaration(int f) { /* We only count till the opening curly (incl if on same line) and not the block's end Space / closing curly. */ }
              }
              """
          )
        );
    }

    @Test
    void semanticallyEqualDoesNotMatchWrongBlock() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

              @Nullable
              SourcePositionService service;

              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  service = cu.service(SourcePositionService.class);
                  return super.visitCompilationUnit(cu, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().stream().anyMatch(v -> "x".equals(v.getSimpleName()))) {
                      J.MethodDeclaration method = getCursor().firstEnclosing(J.MethodDeclaration.class);
                      if (method != null) {
                          if ("method1".equals(method.getSimpleName())) {
                              assertThat(service.positionOf(getCursor())).isEqualTo(Span.builder().startLine(6).startColumn(9).endLine(6).maxColumn(18).endColumn(18).rowIndent(8).build());
                              J.VariableDeclarations removedComment = multiVariable.withPrefix(multiVariable.getPrefix().withComments(emptyList()));
                              assertThat(service.positionOf(new Cursor(getCursor().getParent(), removedComment))).isEqualTo(Span.builder().startLine(5).startColumn(9).endLine(5).maxColumn(18).endColumn(18).rowIndent(8).build());
                              assertThat(service.positionOf(new Cursor(getCursor().getParent(), removedComment), removedComment)).isEqualTo(Span.builder().startLine(5).startColumn(9).endLine(5).maxColumn(18).endColumn(18).rowIndent(8).build());

                              //When the cursor does not contain the referential equal object, we throw
                              assertThrows(IllegalArgumentException.class, () -> assertThat(service.positionOf(getCursor(), removedComment)));
                              assertThrows(IllegalArgumentException.class, () -> assertThat(service.positionOf(getCursor().getParentTreeCursor(), removedComment)));
                          }
                          if ("method2".equals(method.getSimpleName())) {
                              assertThat(service.positionOf(getCursor())).isEqualTo(Span.builder().startLine(10).startColumn(9).endLine(10).maxColumn(18).endColumn(18).rowIndent(8).build());
                          }
                      }
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              package com.example;

              public class Test {
                  public void method1() {
                      //Some comment
                      int x = 1;
                  }

                  public void method2() {
                      int x = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void updatingSomeElementCanImpactPositions() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

              @Nullable
              SourcePositionService service;

              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  service = cu.service(SourcePositionService.class);
                  return super.visitCompilationUnit(cu, ctx);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  //minimize the first method (all declarations should have an updated position)
                  super.visitClassDeclaration(classDecl.withBody(classDecl.getBody().withStatements(ListUtils.mapFirst(classDecl.getBody().getStatements(), SourcePositionServiceTest::minimize))), ctx);
                  return classDecl;
              }

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if ("method1".equals(method.getSimpleName())) {
                      //This is the modified method, so doing getCursor is enough here.
                      assertThat(service.positionOf(getCursor())).isEqualTo(Span.builder().startLine(4).startColumn(5).endLine(7).maxColumn(40).endColumn(6).rowIndent(4).build());
                  } else if ("method2".equals(method.getSimpleName())) {
                      //The parent cursor (= block of class declaration) contains the modified method1 and in order to be used by the position calculation, you must pass that one.
                      assertThat(service.positionOf(getCursor().getParentTreeCursor(), method)).isEqualTo(Span.builder().startLine(9).startColumn(5).endLine(11).maxColumn(28).endColumn(6).rowIndent(4).build());
                      //Just passing this cursor is not enough for the accurate positioning as the modified element in not in this cursor and the service will start top down from JavaSourceFile -> no updated method is used.
                      assertThat(service.positionOf(getCursor(), method)).isEqualTo(Span.builder().startLine(12).startColumn(5).endLine(14).maxColumn(28).endColumn(6).rowIndent(4).build());
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }
          })),
          java(
            """
              package com.example;

              public class Test {
                  public void method1(
                    int a,
                    int b
                  ) {
                      //Some comment
                      int x = 1;
                  }

                  public void method2() {
                      int x = 1;
                  }
              }
              """
          )
        );
    }

    private static  <T extends J> T minimize(T tree) {
        tree = (T) new MinimumViableSpacingVisitor<>(null).visit(tree, -1);
        return (T) new SpacesVisitor<>(IntelliJ.spaces(), null, null, IntelliJ.wrappingAndBraces().withKeepWhenFormatting(IntelliJ.wrappingAndBraces().getKeepWhenFormatting().withLineBreaks(false)), null).visit(tree, -1);
    }
}
