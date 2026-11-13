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

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.*;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnnecessaryBoxing", "StatementWithEmptyBody", "UnusedAssignment", "SizeReplaceableByIsEmpty", "ResultOfMethodCallIgnored", "RedundantOperationOnEmptyContainer"})
class JavaTemplateTest implements RewriteTest {

    @DocumentExample
    @SuppressWarnings({"RedundantOperationOnEmptyContainer"})
    @Test
    void replaceForEachControlVariableType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  var mv = super.visitVariableDeclarations(multiVariable, ctx);
                  if (mv.getVariables().getFirst().getInitializer() == null && TypeUtils.isOfType(mv.getTypeExpression()
                    .getType(), JavaType.Primitive.String)) {
                      mv = JavaTemplate.apply("Object #{}", getCursor(),
                        multiVariable.getCoordinates().replace(),
                        multiVariable.getVariables().getFirst().getSimpleName()
                      );
                  }
                  return mv;
              }
          })),
          java(
            """
              import java.util.ArrayList;
              class T {
                  void m() {
                      for (String s : new ArrayList<String>()) {}
                  }
              }
              """,
            """
              import java.util.ArrayList;
              class T {
                  void m() {
                      for (Object s : new ArrayList<String>()) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void addAnnotation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              private final JavaTemplate template = JavaTemplate.builder("@SuppressWarnings({\"ALL\"})").build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  J.ClassDeclaration cd = classDecl;
                  if (!cd.getLeadingAnnotations().isEmpty()) {
                      return cd;
                  }
                  return template.apply(updateCursor(cd), cd.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
              }
          })),
          java(
            """
              public class A {
              }
              """,
            """
              @SuppressWarnings({"ALL"})
              public class A {
              }
              """
          )
        );
    }

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

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/550")
    @Test
    void genericTypeVariable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getTypeExpression() instanceof J.Identifier && "var".equals(((J.Identifier) multiVariable.getTypeExpression()).getSimpleName())) {
                      return multiVariable;
                  }
                  J.VariableDeclarations.NamedVariable var0 = multiVariable.getVariables().getFirst();
                  return JavaTemplate.apply("var #{} = #{any()};", getCursor(), multiVariable.getCoordinates().replace(), var0.getSimpleName(), var0.getInitializer());
              }
          })),
          java(
            """
              import java.io.Serializable;

              abstract class Outer<T extends Serializable> {
                  abstract T doIt();
                  void trigger() {
                      T x = doIt();
                  }
              }
              """,
            """
              import java.io.Serializable;

              abstract class Outer<T extends Serializable> {
                  abstract T doIt();
                  void trigger() {
                      var x = doIt();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/66")
    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    void lambdaIsNewClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                  var a = assignment;
                  if (a.getAssignment() instanceof J.MethodInvocation) {
                      var mi = (J.MethodInvocation) a.getAssignment();
                      a = JavaTemplate.apply("1", getCursor(), mi.getCoordinates().replace());
                  }
                  return a;
              }
          })),
          java(
            """
              class T {
                  public T (int a, Runnable r, String s) { }
                  static void method() {
                      new T(1,() -> {
                          int i;
                          i = Integer.valueOf(1);
                      }, "hello" );
                  }
              }
              """,
            """
              class T {
                  public T (int a, Runnable r, String s) { }
                  static void method() {
                      new T(1,() -> {
                          int i;
                          i = 1;
                      }, "hello" );
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceForEachControlIterable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, ExecutionContext ctx) {
                  control = super.visitForEachControl(control, ctx);
                  Expression iterable = control.getIterable();
                  if (!TypeUtils.isOfClassType(iterable.getType(), "java.lang.String")) {
                      return control;
                  }
                  return JavaTemplate.apply("new Object[0]", getCursor(),
                    iterable.getCoordinates().replace());
              }
          })),
          java(
            """
              class T {
                  void m() {
                      for (Object o : new String[0]) {}
                  }
              }
              """,
            """
              class T {
                  void m() {
                      for (Object o : new Object[0]) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceBinaryExpression() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Binary visitBinary(J.Binary binary, ExecutionContext ctx) {
                  binary = super.visitBinary(binary, ctx);
                  if (binary.getLeft() instanceof J.Literal lit && lit.getValue().equals(42)) {
                      return JavaTemplate.apply("43", getCursor(), lit.getCoordinates().replace());
                  }
                  return binary;
              }
          })),
          java(
            """
              class T {
                  boolean m() {
                      return 42 == 0x2A;
                  }
              }
              """,
            """
              class T {
                  boolean m() {
                      return 43 == 0x2A;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"StatementWithEmptyBody", "RedundantOperationOnEmptyContainer"})
    @Test
    void replaceForEachControlIterator() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  if (TypeUtils.isOfClassType(newClass.getType(), "java.util.ArrayList")) {
                      return JavaTemplate.builder("Collections.emptyList()")
                        .imports("java.util.Collections").build()
                        .apply(getCursor(), newClass.getCoordinates().replace());
                  }
                  return newClass;
              }
          })),
          java(
            """
              import java.util.ArrayList;
              import java.util.Collections;
              class T {
                  void m() {
                      for (String s : new ArrayList<String>()) {}
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.Collections;
              class T {
                  void m() {
                      for (String s : Collections.emptyList()) {}
                  }
              }
              """
          )
        );
    }

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

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    void replaceAnonymousClassObject() {
        rewriteRun(
          // the template writes a local or anonymous class as its supertype, so it infers that type
          spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build())
            .recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("asList".equals(method.getSimpleName())) {
                      maybeRemoveImport("java.util.Arrays");
                      maybeAddImport("java.util.Collections");

                      return JavaTemplate.builder("Collections.singletonList(#{any()})")
                        .imports("java.util.Collections")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().getFirst());
                  }
                  return method;
              }
          })),
          java(
            """
              import java.util.Arrays;

              class T {
                  void m() {
                      Object l = Arrays.asList(new java.util.HashMap<String, String>() {
                          void foo() {
                          }
                      });
                  }
              }
              """,
            """
              import java.util.Collections;

              class T {
                  void m() {
                      Object l = Collections.singletonList(new java.util.HashMap<String, String>() {
                          void foo() {
                          }
                      });
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    void replaceGenericTypedObject() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("asList".equals(method.getSimpleName())) {
                      maybeRemoveImport("java.util.Arrays");
                      maybeAddImport("java.util.Collections");
                      return JavaTemplate.builder("Collections.singletonList(#{any()})")
                        .imports("java.util.Collections")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().getFirst());
                  }
                  return method;
              }
          })).afterRecipe(run -> new JavaIsoVisitor<Integer>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                  var type = (JavaType.Parameterized) method.getType();
                  assertThat(type.getTypeParameters()).hasSize(1);
                  assertThat(type.getTypeParameters().getFirst()).isInstanceOf(JavaType.GenericTypeVariable.class);
                  return method;
              }
          }.visit(run.getChangeset().getAllResults().getFirst().getAfter(), 0)),
          java(
            """
              import java.util.Arrays;

              class T<T> {
                  void m(T o) {
                      Object l = Arrays.asList(o);
                  }
              }
              """,
            """
              import java.util.Collections;

              class T<T> {
                  void m(T o) {
                      Object l = Collections.singletonList(o);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    @Test
    void replaceParameterizedTypeObject() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if ("asList".equals(method.getSimpleName())) {
                      maybeRemoveImport("java.util.Arrays");
                      maybeAddImport("java.util.Collections");
                      return JavaTemplate.builder("Collections.singletonList(#{any()})")
                        .imports("java.util.Collections")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().getFirst());
                  }
                  return method;
              }
          })).afterRecipe(run -> new JavaIsoVisitor<Integer>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                  var type = (JavaType.Parameterized) method.getType();
                  assertThat(type.getTypeParameters()).hasSize(1);
                  assertThat(type.getTypeParameters().getFirst()).isInstanceOf(JavaType.Parameterized.class);
                  assertThat(((JavaType.Parameterized) type.getTypeParameters().getFirst()).getTypeParameters()
                    .getFirst()).isInstanceOf(JavaType.GenericTypeVariable.class);
                  return method;
              }
          }.visit(run.getChangeset().getAllResults().getFirst().getAfter(), 0)),
          java(
            """
              import java.util.Arrays;
              import java.util.Collection;

              class T<T> {
                  void m(Collection<T> o) {
                      Object l = Arrays.asList(o);
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.Collections;

              class T<T> {
                  void m(Collection<T> o) {
                      Object l = Collections.singletonList(o);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("LoopConditionNotUpdatedInsideLoop")
    @Test
    void templatingWhileLoopCondition() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  if (binary.getLeft() instanceof J.MethodInvocation) {
                      var mi = (J.MethodInvocation) binary.getLeft();
                      J.Binary b = JavaTemplate.apply("!#{any(java.util.List)}.isEmpty()", getCursor(), mi.getCoordinates().replace(), mi.getSelect());
                      return b.getLeft();
                  }
                  return binary;
              }
          })),
          java(
            """
              import java.util.List;
              class T {
                  void m(List<?> l) {
                      while (l.size() != 0) {}
                  }
              }
              """,
            """
              import java.util.List;
              class T {
                  void m(List<?> l) {
                      while (!l.isEmpty()) {}
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"BigDecimalLegacyMethod", "deprecation"})
    @Test
    void javaTemplateControlsSemiColons() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher bigDecimalSetScale = new MethodMatcher("java.math.BigDecimal setScale(int, int)");
              final JavaTemplate twoArgScale = JavaTemplate.builder("#{any(int)}, #{}")
                .imports("java.math.RoundingMode").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  var mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                  if (bigDecimalSetScale.matches(mi)) {
                      mi = twoArgScale.apply(updateCursor(mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getFirst(), "RoundingMode.HALF_UP");
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.math.BigDecimal;
              import java.math.RoundingMode;

              class A {
                  void m() {
                      StringBuilder sb = new StringBuilder();
                      sb.append((new BigDecimal(0).setScale(1, BigDecimal.ROUND_HALF_UP).doubleValue())).append("|");
                  }
              }
              """,
            """
              import java.math.BigDecimal;
              import java.math.RoundingMode;

              class A {
                  void m() {
                      StringBuilder sb = new StringBuilder();
                      sb.append((new BigDecimal(0).setScale(1, RoundingMode.HALF_UP).doubleValue())).append("|");
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings({"UnaryPlus", "UnusedAssignment"})
    @Test
    void replaceExpressionWithAnotherExpression() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitUnary(J.Unary unary, ExecutionContext ctx) {
                  return JavaTemplate.apply("#{any()}++", getCursor(), unary.getCoordinates().replace(), unary.getExpression());
              }
          }).withMaxCycles(1)),
          java(
            """
              class Test {
                  void test(int i) {
                      int n = +i;
                  }
              }
              """,
            """
              class Test {
                  void test(int i) {
                      int n = i++;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2540")
    @Test
    void replaceMemberReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                  return JavaTemplate.builder("() -> new ArrayList<>(1)")
                    .contextSensitive()
                    .imports("java.util.ArrayList")
                    .build()
                    .apply(getCursor(), memberRef.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.ArrayList;
              import java.util.function.Supplier;
              class Test {
                  void consumer(Supplier<?> supplier) {}
                  void test(int i) {
                      consumer(ArrayList::new);
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.function.Supplier;
              class Test {
                  void consumer(Supplier<?> supplier) {}
                  void test(int i) {
                      consumer(() -> new ArrayList<>(1));
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1796")
    @Test
    void replaceFieldAccessWithMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return method.withBody((J.Block) visit(method.getBody(), ctx));
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fa, ExecutionContext ctx) {
                  if ("f".equals(fa.getSimpleName())) {
                      return JavaTemplate.apply("#{any(java.io.File)}.getCanonicalFile().toPath()",
                        getCursor(), fa.getCoordinates().replace(), fa);
                  } else {
                      return fa;
                  }
              }
          }).withMaxCycles(1)),
          java(
            """
              import java.io.File;
              class Test {
                  File f;
                  void test() {
                      System.out.println(this.f);
                  }
              }
              """,
            """
              import java.io.File;
              class Test {
                  File f;
                  void test() {
                      System.out.println(this.f.getCanonicalFile().toPath());
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

    @Test
    void innerEnumWithStaticMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  if (newClass.getArguments().getFirst() instanceof J.Empty) {
                      return newClass;
                  }
                  return JavaTemplate.builder("new A()")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), newClass.getCoordinates().replace());
              }
          })),
          java(
            """
              class A {
                  public enum Type {
                      One;

                      Type() {
                      }

                      String t;

                      public static Type fromType(String type) {
                          return null;
                      }
                  }

                  public A(Type type) {}
                  public A() {}

                  public void method(Type type) {
                      new A(type);
                  }
              }
              """,
            """
              class A {
                  public enum Type {
                      One;

                      Type() {
                      }

                      String t;

                      public static Type fromType(String type) {
                          return null;
                      }
                  }

                  public A(Type type) {}
                  public A() {}

                  public void method(Type type) {
                      new A();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2375")
    @Test
    void arrayInitializer() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final MethodMatcher mm = new MethodMatcher("abc.ArrayHelper of(..)");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (mm.matches(method)) {
                      return JavaTemplate.builder("Arrays.asList(#{any(java.lang.Integer)}, #{any(java.lang.Integer)}, #{any(java.lang.Integer)})")
                        .imports("java.util.Arrays")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().getFirst(),
                          method.getArguments().get(1), method.getArguments().get(2));
                  }
                  return method;
              }
          })),
          java(
            """
              package abc;

              public class ArrayHelper {
                  public static Object[] of(Object... objects){ return null;}
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import abc.ArrayHelper;
              import java.util.Arrays;

              class A {
                  Object[] o = new Object[] {
                      ArrayHelper.of(1, 2, 3)
                  };
              }
              """,
            """
              import abc.ArrayHelper;
              import java.util.Arrays;

              class A {
                  Object[] o = new Object[] {
                          Arrays.asList(1, 2, 3)
                  };
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2375")
    @SuppressWarnings("ALL")
    @Test
    void multiDimensionalArrayInitializer() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher mm = new MethodMatcher("java.util.stream.IntStream sum()");

              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  var nc = (J.NewClass) super.visitNewClass(newClass, ctx);
                  return JavaTemplate.apply("Integer.valueOf(#{any(java.lang.Integer)}",
                    getCursor(), nc.getCoordinates().replace(), nc.getArguments().getFirst());
              }
          })),
          java(
            """
              class A {
                  Integer[][] num2 = new Integer[][]{ {new Integer(1), new Integer(2)}, {new Integer(1), new Integer(2)} };
              }
              """,
            """
              class A {
                  Integer[][] num2 = new Integer[][]{ {Integer.valueOf(1), Integer.valueOf(2)}, {Integer.valueOf(1), Integer.valueOf(2)} };
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2486")
    @Test
    void dontDropTheAssert() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  J isEmpty = JavaTemplate.apply("!#{any(java.util.Collection)}.isEmpty()", getCursor(),
                    binary.getCoordinates().replace(), ((J.MethodInvocation) binary.getLeft()).getSelect());
                  return isEmpty.withPrefix(binary.getPrefix());
              }
          })),
          java(
            """
              import java.util.Collection;

              class Test {
                  void doSomething(Collection<Object> c) {
                      assert c.size() > 0;
                  }
              }
              """,
            """
              import java.util.Collection;

              class Test {
                  void doSomething(Collection<Object> c) {
                      assert !c.isEmpty();
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedEnums() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                  return JavaTemplate.apply("\"ab\"", getCursor(), binary.getCoordinates().replace());
              }
          })),
          java(
            """
              enum Outer {
                  A, B;

                  enum Inner {
                      C, D
                  }

                  private final String s;

                  Outer() {
                      s = "a" + "b";
                  }
              }
              """,
            """
              enum Outer {
                  A, B;

                  enum Inner {
                      C, D
                  }

                  private final String s;

                  Outer() {
                      s = "ab";
                  }
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
    void replaceMethodCallWithGenericParameterWithUnknownType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
            .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
                private final JavaParser.Builder<?, ?> assertionsParser = JavaParser.fromJavaVersion()
                  .classpath("assertj-core");

                private static final MethodMatcher JUNIT_ASSERT_EQUALS = new MethodMatcher("org.junit.jupiter.api.Assertions assertEquals(..)");

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (!JUNIT_ASSERT_EQUALS.matches(method)) {
                        return method;
                    }

                    List<Expression> args = method.getArguments();
                    Expression expected = args.getFirst();
                    Expression actual = args.get(1);

                    maybeRemoveImport("org.junit.jupiter.api.Assertions");
                    maybeAddImport("org.assertj.core.api.Assertions", "assertThat");

                    if (args.size() == 2) {
                        return JavaTemplate.builder("assertThat(#{any()}).isEqualTo(#{any()});")
                          .staticImports("org.assertj.core.api.Assertions.assertThat")
                          .javaParser(assertionsParser)
                          .build()
                          .apply(getCursor(), method.getCoordinates().replace(), actual, expected);
                    } else {
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
            })),
          java(
            """
              import java.util.Map;
              import org.junit.jupiter.api.Assertions;

              class T {
                  void m(String one, Map<String, ?> map) {
                      Assertions.assertEquals(one, map.get("one"));
                  }
              }
              """,
            """
              import java.util.Map;

              import static org.assertj.core.api.Assertions.assertThat;

              class T {
                  void m(String one, Map<String, ?> map) {
                      assertThat(map.get("one")).isEqualTo(one);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceCallWithUnknownGenericReturnValue() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              private static final MethodMatcher OBJECTS_EQUALS = new MethodMatcher("java.util.Objects equals(..)");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (!OBJECTS_EQUALS.matches(method)) {
                      return method;
                  }

                  List<Expression> args = method.getArguments();
                  Expression expected = args.getFirst();
                  Expression actual = args.get(1);

                  maybeAddImport("java.util.Objects", "requireNonNull");

                  if (args.size() == 2) {
                      return JavaTemplate.builder("requireNonNull(#{any()}).equals(#{any()});")
                        .staticImports("java.util.Objects.requireNonNull")
                        .javaParser(JavaParser.fromJavaVersion())
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), actual, expected);
                  } else {
                      return super.visitMethodInvocation(method, ctx);
                  }
              }
          })),
          java(
            """
              import java.util.Objects;
              import java.util.Map;
              import java.util.HashMap;

              class T {
              	void m() {
              		Map<String, ?> map = new HashMap<>();
              		Objects.equals("", map.get("one"));
              	}
              }
              """,
            """
              import java.util.Objects;
              import java.util.Map;

              import static java.util.Objects.requireNonNull;
              import java.util.HashMap;

              class T {
              	void m() {
              		Map<String, ?> map = new HashMap<>();
                      requireNonNull(map.get("one")).equals("");
              	}
              }
              """
          )
        );
    }

    @Test
    void changeFieldToMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  var vd = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
                  if (vd.getVariables().size() == 1 || "a".equals(vd.getVariables().getFirst().getSimpleName())) {
                      return JavaTemplate.apply("String a();", getCursor(), vd.getCoordinates().replace());
                  }
                  return vd;
              }
          })),
          java(
            """
              interface Test {

                  String a;
              }
              """,
            """
              interface Test {

                  String a();
              }
              """
          )
        );
    }

    @Test
    void finalMethodParameter() {
        rewriteRun(
          spec -> spec.recipe(new ReplaceAnnotation("@org.jetbrains.annotations.NotNull", "@lombok.NonNull", null)),
          java(
            """
              import org.jetbrains.annotations.NotNull;

              class A {
                  String testMethod(@NotNull final String test) {}
              }
              """,
            """
              import lombok.NonNull;

              class A {
                  String testMethod(@NonNull final String test) {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-spring/pull/284")
    @Test
    void replaceMethodInChainFollowedByGenericTypeParameters() {
        rewriteRun(
          spec -> spec
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (new MethodMatcher("batch.StepBuilder create()").matches(method)) {
                        return JavaTemplate.apply("new StepBuilder()", getCursor(), method.getCoordinates().replace());
                    }
                    return super.visitMethodInvocation(method, ctx);
                }
            }))
            // Unclear why
            .recipeQualityValidation(RecipeQualityValidation.builder()
              .types(TypeValidation.builder().constructorInvocations(false).build())
              .build())
            .parser(JavaParser.fromJavaVersion().dependsOn(
                """
                  package batch;
                  public class StepBuilder {
                      public static StepBuilder create() { return new StepBuilder(); }
                      public StepBuilder() {}
                      public <T> T method() { return null; }
                  }
                  """
              )
            ),
          java(
            """
              import batch.StepBuilder;
              class Foo {
                  void test() {
                      StepBuilder.create()
                          .<String>method();
                  }
              }
              """,
            """
              import batch.StepBuilder;
              class Foo {
                  void test() {
                      new StepBuilder()
                          .<String>method();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/185")
    @Test
    void replaceMethodArgumentsInIfStatementWithoutBraces() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Foo bar(..)").matches(mi) &&
                    mi.getArguments().getFirst() instanceof J.Binary) {
                      return JavaTemplate.apply("\"Hello, {}\", \"World!\"", new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments());
                  }
                  return mi;
              }
          })),
          java(
            """
              class Foo {
                  void foo(boolean condition) {
                      if (condition)
                          bar("Hello, " + "World!");
                  }
                  String bar(String... arg){ return null; }
              }
              """,
            """
              class Foo {
                  void foo(boolean condition) {
                      if (condition)
                          bar("Hello, {}", "World!");
                  }
                  String bar(String... arg){ return null; }
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithAnotherOverloadsArgument() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Foo bar(String...)").matches(mi)) {
                      return JavaTemplate.apply("#{any(java.lang.String)}.length()", new Cursor(getCursor().getParent(), mi),
                        mi.getCoordinates().replaceArguments(), mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              class Foo {
                  void foo() {
                      bar("Hello");
                  }
                  void bar(String... arg) {}
                  void bar(int length) {}
              }
              """,
            """
              class Foo {
                  void foo() {
                      bar("Hello".length());
                  }
                  void bar(String... arg) {}
                  void bar(int length) {}
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithAFixedArityOverloadsArgument() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Loader using(Object...)").matches(mi) && mi.getArguments().size() == 2) {
                      maybeAddImport("java.util.Map");
                      return JavaTemplate.builder("Map.of(#{any()}, #{any()})")
                        .imports("java.util.Map")
                        .build()
                        .apply(new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                          mi.getArguments().toArray());
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.util.Map;

              interface Loader<T> {
                  Loader<T> using(Object... values);
                  Loader<T> using(Map<String, ?> values);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Loader<Integer> loader) {
                      loader.using("a", "x");
                      loader.using(1, 2);
                  }
              }
              """,
            """
              import java.util.Map;

              class Test {
                  void test(Loader<Integer> loader) {
                      loader.using(Map.of("a", "x"));
                      loader.using(Map.of(1, 2));
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithTheMostSpecificFixedArityOverloadsArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log debug(String, Object...)").matches(mi) && mi.getArguments().size() == 3) {
                      return JavaTemplate.apply("\"failed\", #{any(java.lang.RuntimeException)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getLast());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void debug(String format, Object arg);
                  void debug(String message, Exception e);
                  void debug(String message, Throwable t);
                  void debug(String format, Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, RuntimeException e) {
                      log.debug("{} {}", "a", e);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, RuntimeException e) {
                      log.debug("failed", e);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnotherOverloadsArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log info(String)").matches(mi)) {
                      return JavaTemplate.apply("\"{}\", #{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void info(String message);
                  void info(String format, Object arg);
                  void info(String format, Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.info(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.info("{}", name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadTakingThemByBoxing() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log info(String)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("\"{}\", #{any(int)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void info(String message);
                  void info(String format, Object arg);
                  void info(String format, Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, int count) {
                      log.info("none");
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, int count) {
                      log.info("{}", count);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAVarargsOverloadsArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log info(String)").matches(mi)) {
                      return JavaTemplate.apply("\"{} {}\", #{any(java.lang.String)}, \"x\"",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void info(String message);
                  void info(String format, Object arg);
                  void info(String format, Object... args);
                  void info(Object... args);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.info(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.info("{} {}", name, "x");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAVarargsOverloadWhoseEmptyVarargsAreMoreSpecific() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Sink put(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().getFirst()
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Sink {
                  void put(int index);
                  void put(String key, Object... values);
                  void put(String key, Integer... values);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(String key, Sink sink) {
                      sink.put(0);
                  }
              }
              """,
            """
              class Test {
                  void test(String key, Sink sink) {
                      sink.put(key);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadTakingThemWithoutBoxing() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("* remove(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(0)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.Integer)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Bag {
                  void remove(int index);
                  void remove(Object o);
              }
              """
          ),
          java(
            """
              interface Sack {
                  void remove(Object o);
                  void remove(int index);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Integer i, Bag bag, Sack sack) {
                      bag.remove(0);
                      sack.remove(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Integer i, Bag bag, Sack sack) {
                      bag.remove(i);
                      sack.remove(i);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadReturningAnotherType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Names name(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Names {
                  String name(int index);
                  int name(String key);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Names names, String key) {
                      names.name(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Names names, String key) {
                      names.name(key);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsOnARawReceiverKeepsTheErasedTypes() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Box names(int)").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.util.List;

              interface Box<T> {
                  List<String> names(int index);
                  List<String> names(String key);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Box box, String key) {
                      box.names(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Box box, String key) {
                      box.names(key);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArgumentsWithAnOverloadThatNamesATypeVariable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if ((new MethodMatcher("Sink put(Object)").matches(mi) ||
                       new MethodMatcher("Sink take(int)").matches(mi)) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Sink {
                  <T extends CharSequence> void put(T value);
                  void put(Object value);
                  Object take(int index);
                  <T> T take(String key);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.put(0);
                      sink.take(0);
                  }
              }
              """,
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.put(name);
                      sink.take(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVarargsArgumentsWithAFixedArityOverloadBesideAGenericOne() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Sink add(Object[])").matches(mi) &&
                      getCursor().firstEnclosingOrThrow(J.MethodDeclaration.class).getParameters().get(1)
                        instanceof J.VariableDeclarations param) {
                      return JavaTemplate.apply("#{any(java.lang.String)}",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceArguments(),
                        param.getVariables().getFirst().getName());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Sink {
                  void add(Object... values);
                  void add(Object value);
                  <T extends Number> void add(T value);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.add(1, 2);
                  }
              }
              """,
            """
              class Test {
                  void test(Sink sink, String name) {
                      sink.add(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodWithAnOverloadTakingTheNewArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("IOUtils write(StringBuffer, java.io.OutputStream)").matches(mi)) {
                      maybeAddImport("java.nio.charset.StandardCharsets");
                      return JavaTemplate.builder("write(#{any(java.lang.StringBuffer)}, #{any(java.io.OutputStream)}, StandardCharsets.#{})")
                        .imports("java.nio.charset.StandardCharsets")
                        .build()
                        .apply(new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceMethod(),
                          mi.getArguments().get(0), mi.getArguments().get(1), "UTF_8");
                  }
                  return mi;
              }
          })),
          java(
            """
              import java.io.OutputStream;
              import java.nio.charset.Charset;

              class IOUtils {
                  static void write(StringBuffer data, OutputStream output) {
                  }

                  static void write(CharSequence data, OutputStream output, Charset charset) {
                  }
              }
              """
          ),
          java(
            """
              import java.io.OutputStream;

              class Test {
                  void test(StringBuffer data, OutputStream output) {
                      IOUtils.write(data, output);
                  }
              }
              """,
            """
              import java.io.OutputStream;
              import java.nio.charset.StandardCharsets;

              class Test {
                  void test(StringBuffer data, OutputStream output) {
                      IOUtils.write(data, output, StandardCharsets.UTF_8);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodWithARenamedMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log warning(String)").matches(mi)) {
                      return JavaTemplate.apply("warn(#{any(java.lang.String)})",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceMethod(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void warning(String message);
                  void warn(Object message);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.warning(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.warn(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodWithARenamedVarargsMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                  if (new MethodMatcher("Log warning(String[])").matches(mi)) {
                      return JavaTemplate.apply("warn(#{any(java.lang.String)})",
                        new Cursor(getCursor().getParent(), mi), mi.getCoordinates().replaceMethod(),
                        mi.getArguments().getFirst());
                  }
                  return mi;
              }
          })),
          java(
            """
              interface Log {
                  void warning(String... messages);
                  void warn(Object... messages);
              }
              """
          ),
          java(
            """
              class Test {
                  void test(Log log, String name) {
                      log.warning(name);
                  }
              }
              """,
            """
              class Test {
                  void test(Log log, String name) {
                      log.warn(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableDeclarationWithFinalVar() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
                  if (TypeUtils.isString(vd.getType()) && "String".equals(((J.Identifier) vd.getTypeExpression()).getSimpleName())) {
                      JavaCoordinates coordinates = vd.getCoordinates().replace();
                      return JavaTemplate.builder("final var #{}")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), coordinates, new Object[]{vd.getVariables().getFirst().getSimpleName()});
                  }
                  return vd;
              }
          })),
          java(
            """
              import java.util.List;
              import java.util.ArrayList;

              class A {
                  void bar(List<String> lst) {
                      for (String s : lst) {}
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.ArrayList;

              class A {
                  void bar(List<String> lst) {
                      for (final var s : lst) {}
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5289")
    @Test
    void recursiveType() {
        rewriteRun(
          // a context-sensitive template types a lambda argument by a placeholder method's parameter
          spec -> spec.recipeQualityValidation(RecipeQualityValidation.builder().parserTypedTrees(false).build())
            .recipe(toRecipe(() -> new JavaVisitor<>() {
                @Override
                public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                    if (!(lambda.getBody() instanceof J.Block)) {
                        return lambda;
                    }
                    var param = (J.VariableDeclarations) lambda.getParameters().getParameters().getFirst();
                    J.VariableDeclarations.NamedVariable variable = param.getVariables().getFirst();

                    return JavaTemplate.builder("reference -> System.out.println(#{any()})")
                      .contextSensitive()
                      .build()
                      .apply(getCursor(), lambda.getCoordinates().replace(), variable.getName());
                }
            })),
          java(
            """
              import java.util.Optional;

              class BugTest {
                  void run(One<?, ?> firstBuild) {
                      Optional.of(firstBuild).ifPresent(reference -> {});
                  }

                  abstract static class One<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
                  abstract static class Two<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
              }
              """,
            """
              import java.util.Optional;

              class BugTest {
                  void run(One<?, ?> firstBuild) {
                      Optional.of(firstBuild).ifPresent(reference -> System.out.println(reference));
                  }

                  abstract static class One<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
                  abstract static class Two<TwoT extends Two<TwoT, OneT>, OneT extends One<TwoT, OneT>> {}
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

    @Test
    void replaceExpressionLambdaBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                  if (!(lambda.getBody() instanceof J.Literal) ||
                      !Boolean.TRUE.equals(((J.Literal) lambda.getBody()).getValue())) {
                      return super.visitLambda(lambda, ctx);
                  }
                  J.Literal body = (J.Literal) lambda.getBody();
                  return JavaTemplate.builder("Boolean.FALSE")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), body.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> true;
              }
              """,
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> Boolean.FALSE;
              }
              """
          )
        );
    }

    @Test
    void replaceStatementInsideLambdaBlockBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                  if (!(lambda.getBody() instanceof J.Block)) {
                      return super.visitLambda(lambda, ctx);
                  }
                  J.Block block = (J.Block) lambda.getBody();
                  if (block.getStatements().size() != 1 ||
                      !(block.getStatements().get(0) instanceof J.Return) ||
                      !(((J.Return) block.getStatements().get(0)).getExpression() instanceof J.Literal) ||
                      !Boolean.TRUE.equals(((J.Literal) ((J.Return) block.getStatements().get(0)).getExpression()).getValue())) {
                      return super.visitLambda(lambda, ctx);
                  }
                  return JavaTemplate.builder("return Boolean.FALSE;")
                    .contextSensitive()
                    .build()
                    .apply(getCursor(), block.getStatements().get(0).getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> {
                      return true;
                  };
              }
              """,
            """
              import java.util.function.BooleanSupplier;

              class Test {
                  BooleanSupplier always = () -> {
                      return Boolean.FALSE;
                  };
              }
              """
          )
        );
    }

    @Test
    void repeatedParameterOccurrencesGetFreshIds() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<ExecutionContext>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (!"indexOf".equals(method.getSimpleName()) || getCursor().firstEnclosing(J.Ternary.class) != null) {
                      return super.visitMethodInvocation(method, ctx);
                  }
                  return JavaTemplate.builder("#{any(java.util.List)} == null ? -1 : #{any(java.util.List)}.indexOf(#{any()})")
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace(),
                      method.getSelect(), method.getSelect(), method.getArguments().getFirst());
              }
          })),
          java(
            """
              import java.util.List;

              class Test {
                  int m(List<String> l, String s) {
                      return l.indexOf(s);
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int m(List<String> l, String s) {
                      return l == null ? -1 : l.indexOf(s);
                  }
              }
              """,
            spec -> spec.afterRecipe(JavaTemplateTest::assertUniqueIds)
          )
        );
    }

    @Test
    void repeatedNamedPatternOccurrencesGetFreshIds() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<ExecutionContext>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  if (!"indexOf".equals(method.getSimpleName()) || getCursor().firstEnclosing(J.Ternary.class) != null) {
                      return super.visitMethodInvocation(method, ctx);
                  }
                  return JavaTemplate.builder("#{l:any(java.util.List)} == null ? -1 : #{l}.indexOf(#{any()})")
                    .build()
                    .apply(getCursor(), method.getCoordinates().replace(),
                      method.getSelect(), method.getArguments().getFirst());
              }
          })),
          java(
            """
              import java.util.List;

              class Test {
                  int m(List<String> l, String s) {
                      return l.indexOf(s);
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int m(List<String> l, String s) {
                      return l == null ? -1 : l.indexOf(s);
                  }
              }
              """,
            spec -> spec.afterRecipe(JavaTemplateTest::assertUniqueIds)
          )
        );
    }

    private static void assertUniqueIds(J.CompilationUnit cu) {
        Set<UUID> ids = new HashSet<>();
        new JavaIsoVisitor<Integer>() {
            @Override
            public J postVisit(J tree, Integer p) {
                assertThat(ids.add(tree.getId()))
                  .as("Duplicate id %s on %s", tree.getId(), tree.getClass().getSimpleName())
                  .isTrue();
                return tree;
            }
        }.visit(cu, 0);
    }
}
