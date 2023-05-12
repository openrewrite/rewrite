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
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

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
                  if ((assignment.getAssignment() instanceof J.Literal) && ((J.Literal) assignment.getAssignment()).getValue().equals(1)) {
                      return assignment.withTemplate(
                        JavaTemplate.builder("value = 0").build(),
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

    @Test
    void parseErrorWhenReplacingAssert() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assert visitAssert(J.Assert _assert, ExecutionContext ctx) {
                  _assert.getCondition().withTemplate(JavaTemplate.builder(this::getCursor, "null").build(), _assert.getCondition().getCoordinates().replace());
                  return super.visitAssert(_assert, ctx);
              }
          })),
          java(
            """
              import java.util.List;
              class Test {
                  void m() {
                      assert new ArrayList<>().size() != 0;
                  }
                  List<String> triggeredParsingError() {
                      return null;
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
                  return classDecl.withTemplate(JavaTemplate.builder(this::getCursor, """
                      void m2() {
                      	  #{any()}
                      }
                      """).build(),
                    classDecl.getBody().getStatements().get(0).getCoordinates().after(),
                    ((J.MethodDeclaration) classDecl.getBody().getStatements().get(0)).getBody().getStatements().get(0));
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
                      method = method.withTemplate(JavaTemplate.builder(this::getCursor, "Collections.singletonList(#{any()})")
                          .imports("java.util.Collections").build(),
                        method.getCoordinates().replace(), method.getArguments().get(0));
                      maybeAddImport("java.util.Collections");
                      maybeRemoveImport("java.util.Arrays");
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
                      method = method.withTemplate(JavaTemplate.builder(this::getCursor, "Collections.singletonList(#{any()})")
                          .imports("java.util.Collections").build(),
                        method.getCoordinates().replace(), method.getArguments().get(0));
                      maybeAddImport("java.util.Collections");
                      maybeRemoveImport("java.util.Arrays");
                  }
                  return method;
              }
          })).afterRecipe(run -> {
              new JavaIsoVisitor<Integer>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                      JavaType.Parameterized type = (JavaType.Parameterized) method.getType();
                      assertThat(type.getTypeParameters()).hasSize(1);
                      assertThat(type.getTypeParameters().get(0)).isInstanceOf(JavaType.GenericTypeVariable.class);
                      return method;
                  }
              }.visit(run.getChangeset().getAllResults().get(0).getAfter(), 0);
          }),
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
                      method = method.withTemplate(JavaTemplate.builder(this::getCursor, "Collections.singletonList(#{any()})")
                          .imports("java.util.Collections").build(),
                        method.getCoordinates().replace(), method.getArguments().get(0));
                      maybeAddImport("java.util.Collections");
                      maybeRemoveImport("java.util.Arrays");
                  }
                  return method;
              }
          })).afterRecipe(run -> {
              new JavaIsoVisitor<Integer>() {
                  @Override
                  public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer integer) {
                      JavaType.Parameterized type = (JavaType.Parameterized) method.getType();
                      assertThat(type.getTypeParameters()).hasSize(1);
                      assertThat(type.getTypeParameters().get(0)).isInstanceOf(JavaType.Parameterized.class);
                      assertThat(((JavaType.Parameterized) type.getTypeParameters().get(0)).getTypeParameters()
                        .get(0)).isInstanceOf(JavaType.GenericTypeVariable.class);
                      return method;
                  }
              }.visit(run.getChangeset().getAllResults().get(0).getAfter(), 0);
          }),
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2540")
    void replaceMemberReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMemberReference(J.MemberReference memberRef, ExecutionContext executionContext) {
                      return memberRef.withTemplate(
                        JavaTemplate
                          .builder(this::getCursor, "() -> new ArrayList<>(1)")
                          .imports("java.util.ArrayList")
                          .build(),
                        memberRef.getCoordinates().replace()
                      );
                  }
              })).expectedCyclesThatMakeChanges(1).cycles(1),
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

    @Test
    void nestedEnums() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitBinary(J.Binary binary, ExecutionContext p) {
                  return binary.withTemplate(JavaTemplate.builder(this::getCursor, "\"ab\"").build(),
                      binary.getCoordinates().replace());
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
                      return block.withTemplate(JavaTemplate.builder(this::getCursor, "String x = s;").build(),
                        block.getCoordinates().firstStatement());
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
                      return block.withTemplate(JavaTemplate.builder(this::getCursor, "String x = s;").build(),
                      block.getStatements().get(0).getCoordinates().after());
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
}
