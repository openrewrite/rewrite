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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * Code a template puts in place of code that started its line starts that line as the replaced code did, however the
 * rest of the file is indented.
 */
class JavaTemplateReplacementIndentationTest implements RewriteTest {

    @DocumentExample
    @Test
    void replacedStatementKeepsItsTabInAFileIndentedWithSpaces() {
        rewriteRun(
          spec -> spec.recipe(replaceFooWith("bar()")),
          java(
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                      int i = 0;
              \t\tfoo();
                      i++;
                  }
              }
              """,
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                      int i = 0;
              \t\tbar();
                      i++;
                  }
              }
              """
          )
        );
    }

    @Test
    void replacedStatementKeepsTheCommentAndIndentationItStartedWith() {
        rewriteRun(
          spec -> spec.recipe(replaceFooWith("bar()")),
          java(
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                        // the call that changes
                        foo();
                  }
              }
              """,
            """
              class A {
                  void foo() {
                  }

                  void bar() {
                  }

                  void test() {
                        // the call that changes
                        bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void replacedArgumentKeepsTheIndentationOfItsLine() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  if (!"first".equals(literal.getValue())) {
                      return literal;
                  }
                  return JavaTemplate.apply("\"FIRST\"", getCursor(), literal.getCoordinates().replace());
              }
          })),
          java(
            """
              class A {
                  void log(String a, String b) {
                  }

                  void test() {
                      log(
                        "first",
                        "second");
                  }
              }
              """,
            """
              class A {
                  void log(String a, String b) {
                  }

                  void test() {
                      log(
                        "FIRST",
                        "second");
                  }
              }
              """
          )
        );
    }

    @Test
    void replacedStatementKeepsTheBlankLinesAboveIt() {
        rewriteRun(
          spec -> spec.recipe(replaceFooWith("bar()")),
          java(
            """
              class A {
              \tvoid foo() {
              \t}

              \tvoid bar() {
              \t}

              \tvoid test() {
              \t
                    foo();
                    bar();
              \t}
              }
              """,
            """
              class A {
              \tvoid foo() {
              \t}

              \tvoid bar() {
              \t}

              \tvoid test() {
              \t
                    bar();
                    bar();
              \t}
              }
              """
          )
        );
    }

    @Test
    void maybeAutoFormatKeepsTheIndentationOfWhatChangedInPlace() {
        rewriteRun(
          spec -> spec.recipe(makeLocalVariablesFinal()),
          java(
            """
              class A {
                  void test() {
                      int i = 0;
              \t\tint j = 1;
                        // a comment indented by hand
                        int k = 2;
                  }
              }
              """,
            """
              class A {
                  void test() {
                      final int i = 0;
              \t\tfinal int j = 1;
                        // a comment indented by hand
                        final int k = 2;
                  }
              }
              """
          )
        );
    }

    @Test
    void maybeAutoFormatKeepsTheBlankLinesAboveWhatChangedInPlace() {
        rewriteRun(
          spec -> spec.recipe(makeLocalVariablesFinal()),
          java(
            """
              class A {
                  void test() {
                      int i = 0;
                \s

                      int j = 1;
                  }
              }
              """,
            """
              class A {
                  void test() {
                      final int i = 0;
                \s

                      final int j = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void addedAnnotationKeepsTheLinesOfItsDeclaration() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations field, ExecutionContext ctx) {
                  if (!"value".equals(field.getVariables().getFirst().getSimpleName()) || !field.getLeadingAnnotations().isEmpty()) {
                      return field;
                  }
                  return JavaTemplate.apply("@Deprecated", getCursor(),
                    field.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
              }
          })),
          java(
            """
              class A {
                  int first;
              \t
              \t/**
              \t * A field.
              \t */
              \tprivate int value;
                  int last;
              }
              """,
            """
              class A {
                  int first;
              \t
              \t/**
              \t * A field.
              \t */
              \t@Deprecated
              \tprivate int value;
                  int last;
              }
              """
          )
        );
    }

    @Test
    void replacedDeclarationKeepsTheLineUnderItsAnnotation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (!"foo".equals(method.getSimpleName())) {
                      return method;
                  }
                  return JavaTemplate.builder("@Deprecated\npublic abstract void bar();").contextSensitive().build()
                    .apply(getCursor(), method.getCoordinates().replace());
              }
          })),
          java(
            """
              abstract class A {
                  abstract void x();

              \t@Deprecated
              \tpublic abstract void foo();

                  abstract void y();
              }
              """,
            """
              abstract class A {
                  abstract void x();

              \t@Deprecated
              \tpublic abstract void bar();

                  abstract void y();
              }
              """
          )
        );
    }

    @Test
    void maybeAutoFormatKeepsTheLineUnderTheAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                                      ExecutionContext ctx) {
                  J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                  if (!v.hasModifier(J.Modifier.Type.Final)) {
                      return v;
                  }
                  return maybeAutoFormat(v, v.withModifiers(ListUtils.filter(v.getModifiers(),
                    m -> m.getType() != J.Modifier.Type.Final)), ctx);
              }
          })),
          java(
            """
              class A {
              \t@Deprecated
              \tstatic final int x = 1;
                  int y;
                  int z;
              }
              """,
            """
              class A {
              \t@Deprecated
              \tstatic int x = 1;
                  int y;
                  int z;
              }
              """
          )
        );
    }

    @Test
    void maybeAutoFormatIndentsCodeMovedToAnotherLevel() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitIf(J.If iff, ExecutionContext ctx) {
                  // Hoists the then branch's only statement into the place of the if, one level out
                  if (iff.getElsePart() != null || !(iff.getThenPart() instanceof J.Block)) {
                      return iff;
                  }
                  J.Block then = (J.Block) iff.getThenPart();
                  if (then.getStatements().size() != 1) {
                      return iff;
                  }
                  J inner = then.getStatements().get(0);
                  return maybeAutoFormat(inner, inner.withPrefix(iff.getPrefix()), ctx, getCursor().getParentTreeCursor());
              }
          })),
          java(
            """
              class A {
                  void test(boolean b) {
                      if (b) {
                          System.out.println(b);
                      }
                  }
              }
              """,
            """
              class A {
                  void test(boolean b) {
                      System.out.println(b);
                  }
              }
              """
          )
        );
    }

    private static Recipe makeLocalVariablesFinal() {
        return toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable,
                                                                    ExecutionContext ctx) {
                J.VariableDeclarations v = super.visitVariableDeclarations(multiVariable, ctx);
                if (!(getCursor().getParentTreeCursor().getValue() instanceof J.Block) || v.hasModifier(J.Modifier.Type.Final)) {
                    return v;
                }
                J.Modifier finalModifier = new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null,
                  J.Modifier.Type.Final, emptyList());
                J.VariableDeclarations withFinal = v.withModifiers(ListUtils.concat(finalModifier, v.getModifiers()));
                if (withFinal.getTypeExpression() != null) {
                    withFinal = withFinal.withTypeExpression(withFinal.getTypeExpression().withPrefix(Space.SINGLE_SPACE));
                }
                return maybeAutoFormat(v, withFinal, ctx);
            }
        });
    }

    private static Recipe replaceFooWith(String code) {
        return toRecipe(() -> new JavaVisitor<>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (!"foo".equals(method.getSimpleName())) {
                    return method;
                }
                return JavaTemplate.builder(code).contextSensitive().build()
                  .apply(getCursor(), method.getCoordinates().replace());
            }
        });
    }
}
