/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateTest8Test implements RewriteTest {

    @Test
    void replaceMethodInvocationInsideReturn() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("String.valueOf(#{any(String)})")
                .build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("toString")) {
                      return t.apply(getCursor(), method.getCoordinates().replace(),
                        method.getSelect());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  String test(String s) {
                      return s.toString();
                  }
              }
              """,
            """
              class Test {
                  String test(String s) {
                      return String.valueOf(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceNewClassInsideReturn() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("new StringBuilder(#{any(String)})")
                .build();

              @Override
              public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  newClass = super.visitNewClass(newClass, ctx);
                  if (newClass.getClazz() != null &&
                      newClass.getClazz().toString().equals("StringBuffer") &&
                      newClass.getArguments().size() == 1) {
                      return t.apply(getCursor(), newClass.getCoordinates().replace(),
                        newClass.getArguments().get(0));
                  }
                  return newClass;
              }
          })),
          java(
            """
              class Test {
                  CharSequence test(String s) {
                      return new StringBuffer(s);
                  }
              }
              """,
            """
              class Test {
                  CharSequence test(String s) {
                      return new StringBuilder(s);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7153")
    @Test
    void replaceMethodInvocationInsideTypeCast() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("String.valueOf(#{any(String)})")
                .build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("toString")) {
                      return t.apply(getCursor(), method.getCoordinates().replace(),
                        method.getSelect());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  Object test(String s) {
                      return (Object) s.toString();
                  }
              }
              """,
            """
              class Test {
                  Object test(String s) {
                      return (Object) String.valueOf(s);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7153")
    @Test
    void replaceNewClassInsideTypeCast() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("new StringBuilder(#{any(String)})")
                .build();

              @Override
              public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  newClass = super.visitNewClass(newClass, ctx);
                  if (newClass.getClazz() != null &&
                      newClass.getClazz().toString().equals("StringBuffer") &&
                      newClass.getArguments().size() == 1) {
                      return t.apply(getCursor(), newClass.getCoordinates().replace(),
                        newClass.getArguments().get(0));
                  }
                  return newClass;
              }
          })),
          java(
            """
              class Test {
                  Object test(String s) {
                      return (CharSequence) new StringBuffer(s);
                  }
              }
              """,
            """
              class Test {
                  Object test(String s) {
                      return (CharSequence) new StringBuilder(s);
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
            .typeValidationOptions(org.openrewrite.test.TypeValidation.builder().methodInvocations(false).build())
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

    @DocumentExample
    @Test
    void parameterizedMatch() {
        JavaTemplate template = JavaTemplate.builder("#{any(java.util.List<String>)}")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  // the cursor points at the parent when `visitTypeName()` is called
                  if (template.matches(new Cursor(getCursor(), nameTree))) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<Object> o;
                  List<String> s;
                  List<Number> n;
                  List<Integer> i;
                  List<java.lang.Object> qo;
                  List<java.lang.String> qs;
                  List<java.lang.Number> qn;
                  List<java.lang.Integer> qi;
              }
              """,
            """
              import java.util.List;
              class Test {
                  List<Object> o;
                  /*~~>*/List<String> s;
                  List<Number> n;
                  List<Integer> i;
                  List<java.lang.Object> qo;
                  /*~~>*/List<java.lang.String> qs;
                  List<java.lang.Number> qn;
                  List<java.lang.Integer> qi;
              }
              """
          )
        );
    }

    @Test
    void parameterizedMatchWithBounds() {
        JavaTemplate template = JavaTemplate.builder("#{any(java.util.List<? extends java.lang.Number>)}")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  // the cursor points at the parent when `visitTypeName()` is called
                  if (getCursor().getValue() != nameTree && template.matches(new Cursor(getCursor(), nameTree))) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<Object> o;
                  List<String> s;
                  List<Number> n;
                  List<Integer> i;
                  List<java.lang.Object> qo;
                  List<java.lang.String> qs;
                  List<java.lang.Number> qn;
                  List<java.lang.Integer> qi;
              }
              """,
            """
              import java.util.List;
              class Test {
                  List<Object> o;
                  List<String> s;
                  /*~~>*/List<Number> n;
                  /*~~>*/List<Integer> i;
                  List<java.lang.Object> qo;
                  List<java.lang.String> qs;
                  /*~~>*/List<java.lang.Number> qn;
                  /*~~>*/List<java.lang.Integer> qi;
              }
              """
          )
        );
    }

    @Test
    void parameterizedArrayMatch() {
        JavaTemplate template = JavaTemplate.builder("#{anyArray(java.util.List<String>)}")
          .build();

        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                  // the cursor points at the parent when `visitTypeName()` is called
                  if (template.matches(new Cursor(getCursor(), nameTree))) {
                      return SearchResult.found(nameTree);
                  }
                  return super.visitTypeName(nameTree, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  List<Object>[] o;
                  List<String>[] s;
                  List<Number>[] n;
                  List<Integer>[] i;
                  List<java.lang.Object>[] qo;
                  List<java.lang.String>[] qs;
                  List<java.lang.Number>[] qn;
                  List<java.lang.Integer>[] qi;
              }
              """,
            """
              import java.util.List;
              class Test {
                  List<Object>[] o;
                  /*~~>*/List<String>[] s;
                  List<Number>[] n;
                  List<Integer>[] i;
                  List<java.lang.Object>[] qo;
                  /*~~>*/List<java.lang.String>[] qs;
                  List<java.lang.Number>[] qn;
                  List<java.lang.Integer>[] qi;
              }
              """
          )
        );
    }
}
