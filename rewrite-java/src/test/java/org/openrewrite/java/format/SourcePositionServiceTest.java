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
import org.openrewrite.java.tree.*;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.function.Function;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.java.Assertions.java;

/**
 * Tests for {@link SourcePositionService}.
 * <p>
 * <b>Important:</b> The indentation in these test cases is intentionally non-standard and sometimes incorrect.
 * This is deliberate, as we want to verify that the service correctly calculates alignment positions based on
 * the <i>actual</i> indentation of the previous newlined element plus the continuation indent, rather than
 * assuming the code is already properly formatted.
 * <p>
 * The service is designed to work with code in any state of formatting, determining the correct alignment
 * position by finding the previous element with a newline prefix and using its actual indentation as the
 * baseline. This allows formatting recipes to progressively correct indentation issues.
 */
class SourcePositionServiceTest implements RewriteTest {

    @DocumentExample
    @Test
    void correctlyCalculatesLineLength() {
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
              public J.Package visitPackage(J.Package pkg, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(20);
                  return super.visitPackage(pkg, ctx);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if ("Test".equals(classDecl.getSimpleName())) {
                      assertThat(service.computeTreeLength(getCursor())).isEqualTo(461);
                  }
                  if ("Inner".equals(classDecl.getSimpleName())) {
                      assertThat(service.computeTreeLength(getCursor())).isEqualTo(72);
                  }
                  return super.visitClassDeclaration(classDecl, ctx);
              }

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(365);
                  return super.visitMethodDeclaration(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(80);
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("valueOf".equals(method.getSimpleName())) {
                      assertThat(service.computeTreeLength(getCursor())).isEqualTo(80);
                  }
                  return super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J.Assert visitAssert(J.Assert _assert, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(28);
                  return super.visitAssert(_assert, ctx);
              }

              @Override
              public J.Return visitReturn(J.Return _return, ExecutionContext ctx) {
                  assertThat(service.computeTreeLength(getCursor())).isEqualTo(51);
                  return super.visitReturn(_return, ctx);
              }
          })),
          java(
            """
              package com.example;
              
              // Comments should not affect line length calculation
              public class Test {
                  public int /* multiline comments can impact though */ example() {
                      String text = "This is a sample string to test line length calculation";
                      assert text != null;
                      // Another comment that is not counted
                      String invocation = String.valueOf("Both lines share the same length.");
                      return text.length() + invocation.length();
                  }
              
                  class Inner {
                      // Inner class to test nested structures
                  }
              }
              """
          )
        );
    }

    /**
     * Tests alignment calculations when the first element in a container is on the same line as the opening delimiter.
     * In these cases, subsequent elements should align with the first element's position, not just use
     * continuation indent.
     * <p>
     * Note: Record5 in the third java() block has intentionally bizarre indentation (e.g., "Integer t2," at column 0
     * and "Double u2," extremely far to the right) to verify the service handles any actual indentation pattern.
     */
    @Test
    void correctlyCalculatesIndentationToAlign() {
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
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("valueOf".equals(method.getSimpleName())) {
                      assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(34);
                  }
                  if ("reverse".equals(method.getSimpleName()) || "toString".equals(method.getSimpleName())) {
                      assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(41);
                  }
                  if ("repeat".equals(method.getSimpleName())) {
                      assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(31);
                  }
                  return super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().stream().anyMatch(v -> "stub".contains(v.getSimpleName().substring(0, 1)))) {
                      if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("1"))) {
                          assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(22);
                      }
                      if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("2"))) {
                          assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(26);
                      }
                      if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("3"))) {
                          assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(12);
                      }
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "parm".contains(v.getSimpleName().substring(0, 1)))) {
                      assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(67);
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              package com.example;
              
              public class Test {
                  public void /* multiline comments can impact though */ example(int p, String a, double r, Float m) {
                      // comments should not be counted
                      String invocation = String.valueOf("Both lines share the same length.");
                      String text = new StringBuilder().append("text").reverse().toString();
                      StringBuilder builder = 
                          new StringBuilder().append("text").repeat("text", 2);
                  }
              }
              """
          ),
          java(
            """
              public record Record1(String s1, Integer t1, Double u1, Float b1) {
                  public record Record2(String s2, Integer t2, Double u2, Float b2) {}
                  public record Record3(
                          String s3, Integer t3, Double u3, Float b3) {}
              }
              """
          ),
          java(
            """
              public record Record4(String s1, Integer t1, Double u1, Float b1) {
                  public record Record5(String s2,
              Integer t2,
                                                         Double u2,
              
              Float b2) {}
                  public record Record6(
              String s3, Integer t3, Double u3, Float b3) {}
              }
              """
          )
        );
    }

    @Test
    void correctlyCalculatesIndentationToAlignForMissingTypeInformation() {
        rewriteRun(
          spec ->
            spec.typeValidationOptions(TypeValidation.none()) //Deliberately testing missing types here
              .recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

                  @Nullable
                  SourcePositionService service;

                  @Override
                  public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                      service = cu.service(SourcePositionService.class);
                      return super.visitCompilationUnit(cu, ctx);
                  }

                  @Override
                  public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                      if (multiVariable.getVariables().stream().anyMatch(v -> "stub".contains(v.getSimpleName().substring(0, 1)))) {
                          if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("1"))) {
                              assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(22);
                          }
                          if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("2"))) {
                              assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(26);
                          }
                          if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("3"))) {
                              assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(12);
                          }
                      }
                      if (multiVariable.getVariables().stream().anyMatch(v -> "parm".contains(v.getSimpleName().substring(0, 1)))) {
                          assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(67);
                      }
                      return super.visitVariableDeclarations(multiVariable, ctx);
                  }
              })),
          java(
            """
              package com.example;
              
              public class Test {
                  public void /* multiline comments can impact though */ example(File p, File a, File r, File m) {
                  }
              }
              """
          ),
          java(
            """
              public record Record1(File s1, File t1, File u1, File b1) {
                  public record Record2(File s2, File t2, File u2, File b2) {}
                  public record Record3(
                          File s3, File t3, File u3, File b3) {}
              }
              """
          ),
          java(
            """
              public record Record4(File s1, File t1, File u1, File b1) {
                  public record Record5(File s2,
              File t2,
                                                         File u2,
              
              File b2) {}
                  public record Record6(
              File s3, File t3, File u3, File b3) {}
              }
              """
          )
        );
    }

    /**
     * Tests alignment calculations when the first element in a container starts on a new line after the opening delimiter.
     * In these cases, elements should NOT align with each other, but instead use the parent's indentation plus
     * continuation indent.
     * <p>
     * For example, in {@code example(\n    int p,\n    String a)}, the parameters start on a new line after the
     * opening parenthesis, so "String a" should be calculated based on the method's indentation + continuation,
     * not based on aligning with "int p".
     * <p>
     * Note: The method chains and parameters have inconsistent indentation (e.g., ".valueOf" starting at column 0)
     * to verify the service handles improperly formatted code correctly.
     */
    @Test
    void calculatesIndentationForNonAlignedElement() {
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
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(16);
                  return super.visitMethodInvocation(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().stream().anyMatch(v -> "stub".contains(v.getSimpleName().substring(0, 1)))) {
                      if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("1"))) {
                          assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(8);
                      }
                      if (multiVariable.getVariables().stream().anyMatch(v -> v.getSimpleName().endsWith("2"))) {
                          assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(12);
                      }
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "parm".contains(v.getSimpleName().substring(0, 1)))) {
                      assertThat(service.computeColumnToAlignTo(getCursor(), 8)).isEqualTo(12);
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }
          })),
          java(
            """
              package com.example;
              
              public class Test {
                  public void /* multiline comments can impact though */ example(
              int p,
              String a,
              double r,
              Float m) {
                      // comments should not be counted
                      String invocation = String
              .valueOf("Both lines share the same length.");
                      String text = new StringBuilder()
              .append("text")
              .reverse()
              .toString();
                  }
              }
              """
          ),
          java(
            """
              public record Record1(
              String s1,
              Integer t1,
              Double u1,
              Float b1) {
                  public record Record2(
              String s2,
              Integer t2,
              Double u2,
              Float b2) {}
              }
              """
          )
        );
    }

    @Test
    void correctlyCalculatesPosition() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

              @Nullable
              SourcePositionService service;

              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  service = cu.service(SourcePositionService.class);
                  assertResult(1, 1, 33, 2, 153); //entire file
                  assertResult(cu.getClasses().getFirst(), 4, 1, 33, 2, 153); //entire Test class declaration
                  return super.visitCompilationUnit(cu, ctx);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if ("Test".equals(classDecl.getSimpleName())) {
                      assertResult(4, 1, 33, 2, 153); //entire Test class declaration
                      assertResult(classDecl.getBody().getStatements().get(4), 17, 5, 20, 6, 77); //example2 method
                  }
                  if ("Inner".equals(classDecl.getSimpleName())) {
                      assertResult(27, 5, 30, 6, 85); //entire Inner class declaration
                  }
                  if ("RecordDeclaration".equals(classDecl.getSimpleName())) {
                      assertResult(32, 5, 32, 153, 153); //entire Inner class declaration
                      assertResult(classDecl.getPadding().getPrimaryConstructor(), 32, 30, 32, 35, 35);
                  }
                  return super.visitClassDeclaration(classDecl, ctx);
              }

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  switch (method.getSimpleName()) {
                      case "example1":
                          assertResult(10, 5, 15, 6, 61); // calculate the span as is.
                          assertMinimizedResult(10, 5, 13, 6, 77); // collapsing the method args to a single line using minimized (correctly calculate a span after changing an element)
                          assertResult(method.getPadding().getParameters(), 10, 55, 12, 18, 61); // calculate the span of the parameters within the declaration
                          assertMinimizedResult(m -> ((J.MethodDeclaration) m).getPadding().getParameters(), 10, 55, 10, 74, 74); // calculate the span of the parameters after minimization (correctly calculate a nested span after changing an element)
                          break;
                      case "example2":
                          assertResult(17, 5, 20, 6, 77);
                          assertMinimizedResult(17, 5, 20, 6, 77);
                          assertResult(method.getPadding().getParameters(), 17, 55, 17, 74, 74);
                          assertMinimizedResult(m -> ((J.MethodDeclaration) m).getPadding().getParameters(), 17, 55, 17, 74, 74);
                          break;
                      case "someVeryLongMethodNameThatIsAsLongAsTheMethodsAbove":
                          assertResult(22, 5, 25, 6, 75);
                          assertMinimizedResult(22, 5, 24, 6, 77); // minimization moves the opening curly causing the end-line to shift by 1 and the maxColumn by 2
                          assertResult(method.getPadding().getParameters(), 23, 69, 23, 74, 74);
                          assertMinimizedResult(m -> ((J.MethodDeclaration) m).getPadding().getParameters(), 23, 69, 23, 74, 74);
                          break;
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().stream().anyMatch(v -> "abc".contains(v.getSimpleName()))) {
                      assertResult(6, 5, 6, 38, 38);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "d".equals(v.getSimpleName()))) {
                      assertResult(7, 5, 7, 14, 14);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "e".equals(v.getSimpleName()))) {
                      assertResult(8, 5, 8, 10, 10);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "f".equals(v.getSimpleName()))) {
                      assertResult(32, 30, 32, 35, 35);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "sum1".equals(v.getSimpleName()))) {
                      assertResult(13, 9, 13, 25, 25);
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }

              private void assertResult(int line, int column, int endLine, int endColumn, int maxColumn) {
                  assertThat(service.positionOf(getCursor()))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).build());
              }

              private void assertResult(J j, int line, int column, int endLine, int endColumn, int maxColumn) {
                  assertThat(service.positionOf(getCursor(), j))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).build());
              }

              private void assertResult(JContainer<Statement> j, int line, int column, int endLine, int endColumn, int maxColumn) {
                  assertThat(service.positionOf(getCursor(), j))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).build());
              }

              private void assertMinimizedResult(int line, int column, int endLine, int endColumn, int maxColumn) {
                  assertThat(service.positionOf(new Cursor(getCursor().getParent(), minimize(getCursor().getValue()))))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).build());
              }

              private void assertMinimizedResult(Function<J, JContainer<Statement>> find, int line, int column, int endLine, int endColumn, int maxColumn) {
                  J minimized = minimize(getCursor().getValue());
                  assertThat(service.positionOf(new Cursor(getCursor().getParent(), minimized), find.apply(minimized)))
                    .isEqualTo(Span.builder().startLine(line).startColumn(column).endLine(endLine).maxColumn(maxColumn).endColumn(endColumn).build());
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
                              assertThat(service.positionOf(getCursor())).isEqualTo(Span.builder().startLine(6).startColumn(9).endLine(6).maxColumn(18).endColumn(18).build());
                              J.VariableDeclarations removedComment = multiVariable.withPrefix(multiVariable.getPrefix().withComments(emptyList()));
                              assertThat(service.positionOf(new Cursor(getCursor().getParent(), removedComment))).isEqualTo(Span.builder().startLine(5).startColumn(9).endLine(5).maxColumn(18).endColumn(18).build());
                              assertThat(service.positionOf(new Cursor(getCursor().getParent(), removedComment), removedComment)).isEqualTo(Span.builder().startLine(5).startColumn(9).endLine(5).maxColumn(18).endColumn(18).build());

                              //When the cursor does not contain the referential equal object, we throw
                              assertThrows(IllegalArgumentException.class, () -> assertThat(service.positionOf(getCursor(), removedComment)));
                              assertThrows(IllegalArgumentException.class, () -> assertThat(service.positionOf(getCursor().getParentTreeCursor(), removedComment)));
                          }
                          if ("method2".equals(method.getSimpleName())) {
                              assertThat(service.positionOf(getCursor())).isEqualTo(Span.builder().startLine(10).startColumn(9).endLine(10).maxColumn(18).endColumn(18).build());
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
                      assertThat(service.positionOf(getCursor())).isEqualTo(Span.builder().startLine(4).startColumn(5).endLine(7).maxColumn(40).endColumn(6).build());
                  } else if ("method2".equals(method.getSimpleName())) {
                      //The parent cursor (= block of class declaration) contains the modified method1 and in order to be used by the position calculation, you must pass that one.
                      assertThat(service.positionOf(getCursor().getParentTreeCursor(), method)).isEqualTo(Span.builder().startLine(9).startColumn(5).endLine(11).maxColumn(28).endColumn(6).build());
                      //Just passing this cursor is not enough for the accurate positioning as the modified element in not in this cursor and the service will start top down from JavaSourceFile -> no updated method is used.
                      assertThat(service.positionOf(getCursor(), method)).isEqualTo(Span.builder().startLine(12).startColumn(5).endLine(14).maxColumn(28).endColumn(6).build());
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
        J j = new JavaIsoVisitor<Integer>() {
            @Override
            public @Nullable <T> JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right,
                                                                  JRightPadded.Location loc,
                                                                  Integer ctx) {
                switch (loc) {
                    case METHOD_DECLARATION_PARAMETER:
                    case RECORD_STATE_VECTOR: {
                        if (right != null && right.getElement() instanceof J) {
                            //noinspection unchecked
                            right = right
                              .withAfter(minimized(right.getAfter()))
                              .withElement(((J) right.getElement()).withPrefix(minimized(((J) right.getElement()).getPrefix())));
                        }
                        break;
                    }
                }
                return super.visitRightPadded(right, loc, ctx);
            }

            @Override
            public Space visitSpace(@Nullable Space space,
                                    Space.Location loc,
                                    Integer ctx) {
                if (space == null) {
                    return super.visitSpace(space, loc, ctx);
                }
                if (space == tree.getPrefix()) {
                    return space;
                }
                switch (loc) {
                    case BLOCK_PREFIX:
                    case MODIFIER_PREFIX:
                    case METHOD_DECLARATION_PARAMETER_SUFFIX:
                    case METHOD_DECLARATION_PARAMETERS:
                    case METHOD_SELECT_SUFFIX:
                    case METHOD_INVOCATION_ARGUMENTS:
                    case METHOD_INVOCATION_ARGUMENT_SUFFIX:
                    case METHOD_INVOCATION_NAME:
                    case RECORD_STATE_VECTOR_SUFFIX: {
                        space = minimized(space);
                        break;
                    }
                }
                return super.visitSpace(space, loc, ctx);
            }

            //IntelliJ does not format when comments are present.
            private Space minimized(Space space) {
                if (space.getComments().isEmpty()) {
                    return space.getWhitespace().isEmpty() ? space : Space.EMPTY;
                }
                return space;
            }
        }.visit(tree, -1);
        if (j != tree) {
            j = new MinimumViableSpacingVisitor<>(null).visit(j, -1);
            j = new SpacesVisitor<>(IntelliJ.spaces(), null, null).visit(j, -1);
        }
        return (T) j;
    }
}
