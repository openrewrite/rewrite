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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * These JavaTemplate tests are specific to the `instanceof` pattern matching syntax.
 * The tests always perform the same substitution: Every `42` literal is replaced with a call to `s.length()`.
 * The tests then contain `invalid` marker comments to indicate that the substitution is
 * expected to result in a missing type error in that position and in the end the test
 * cases validate that the actual and expected missing types are the same.
 */
class JavaTemplateInstanceOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        //noinspection DataFlowIssue
        spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLiteral(J.Literal literal, ExecutionContext ctx) {
                  return literal.getValue() == Integer.valueOf(42) ?
                    JavaTemplate.builder("s.length()")
                      .contextSensitive()
                      .build()
                      .apply(
                        getCursor(),
                        literal.getCoordinates().replace()
                      ) :
                    super.visitLiteral(literal, ctx);
              }
          }))
          // custom missing type validation
          .afterTypeValidationOptions(TypeValidation.none())
          .afterRecipe(run -> run.getChangeset().getAllResults().forEach(r -> assertTypeAttribution((J) r.getAfter())));
    }

    @SuppressWarnings({"PointlessBooleanExpression", "IfStatementWithIdenticalBranches", "ConstantValue"})
    @Test
    void replaceExpressionInNestedIfCondition() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (true || (o instanceof String s && 42 != 1)) {
                          return /*invalid*/ 42;
                      }
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @Test
    @ExpectedToFail
    @Issue("https://github.com/openrewrite/rewrite/issues/2958")
    @SuppressWarnings({"ConstantValue", "IfStatementWithIdenticalBranches"})
    void referenceFromWithinLambdaInIfCondition() {
        rewriteRun(
          templatedJava17(
            """
              import java.util.stream.Stream;
              class T {
                  Object m(Object o) {
                      if (o instanceof String s && Stream.of("x").anyMatch(e -> 42 == e.length())) {
                          return 42;
                      }
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"EmptyTryBlock", "finally", "ReturnInsideFinallyBlock"})
    @Test
    void elseWithEmptyTryAndReturnFromFinally() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          try {
                          } finally {
                              return 42;
                          }
                      }
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"UnnecessaryBreak"})
    @Test
    void normallyCompletingLabeledBreak() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          A: {
                              break A;
                          }
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void abnormallyCompletingLabeledBreak() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      A: {
                          if (!(o instanceof String s)) {
                              return /*invalid 1*/ 42;
                          } else {
                              System.out.println(42);
                              break A;
                          }
                          return /*invalid 2*/ 42;
                      }
                      return /*invalid 3*/ 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceInThenWithReturn() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (o instanceof String s) {
                          return 42;
                      } else {
                          System.out.println(/*invalid*/ 42);
                      }
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceInUnbracedBlocks() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (o instanceof String s)
                          return 42;
                      else
                          System.out.println(/*invalid*/ 42);
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceInThenWithoutReturn() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (o instanceof String s) {
                          System.out.println(42);
                      } else {
                          System.out.println(/*invalid*/ 42);
                      }
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceInThenWithReturnInElse() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (o instanceof String s) {
                          System.out.println(42);
                      } else {
                          return /*invalid*/ 42;
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceInElse() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          System.out.println(42);
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    @Test
    void replaceInElseWithAnonymousClass() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          new Runnable() {
                              public void run() {
                                  System.out.println(42);
                                  return;
                              }
                          };
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    @Test
    void replaceInElseWithLambda() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          Runnable r = () -> {
                              System.out.println(42);
                              return;
                          };
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAfterIf() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          System.out.println(42);
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"SwitchStatementWithTooFewBranches", "EnhancedSwitchMigration", "DataFlowIssue"})
    @Test
    void incompleteSwitch() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          switch (true) {
                              case true:
                                  return 42;
                          }
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"SwitchStatementWithTooFewBranches", "EnhancedSwitchMigration", "DataFlowIssue"})
    @Test
    void completeSwitch() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          switch (true) {
                              case true:
                              default:
                                  return 42;
                          }
                      }
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @Test
    void tryCatchWithoutRethrow() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          try {
                              return 42;
                          } catch (RuntimeException ignore) {
                          }
                      }
                      return 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("CaughtExceptionImmediatelyRethrown")
    @Test
    void tryCatchWithRethrow() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      if (!(o instanceof String s)) {
                          return /*invalid*/ 42;
                      } else {
                          try {
                              return 42;
                          } catch (RuntimeException e) {
                              throw e;
                          }
                      }
                      return /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"ConditionalExpressionWithIdenticalBranches", "PointlessBooleanExpression", "ConstantValue"})
    @Test
    void replaceTernaryCondition() {
        rewriteRun(
          templatedJava17(
            """
              class T {
                  Object m(Object o) {
                      return o instanceof String s && 42 > 0 ? 42 : /*invalid*/ 42;
                  }
              }
              """
          )
        );
    }


    @Test
    void replaceNestedMethodInvocationInTernaryTrue() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                    if (!new MethodMatcher("java.lang.String format(String, Object[])").matches(mi)) {
                        return mi;
                    }

                    List<Expression> arguments = mi.getArguments();
                    mi = JavaTemplate.builder("#{any(java.lang.String)}.formatted(#{any()})")
                      .contextSensitive()
                      .build()
                      .apply(
                        updateCursor(mi),
                        mi.getCoordinates().replace(),
                        arguments.toArray()
                      );

                    mi = maybeAutoFormat(mi, mi.withArguments(
                      ListUtils.map(arguments.subList(1, arguments.size()), (a, b) -> b.withPrefix(arguments.get(a + 1).getPrefix()))), ctx);
                    return mi;
                }
            }
          )),
          version(
            java(
              """
                class A {
                    String foo(String s) { return s; }
                    void bar(Object o) {
                        String s = (o instanceof String s2) ?
                            foo(String.format("%s", "sam")) :
                            "";
                    }
                }
                """,
              """
                class A {
                    String foo(String s) { return s; }
                    void bar(Object o) {
                        String s = (o instanceof String s2) ?
                            foo("%s".formatted("sam")) :
                            "";
                    }
                }
                """
            ),
            17
          )
        );
    }

    private SourceSpecs templatedJava17(@Language("java") String before) {
        return version(java(
          before,
          before.replaceAll("\\b42\\b", "s.length()")
        ), 17);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    static void assertTypeAttribution(J sf) {
        List<FindMissingTypes.MissingTypeResult> missingTypes = FindMissingTypes.findMissingTypes(sf);
        Map<J, Cursor> expectedMissing = new LinkedHashMap<>();
        Map<J, Cursor> actualMissing = new LinkedHashMap<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, Integer ignore) {
                if (tree instanceof J j) {
                    if (j.getComments().stream().anyMatch(c -> c instanceof TextComment t && t.getText().startsWith("invalid"))) {
                        expectedMissing.put(j, getCursor());
                    }
                    if (missingTypes.stream().anyMatch(t -> t.getJ().isScope(j))) {
                        actualMissing.put(j, getCursor());
                    }
                }
                return super.visit(tree, ignore);
            }
        }.visit(sf, 0);

        for (var entry : new HashMap<>(actualMissing).entrySet()) {
            if (expectedMissing.containsKey(entry.getKey())) {
                actualMissing.remove(entry.getKey());
                expectedMissing.remove(entry.getKey());
                continue;
            }
            for (Cursor cursor = entry.getValue(); cursor.getParent() != null; cursor = cursor.getParent()) {
                if (expectedMissing.containsKey(cursor.getValue())) {
                    actualMissing.remove(entry.getKey());
                    expectedMissing.remove(cursor.getValue());
                    break;
                }
            }
        }
        if (!expectedMissing.isEmpty()) {
            fail("Expected missing types not found:\n" + expectedMissing.entrySet().stream()
              .map(e -> {
                  String path = e.getValue().getPathAsStream().filter(J.class::isInstance).map(t -> t.getClass().getSimpleName()).collect(Collectors.joining("->"));
                  return path + ": " + e.getKey().printTrimmed(new InMemoryExecutionContext(), e.getValue().getParentOrThrow());
              })
              .collect(Collectors.joining("\n")));
        }
        if (!actualMissing.isEmpty()) {
            fail("Unexpected missing types found:\n" + actualMissing.entrySet().stream()
              .map(e -> {
                  String path = e.getValue().getPathAsStream().filter(J.class::isInstance).map(t -> t.getClass().getSimpleName()).collect(Collectors.joining("->"));
                  return path + ": " + e.getKey().printTrimmed(new InMemoryExecutionContext(), e.getValue().getParentOrThrow());
              })
              .collect(Collectors.joining("\n")));
        }

    }
}
