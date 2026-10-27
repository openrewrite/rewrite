/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JavaTemplateContextFreeTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceMethodBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return method.getBody() != null && JavaTemplate.matches("System.out.println(1);",
                    new Cursor(new Cursor(getCursor(), method.getBody()), method.getBody().getStatements().getFirst())) ?
                    JavaTemplate.apply("System.out.println(2);", getCursor(), method.getCoordinates().replaceBody()) :
                    super.visitMethodDeclaration(method, ctx);
              }
          })),
          java(
            """
              class Test {
                  void m() {
                      System.out.println(1);
                  }
              }
              """,
            """
              class Test {
                  void m() {
                      System.out.println(2);
                  }
              }
              """
          ));
    }

    @Test
    void replaceField() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
                  if (vd.getVariables().size() == 1 && "i".equals(vd.getVariables().getFirst().getSimpleName())) {
                      return JavaTemplate.apply("Integer i = 2;", getCursor(), vd.getCoordinates().replace());
                  }
                  return super.visitVariableDeclarations(vd, ctx);
              }
          }).withMaxCycles(1)),
          java(
            """
              class Test {
                  private Integer i = 1;
                  void m() {
                      Integer i = 1;
                      Object o = new Object() {
                          private final Integer i = 1;
                      };
                  }
              }
              """,
            """
              class Test {
                  Integer i = 2;
                  void m() {
                      Integer i = 2;
                      Object o = new Object() {
                          Integer i = 2;
                      };
                  }
              }
              """
          ));
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void genericsAndAnyParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
                  if (literal.getMarkers().findFirst(SearchResult.class).isEmpty() &&
                      (Objects.equals(literal.getValue(), 1) || "s".equals(Objects.requireNonNull(literal.getValue())))) {
                      return JavaTemplate.apply("java.util.List.of(#{any()})", getCursor(), literal.getCoordinates().replace(), SearchResult.found(literal));
                  }
                  return super.visitLiteral(literal, executionContext);
              }
          })),
          java(
            """
              class Test {
                  void m() {
                      Object o;
                      o = 1;
                      o = 2;
                      o = "s";
                      o = "s2";
                  }
              }
              """,
            """
              class Test {
                  void m() {
                      Object o;
                      o = java.util.List.of(/*~~>*/1);
                      o = 2;
                      o = java.util.List.of(/*~~>*/"s");
                      o = "s2";
                  }
              }
              """,
            sourceSpecs -> sourceSpecs.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                @SuppressWarnings("DataFlowIssue")
                public <M extends Marker> M visitMarker(Marker marker, Object o) {
                    if (marker instanceof SearchResult) {
                        J.Literal literal = getCursor().getValue();
                        Expression parent = getCursor().getParentTreeCursor().getValue();
                        if (literal.getType() == JavaType.Primitive.Int) {
                            assertThat(parent.getType().toString()).isEqualTo("java.util.List<java.lang.Integer>");
                        } else if (literal.getType() == JavaType.Primitive.String) {
                            assertThat(parent.getType().toString()).isEqualTo("java.util.List<java.lang.String>");
                        }
                    }
                    return super.visitMarker(marker, o);
                }
            }.visit(cu, 0))
          ));
    }

    @Test
    void contextFreeTypeParameter() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if ("o".equals(multiVariable.getVariables().getFirst().getSimpleName()) &&
                      !(multiVariable.getTypeExpression() instanceof J.Identifier id && "var".equals(id.getSimpleName()))) {
                      return JavaTemplate.apply("var o = #{any()};", getCursor(), multiVariable.getCoordinates().replace(), multiVariable.getVariables().getFirst().getInitializer());
                  }
                  return multiVariable;
              }
          })),
          java(
            """
              import java.util.Optional;

              class Test {
                  <T extends Number> void test(T element) {
                      Optional<T> o = Optional.of(element);
                  }
              }
              """,
            """
              import java.util.Optional;

              class Test {
                  <T extends Number> void test(T element) {
                      var o = Optional.of(element);
                  }
              }
              """
          )
        );
    }

    @Test
    void contextFreeTypeParameterConflictNames() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  if ("printf".equals(method.getSimpleName())) {
                      return JavaTemplate.builder("java.util.Arrays.asList(#{any()}, #{any()});")
                        .javaParser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true))
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().toArray());
                  }
                  return super.visitMethodInvocation(method, executionContext);
              }
          })),
          java(
            """
              class Test<T extends Number> {
                  T parameter;
                  <T extends String> void test(T element) {
                      System.out.printf(element, parameter);
                  }
              }
              """,
            """
              class Test<T extends Number> {
                  T parameter;
                  <T extends String> void test(T element) {
                      java.util.Arrays.asList(element, parameter);
                  }
              }
              """
          )
        );
    }

    @Disabled("Requires renaming generic variables in JavaTemplate")
    @Test
    void contextFreeTypeParameterConflictNames_broken() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  if ("printf".equals(method.getSimpleName()) && method.getArguments().size() == 2) {
                      return JavaTemplate.apply("System.out.printf(#{any()}, #{any()}, 0);", getCursor(), method.getCoordinates().replace(), method.getArguments().toArray());
                  }
                  return super.visitMethodInvocation(method, executionContext);
              }
          })),
          java(
            """
              class Test<T extends Number> {
                  T parameter;
                  <T extends String> void test(T element) {
                      System.out.printf(element, parameter);
                  }
              }
              """,
            """
              class Test<T extends Number> {
                  T parameter;
                  <T extends String> void test(T element) {
                      System.out.printf(element, parameter, 0);
                  }
              }
              """
          )
        );
    }

    @Test
    void contextFreeRecursiveTypeParameters() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if ("o".equals(multiVariable.getVariables().getFirst().getSimpleName()) &&
                      !(multiVariable.getTypeExpression() instanceof J.Identifier id && "var".equals(id.getSimpleName()))) {
                      return JavaTemplate.apply("var o = #{any()};", getCursor(), multiVariable.getCoordinates().replace(), multiVariable.getVariables().getFirst().getInitializer());
                  }
                  return multiVariable;
              }
          })),
          java(
            """
              import java.util.Map;
              import java.util.function.Supplier;

              class Test {
                  <T extends Supplier<S>, S extends Supplier<T>> void test(S s, T t) {
                      Map.Entry<S, T> o = Map.entry(s, t);
                  }
              }
              """,
            """
              import java.util.Map;
              import java.util.function.Supplier;

              class Test {
                  <T extends Supplier<S>, S extends Supplier<T>> void test(S s, T t) {
                      var o = Map.entry(s, t);
                  }
              }
              """
          )
        );
    }

    @Test
    void contextFreeSequenceTypeParameters() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true))
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if ("o".equals(multiVariable.getVariables().getFirst().getSimpleName()) &&
                      !(multiVariable.getTypeExpression() instanceof J.Identifier id && "var".equals(id.getSimpleName()))) {
                      return JavaTemplate.apply("var o = #{any()};", getCursor(), multiVariable.getCoordinates().replace(), multiVariable.getVariables().getFirst().getInitializer());
                  }
                  return multiVariable;
              }
          })),
          java(
            """
              import java.util.Comparator;
              import java.util.Map;
              import java.util.Optional;

              class Test<A extends B, B extends C, C extends Number> {
                  void test(A a, Comparator<? super B> comparator) {
                      Map.Entry<A, Comparator<? super B>> o = Map.entry(a, comparator);
                  }
              }
              """,
            """
              import java.util.Comparator;
              import java.util.Map;
              import java.util.Optional;

              class Test<A extends B, B extends C, C extends Number> {
                  void test(A a, Comparator<? super B> comparator) {
                      var o = Map.entry(a, comparator);
                  }
              }
              """
          )
        );
    }

    @Test
    void contextFreeMultipleBoundParameters() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if ("o".equals(multiVariable.getVariables().getFirst().getSimpleName()) &&
                      !(multiVariable.getTypeExpression() instanceof J.Identifier id && "var".equals(id.getSimpleName()))) {
                      return JavaTemplate.apply("var o = #{any()};", getCursor(), multiVariable.getCoordinates().replace(), multiVariable.getVariables().getFirst().getInitializer());
                  }
                  return multiVariable;
              }
          })),
          java(
            """
              import java.io.Serializable;
              import java.util.function.Supplier;
              import java.util.Optional;

              class Test {
                  <A extends Comparable<? super A> & Serializable & Cloneable, B extends Supplier<A>> void test(B b) {
                      Optional<B> o = Optional.of(b);
                  }
              }
              """,
            """
              import java.io.Serializable;
              import java.util.function.Supplier;
              import java.util.Optional;

              class Test {
                  <A extends Comparable<? super A> & Serializable & Cloneable, B extends Supplier<A>> void test(B b) {
                      var o = Optional.of(b);
                  }
              }
              """
          )
        );
    }

    @Test
    void insertBeforeNonVoidExpressionStatements() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (!(getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration) ||
                      b.getStatements().size() != 3) {
                      return b;
                  }
                  for (int i = 0; i < block.getStatements().size(); i++) {
                      b = JavaTemplate.apply("int before#{} = 0;", new Cursor(getCursor().getParentOrThrow(), b),
                        block.getStatements().get(i).getCoordinates().before(), i);
                  }
                  return b;
              }
          })),
          java(
            """
              import java.util.List;

              class Test {
                  int i;
                  void m(List<String> list) {
                      list.add("a");
                      new StringBuilder();
                      i++;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int i;
                  void m(List<String> list) {
                      int before0 = 0;
                      list.add("a");
                      int before1 = 0;
                      new StringBuilder();
                      int before2 = 0;
                      i++;
                  }
              }
              """
          )
        );
    }

    @Test
    void insertAfterExplicitConstructorInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                  J.Block b = super.visitBlock(block, ctx);
                  if (b.getStatements().size() == 1 && b.getStatements().getFirst() instanceof J.MethodInvocation superCall &&
                      "super".equals(superCall.getSimpleName())) {
                      return JavaTemplate.apply("String copy = #{any(java.lang.String)};", getCursor(),
                        superCall.getCoordinates().after(), superCall.getArguments().getFirst());
                  }
                  return b;
              }
          })),
          java(
            """
              class Parent {
                  Parent(String s) {
                  }
              }
              class Child extends Parent {
                  Child(String s) {
                      super(s);
                  }
              }
              """,
            """
              class Parent {
                  Parent(String s) {
                  }
              }
              class Child extends Parent {
                  Child(String s) {
                      super(s);
                      String copy = s;
                  }
              }
              """
          )
        );
    }

    @Test
    void insertingTemplateThatGeneratesNoStatementFailsLoudly() {
        J.CompilationUnit cu = JavaParser.fromJavaVersion().build()
          .parse(
            """
              class Test {
                  void m() {
                      System.out.println();
                  }
              }
              """
          )
          .map(J.CompilationUnit.class::cast)
          .findFirst()
          .orElseThrow();

        assertThatThrownBy(() -> new JavaIsoVisitor<Integer>() {
            @Override
            public J.Block visitBlock(J.Block block, Integer p) {
                if (getCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                    return JavaTemplate.apply("/* not a statement */", getCursor(),
                      block.getStatements().getFirst().getCoordinates().before());
                }
                return super.visitBlock(block, p);
            }
        }.visit(cu, 0))
          .rootCause()
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("generated none");
    }
}
