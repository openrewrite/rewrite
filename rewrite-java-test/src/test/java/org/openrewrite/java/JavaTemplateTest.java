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
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeQualityValidation;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed", "UnnecessaryBoxing", "StatementWithEmptyBody", "UnusedAssignment", "SizeReplaceableByIsEmpty", "ResultOfMethodCallIgnored", "RedundantOperationOnEmptyContainer", "StringOperationCanBeSimplified"})
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

    private final Recipe replaceToStringWithLiteralRecipe = toRecipe(() -> new JavaVisitor<>() {
        private final MethodMatcher toString = new MethodMatcher("java.lang.String toString()");

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J mi = super.visitMethodInvocation(method, ctx);
            if (mi instanceof J.MethodInvocation && toString.matches((J.MethodInvocation) mi)) {
                return JavaTemplate.apply("#{any(java.lang.String)}", getCursor(), ((J.MethodInvocation) mi).getCoordinates().replace(),
                  ((J.MethodInvocation) mi).getSelect());
            }
            return mi;
        }
    });

    @Test
    void chainedMethodInvocationsAsNewClassArgument2() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              class T {
                  void m(String jsonPayload) {
                      HttpEntity entity = new HttpEntity(jsonPayload.toString(), 0);
                  }
                  class HttpEntity {
                      HttpEntity(String s, int i){}
                  }
              }
              """,
            """
              class T {
                  void m(String jsonPayload) {
                      HttpEntity entity = new HttpEntity(jsonPayload, 0);
                  }
                  class HttpEntity {
                      HttpEntity(String s, int i){}
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2185")
    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void chainedMethodInvocationsAsNewClassArgument() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              import java.util.ArrayList;
              import java.util.Collections;
              public class T {
                  void m(String arg) {
                      U u = new U(arg.toString().toCharArray());
                  }
                  class U {
                      U(char[] chars){}
                  }
              }
              """,
            """
              import java.util.ArrayList;
              import java.util.Collections;
              public class T {
                  void m(String arg) {
                      U u = new U(arg.toCharArray());
                  }
                  class U {
                      U(char[] chars){}
                  }
              }
              """
          )
        );
    }

    @Test
    void methodArgumentStopCommentsOnlyTerminateEnumInitializers() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              import java.io.File;
              import java.io.IOException;
              import java.util.List;

              class Test {
                  File getFile(File testDir, List<String> compileClassPath ) throws IOException {
                      assertEquals(new File(testDir, "ejbs/target/classes").getCanonicalFile(),
                          new File(compileClassPath.get(1).toString()).getCanonicalFile());
                  }
                  void assertEquals(File f1, File f2) {}
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              import java.util.List;

              class Test {
                  File getFile(File testDir, List<String> compileClassPath ) throws IOException {
                      assertEquals(new File(testDir, "ejbs/target/classes").getCanonicalFile(),
                          new File(compileClassPath.get(1)).getCanonicalFile());
                  }
                  void assertEquals(File f1, File f2) {}
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2475")
    @Test
    void enumWithinEnum() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              public enum Test {
                  INSTANCE;
                  public enum MatchMode { DEFAULT }
                  public String doSomething() {
                      return "STARTING".toString();
                  }
              }
              """,
            """
              public enum Test {
                  INSTANCE;
                  public enum MatchMode { DEFAULT }
                  public String doSomething() {
                      return "STARTING";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1796")
    @Test
    void replaceIdentifierWithMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  return method.withBody((J.Block) visit(method.getBody(), ctx));
              }

              @Override
              public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                  if ("f".equals(identifier.getSimpleName())) {
                      return JavaTemplate.apply("#{any(java.io.File)}.getCanonicalFile().toPath()",
                        getCursor(), identifier.getCoordinates().replace(), identifier);
                  }
                  return identifier;
              }
          }).withMaxCycles(1)),
          java(
            """
              import java.io.File;
              class Test {
                  void test(File f) {
                      System.out.println(f);
                  }
              }
              """,
            """
              import java.io.File;
              class Test {
                  void test(File f) {
                      System.out.println(f.getCanonicalFile().toPath());
                  }
              }
              """
          )
        );
    }

    @Test
    void enumClassWithAnonymousInnerClassConstructor() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              enum MyEnum {
                  THING_ONE(new MyEnumThing() {
                      @Override
                      public String getName() {
                          return "Thing One".toString();
                      }
                  });
                  private final MyEnumThing enumThing;
                  MyEnum(MyEnumThing myEnumThing) {
                      this.enumThing = myEnumThing;
                  }
                  interface MyEnumThing {
                      String getName();
                  }
              }
              """,
            """
              enum MyEnum {
                  THING_ONE(new MyEnumThing() {
                      @Override
                      public String getName() {
                          return "Thing One";
                      }
                  });
                  private final MyEnumThing enumThing;
                  MyEnum(MyEnumThing myEnumThing) {
                      this.enumThing = myEnumThing;
                  }
                  interface MyEnumThing {
                      String getName();
                  }
              }
              """
          )
        );
    }

    @Test
    void replacingMethodInvocationWithinEnum() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              public enum Options {

                  JAR("instance.jar.file"),
                  JVM_ARGUMENTS("instance.vm.args");

                  private String name;

                  Options(String name) {
                      this.name = name;
                  }

                  public String asString() {
                      return System.getProperty(name);
                  }

                  public Integer asInteger(int defaultValue) {
                      String string  = asString();
                      return new Integer(string.toString());
                  }
              }
              """,
            """
              public enum Options {

                  JAR("instance.jar.file"),
                  JVM_ARGUMENTS("instance.vm.args");

                  private String name;

                  Options(String name) {
                      this.name = name;
                  }

                  public String asString() {
                      return System.getProperty(name);
                  }

                  public Integer asInteger(int defaultValue) {
                      String string  = asString();
                      return new Integer(string);
                  }
              }
              """
          )
        );
    }

    @Test
    void replacingMethodInvocationWithinInnerEnum() {
        rewriteRun(
          spec -> spec.recipe(replaceToStringWithLiteralRecipe),
          java(
            """
              public class Test {
                  void doSomething(Options options) {
                      switch (options) {
                          case JAR:
                          case JVM_ARGUMENTS:
                              System.out.println("");
                      }
                  }
                  enum Options {
                      JAR(0, "instance.jar.file".toString()),
                      JVM_ARGUMENTS(1, "instance.vm.args");

                      private final String name;
                      private final int id;

                      Options(int id,String name) {
                          this.id = id;
                          this.name = name;
                      }

                      public String asString() {
                          return System.getProperty(name);
                      }

                      public Integer asInteger(int defaultValue) {
                          String string  = asString();
                          return new Integer(string);
                      }
                  }
              }
              """,
            """
              public class Test {
                  void doSomething(Options options) {
                      switch (options) {
                          case JAR:
                          case JVM_ARGUMENTS:
                              System.out.println("");
                      }
                  }
                  enum Options {
                      JAR(0, "instance.jar.file"),
                      JVM_ARGUMENTS(1, "instance.vm.args");

                      private final String name;
                      private final int id;

                      Options(int id,String name) {
                          this.id = id;
                          this.name = name;
                      }

                      public String asString() {
                          return System.getProperty(name);
                      }

                      public Integer asInteger(int defaultValue) {
                          String string  = asString();
                          return new Integer(string);
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1198")
    @SuppressWarnings({
      "UnnecessaryBoxing",
      "CachedNumberConstructorCall",
      "ResultOfMethodCallIgnored"
      , "Convert2MethodRef"})
    @Test
    void replaceNamedVariableInitializerMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher matcher = new MethodMatcher("Integer valueOf(..)");
              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (matcher.matches(method)) {
                      return JavaTemplate.apply("new Integer(#{any()})",
                        getCursor(), method.getCoordinates().replace(), method.getArguments().getFirst());
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.Arrays;
              import java.util.List;
              import java.util.function.Function;
              class Test {
                  void t() {
                      List<String> nums = Arrays.asList("1", "2", "3");
                      nums.forEach(s -> Integer.valueOf(s));
                  }
                  void inLambda(int i) {
                      Function<String, Integer> toString = it -> {
                          try {
                              return Integer.valueOf(it);
                          }catch (NumberFormatException ex) {
                              ex.printStackTrace();
                          }
                          return 0;
                      };
                  }
                  String inClassDeclaration(int i) {
                      return new Object() {
                          void foo() {
                              Integer.valueOf(i);
                          }
                      }.toString();
                  }
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;
              import java.util.function.Function;
              class Test {
                  void t() {
                      List<String> nums = Arrays.asList("1", "2", "3");
                      nums.forEach(s -> new Integer(s));
                  }
                  void inLambda(int i) {
                      Function<String, Integer> toString = it -> {
                          try {
                              return new Integer(it);
                          }catch (NumberFormatException ex) {
                              ex.printStackTrace();
                          }
                          return 0;
                      };
                  }
                  String inClassDeclaration(int i) {
                      return new Object() {
                          void foo() {
                              new Integer(i);
                          }
                      }.toString();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1821")
    @SuppressWarnings("UnusedAssignment")
    @Test
    void assignmentNotPartOfVariableDeclaration() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext p) {
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
              class A {
                  void foo() {
                      int i;
                      i = Integer.valueOf(1);
                  }
              }
              """,
            """
              class A {
                  void foo() {
                      int i;
                      i = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodInvocationInsideReturn() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("String.valueOf(#{any(String)})")
                .build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("toString")) {
                      return t.apply(getCursor(), method.getCoordinates().replace(),
                        method.getSelect());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  String test(String s) {
                      return s.toString();
                  }
              }
              """,
            """
              class Test {
                  String test(String s) {
                      return String.valueOf(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceNewClassInsideReturn() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("new StringBuilder(#{any(String)})")
                .build();

              @Override
              public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  newClass = super.visitNewClass(newClass, ctx);
                  if (newClass.getClazz() != null &&
                      newClass.getClazz().toString().equals("StringBuffer") &&
                      newClass.getArguments().size() == 1) {
                      return t.apply(getCursor(), newClass.getCoordinates().replace(),
                        newClass.getArguments().get(0));
                  }
                  return newClass;
              }
          })),
          java(
            """
              class Test {
                  CharSequence test(String s) {
                      return new StringBuffer(s);
                  }
              }
              """,
            """
              class Test {
                  CharSequence test(String s) {
                      return new StringBuilder(s);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7153")
    @Test
    void replaceMethodInvocationInsideTypeCast() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("String.valueOf(#{any(String)})")
                .build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                  method = super.visitMethodInvocation(method, ctx);
                  if (method.getSimpleName().equals("toString")) {
                      return t.apply(getCursor(), method.getCoordinates().replace(),
                        method.getSelect());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  Object test(String s) {
                      return (Object) s.toString();
                  }
              }
              """,
            """
              class Test {
                  Object test(String s) {
                      return (Object) String.valueOf(s);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/7153")
    @Test
    void replaceNewClassInsideTypeCast() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder("new StringBuilder(#{any(String)})")
                .build();

              @Override
              public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  newClass = super.visitNewClass(newClass, ctx);
                  if (newClass.getClazz() != null &&
                      newClass.getClazz().toString().equals("StringBuffer") &&
                      newClass.getArguments().size() == 1) {
                      return t.apply(getCursor(), newClass.getCoordinates().replace(),
                        newClass.getArguments().get(0));
                  }
                  return newClass;
              }
          })),
          java(
            """
              class Test {
                  Object test(String s) {
                      return (CharSequence) new StringBuffer(s);
                  }
              }
              """,
            """
              class Test {
                  Object test(String s) {
                      return (CharSequence) new StringBuilder(s);
                  }
              }
              """
          )
        );
    }
}
