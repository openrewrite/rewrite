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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings({"ConstantConditions", "PatternVariableCanBeUsed"})
class JavaTemplateTest implements RewriteTest {

    private final Recipe replaceToStringWithLiteralRecipe = toRecipe(() -> new JavaVisitor<>() {
        private final MethodMatcher toString = new MethodMatcher("java.lang.String toString()");
        private final JavaTemplate t = JavaTemplate.builder(this::getCursor, "#{any(java.lang.String)}").build();

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J mi = super.visitMethodInvocation(method, ctx);
            if (mi instanceof J.MethodInvocation && toString.matches((J.MethodInvocation) mi)) {
                return mi.withTemplate(t, ((J.MethodInvocation) mi).getCoordinates().replace(),
                  ((J.MethodInvocation) mi).getSelect());
            }
            return mi;
        }
    });

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

    @Issue("https://github.com/openrewrite/rewrite/issues/1339")
    @Test
    void templateStatementIsWithinTryWithResourcesBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                  var nc = super.visitNewClass(newClass, ctx);
                  var md = getCursor().firstEnclosing(J.MethodDeclaration.class);
                  if (md != null && md.getSimpleName().equals("createBis")) {
                      return nc;
                  }
                  if (newClass.getType() != null &&
                      TypeUtils.asFullyQualified(newClass.getType()).getFullyQualifiedName().equals("java.io.ByteArrayInputStream") &&
                      !newClass.getArguments().isEmpty()) {
                      nc = nc.withTemplate(
                        JavaTemplate.builder(this::getCursor, "createBis(#{anyArray()})").build(),
                        newClass.getCoordinates().replace(), newClass.getArguments().get(0)
                      );
                  }
                  return nc;
              }
          })),
          java(
            """
              import java.io.*;
              import java.nio.charset.StandardCharsets;
                            
              class Test {
                  ByteArrayInputStream createBis(byte[] bytes) {
                      return new ByteArrayInputStream(bytes);
                  }
                  
                  void doSomething() {
                      String sout = "";
                      try (BufferedReader br = new BufferedReader(new FileReader(null))) {
                          new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8));
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
              """,
            """
              import java.io.*;
              import java.nio.charset.StandardCharsets;
                            
              class Test {
                  ByteArrayInputStream createBis(byte[] bytes) {
                      return new ByteArrayInputStream(bytes);
                  }
                  
                  void doSomething() {
                      String sout = "";
                      try (BufferedReader br = new BufferedReader(new FileReader(null))) {
                          createBis("bytes".getBytes(StandardCharsets.UTF_8));
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
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
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  return method.withBody((J.Block) visit(method.getBody(), p));
              }

              @Override
              public J visitIdentifier(J.Identifier identifier, ExecutionContext p) {
                  if (identifier.getSimpleName().equals("f")) {
                      return identifier.withTemplate(
                        JavaTemplate.builder(this::getCursor, "#{any(java.io.File)}.getCanonicalFile().toPath()").build(),
                        identifier.getCoordinates().replace(),
                        identifier
                      );
                  }
                  return identifier;
              }
          })).expectedCyclesThatMakeChanges(1).cycles(1),
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
    void replacePackage() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "b").build();

              @Override
              public J.Package visitPackage(J.Package pkg, ExecutionContext p) {
                  if (pkg.getExpression().printTrimmed(getCursor()).equals("a")) {
                      return pkg.withTemplate(t, pkg.getCoordinates().replace());
                  }
                  return super.visitPackage(pkg, p);
              }

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  var cd = super.visitClassDeclaration(classDecl, p);
                  if (classDecl.getType().getPackageName().equals("a")) {
                      cd = cd.withType(cd.getType().withFullyQualifiedName("b.${cd.getSimpleName()}"));
                  }
                  return cd;
              }
          })),
          java(
            """
              package a;
              class Test {
              }
              """,
            """
              package b;
              class Test {
              }
              """
          )
        );
    }

    @Test
    void replaceMethod() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int test2(int n) { return n; }").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getSimpleName().equals("test")) {
                      return method.withTemplate(t, method.getCoordinates().replace());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              var methodType = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
              assertThat(methodType.getReturnType()).isEqualTo(JavaType.Primitive.Int);
              assertThat(methodType.getParameterTypes()).containsExactly(JavaType.Primitive.Int);
          }),
          java(
            """
              class Test {
                  void test() {
                  }
              }
              """,
            """
              class Test {
                            
                  int test2(int n) {
                      return n;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("Convert2MethodRef")
    @Test
    void replaceLambdaWithMethodReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Object::toString").build();

              @Override
              public J visitLambda(J.Lambda lambda, ExecutionContext p) {
                  return lambda.withTemplate(t, lambda.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = it -> it.toString();
              }
              """,
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = Object::toString;
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "CodeBlock2Expr"})
    void replaceStatementInLambdaBodySingleStatementBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "return n == 1;").build();

              @Override
              public J visitReturn(J.Return retrn, ExecutionContext p) {
                  if (retrn.getExpression() instanceof J.Binary) {
                      J.Binary binary = (J.Binary) retrn.getExpression();
                      if (binary.getRight() instanceof J.Literal &&
                          ((J.Literal) binary.getRight()).getValue().equals(0)) {
                          return retrn.withTemplate(t, retrn.getCoordinates().replace());
                      }
                  }
                  return retrn;
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  int n;

                  void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          return n == 0;
                      });
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  int n;

                  void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          return n == 1;
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    void replaceStatementInLambdaBodyWithVariableDeclaredInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "return n == 1;").build();

              @Override
              public J visitReturn(J.Return retrn, ExecutionContext p) {
                  if (retrn.getExpression() instanceof J.Binary) {
                      J.Binary binary = (J.Binary) retrn.getExpression();
                      if (binary.getRight() instanceof J.Literal && ((J.Literal) binary.getRight()).getValue().equals(0)) {
                          return retrn.withTemplate(t, retrn.getCoordinates().replace());
                      }
                  }
                  return retrn;
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          int n = 0;
                          return n == 0;
                      });
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<Object> obj) {
                      obj.filter(o -> {
                          int n = 0;
                          return n == 1;
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    void replaceStatementInLambdaBodyMultiStatementBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "#{any(java.lang.String)}.toUpperCase()").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (method.getSimpleName().equals("toLowerCase")) {
                      return method.withTemplate(t, method.getCoordinates().replace(), method.getSelect());
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.map(o -> {
                          String str = o;
                          str = o.toLowerCase();
                          return str;
                      });
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.map(o -> {
                          String str = o;
                          str = o.toUpperCase();
                          return str;
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1120")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "SizeReplaceableByIsEmpty"})
    void replaceSingleExpressionInLambdaBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "#{any(java.lang.String)}.toUpperCase()").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (method.getSimpleName().equals("toLowerCase")) {
                      return method.withTemplate(t, method.getCoordinates().replace(), method.getSelect());
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.filter(o -> o.toLowerCase().length() > 0);
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  static void method(Stream<String> obj) {
                      obj.filter(o -> o.toUpperCase().length() > 0);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2176")
    @Test
    void replaceSingleExpressionInLambdaBodyWithExpression() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher enumEquals = new MethodMatcher("java.lang.Enum equals(java.lang.Object)");
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "#{any()} == #{any()}").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (enumEquals.matches(method)) {
                      return method.withTemplate(t, method.getCoordinates().replace(), method.getSelect(), method.getArguments().get(0));
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.stream.Stream;

              class Test {
                  enum Abc {A,B,C}
                  static void method(Stream<Abc> obj) {
                      Object a = obj.filter(o -> o.equals(Abc.A));
                  }
              }
              """,
            """
              import java.util.stream.Stream;

              class Test {
                  enum Abc {A,B,C}
                  static void method(Stream<Abc> obj) {
                      Object a = obj.filter(o -> o == Abc.A);
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("ClassInitializerMayBeStatic")
    @Test
    void replaceMethodNameAndArgumentsSimultaneously() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "acceptString(#{any()}.toString())")
                .javaParser(() -> JavaParser.fromJavaVersion()
                  .dependsOn(
                    """
                          package org.openrewrite;
                          public class A {
                              public A acceptInteger(Integer i) { return this; }
                              public A acceptString(String s) { return this; }
                              public A someOtherMethod() { return this; }
                          }
                      """
                  )
                  .build()).build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, p);
                  if (m.getSimpleName().equals("acceptInteger")) {
                      m = m.withTemplate(t, m.getCoordinates().replaceMethod(), m.getArguments().get(0));
                  }
                  return m;
              }
          })),
          java(
            """
              package org.openrewrite;
              public class A {
                  public A acceptInteger(Integer i) { return this; }
                  public A acceptString(String s) { return this; }
                  public A someOtherMethod() { return this; }
              }
              """
          ),
          java(
            """
              package org.openrewrite;
                            
              public class Foo {
                  {
                      Integer i = 1;
                      new A().someOtherMethod()
                              .acceptInteger(i)
                              .someOtherMethod();
                  }
              }
              """,
            """
              package org.openrewrite;
                            
              public class Foo {
                  {
                      Integer i = 1;
                      new A().someOtherMethod()
                              .acceptString(i.toString())
                              .someOtherMethod();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodInvocationWithArray() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "#{anyArray(int)}").build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  J.MethodInvocation m = super.visitMethodInvocation(method, p);
                  if (m.getSimpleName().equals("method") && m.getArguments().size() == 2) {
                      m = m.withTemplate(t, m.getCoordinates().replaceArguments(), m.getArguments().get(0));
                  }
                  return m;
              }
          })),
          java(
            """
              package org.openrewrite;
              public class Test {
                  public void method(int[] val) {}
                  public void method(int[] val1, String val2) {}
              }
              """
          ),
          java(
            """
              import org.openrewrite.Test;
              class A {
                  public void method() {
                      Test test = new Test();
                      int[] arr = new int[]{};
                      test.method(arr, null);
                  }
              }
              """,
            """
              import org.openrewrite.Test;
              class A {
                  public void method() {
                      Test test = new Test();
                      int[] arr = new int[]{};
                      test.method(arr);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/602")
    @Test
    void replaceMethodInvocationWithMethodReference() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Object::toString").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  return method.withTemplate(t, method.getCoordinates().replace());
              }
          })),
          java(
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = getToString();
                  
                  static Function<Object, String> getToString() {
                      return Object::toString;
                  }
              }
              """,
            """
              import java.util.function.Function;

              class Test {
                  Function<Object, String> toString = Object::toString;
                  
                  static Function<Object, String> getToString() {
                      return Object::toString;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int m, java.util.List<String> n")
                .build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getSimpleName().equals("test") && method.getParameters().size() == 1) {
                      // insert in outer method
                      J.MethodDeclaration m = method.withTemplate(t, method.getCoordinates().replaceParameters());
                      J.NewClass newRunnable = (J.NewClass) method.getBody().getStatements().get(0);

                      // insert in inner method
                      J.MethodDeclaration innerMethod = (J.MethodDeclaration) newRunnable.getBody().getStatements().get(0);
                      return m.withTemplate(t, innerMethod.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("m", "n");
              assertThat(type.getParameterTypes().get(0))
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1))
                .matches(jt -> jt instanceof JavaType.Parameterized
                               && ((JavaType.Parameterized) jt).getType().getFullyQualifiedName().equals("java.util.List")
                               && ((JavaType.Parameterized) jt).getTypeParameters().size() == 1
                               && TypeUtils.asFullyQualified(((JavaType.Parameterized) jt).getTypeParameters().get(0)).getFullyQualifiedName().equals("java.lang.String"),
                  "Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'"
                );
          }),
          java(
            """
              class Test {
                  void test() {
                      new Runnable() {
                          void inner() {
                          }
                          @Override
                          public void run() {}
                      };
                  }
              }
              """,
            """
              class Test {
                  void test(int m, java.util.List<String> n) {
                      new Runnable() {
                          void inner(int m, java.util.List<String> n) {
                          }
                          @Override
                          public void run() {}
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMethodParametersVariadicArray() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Object[]... values")
                .build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getSimpleName().equals("test") && method.getParameters().get(0) instanceof J.Empty) {
                      // insert in outer method
                      J.MethodDeclaration m = method.withTemplate(t, method.getCoordinates().replaceParameters());
                      J.NewClass newRunnable = (J.NewClass) method.getBody().getStatements().get(0);

                      // insert in inner method
                      J.MethodDeclaration innerMethod = (J.MethodDeclaration) newRunnable.getBody().getStatements().get(0);
                      return m.withTemplate(t, innerMethod.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              final JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0))
                .getMethodType();

              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("values");

              var param = TypeUtils.asArray(type.getParameterTypes().get(0));
              assertThat(param.getElemType())
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'Object[]'")
                .matches(at -> TypeUtils.asFullyQualified(TypeUtils.asArray(at).getElemType()).getFullyQualifiedName()
                  .equals("java.lang.Object"));
          }),
          java(
            """
              class Test {
                  void test() {
                      new Runnable() {
                          void inner() {
                          }
                      };
                  }
              }
              """,
            """
              class Test {
                  void test(Object[]... values) {
                      new Runnable() {
                          void inner(Object[]... values) {
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAndInterpolateMethodParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int n, #{}").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  {
                      if (method.getSimpleName().equals("test") && method.getParameters().size() == 1) {
                          return method.withTemplate(
                            t,
                            method.getCoordinates().replaceParameters(),
                            method.getParameters().get(0)
                          );
                      }
                      return method;
                  }
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();

              assertThat(type.getParameterNames())
                .as("Changing the method's parameters should have also updated its type's parameter names")
                .containsExactly("n", "s");
              assertThat(type.getParameterTypes().get(0))
                .as("Changing the method's parameters should have resulted in the first parameter's type being 'int'")
                .isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1))
                .as("Changing the method's parameters should have resulted in the second parameter's type being 'List<String>'")
                .matches(jt -> TypeUtils.asFullyQualified(jt).getFullyQualifiedName().equals("java.lang.String"));
          }),
          java(
            """
              class Test {
                  void test(String s) {
                  }
              }
              """,
            """
              class Test {
                  void test(int n, String s) {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceLambdaParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int m, int n").build();

              @Override
              public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext p) {
                  if (lambda.getParameters().getParameters().size() == 1) {
                      return lambda.withTemplate(t, lambda.getParameters().getCoordinates().replace());
                  }
                  return super.visitLambda(lambda, p);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      Object o = () -> 1;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      Object o = (int m, int n) -> 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSingleStatement() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(),
                  """
                    if(n != 1) {
                      n++;
                    }"""
                )
                .build();

              @Override
              public J visitAssert(J.Assert azzert, ExecutionContext p) {
                  return azzert.withTemplate(t, azzert.getCoordinates().replace());
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      if (n != 1) {
                          n++;
                      }
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void replaceStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "n = 2;\nn = 3;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(1);
                  if (statement instanceof J.Unary) {
                      return method.withTemplate(t, statement.getCoordinates().replace());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                      n++;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                      n = 2;
                      n = 3;
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "assert n == 0;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(0);
                  if (statement instanceof J.Assignment) {
                      return method.withTemplate(t, statement.getCoordinates().before());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void afterStatementInBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "n = 1;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return method.withTemplate(t, method.getBody().getStatements().get(0).getCoordinates().after());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    void firstStatementInClassBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int m;").build();

              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getBody().getStatements().size() == 1) {
                      return classDecl.withTemplate(t, classDecl.getBody().getCoordinates().firstStatement());
                  }
                  return classDecl;
              }
          })),
          java(
            """
              class Test {
                  // comment
                  int n;
              }
              """,
            """
              class Test {
                  int m;
                  // comment
                  int n;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1093")
    @Test
    void firstStatementInMethodBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int m = 0;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return method.withTemplate(t, method.getBody().getCoordinates().firstStatement());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      // comment
                      int n = 1;
                  }
              }
              """,
            """
                  class Test {
                      int n;
                      void test() {
                          int m = 0;
                          // comment
                          int n = 1;
                      }
                  }
              """
          )
        );
    }

    @Test
    void lastStatementInClassBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "int n;").build();

              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getBody().getStatements().isEmpty()) {
                      return classDecl.withTemplate(t, classDecl.getBody().getCoordinates().lastStatement());
                  }
                  return classDecl;
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              class Test {
                  int n;
              }
              """
          )
        );
    }

    @Test
    void lastStatementInMethodBlock() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "n = 1;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getBody().getStatements().size() == 1) {
                      return method.withTemplate(t, method.getBody().getCoordinates().lastStatement());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceStatementRequiringNewImport() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "List<String> s = null;")
                .imports("java.util.List")
                .build();

              @Override
              public J visitAssert(J.Assert azzert, ExecutionContext p) {
                  maybeAddImport("java.util.List");
                  return azzert.withTemplate(t, azzert.getCoordinates().replace());
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      assert n == 0;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int n;
                  void test() {
                      List<String> s = null;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessaryBoxing")
    @Test
    void replaceArguments() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "m, Integer.valueOf(n), \"foo\"").build();

              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (method.getArguments().size() == 1) {
                      return method.withTemplate(t, method.getCoordinates().replaceArguments());
                  }
                  return method;
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              J.MethodInvocation m = (J.MethodInvocation) ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(2)).getBody()
                .getStatements().get(0);
              JavaType.Method type = m.getMethodType();
              assertThat(type.getParameterTypes().get(0)).isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(1)).isEqualTo(JavaType.Primitive.Int);
              assertThat(type.getParameterTypes().get(2)).matches(jt ->
                TypeUtils.asFullyQualified(jt).getFullyQualifiedName().equals("java.lang.String"));
          }),
          java(
            """
              abstract class Test {
                  abstract void test();
                  abstract void test(int m, int n, String foo);
                  void fred(int m, int n, String foo) {
                      test();
                  }
              }
              """,
            """
              abstract class Test {
                  abstract void test();
                  abstract void test(int m, int n, String foo);
                  void fred(int m, int n, String foo) {
                      test(m, Integer.valueOf(n), "foo");
                  }
              }
              """
          )
        );
    }

    Recipe replaceAnnotationRecipe = toRecipe(() -> new JavaIsoVisitor<ExecutionContext>() {
        final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@Deprecated").build();

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
            if (annotation.getSimpleName().equals("SuppressWarnings")) {
                return annotation.withTemplate(t, annotation.getCoordinates().replace());
            } else if (annotation.getSimpleName().equals("A1")) {
                return annotation.withTemplate(JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@A2")
                  .build(), annotation.getCoordinates().replace());
            }
            return super.visitAnnotation(annotation, p);
        }
    });

    @Test
    void replaceClassAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              @SuppressWarnings("ALL") class Test {}
              """,
            """
              @Deprecated class Test {}
              """
          )
        );
    }

    @Test
    void replaceMethodDeclarationAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              class A {
                  @SuppressWarnings("ALL")
                  void someTest() {}
              }
              """,
            """
              class A {
                  @Deprecated
                  void someTest() {}
              }
              """
          )
        );
    }

    @Test
    void replaceVariableDeclarationAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  @A1
                  Object someObject;
              }
              """,
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  @A2
                  Object someObject;
              }
              """
          )
        );
    }

    @Test
    void replaceMethodDeclarationVariableDeclarationAnnotation() {
        rewriteRun(
          spec -> spec.recipe(replaceAnnotationRecipe),
          java(
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  void someMethod(@A1 String a){}
              }
              """,
            """
              class A {
                  @interface A1{}
                  @interface A2{}

                  void someMethod(@A2 String a){}
              }
              """
          )
        );
    }

    @Test
    void replaceMethodAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getLeadingAnnotations().isEmpty()) {
                      return method.withTemplate(t, method.getCoordinates().replaceAnnotations());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })),
          java(
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  public @SuppressWarnings(WARNINGS) Test() {
                  }

                  public void test1() {
                  }

                  public @SuppressWarnings(WARNINGS) void test2() {
                  }
              }
              """,
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  @SuppressWarnings("other")
                  public Test() {
                  }

                  @SuppressWarnings("other")
                  public void test1() {
                  }

                  @SuppressWarnings("other")
                  public void test2() {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceClassAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getLeadingAnnotations().isEmpty() && !classDecl.getSimpleName().equals("Test")) {
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceAnnotations());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  class Inner1 {
                  }
              }
              """,
            """
              class Test {
                  static final String WARNINGS = "ALL";

                  @SuppressWarnings("other")
                  class Inner1 {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  if (multiVariable.getLeadingAnnotations().isEmpty()) {
                      return multiVariable.withTemplate(t, multiVariable.getCoordinates().replaceAnnotations());
                  }
                  return super.visitVariableDeclarations(multiVariable, p);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      // the m
                      int m;
                      final @SuppressWarnings("ALL") int n;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      // the m
                      @SuppressWarnings("other")
                      int m;
                      @SuppressWarnings("other")
                      final int n;
                  }
              }
              """
          )
        );
    }

    @Test
    void addVariableAnnotationsToVariableAlreadyAnnotated() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@Deprecated").build();

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  if (multiVariable.getLeadingAnnotations().size() == 1) {
                      return multiVariable.withTemplate(t, multiVariable.getCoordinates().addAnnotation(comparing(a -> 0)));
                  }
                  return super.visitVariableDeclarations(multiVariable, p);
              }
          })),
          java(
            """
              class Test {
                  @SuppressWarnings("ALL") private final int m, a;
                  void test() {
                      @SuppressWarnings("ALL") /* hello */
                      Boolean z;
                      // comment n
                      @SuppressWarnings("ALL")
                      int n;
                      @SuppressWarnings("ALL") final Boolean b;
                      @SuppressWarnings("ALL")
                      // comment x, y
                      private Boolean x, y;
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings("ALL")
                  @Deprecated
                  private final int m, a;
                  void test() {
                      @SuppressWarnings("ALL")
                      @Deprecated /* hello */
                      Boolean z;
                      // comment n
                      @SuppressWarnings("ALL")
                      @Deprecated
                      int n;
                      @SuppressWarnings("ALL")
                      @Deprecated
                      final Boolean b;
                      @SuppressWarnings("ALL")
                      @Deprecated
                      // comment x, y
                      private Boolean x, y;
                  }
              }
              """
          )
        );
    }

    @Test
    void addVariableAnnotationsToVariableNotAnnotated() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"ALL\")").build();

              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  if (multiVariable.getLeadingAnnotations().isEmpty()) {
                      return multiVariable.withTemplate(
                        t,
                        multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName))
                      );
                  }
                  return super.visitVariableDeclarations(multiVariable, p);
              }
          })),
          java(
            """
              class Test {
                  void test() {
                      final int m;
                      int n;
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      @SuppressWarnings("ALL")
                      final int m;
                      @SuppressWarnings("ALL")
                      int n;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1111")
    @Test
    void addMethodAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getLeadingAnnotations().isEmpty()) {
                      return method.withTemplate(t, method.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })),
          java(
            """
              class Test {
                  public void test0() {
                  }

                  static final String WARNINGS = "ALL";

                  void test1() {
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings("other")
                  public void test0() {
                  }

                  static final String WARNINGS = "ALL";

                  @SuppressWarnings("other")
                  void test1() {
                  }
              }
              """
          )
        );
    }

    @Test
    void addClassAnnotations() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@SuppressWarnings(\"other\")").build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getLeadingAnnotations().isEmpty() && !classDecl.getSimpleName().equals("Test")) {
                      return classDecl.withTemplate(
                        t,
                        classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
                  class Inner1 {
                  }
              }
              """,
            """
              class Test {
                  @SuppressWarnings("other")
                  class Inner1 {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAnnotation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "@Deprecated").build();

              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
                  if (annotation.getSimpleName().equals("SuppressWarnings")) {
                      return annotation.withTemplate(t, annotation.getCoordinates().replace());
                  }
                  return annotation;
              }
          })),
          java(
            """
              @SuppressWarnings("ALL")
              class Test {
              }
              """,
            """
              @Deprecated
              class Test {
              }
              """
          )
        );
    }

    @Test
    void replaceClassImplements() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Serializable, Closeable")
                .imports("java.io.*")
                .build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getImplements() == null) {
                      maybeAddImport("java.io.Closeable");
                      maybeAddImport("java.io.Serializable");
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceImplementsClause());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              import java.io.Closeable;
              import java.io.Serializable;

              class Test implements Serializable, Closeable {
              }
              """
          )
        );
    }

    @Test
    void replaceClassExtends() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "List<String>")
                .imports("java.util.*")
                .build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getExtends() == null) {
                      maybeAddImport("java.util.List");
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceExtendsClause());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              import java.util.List;

              class Test extends List<String> {
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void replaceThrows() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "Exception").build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getThrows() == null) {
                      return method.withTemplate(t, method.getCoordinates().replaceThrows());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              J.MethodDeclaration testMethodDecl = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
              assertThat(testMethodDecl.getMethodType().getThrownExceptions().stream().map(JavaType.FullyQualified::getFullyQualifiedName))
                .containsExactly("java.lang.Exception");
          }),
          java(
            """
              class Test {
                  void test() {}
              }
              """,
            """
              class Test {
                  void test() throws Exception {}
              }
              """
          )
        );
    }

    @Disabled
    @Test
    void replaceMethodTypeParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate typeParamsTemplate = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "T, U").build();

              final JavaTemplate methodArgsTemplate = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "List<T> t, U u")
                .imports("java.util.List")
                .build();

              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getTypeParameters() == null) {
                      return method.withTemplate(
                          typeParamsTemplate,
                          method.getCoordinates().replaceTypeParameters()
                        )
                        .withTemplate(methodArgsTemplate, method.getCoordinates().replaceParameters());
                  }
                  return super.visitMethodDeclaration(method, p);
              }
          })).afterRecipe(run -> {
              J.CompilationUnit cu = (J.CompilationUnit) run.getResults().get(0).getAfter();
              JavaType.Method type = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
              assertThat(type).isNotNull();
              var paramTypes = type.getParameterTypes();
              assertThat(paramTypes.get(0))
                .as("The method declaration's type's genericSignature first argument should have have type 'java.util.List'")
                .matches(tType -> tType instanceof JavaType.FullyQualified &&
                                  TypeUtils.asFullyQualified(tType).getFullyQualifiedName().equals("java.util.List"));

              assertThat(paramTypes.get(1))
                .as("The method declaration's type's genericSignature second argument should have type 'U' with bound 'java.lang.Object'")
                .matches(uType ->
                  uType instanceof JavaType.GenericTypeVariable &&
                  TypeUtils.asGeneric(uType).getName().equals("U") &&
                  TypeUtils.asGeneric(uType).getBounds().isEmpty());
          }),
          java(
            """
              import java.util.List;

              class Test {

                  void test() {
                  }
              }
              """,
            """
              import java.util.List;

              class Test {

                  <T, U> void test(List<T> t, U u) {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceClassTypeParameters() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "T, U").build();

              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getTypeParameters() == null) {
                      return classDecl.withTemplate(t, classDecl.getCoordinates().replaceTypeParameters());
                  }
                  return super.visitClassDeclaration(classDecl, p);
              }
          })),
          java(
            """
              class Test {
              }
              """,
            """
              class Test<T, U> {
              }
              """
          )
        );
    }

    @Test
    void replaceBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "n = 1;").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var statement = method.getBody().getStatements().get(0);
                  if (statement instanceof J.Unary) {
                      return method.withTemplate(t, method.getCoordinates().replaceBody());
                  }
                  return method;
              }
          })),
          java(
            """
              class Test {
                  int n;
                  void test() {
                      n++;
                  }
              }
              """,
            """
              class Test {
                  int n;
                  void test() {
                      n = 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMissingBody() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "").build();

              @Override
              public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  var m = method;
                  if (!m.isAbstract()) {
                      return m;
                  }
                  m = m.withReturnTypeExpression(m.getReturnTypeExpression().withPrefix(Space.EMPTY));
                  m = m.withModifiers(emptyList());
                  m = m.withTemplate(t, m.getCoordinates().replaceBody());
                  return m;
              }
          })),
          java(
            """
              abstract class Test {
                  abstract void test();
              }
              """,
            """
              abstract class Test {
                  void test(){
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1198")
    @Test
    @SuppressWarnings({
      "UnnecessaryBoxing",
      "CachedNumberConstructorCall",
      "ResultOfMethodCallIgnored"
    })
    void replaceNamedVariableInitializerMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher matcher = new MethodMatcher("Integer valueOf(..)");
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "new Integer(#{any()})").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (matcher.matches(method)) {
                      return method.withTemplate(t, method.getCoordinates().replace(), method.getArguments().get(0));
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1198")
    @Test
    @SuppressWarnings({
      "CachedNumberConstructorCall",
      "Convert2MethodRef"
    })
    void lambdaIsVariableInitializer() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              final MethodMatcher matcher = new MethodMatcher("Integer valueOf(..)");
              final JavaTemplate t = JavaTemplate.builder(() -> getCursor().getParentOrThrow(), "new Integer(#{any()})").build();

              @Override
              public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
                  if (matcher.matches(method)) {
                      return method.withTemplate(t, method.getCoordinates().replace(), method.getArguments().get(0));
                  }
                  return super.visitMethodInvocation(method, p);
              }
          })),
          java(
            """
              import java.util.function.Function;
              class Test {
                  Function<String, Integer> asInteger = it -> Integer.valueOf(it);
              }
              """,
            """
              import java.util.function.Function;
              class Test {
                  Function<String, Integer> asInteger = it -> new Integer(it);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1505")
    @Test
    void methodDeclarationWithComment() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
              @Override
              public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                  var cd = classDecl;
                  if (cd.getBody().getStatements().isEmpty()) {
                      cd = cd.withBody(
                        cd.getBody().withTemplate(
                          JavaTemplate.builder(() -> getCursor().getParentOrThrow(),
                              //language=groovy
                              """
                                /**
                                 * comment
                                 */
                                void foo() {
                                }
                                """
                            )
                            .build(),
                          cd.getBody().getCoordinates().firstStatement()
                        )
                      );
                  }
                  return cd;
              }
          })),
          java(
            """
              class A {

              }
              """,
            """
              class A {
                  /**
                   * comment
                   */
                  void foo() {
                  }

              }
              """
          )
        );
    }

    @SuppressWarnings("UnusedAssignment")
    @Issue("https://github.com/openrewrite/rewrite/issues/1821")
    @Test
    void assignmentNotPartOfVariableDeclaration() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext p) {
                  var a = assignment;
                  if (a.getAssignment() instanceof J.MethodInvocation) {
                      J.MethodInvocation mi = (J.MethodInvocation) a.getAssignment();
                      a = a.withAssignment(mi.withTemplate(
                        JavaTemplate.builder(this::getCursor, "1")
                          .build(),
                        mi.getCoordinates().replace()
                      ));
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
    void replaceForEachControlVariable() {
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

    @SuppressWarnings("StringOperationCanBeSimplified")
    @Issue("https://github.com/openrewrite/rewrite/issues/2185")
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
}
