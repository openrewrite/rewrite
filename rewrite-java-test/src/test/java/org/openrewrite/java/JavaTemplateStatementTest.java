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
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "StatementWithEmptyBody", "UnusedAssignment", "ResultOfMethodCallIgnored"})
class JavaTemplateStatementTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/3102")
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Test
    void expressionAsStatementWithoutTerminatingSemicolon() {
        // NOTE: I am not convinced that we really need to support this case. It is not valid Java.
        // But since this has been working up until now, I am leaving it in.
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if (classDecl.getBody().getStatements().size() > 1) {
                      return classDecl;
                  }
                  return JavaTemplate.apply("""
                      void m2() {
                      	  #{any()}
                      }
                      """, getCursor(), classDecl.getBody().getStatements().getFirst().getCoordinates().after(), ((J.MethodDeclaration) classDecl.getBody().getStatements().getFirst()).getBody().getStatements().getFirst());
              }
          })),
          java(
            """
              class T {
                  void m() {
                      hashCode();
                  }
              }
              """,
            """
              class T {
                  void m() {
                      hashCode();
                  }

                  void m2() {
                      hashCode();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStatementKeepsItsLocalIndent() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (method.getSimpleName().equals("hashCode")) {
                      return JavaTemplate.apply("toString()", getCursor(), method.getCoordinates().replace());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              class T {
              \tvoid m() {
              \t
                    hashCode();
                    notify();
              \t}
              }
              """,
            """
              class T {
              \tvoid m() {
              \t
                    toString();
                    notify();
              \t}
              }
              """
          )
        );
    }

    @Test
    void replaceStatementIndentsGeneratedBlockLocally() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(singletonList(IntelliJ.defaults())))
            .recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitIf(J.If iff, ExecutionContext ctx) {
                  return JavaTemplate.apply("switch (0) { default -> {} }", getCursor(), iff.getCoordinates().replace());
              }
          })),
          java(
            """
              class T {
                  int first;
                  int last;
              \tvoid m() {
              \t\tif (true) {
              \t\t\tnotify();
              \t\t}
              \t\tnotify();
              \t}
              }
              """,
            """
              class T {
                  int first;
                  int last;
              \tvoid m() {
              \t\tswitch (0) {
              \t\t\tdefault -> {
              \t\t\t}
              \t\t}
              \t\tnotify();
              \t}
              }
              """
          )
        );
    }

    @Test
    void replaceStatementUsesSpacesInsideMixedPrefix() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(singletonList(IntelliJ.defaults())))
            .recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitIf(J.If iff, ExecutionContext ctx) {
                  return JavaTemplate.apply("switch (0) { default -> {} }", getCursor(), iff.getCoordinates().replace());
              }
          })),
          java(
            """
              class T {
                  int first;
                  int last;
              \tvoid m() {
              \t    if (true) {
              \t      notify();
              \t    }
              \t    notify();
              \t}
              }
              """,
            """
              class T {
                  int first;
                  int last;
              \tvoid m() {
              \t    switch (0) {
              \t      default -> {
              \t      }
              \t    }
              \t    notify();
              \t}
              }
              """
          )
        );
    }

    @Test
    void firstClassMember() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBlock(J.Block block, ExecutionContext ctx) {
                  if (block.getStatements().size() == 1) {
                      return JavaTemplate.builder("String x = s;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), block.getCoordinates().firstStatement());
                  }
                  return super.visitBlock(block, ctx);
              }
          })),
          java(
            """
              class T {
                  String s = "s";
              }
              """,
            """
              class T {
                  String x = s;
                  String s = "s";
              }
              """
          )
        );
    }

    @Test
    void lastClassMember() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBlock(J.Block block, ExecutionContext ctx) {
                  if (block.getStatements().size() == 1) {
                      return JavaTemplate.builder("String x = s;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), block.getStatements().getFirst().getCoordinates().after());
                  }
                  return super.visitBlock(block, ctx);
              }
          })),
          java(
            """
              class T {
                  String s = "s";
              }
              """,
            """
              class T {
                  String s = "s";
                  String x = s;
              }
              """
          )
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void addStatementInIfBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final MethodMatcher lowerCaseMatcher = new MethodMatcher("java.lang.String toLowerCase()");

              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block newBlock = super.visitBlock(block, ctx);
                  if (newBlock.getStatements().stream().noneMatch(J.VariableDeclarations.class::isInstance)) {
                      return newBlock;
                  }
                  for (Statement statement : newBlock.getStatements()) {
                      if (statement instanceof J.MethodInvocation && lowerCaseMatcher.matches((J.MethodInvocation) statement)) {
                          return newBlock;
                      }
                  }
                  Statement lastStatement = newBlock.getStatements().get(newBlock.getStatements().size() - 1);
                  return JavaTemplate
                    .builder("s.toLowerCase();")
                    .contextSensitive()
                    .javaParser(JavaParser.fromJavaVersion())
                    .build()
                    .apply(new Cursor(getCursor().getParent(), newBlock), lastStatement.getCoordinates().before());
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      if (true) {
                          String s = "Hello world!";
                          System.out.println(s);
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      if (true) {
                          String s = "Hello world!";
                          s.toLowerCase();
                          System.out.println(s);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSingleStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitAssert(J.Assert assert_, ExecutionContext p) {
                  return JavaTemplate.builder(
                      """
                        if(n != 1) {
                          n++;
                        }"""
                    )
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), assert_.getCoordinates().replace());
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      if (n != 1) {
                          n++;
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void replaceStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(1);
                  if (statement instanceof J.Unary) {
                      return JavaTemplate.builder("n = 2;\nn = 3;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), statement.getCoordinates().replace());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                      n++;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                      n = 2;
                      n = 3;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void replaceStatementInBlockPreservesLeadingBlankLine() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(1);
                  if (statement instanceof J.Unary) {
                      return JavaTemplate.builder("n = 2;\nn = 3;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), statement.getCoordinates().replace());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;

                      n++;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;

                      n = 2;
                      n = 3;
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().getFirst();
                  if (statement instanceof J.Assignment) {
                      return JavaTemplate.builder("assert n == 0;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), statement.getCoordinates().before());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void afterStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return JavaTemplate.builder("n = 1;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getBody().getStatements().getFirst().getCoordinates().after());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    void firstStatementInClassBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getBody().getStatements().size() == 1) {
                      return JavaTemplate.apply("int m;", getCursor(), classDecl.getBody().getCoordinates().firstStatement());
                  }
                  return classDecl;
              }
          })),
          java(
            """
              class Test {
                  // comment
                  int n;
              }
              """,
            """
              class Test {
                  int m;
                  // comment
                  int n;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    void firstStatementInMethodBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return JavaTemplate.apply("int m = 0;", getCursor(), method.getBody().getCoordinates().firstStatement());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      // comment
                      int n = 1;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      int m = 0;
                      // comment
                      int n = 1;
                  }
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void lastStatementInClassBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if (classDecl.getBody().getStatements().isEmpty()) {
                      return JavaTemplate.apply("int n;", getCursor(), classDecl.getBody().getCoordinates().lastStatement());
                  }
                  return classDecl;
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              class Test {
                  int n;
              }
              """
          )
        );
    }

    @Test
    void lastStatementInMethodBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (method.getBody().getStatements().size() == 1) {
                      return JavaTemplate.builder("n = 1;")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), method.getBody().getCoordinates().lastStatement());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStatementRequiringNewImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitAssert(J.Assert assert_, ExecutionContext ctx) {
                  maybeAddImport("java.util.List");
                  return JavaTemplate.builder("List<String> s = null;")
                    .imports("java.util.List")
                    .build()
                    .apply(getCursor(), assert_.getCoordinates().replace());
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int n;
                  void test() {
                      List<String> s = null;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1505")
    @Test
    void methodDeclarationWithComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                  var cd = classDecl;
                  if (cd.getBody().getStatements().isEmpty()) {
                      cd = JavaTemplate.apply("""
                          /**
                           * comment
                           */
                          void foo() {
                          }
                          """, getCursor(), cd.getBody().getCoordinates().firstStatement());
                  }
                  return cd;
              }
          })),
          java(
            """
              class A {

              }
              """,
            """
              class A {
                  /**
                   * comment
                   */
                  void foo() {
                  }

              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8327")
    @Test
    void unreachableCoordinatesFailLoudly() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build()
          .parse(
            """
              class Test {
                  void test() {
                      int n = 1;
                      System.out.println(hashCode());
                  }
              }
              """
          )
          .map(J.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();

        // The coordinate names a statement that is not inside `scope`, so nothing can ever match it
        assertThatThrownBy(() -> new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                if ("println".equals(method.getSimpleName())) {
                    J.MethodDeclaration enclosing = getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class);
                    Statement sibling = requireNonNull(enclosing.getBody()).getStatements().getFirst();
                    return JavaTemplate.apply("0", getCursor(), sibling.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, p);
            }
        }.visit(cu, 0))
          .rootCause()
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("JavaTemplate coordinates were never matched")
          .hasMessageContaining(J.VariableDeclarations.class.getName());
    }

    @Test
    void coordinatesNestedInArgumentsAreReachable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("println".equals(method.getSimpleName()) && method.getArguments().getFirst() instanceof J.MethodInvocation) {
                      return JavaTemplate.apply("0", getCursor(), method.getArguments().getFirst().getCoordinates().replace());
                  }
                  return super.visitMethodInvocation(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      System.out.println(hashCode());
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      System.out.println(0);
                  }
              }
              """
          )
        );
    }

    @Test
    void statementInNestedBlockIsReachableFromEnclosingMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                  if (m.getBody() == null || !(m.getBody().getStatements().getFirst() instanceof J.Try t) ||
                      t.getBody().getStatements().size() != 1) {
                      return m;
                  }
                  Statement nested = t.getBody().getStatements().getFirst();
                  return JavaTemplate.apply("int b = 2;", updateCursor(m), nested.getCoordinates().after());
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      try {
                          int a = 1;
                      } finally {
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      try {
                          int a = 1;
                          int b = 2;
                      } finally {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void statementInLambdaBlockIsReachableFromEnclosingMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                  if (m.getBody() == null || !(m.getBody().getStatements().getFirst() instanceof J.VariableDeclarations v) ||
                      !(v.getVariables().getFirst().getInitializer() instanceof J.Lambda l) ||
                      !(l.getBody() instanceof J.Block b) || b.getStatements().size() != 1) {
                      return m;
                  }
                  Statement nested = b.getStatements().getFirst();
                  return JavaTemplate.apply("int b = 2;", updateCursor(m), nested.getCoordinates().before());
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      Runnable r = () -> {
                          int a = 1;
                      };
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      Runnable r = () -> {
                          int b = 2;
                          int a = 1;
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void addElseBranchToAnIfThatHasNone() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  if (iff.getElsePart() != null) {
                      return iff;
                  }
                  return JavaTemplate.apply("if (true) {\n}", getCursor(), iff.getCoordinates().addElseBranch());
              }
          })),
          java(
            """
              class Test {
                  void test(int n) {
                      if (n == 1) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void test(int n) {
                      if (n == 1) {
                      } else if (true) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addElseBranchExtendsAnExistingChain() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  // Only the head of the chain, and only while it still falls through to a plain else
                  if (getCursor().getParentTreeCursor().getValue() instanceof J.If.Else ||
                      iff.getElsePart() == null || !(iff.getElsePart().getBody() instanceof J.Block)) {
                      return super.visitIf(iff, ctx);
                  }
                  return JavaTemplate.apply("if (true) {\n}", getCursor(), iff.getCoordinates().addElseBranch());
              }
          })),
          java(
            """
              class Test {
                  void test(int n) {
                      if (n == 1) {
                      } else {
                          System.out.println(n);
                      }
                  }
              }
              """,
            """
              class Test {
                  void test(int n) {
                      if (n == 1) {
                      } else if (true) {
                      } else {
                          System.out.println(n);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addElseBranchWithABlockMakesAPlainElse() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.If visitIf(J.If iff, ExecutionContext ctx) {
                  if (iff.getElsePart() != null) {
                      return iff;
                  }
                  return JavaTemplate.apply("{\n    System.out.println(\"fallthrough\");\n}", getCursor(),
                    iff.getCoordinates().addElseBranch());
              }
          })),
          java(
            """
              class Test {
                  void test(int n) {
                      if (n == 1) {
                      }
                  }
              }
              """,
            """
              class Test {
                  void test(int n) {
                      if (n == 1) {
                      } else {
                          System.out.println("fallthrough");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addElseBranchRejectsANonIfWhenAnElseAlreadyExists() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build()
          .parse(
            """
              class Test {
                  void test(int n) {
                      if (n == 1) {
                      } else {
                      }
                  }
              }
              """
          )
          .map(J.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();

        assertThatThrownBy(() -> new JavaIsoVisitor<Integer>() {
            @Override
            public J.If visitIf(J.If iff, Integer p) {
                return JavaTemplate.apply("{\n}", getCursor(), iff.getCoordinates().addElseBranch());
            }
        }.visit(cu, 0))
          .rootCause()
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("requires a template that produces an `if`");
    }
}
