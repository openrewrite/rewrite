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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.SourcePositionService;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
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
          java (
            """
            public record Record1(String s1, Integer t1, Double u1, Float b1) {
                public record Record2(String s2, Integer t2, Double u2, Float b2) {}
                public record Record3(
                        String s3, Integer t3, Double u3, Float b3) {}
            }
            """
          ),
          java (
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
          java (
            """
            public record Record1(File s1, File t1, File u1, File b1) {
                public record Record2(File s2, File t2, File u2, File b2) {}
                public record Record3(
                        File s3, File t3, File u3, File b3) {}
            }
            """
          ),
          java (
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
          java (
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
    void correctlyCalculatesDeclarationLength() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new JavaIsoVisitor<>() {

              @Nullable
              SourcePositionService service;

              @Override
              public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                  service = cu.service(SourcePositionService.class);
                  assertResult(1, 0, 33, 152); //entire file
                  assertResult(cu.getClasses().getFirst(), 4, 0, 30, 152); //entire Test class declaration
                  return super.visitCompilationUnit(cu, ctx);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if ("Test".equals(classDecl.getSimpleName())) {
                      assertResult(4, 0, 30, 152); //entire Test class declaration
                      assertResult(classDecl.getBody().getStatements().get(4), 17, 4, 4, 76); //example2 method
                  }
                  if ("Inner".equals(classDecl.getSimpleName())) {
                      assertResult(27, 4, 4, 84); //entire Inner class declaration
                  }
                  if ("RecordDeclaration".equals(classDecl.getSimpleName())) {
                      assertResult(32, 4, 1, 152); //entire Inner class declaration
                      assertResult(classDecl.getPadding().getPrimaryConstructor(), 32, 29, 1, 34);
                  }
                  return super.visitClassDeclaration(classDecl, ctx);
              }

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  switch (method.getSimpleName()) {
                      case "example1":
                          assertMinimizedResult(10, 4, 4, 76);
                          assertResult(10, 4, 6, 60);
                          assertMinimizedResult(method.getPadding().getParameters(), 10, 54, 1, 73);
                          assertResult(method.getPadding().getParameters(), 10, 54, 3, 60);
                          break;
                      case "example2":
                          assertMinimizedResult(17, 4, 4, 76);
                          assertResult(17, 4, 4, 76);
                          assertMinimizedResult(method.getPadding().getParameters(), 17, 54, 1, 73);
                          assertResult(method.getPadding().getParameters(), 17, 54, 1, 73);
                          break;
                      case "someVeryLongMethodNameThatIsAsLongAsTheMethodsAbove":
                          assertMinimizedResult(22, 4, 3, 76);
                          assertResult(22, 4, 4, 74);
                          assertMinimizedResult(method.getPadding().getParameters(), 23, 68, 1, 73);
                          assertResult(method.getPadding().getParameters(), 23, 68, 1, 73);
                          break;
                  }
                  return super.visitMethodDeclaration(method, ctx);
              }

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getVariables().stream().anyMatch(v -> "abc".contains(v.getSimpleName()))) {
                      assertResult(6, 4, 1, 37);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "d".equals(v.getSimpleName()))) {
                      assertResult(7, 4, 1, 13);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "e".equals(v.getSimpleName()))) {
                      assertResult(8, 4, 1, 9);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "f".equals(v.getSimpleName()))) {
                      assertResult(32, 29, 1, 34);
                  }
                  if (multiVariable.getVariables().stream().anyMatch(v -> "sum1".equals(v.getSimpleName()))) {
                      assertResult(13, 8, 1, 24);
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
              }

              private void assertResult(int line, int column, int lines, int maxColumn) {
                  assertThat(service.retriever(getCursor()).find())
                    .usingRecursiveComparison()
                    .isEqualTo(new SourcePositionService.SourcePositionRetriever.SearchResult(line, column, maxColumn, lines));
              }

              private void assertResult(J j, int line, int column, int lines, int maxColumn) {
                  assertThat(service.retriever(getCursor()).find(j))
                    .usingRecursiveComparison()
                    .isEqualTo(new SourcePositionService.SourcePositionRetriever.SearchResult(line, column, maxColumn, lines));
              }

              private void assertResult(JContainer<Statement> j, int line, int column, int lines, int maxColumn) {
                  assertThat(service.retriever(getCursor()).find(j))
                    .usingRecursiveComparison()
                    .isEqualTo(new SourcePositionService.SourcePositionRetriever.SearchResult(line, column, maxColumn, lines));
              }

              private void assertMinimizedResult(int line, int column, int lines, int maxColumn) {
                  assertThat(service.retriever(getCursor()).minimized(IntelliJ.spaces()).find())
                    .usingRecursiveComparison()
                    .isEqualTo(new SourcePositionService.SourcePositionRetriever.SearchResult(line, column, maxColumn, lines));
              }

              private void assertMinimizedResult(JContainer<Statement> j, int line, int column, int lines, int maxColumn) {
                  assertThat(service.retriever(getCursor()).minimized(IntelliJ.spaces()).find(j))
                    .usingRecursiveComparison()
                    .isEqualTo(new SourcePositionService.SourcePositionRetriever.SearchResult(line, column, maxColumn, lines));
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
}
