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
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnnecessaryBoxing", "StatementWithEmptyBody", "UnusedAssignment"})
class JavaTemplateTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2090")
    @Test
    void assignmentWithinIfPredicate() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext p) {
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

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/66")
    @Test
    void lambdaIsNewClass() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext p) {
                  var a = assignment;
                  if (a.getAssignment() instanceof J.MethodInvocation) {
                      J.MethodInvocation mi = (J.MethodInvocation) a.getAssignment();
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

    @DocumentExample
    @SuppressWarnings({"RedundantOperationOnEmptyContainer"})
    @Test
    void replaceForEachControlVariableType() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  var mv = super.visitVariableDeclarations(multiVariable, p);
                  if (mv.getVariables().get(0).getInitializer() == null && TypeUtils.isOfType(mv.getTypeExpression()
                    .getType(), JavaType.Primitive.String)) {
                      mv = JavaTemplate.apply("Object #{}", getCursor(),
                        multiVariable.getCoordinates().replace(),
                        multiVariable.getVariables().get(0).getSimpleName()
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
    void replaceForEachControlIterable() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.ForEachLoop.Control visitForEachControl(J.ForEachLoop.Control control, ExecutionContext executionContext) {
                  control = super.visitForEachControl(control, executionContext);
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
              public J.Binary visitBinary(J.Binary binary, ExecutionContext executionContext) {
                  binary = super.visitBinary(binary, executionContext);
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
              public J visitNewClass(J.NewClass newClass, ExecutionContext p) {
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/3102")
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
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
                  return JavaTemplate.builder("""
                      void m2() {
                      	  #{any()}
                      }
                      """
                    )
                    .build()
                    .apply(
                      getCursor(),
                      classDecl.getBody().getStatements().get(0).getCoordinates().after(),
                      ((J.MethodDeclaration) classDecl.getBody().getStatements().get(0)).getBody().getStatements().get(0)
                    );
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
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    void replaceAnonymousClassObject() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("asList")) {
                      maybeAddImport("java.util.Collections");
                      maybeRemoveImport("java.util.Arrays");

                      return JavaTemplate.builder("Collections.singletonList(#{any()})")
                        .imports("java.util.Collections")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
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

    @Test
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    void replaceGenericTypedObject() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("asList")) {
                      maybeAddImport("java.util.Collections");
                      maybeRemoveImport("java.util.Arrays");
                      return JavaTemplate.builder("Collections.singletonList(#{any()})")
                        .contextSensitive()
                        .imports("java.util.Collections")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                  }
                  return method;
              }
          })).afterRecipe(run -> new JavaIsoVisitor<Integer>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                  JavaType.Parameterized type = (JavaType.Parameterized) method.getType();
                  assertThat(type.getTypeParameters()).hasSize(1);
                  assertThat(type.getTypeParameters().get(0)).isInstanceOf(JavaType.GenericTypeVariable.class);
                  return method;
              }
          }.visit(run.getChangeset().getAllResults().get(0).getAfter(), 0)),
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

    @Test
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    void replaceParameterizedTypeObject() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("asList")) {
                      maybeAddImport("java.util.Collections");
                      maybeRemoveImport("java.util.Arrays");
                      return JavaTemplate.builder("Collections.singletonList(#{any()})")
                        .contextSensitive()
                        .imports("java.util.Collections")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                  }
                  return method;
              }
          })).afterRecipe(run -> new JavaIsoVisitor<Integer>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                  JavaType.Parameterized type = (JavaType.Parameterized) method.getType();
                  assertThat(type.getTypeParameters()).hasSize(1);
                  assertThat(type.getTypeParameters().get(0)).isInstanceOf(JavaType.Parameterized.class);
                  assertThat(((JavaType.Parameterized) type.getTypeParameters().get(0)).getTypeParameters()
                    .get(0)).isInstanceOf(JavaType.GenericTypeVariable.class);
                  return method;
              }
          }.visit(run.getChangeset().getAllResults().get(0).getAfter(), 0)),
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
          spec -> spec.expectedCyclesThatMakeChanges(2).recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext p) {
                  if (binary.getLeft() instanceof J.MethodInvocation) {
                      J.MethodInvocation mi = (J.MethodInvocation) binary.getLeft();
                      return JavaTemplate.builder("!#{any(java.util.List)}.isEmpty()")
                        .build()
                        .apply(getCursor(), mi.getCoordinates().replace(), mi.getSelect());
                  } else if (binary.getLeft() instanceof J.Unary) {
                      return binary.getLeft();
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
              final JavaTemplate twoArgScale = JavaTemplate.builder("#{any(int)}, #{}").contextSensitive()
                .imports("java.math.RoundingMode").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, p);
                  if (bigDecimalSetScale.matches(mi)) {
                      mi = twoArgScale.apply(updateCursor(mi), mi.getCoordinates().replaceArguments(),
                        mi.getArguments().get(0), "RoundingMode.HALF_UP");
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
              public J visitUnary(J.Unary unary, ExecutionContext p) {
                  return JavaTemplate.builder("#{any()}++")
                    .contextSensitive()
                    .build().apply(
                      getCursor(),
                      unary.getCoordinates().replace(),
                      unary.getExpression()
                    );
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2540")
    void replaceMemberReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMemberReference(J.MemberReference memberRef, ExecutionContext executionContext) {
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
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  return method.withBody((J.Block) visit(method.getBody(), p));
              }

              @Override
              public J visitFieldAccess(J.FieldAccess fa, ExecutionContext p) {
                  if (fa.getSimpleName().equals("f")) {
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
                  if (method.getSimpleName().equals("clear")) {
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
              public J visitNewClass(J.NewClass newClass, ExecutionContext p) {
                  if (newClass.getArguments().get(0) instanceof J.Empty) {
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
                            
                      public Type(String t) {
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
                            
                      public Type(String t) {
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2375")
    void arrayInitializer() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final MethodMatcher mm = new MethodMatcher("abc.ArrayHelper of(..)");

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (mm.matches(method)) {
                      return JavaTemplate.builder("Arrays.asList(#{any(java.lang.Integer)}, #{any(java.lang.Integer)}, #{any(java.lang.Integer)})")
                        .imports("java.util.Arrays")
                        .build()
                        .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0),
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
              """
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

    @SuppressWarnings("ALL")
    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2375")
    void multiDimensionalArrayInitializer() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher mm = new MethodMatcher("java.util.stream.IntStream sum()");

              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext p) {
                  J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, p);
                  return JavaTemplate.apply("Integer.valueOf(#{any(java.lang.Integer)}",
                    getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
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
              public J visitBinary(J.Binary binary, ExecutionContext p) {
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
              public J visitBinary(J.Binary binary, ExecutionContext p) {
                  return JavaTemplate.builder("\"ab\"").contextSensitive().build()
                    .apply(getCursor(), binary.getCoordinates().replace());
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
                        .apply(getCursor(), block.getStatements().get(0).getCoordinates().after());
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
                  String code = "s.toLowerCase();";
                  return JavaTemplate
                    .builder(code)
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
                    Expression expected = args.get(0);
                    Expression actual = args.get(1);

                    maybeAddImport("org.assertj.core.api.Assertions", "assertThat");
                    maybeRemoveImport("org.junit.jupiter.api.Assertions");

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
                  Expression expected = args.get(0);
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
                  J.VariableDeclarations vd = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
                  if (vd.getVariables().size() == 1 || vd.getVariables().get(0).getSimpleName().equals("a")) {
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
}
