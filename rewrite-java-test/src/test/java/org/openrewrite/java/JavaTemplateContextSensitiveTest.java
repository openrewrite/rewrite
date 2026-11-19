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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "StatementWithEmptyBody", "UnusedAssignment", "ResultOfMethodCallIgnored"})
class JavaTemplateContextSensitiveTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2090")
    @Test
    void assignmentWithinIfPredicate() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                  if ((assignment.getAssignment() instanceof J.Literal) &&
                    ((J.Literal) assignment.getAssignment()).getValue().equals(1)) {
                      return JavaTemplate.builder("value = 0")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), assignment.getCoordinates().replace());
                  }
                  return assignment;
              }
          })),
          java(
            """
              class A {
                  void foo() {
                      double value = 0;
                      if ((value = 1) == 0) {}
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      double value = 0;
                      if ((value = 0) == 0) {}
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1092")
    @Test
    void methodInvocationReplacementHasContextAboutLocalVariables() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("clear".equals(method.getSimpleName())) {
                      return JavaTemplate.builder("words.add(\"jon\");")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace());
                  }
                  return method;
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<String> words;
                  void test() {
                      words.clear();
                  }
              }
              """,
            """
              import java.util.List;
              class Test {
                  List<String> words;
                  void test() {
                      words.add("jon");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7443")
    @Test
    void replaceExpressionInSwitchExpressionRuleArm() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final MethodMatcher emptyListMatcher = new MethodMatcher("java.util.Collections emptyList()");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (emptyListMatcher.matches(m)) {
                      return JavaTemplate.builder("List.of()")
                        .contextSensitive()
                        .imports("java.util.List")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace());
                  }
                  return m;
              }
          })),
          java(
            """
              import java.util.Collections;
              import java.util.List;

              class T {
                  List<Object> m(int i) {
                      return switch (i) {
                          case 0 -> Collections.emptyList();
                          default -> null;
                      };
                  }
              }
              """,
            """
              import java.util.Collections;
              import java.util.List;

              class T {
                  List<Object> m(int i) {
                      return switch (i) {
                          case 0 -> List.of();
                          default -> null;
                      };
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7443")
    @Test
    void replaceExpressionInSwitchExpressionColonArm() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final MethodMatcher emptyListMatcher = new MethodMatcher("java.util.Collections emptyList()");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (emptyListMatcher.matches(m)) {
                      return JavaTemplate.builder("List.of()")
                        .contextSensitive()
                        .imports("java.util.List")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace());
                  }
                  return m;
              }
          })),
          java(
            """
              import java.util.Collections;
              import java.util.List;

              class T {
                  List<Object> m(int i) {
                      return switch (i) {
                          case 0: yield Collections.emptyList();
                          default: yield null;
                      };
                  }
              }
              """,
            """
              import java.util.Collections;
              import java.util.List;

              class T {
                  List<Object> m(int i) {
                      return switch (i) {
                          case 0: yield List.of();
                          default: yield null;
                      };
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7443")
    @Test
    void replaceExpressionInSwitchExpressionPreservesUnrelatedArms() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final MethodMatcher emptyListMatcher = new MethodMatcher("java.util.Collections emptyList()");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                  if (emptyListMatcher.matches(m)) {
                      return JavaTemplate.builder("List.of()")
                        .contextSensitive()
                        .imports("java.util.List")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace());
                  }
                  return m;
              }
          })),
          java(
            """
              import java.util.Arrays;
              import java.util.Collections;
              import java.util.List;

              class T {
                  List<Object> m(int i) {
                      return switch (i) {
                          case 0 -> Arrays.asList("a");
                          case 1 -> Collections.emptyList();
                          case 2 -> Arrays.asList("b");
                          default -> null;
                      };
                  }
              }
              """,
            """
              import java.util.Arrays;
              import java.util.Collections;
              import java.util.List;

              class T {
                  List<Object> m(int i) {
                      return switch (i) {
                          case 0 -> Arrays.asList("a");
                          case 1 -> List.of();
                          case 2 -> Arrays.asList("b");
                          default -> null;
                      };
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1339")
    @Test
    void templateStatementIsWithinTryWithResourcesBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  var md = getCursor().firstEnclosing(J.MethodDeclaration.class);
                  if (md != null && "createBis".equals(md.getSimpleName())) {
                      return newClass;
                  }
                  if (newClass.getType() != null &&
                      "java.io.ByteArrayInputStream".equals(TypeUtils.asFullyQualified(newClass.getType()).getFullyQualifiedName()) &&
                      !newClass.getArguments().isEmpty()) {
                      return JavaTemplate.builder("createBis(#{anyArray()})")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), newClass.getCoordinates().replace(), newClass.getArguments().getFirst());
                  }
                  return newClass;
              }
          })),
          java(
            """
              import java.io.*;
              import java.nio.charset.StandardCharsets;

              class Test {
                  ByteArrayInputStream createBis(byte[] bytes) {
                      return new ByteArrayInputStream(bytes);
                  }

                  void doSomething() {
                      String sout = "";
                      try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) {
                          new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8));
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
              """,
            """
              import java.io.*;
              import java.nio.charset.StandardCharsets;

              class Test {
                  ByteArrayInputStream createBis(byte[] bytes) {
                      return new ByteArrayInputStream(bytes);
                  }

                  void doSomething() {
                      String sout = "";
                      try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) {
                          createBis("bytes".getBytes(StandardCharsets.UTF_8));
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceContextSensitiveMethodInvocationInsideAssignment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("visitClassDeclaration") &&
                      method.getSelect() != null &&
                      !(method.getSelect() instanceof J.Identifier &&
                        "super".equals(((J.Identifier) method.getSelect()).getSimpleName()))) {
                      return JavaTemplate.builder("#{any()}.visit(#{any()}, #{any()}, getCursor().getParentTreeCursor())")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(),
                          method.getSelect(), method.getArguments().get(0), method.getArguments().get(1));
                  }
                  return method;
              }
          })),
          java(
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {}, new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                              J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                              cd = new JavaVisitor<ExecutionContext>() {}.visitClassDeclaration(cd, ctx);
                              return cd;
                          }
                      });
                  }
              }
              """,
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {}, new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                              J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                              cd = new JavaVisitor<ExecutionContext>() {
                              }.visit(cd, ctx, getCursor().getParentTreeCursor());
                              return cd;
                          }
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceContextSensitiveMethodInvocationInsideTypeCastInReturn() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("visitClassDeclaration") &&
                      method.getSelect() != null &&
                      !(method.getSelect() instanceof J.Identifier &&
                        "super".equals(((J.Identifier) method.getSelect()).getSimpleName()))) {
                      return JavaTemplate.builder("#{any()}.visit(#{any()}, #{any()}, getCursor().getParentTreeCursor())")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(),
                          method.getSelect(), method.getArguments().get(0), method.getArguments().get(1));
                  }
                  return method;
              }
          })),
          java(
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                              J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                              return (J.ClassDeclaration) new JavaVisitor<ExecutionContext>() {}.visitClassDeclaration(cd, ctx);
                          }
                      };
                  }
              }
              """,
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                              J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                              return (J.ClassDeclaration) new JavaVisitor<ExecutionContext>() {
                              }.visit(cd, ctx, getCursor().getParentTreeCursor());
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceContextSensitiveExpressionWithInnerClassOfAnonymousClass() {
        rewriteRun(
          spec -> spec
            .parser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("visitMethodDeclaration") &&
                      method.getSelect() != null &&
                      method.getArguments().size() == 2 &&
                      !(method.getSelect() instanceof J.Identifier &&
                        "super".equals(((J.Identifier) method.getSelect()).getSimpleName()))) {
                      return JavaTemplate.builder("#{any()}.visit(#{any()}, #{any()}, getCursor().getParentTreeCursor())")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(),
                          method.getSelect(), method.getArguments().get(0), method.getArguments().get(1));
                  }
                  return method;
              }
          })),
          java(
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          class InnerVisitor extends JavaIsoVisitor<ExecutionContext> {
                          }

                          @Override
                          public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                              J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                              md = new InnerVisitor().visitMethodDeclaration(md, ctx);
                              return md;
                          }
                      };
                  }
              }
              """,
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          class InnerVisitor extends JavaIsoVisitor<ExecutionContext> {
                          }

                          @Override
                          public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                              J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                              md = new InnerVisitor().visit(md, ctx, getCursor().getParentTreeCursor());
                              return md;
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceContextSensitiveMethodInvocationStandaloneStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("visitClassDeclaration") &&
                      method.getSelect() != null &&
                      !(method.getSelect() instanceof J.Identifier &&
                        "super".equals(((J.Identifier) method.getSelect()).getSimpleName()))) {
                      return JavaTemplate.builder("#{any()}.visit(#{any()}, #{any()}, getCursor().getParentTreeCursor())")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(),
                          method.getSelect(), method.getArguments().get(0), method.getArguments().get(1));
                  }
                  return method;
              }
          })),
          java(
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                              J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                              new JavaVisitor<ExecutionContext>() {}.visitClassDeclaration(cd, ctx);
                              return cd;
                          }
                      };
                  }
              }
              """,
            """
              import org.openrewrite.*;
              import org.openrewrite.java.JavaIsoVisitor;
              import org.openrewrite.java.JavaVisitor;
              import org.openrewrite.java.tree.J;

              class MyRecipe extends Recipe {
                  @Override
                  public String getDisplayName() { return ""; }
                  @Override
                  public String getDescription() { return ""; }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                              J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                              new JavaVisitor<ExecutionContext>() {
                              }.visit(cd, ctx, getCursor().getParentTreeCursor());
                              return cd;
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceContextSensitiveMethodInvocationInAssignmentWithLocalClassType() {
        rewriteRun(
          // the template writes a local or anonymous class as its supertype, so it infers that type
          spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build())
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("singletonMap") &&
                      method.getMethodType() != null &&
                      method.getMethodType().getDeclaringType().getFullyQualifiedName().equals("java.util.Collections")) {
                      return JavaTemplate.builder("Map.of(#{any()}, #{any()})")
                        .contextSensitive()
                        .imports("java.util.Map")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(),
                          method.getArguments().toArray());
                  }
                  return method;
              }
          })),
          java(
            """
              import java.util.Collections;
              import java.util.List;
              import java.util.Map;

              class Test {
                  void test() {
                      class Inner {
                          String value;
                      }
                      class Entity {
                          Map<String, List<Map<String, Inner>>> map;
                      }
                      var entity = new Entity();
                      entity.map = Collections.singletonMap("collection",
                              Collections.singletonList(Collections.singletonMap("dest", new Inner())));
                  }
              }
              """,
            """
              import java.util.Collections;
              import java.util.List;
              import java.util.Map;

              class Test {
                  void test() {
                      class Inner {
                          String value;
                      }
                      class Entity {
                          Map<String, List<Map<String, Inner>>> map;
                      }
                      var entity = new Entity();
                      entity.map = Map.of("collection", Collections.singletonList(Map.of("dest", new Inner())));
                  }
              }
              """
          )
        );
    }

    @Test
    void contextSensitiveTemplateGivesArrayVariablesTheirDeclaredDimensions() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (method.getSimpleName().equals("clear")) {
                      return JavaTemplate.builder("reset(a, b, c, d)")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  private int[] a, b[];
                  private int c[];
                  void reset(int[] a, int[][] b, int[] c, int[][] d) {}
                  void clear() {}
                  void test() {
                      int[][] d = new int[2][3];
                      clear();
                  }
              }
              """,
            """
              class Test {
                  private int[] a, b[];
                  private int c[];
                  void reset(int[] a, int[][] b, int[] c, int[][] d) {}
                  void clear() {}
                  void test() {
                      int[][] d = new int[2][3];
                      reset(a, b, c, d);
                  }
              }
              """
          )
        );
    }

    @Test
    void contextSensitiveTemplateStubDeclaresVarargsParameterAsArray() {
        List<String> stubs = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  if ("unused".equals(literal.getValue())) {
                      return JavaTemplate.builder("\"all\"")
                        .contextSensitive()
                        .doBeforeParseTemplate(stubs::add)
                        .build()
                        .apply(getCursor(), literal.getCoordinates().replace());
                  }
                  return literal;
              }
          })),
          java(
            """
              class Test {
                  void test(@SuppressWarnings("unused") String... args) {
                  }
              }
              """,
            """
              class Test {
                  void test(@SuppressWarnings("all") String... args) {
                  }
              }
              """
          )
        );
        // an annotation argument cannot refer to the parameter, so only the stub shows the type it is declared with
        assertThat(stubs)
          .isNotEmpty()
          .allSatisfy(stub -> assertThat(stub).contains("String[] args"));
    }

    @Test
    void contextSensitiveTemplateInitializingOneOfSeveralVariablesTakesThatVariablesType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (method.getSimpleName().equals("make")) {
                      return JavaTemplate.builder("java.util.Collections.emptyList()")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<String> make() { return null; }
                  void test() {
                      List<String> a = make(), b[] = null;
                  }
              }
              """,
            """
              import java.util.List;
              class Test {
                  List<String> make() { return null; }
                  void test() {
                      List<String> a = java.util.Collections.emptyList(), b[] = null;
                  }
              }
              """
          )
        );
    }

    @Test
    void contextSensitiveTemplateSeesVarLocalsWithTheirInferredTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (method.getSimpleName().equals("clear")) {
                      return JavaTemplate.builder("take(arr, n)")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  void take(int[] a, int n) {}
                  void take(int[] a, long n) {}
                  void clear() {}
                  void test() {
                      var arr = new int[3];
                      var n = 1L;
                      clear();
                  }
              }
              """,
            """
              class Test {
                  void take(int[] a, int n) {}
                  void take(int[] a, long n) {}
                  void clear() {}
                  void test() {
                      var arr = new int[3];
                      var n = 1L;
                      take(arr, n);
                  }
              }
              """
          )
        );
    }
}
