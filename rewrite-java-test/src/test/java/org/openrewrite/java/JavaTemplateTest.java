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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.cleanup.IndexOfReplaceableByContains;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

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
                  if ((assignment.getAssignment() instanceof J.Literal) && ((J.Literal) assignment.getAssignment()).getValue().equals(1)) {
                      return assignment.withTemplate(
                        JavaTemplate.builder(this::getCursor, "value = 0").build(),
                        assignment.getCoordinates().replace()
                      );
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
                      a = a.withAssignment(mi.withTemplate(
                        JavaTemplate.builder(this::getCursor, "1").build(), mi.getCoordinates().replace()
                      ));
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
                      mv = multiVariable.withTemplate(
                        JavaTemplate.builder(this::getCursor, "Object #{}").build(),
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
                  if ( !TypeUtils.isOfClassType(iterable.getType(), "java.lang.String"))
                      return control;
                  iterable = iterable.withTemplate(
                    JavaTemplate.builder(this::getCursor, "new Object[0]")
                      .build(),
                    iterable.getCoordinates().replace()
                  );
                  return control.withIterable(iterable);
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
                      lit = lit.withTemplate(
                        JavaTemplate.builder(this::getCursor, "43")
                          .build(),
                        lit.getCoordinates().replace()
                      );
                      return binary.withLeft(lit);
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
                  var nc = super.visitNewClass(newClass, p);
                  if (TypeUtils.isOfClassType(newClass.getType(), "java.util.ArrayList")) {
                      nc = nc.withTemplate(JavaTemplate.builder(this::getCursor, "Collections.emptyList()")
                          .imports("java.util.Collections").build(),
                        newClass.getCoordinates().replace());
                  }
                  return nc;
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

    @SuppressWarnings("LoopConditionNotUpdatedInsideLoop")
    @Test
    void templatingWhileLoopCondition() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext p) {
                  if (binary.getLeft() instanceof J.MethodInvocation) {
                      J.MethodInvocation mi = (J.MethodInvocation) binary.getLeft();
                      return binary.withTemplate(
                        JavaTemplate.builder(this::getCursor, "!#{any(java.util.List)}.isEmpty()")
                          .build(), mi.getCoordinates().replace(), mi.getSelect()
                      );
                  } else if (binary.getLeft() instanceof J.Unary) {
                      return binary.getLeft();
                  }
                  return binary;
              }
          })).expectedCyclesThatMakeChanges(2),
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
              final JavaTemplate twoArgScale = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "#{any(int)}, #{}")
                .imports("java.math.RoundingMode").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, p);
                  if (bigDecimalSetScale.matches(mi)) {
                      mi = mi.withTemplate(
                        twoArgScale,
                        mi.getCoordinates().replaceArguments(),
                        mi.getArguments().get(0), "RoundingMode.HALF_UP"
                      );
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
                  return unary.withTemplate(
                    JavaTemplate.builder(this::getCursor, "#{any()}++").build(),
                    unary.getCoordinates().replace(),
                    unary.getExpression()
                  );
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
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
                      return fa.withTemplate(
                        JavaTemplate.builder(this::getCursor, "#{any(java.io.File)}.getCanonicalFile().toPath()").build(),
                        fa.getCoordinates().replace(),
                        fa
                      );
                  } else {
                      return fa;
                  }
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
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
                      return method.withTemplate(
                        JavaTemplate.builder(this::getCursor, "words.add(\"jon\");").build(),
                        method.getCoordinates().replace()
                      );
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
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "new A()")
                .build();

              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext p) {
                  if (newClass.getArguments().get(0) instanceof J.Empty) {
                      return newClass;
                  }
                  return newClass.withTemplate(t, newClass.getCoordinates().replace());
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
                  var mi = super.visitMethodInvocation(method, p);
                  if (mm.matches(mi)) {
                      mi = mi.withTemplate(
                        JavaTemplate.builder(this::getCursor, "Arrays.asList(#{any(java.lang.Integer)}, #{any(java.lang.Integer)}, #{any(java.lang.Integer)})")
                          .imports("java.util.Arrays").build(),
                        mi.getCoordinates().replace(), mi.getArguments().get(0), mi.getArguments().get(1), mi.getArguments().get(2)
                      );
                  }
                  return mi;
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
    void multiDimentionalArrayInitializer() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher mm = new MethodMatcher("java.util.stream.IntStream sum()");

              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext p) {
                  J.NewClass nc = (J.NewClass) super.visitNewClass(newClass, p);
                  return nc.withTemplate(
                    JavaTemplate.builder(this::getCursor, "Integer.valueOf(#{any(java.lang.Integer)}")
                      .build(), nc.getCoordinates().replace(), nc.getArguments().get(0)
                  );
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
                  J.MethodInvocation sizeCall = (J.MethodInvocation) binary.getLeft();
                  return sizeCall.withTemplate(JavaTemplate.builder(this::getCursor, "!#{any(java.util.Collection)}.isEmpty()").build(),
                      sizeCall.getCoordinates().replace(), sizeCall.getSelect()).
                    withPrefix(binary.getPrefix());
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

    @SuppressWarnings({"IndexOfReplaceableByContains", "InfiniteLoopStatement"})
    @Issue("https://github.com/openrewrite/rewrite/issues/2565")
    @Test
    void noBlockControlFlow() {
        rewriteRun(
          spec -> spec.recipe(new IndexOfReplaceableByContains()),
          java(
            """
              class Test {
                  void test(String str) {
                      for (;;)
                          if ("".indexOf(str) >= 0)
                              System.out.println("help");
                  }
              }
              """,
            """
              class Test {
                  void test(String str) {
                      for (;;)
                          if ("".contains(str))
                              System.out.println("help");
                  }
              }
              """
          )
        );
    }
}
