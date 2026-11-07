/*
 * Copyright 2020 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;
import static org.openrewrite.test.TypeValidation.all;

class JavaVisitorTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings("RedundantThrows")
    @Test
    void javaVisitorHandlesPaddedWithNullElem() {
        rewriteRun(
          spec -> spec
            .recipes(
              toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                      var mi = super.visitMethodInvocation(method, p);
                      if ("removeMethod".equals(mi.getSimpleName())) {
                          //noinspection ConstantConditions
                          return null;
                      }
                      return mi;
                  }
              }),
              toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                      if ("allTheThings".equals(method.getSimpleName()) && method.getThrows() == null) {
                          return JavaTemplate.builder("Exception").contextSensitive().build()
                            .apply(getCursor(), method.getCoordinates().replaceThrows());
                      }
                      return method;
                  }
              })
            ),
          java(
            """
              class A {
                  void allTheThings() {
                      doSomething();
                      removeMethod();
                  }
                  void doSomething() {}
                  void removeMethod() {}
              }
              """,
            """
              class A {
                  void allTheThings() throws Exception {
                      doSomething();
                  }
                  void doSomething() {}
                  void removeMethod() {}
              }
              """
          )
        );
    }

    @Test
    void topVisitor() {
        final JavaIsoVisitor<ExecutionContext> afterVisitor = new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                for (Cursor parent = getCursor().getParent(); parent != null; parent = parent.getParent()) {
                    if (method == parent.getValue()) {
                        fail("Duplicate cursor: %s".formatted(getCursor()));
                    }
                }
                return super.visitMethodDeclaration(method, p);
            }
        };
        rewriteRun(
          spec -> spec.recipe(
            toRecipe(() -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                    var md = super.visitMethodDeclaration(method, p);
                    if ("myMethod".equals(md.getSimpleName())) {
                        //noinspection ConstantConditions
                        return (J.MethodDeclaration) new JavaIsoVisitor<ExecutionContext>() {
                            @Override
                            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                                doAfterVisit(afterVisitor);
                                return super.visitMethodDeclaration(method, p);
                            }
                        }.visit(md, p, getCursor().getParent());
                    }
                    return md;
                }
            })
          ),
          java(
            """
              class A {
                public void method1() {
                }

                @Deprecated
                public String myMethod() {
                  return "hello";
                }

                public void method2() {
                }
              }
              """
          )
        );
    }

    @Test
    void javaVisitorHandlesErroneousNodes() {
        rewriteRun(
          spec -> spec
            .recipes(
              toRecipe(() -> new JavaIsoVisitor<>() {
                  @Override
                  public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                      if ("test".equals(method.getSimpleName()) && method.getThrows() == null) {
                          return JavaTemplate.builder("Exception").contextSensitive().build()
                            .apply(getCursor(), method.getCoordinates().replaceThrows());
                      }
                      return method;
                  }
              })
            )
            .typeValidationOptions(all().erroneous(false)),
          java(
            """
              class A {
                  void test() {
                      owner
                  }
              }
              """,
            """
              class A {
                  void test() throws Exception {
                      owner
                  }
              }
              """
          )
        );
    }

    @Test
    void cursorPathDoesNotDependOnHowOftenItIsAskedFor() {
        J.CompilationUnit cu = parseCalls();
        String expected = "Literal < JRightPadded < JContainer < MethodInvocation < JRightPadded < Block < " +
                          "MethodDeclaration < JRightPadded < Block < ClassDeclaration < CompilationUnit < root";

        PathOfLastLiteral rarely = new PathOfLastLiteral(false);
        rarely.visit(cu, 0);
        assertThat(rarely.path).isEqualTo(expected);

        PathOfLastLiteral everywhere = new PathOfLastLiteral(true);
        everywhere.visit(cu, 0);
        assertThat(everywhere.path).isEqualTo(expected);

        SetCursorOverrider overrider = new SetCursorOverrider();
        overrider.visit(cu, 0);
        assertThat(overrider.path).isEqualTo(expected);
        assertThat(overrider.treesNotAnnounced).isZero();
    }

    @Test
    void messageOnAnAncestorReachesLaterSiblings() {
        List<@Nullable Object> seen = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                if ("first".equals(method.getSimpleName())) {
                    getCursor().dropParentUntil(J.Block.class::isInstance).putMessage("seen", true);
                } else if ("last".equals(method.getSimpleName())) {
                    seen.add(getCursor().getNearestMessage("seen"));
                }
                return method;
            }
        }.visit(parseCalls(), 0);
        assertThat(seen).containsExactly(true);
    }

    @Test
    void retainedCursorKeepsItsPathAfterTheVisit() {
        List<Cursor> kept = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, Integer p) {
                if (Integer.valueOf(0).equals(literal.getValue())) {
                    kept.add(getCursor());
                }
                return literal;
            }
        }.visit(parseCalls(), 0);
        assertThat(describe(kept.get(0))).isEqualTo(
          "Literal < JRightPadded < JContainer < MethodInvocation < JRightPadded < Block < " +
          "MethodDeclaration < JRightPadded < Block < ClassDeclaration < CompilationUnit < root");
    }

    @Test
    void stopAfterPreVisitStopsOnlyTheNodeItIsCalledFor() {
        List<Object> literals = new ArrayList<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J preVisit(J tree, Integer p) {
                if (tree instanceof J.MethodInvocation && "a1".equals(((J.MethodInvocation) tree).getSimpleName())) {
                    stopAfterPreVisit();
                }
                return tree;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                J.MethodInvocation m = super.visitMethodInvocation(method, p);
                if ("a2".equals(m.getSimpleName())) {
                    // too late to stop anything, so the next call is still visited
                    stopAfterPreVisit();
                }
                return m;
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, Integer p) {
                literals.add(literal.getValue());
                return literal;
            }
        }.visit(parseCalls(), 0);
        assertThat(literals).startsWith(0, 2, 3).doesNotContain(1);
    }

    @Test
    void visitingATreeUnderItselfFailsInTests() {
        assertThatThrownBy(() -> new JavaIsoVisitor<Integer>() {
            boolean revisited;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                if ("last".equals(method.getSimpleName()) && !revisited) {
                    revisited = true;
                    visit(method, p);
                }
                return method;
            }
        }.visit(parseCalls(), 0))
          .hasStackTraceContaining("is a cursor on the same J.MethodInvocation");
    }

    @Test
    void nullListsPassThroughUnchanged() {
        J.CompilationUnit cu = parse("class A { void m() { } }");
        J.ClassDeclaration c = cu.getClasses().get(0);
        J.MethodDeclaration m = (J.MethodDeclaration) c.getBody().getStatements().get(0);

        // A tree received over RPC carries null for a list its sender deleted
        J.MethodDeclaration noStatements = m.withBody(requireNonNull(m.getBody()).getPadding().withStatements(null));
        J.ClassDeclaration noModifiers = c.withModifiers(null).withBody(c.getBody().withStatements(singletonList(noStatements)));
        J.CompilationUnit withNulls = cu.withClasses(singletonList(noModifiers));

        assertThat(new JavaIsoVisitor<Integer>().visit(withNulls, 0)).isSameAs(withNulls);
    }

    /**
     * A method body with enough calls that a visitor asking for its cursor at every node has switched to
     * building cursors as it enters them before it reaches the last call.
     */
    private static J.CompilationUnit parseCalls() {
        String calls = IntStream.range(0, 40).mapToObj(i -> "a" + i + "(" + i + ");").collect(joining("\n        "));
        return parse("class A {\n    void m() {\n        first();\n        " + calls + "\n        last(42);\n    }\n}");
    }

    private static J.CompilationUnit parse(String source) {
        return (J.CompilationUnit) JavaParser.fromJavaVersion().build()
          .parse(new InMemoryExecutionContext(), source)
          .findFirst()
          .orElseThrow();
    }

    private static String describe(Cursor cursor) {
        return cursor.getPathAsStream()
          .map(v -> v instanceof String ? (String) v : v.getClass().getSimpleName())
          .collect(joining(" < "));
    }

    private static class PathOfLastLiteral extends JavaIsoVisitor<Integer> {
        private final boolean askEverywhere;

        @Nullable
        String path;

        PathOfLastLiteral(boolean askEverywhere) {
            this.askEverywhere = askEverywhere;
        }

        @Override
        public J preVisit(J tree, Integer p) {
            if (askEverywhere) {
                getCursor();
            }
            return tree;
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, Integer p) {
            if (Integer.valueOf(42).equals(literal.getValue())) {
                path = describe(getCursor());
            }
            return literal;
        }
    }

    private static class SetCursorOverrider extends PathOfLastLiteral {
        int treesNotAnnounced;

        @Nullable
        private Object lastAnnounced;

        SetCursorOverrider() {
            super(false);
        }

        @Override
        public void setCursor(@Nullable Cursor cursor) {
            super.setCursor(cursor);
            lastAnnounced = cursor == null ? null : cursor.getValue();
        }

        @Override
        public J preVisit(J tree, Integer p) {
            if (lastAnnounced != tree) {
                treesNotAnnounced++;
            }
            return tree;
        }
    }
}
