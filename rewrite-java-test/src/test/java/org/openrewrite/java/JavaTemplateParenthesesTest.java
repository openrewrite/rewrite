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
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnnecessaryBoxing", "StatementWithEmptyBody", "UnusedAssignment", "SizeReplaceableByIsEmpty", "ResultOfMethodCallIgnored", "RedundantOperationOnEmptyContainer"})
class JavaTemplateParenthesesTest implements RewriteTest {

    @Test
    void addParentheses() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public <T extends J> J visitParentheses(J.Parentheses<T> parens, ExecutionContext ctx) {
                    return JavaTemplate.apply("#{any()}", getCursor(), parens.getCoordinates().replace(), parens.getTree());
                }
            }))
            // the visitor rebuilds the parentheses it replaces in every cycle
            .cycles(1)
            .expectedCyclesThatMakeChanges(1),
          java(
            """
              public class A {
                  int a = (1 + 2) * 3;
              }
              """,
            """
              public class A {
                  int a = (1 + 2) * 3;
              }
              """
          )
        );
    }

    @Test
    void addParenthesesToParameter() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                    if (binary.getOperator() != J.Binary.Type.Addition) {
                        return binary;
                    }
                    return JavaTemplate.apply("#{any(int)} * 3", getCursor(), binary.getCoordinates().replace(), binary);
                }
            })),
          java(
            """
              public class A {
                  int a = 1 + 2;
              }
              """,
            """
              public class A {
                  int a = (1 + 2) * 3;
              }
              """
          )
        );
    }

    @Test
    void parenthesizeParameterOnTheRightOfAnEquallyBindingOperator() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (!"shift".equals(method.getSimpleName())) {
                        return super.visitMethodInvocation(method, ctx);
                    }
                    return JavaTemplate.apply("#{any(int)} << #{any(int)}", getCursor(),
                      method.getCoordinates().replace(), method.getArguments().get(0), method.getArguments().get(1));
                }
            })),
          java(
            """
              class A {
                  int a = shift(1, 2 << 3);

                  static int shift(int value, int distance) {
                      return value << distance;
                  }
              }
              """,
            """
              class A {
                  int a = 1 << (2 << 3);

                  static int shift(int value, int distance) {
                      return value << distance;
                  }
              }
              """
          )
        );
    }

    @Test
    void parenthesizeReplacementOnTheRightOfAnEquallyBindingOperator() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (!"either".equals(method.getSimpleName())) {
                        return super.visitMethodInvocation(method, ctx);
                    }
                    return JavaTemplate.apply("#{any(boolean)} || #{any(boolean)}", getCursor(),
                      method.getCoordinates().replace(), method.getArguments().get(0), method.getArguments().get(1));
                }
            })),
          java(
            """
              class A {
                  boolean test(boolean x, boolean y, boolean z) {
                      return x || either(y, z);
                  }

                  static boolean either(boolean a, boolean b) {
                      return a || b;
                  }
              }
              """,
            """
              class A {
                  boolean test(boolean x, boolean y, boolean z) {
                      return x || (y || z);
                  }

                  static boolean either(boolean a, boolean b) {
                      return a || b;
                  }
              }
              """
          )
        );
    }

    @Test
    void parenthesizeParameterThatIsCast() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (!"toInt".equals(method.getSimpleName())) {
                        return super.visitMethodInvocation(method, ctx);
                    }
                    return JavaTemplate.apply("(int) #{any(long)}", getCursor(),
                      method.getCoordinates().replace(), method.getArguments().get(0));
                }
            })),
          java(
            """
              class A {
                  int test(long x, long y) {
                      return toInt(x + y);
                  }

                  static int toInt(long value) {
                      return (int) value;
                  }
              }
              """,
            """
              class A {
                  int test(long x, long y) {
                      return (int) (x + y);
                  }

                  static int toInt(long value) {
                      return (int) value;
                  }
              }
              """
          )
        );
    }

    @Test
    void parenthesizeParameterThatIsAFieldAccessTarget() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (!"length".equals(method.getSimpleName())) {
                        return super.visitMethodInvocation(method, ctx);
                    }
                    return JavaTemplate.apply("#{any(int[])}.length", getCursor(),
                      method.getCoordinates().replace(), method.getArguments().get(0));
                }
            })),
          java(
            """
              class A {
                  int test(boolean x, int[] a, int[] b) {
                      return length(x ? a : b);
                  }

                  static int length(int[] array) {
                      return array.length;
                  }
              }
              """,
            """
              class A {
                  int test(boolean x, int[] a, int[] b) {
                      return (x ? a : b).length;
                  }

                  static int length(int[] array) {
                      return array.length;
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceofParameterAsArgumentNeedsNoParentheses() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (!"isString".equals(method.getSimpleName())) {
                        return super.visitMethodInvocation(method, ctx);
                    }
                    return JavaTemplate.apply("String.valueOf(#{any(boolean)})", getCursor(),
                      method.getCoordinates().replace(), method.getArguments().get(0));
                }
            })),
          java(
            """
              class A {
                  Object test(Object o) {
                      return isString(o instanceof String);
                  }

                  static boolean isString(boolean value) {
                      return value;
                  }
              }
              """,
            """
              class A {
                  Object test(Object o) {
                      return String.valueOf(o instanceof String);
                  }

                  static boolean isString(boolean value) {
                      return value;
                  }
              }
              """
          )
        );
    }
}
