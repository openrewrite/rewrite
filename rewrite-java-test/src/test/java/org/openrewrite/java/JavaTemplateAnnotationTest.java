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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

/**
 * These JavaTemplate tests are specific to the annotation matching syntax.
 */
class JavaTemplateAnnotationTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceClassAnnotation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                  @Override
                  public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                      if (annotation.toString().contains("2.0")) {
                          return annotation;
                      }
                      return JavaTemplate.apply("@Deprecated(since = \"#{}\", forRemoval = true)",
                        getCursor(), annotation.getCoordinates().replace(), "2.0");
                  }
              }
            )),
          java(
            """
              @Deprecated(since = "1.0", forRemoval = true)
              class A {
              }
              """,
            """
              @Deprecated(since = "2.0", forRemoval = true)
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceNestedClassAnnotation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaVisitor<>() {
                  @Override
                  public J visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                      if (annotation.toString().contains("2.0")) {
                          return annotation;
                      }
                      return JavaTemplate.apply("@Deprecated(since = \"#{}\", forRemoval = true)",
                        getCursor(), annotation.getCoordinates().replace(), "2.0");
                  }
              }
            )),
          java(
            """
              class A {
                  @Deprecated(since = "1.0", forRemoval = true)
                  class B {
                  }
              }
              """,
            """
              class A {
                  @Deprecated(since = "2.0", forRemoval = true)
                  class B {
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceAnnotationArgumentsOnMethodWithNestedClassSibling() {
        // javac recovers from unbalanced braces in the parse stub, so additionally assert the
        // stub is well-formed; stricter parsers (Kotlin, Groovy, Scala) reject unbalanced stubs
        List<String> stubs = new ArrayList<>();
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("SuppressWarnings".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null &&
                      annotation.getArguments().stream().noneMatch(a -> a.toString().contains("all"))) {
                      return JavaTemplate.builder("\"all\"")
                        .doBeforeParseTemplate(stubs::add)
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                  }
                  return annotation;
              }
          })),
          java(
            """
              class A {
                  static class Sibling {
                  }

                  @SuppressWarnings("unchecked")
                  void method() {}
              }
              """,
            """
              class A {
                  static class Sibling {
                  }

                  @SuppressWarnings("all")
                  void method() {}
              }
              """
          )
        );
        assertThat(stubs)
          .isNotEmpty()
          .allSatisfy(stub -> assertThat(stub.chars().filter(c -> c == '{').count())
            .as("balanced braces in template stub:%n%s", stub)
            .isEqualTo(stub.chars().filter(c -> c == '}').count()));
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/2824")
    @Test
    void replaceAnnotationArgumentsWithAssignmentShapedSubstitution() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                  if ("Retry".equals(annotation.getSimpleName()) &&
                      annotation.getArguments() != null && !annotation.getArguments().isEmpty() &&
                      annotation.getArguments().getFirst() instanceof J.Assignment assignment &&
                      assignment.getVariable() instanceof J.Identifier attribute &&
                      "include".equals(attribute.getSimpleName())) {
                      return JavaTemplate.builder("includes = #{any()}")
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments(),
                          assignment.getAssignment());
                  }
                  return annotation;
              }
          })),
          java(
            """
              @interface Retry {
                  Class<? extends Throwable>[] include() default {};
                  Class<? extends Throwable>[] includes() default {};
              }
              """
          ),
          java(
            """
              class MyService {
                  @Retry(include = {IllegalStateException.class})
                  void doWork() {}
              }
              """,
            """
              class MyService {
                  @Retry(includes = {IllegalStateException.class})
                  void doWork() {}
              }
              """
          ));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4634")
    @Test
    void replacesInRecordVisitor() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
                  if ("NotNull".equals(annotation.getSimpleName())) {
                      maybeRemoveImport("org.jetbrains.annotations.NotNull");
                      maybeAddImport("lombok.NonNull");
                      return JavaTemplate.builder("@NonNull")
                        .imports("lombok.NonNull")
                        .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replace());
                  }
                  return annotation;
              }
          })),
          java(
            """
              import org.jetbrains.annotations.NotNull;

              public record Person(
                  @NotNull String firstName,
                  @NotNull String lastName
              ) {}
              """,
            """
              import lombok.NonNull;

              public record Person(
                  @NonNull String firstName,
                  @NonNull String lastName
              ) {}
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

    Recipe replaceAnnotationRecipe = toRecipe(() -> new JavaIsoVisitor<>() {
        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            if ("SuppressWarnings".equals(annotation.getSimpleName())) {
                return JavaTemplate.apply("@Deprecated", getCursor(), annotation.getCoordinates().replace());
            } else if ("A1".equals(annotation.getSimpleName())) {
                return JavaTemplate.apply("@A2", getCursor(), annotation.getCoordinates().replace());
            }
            return super.visitAnnotation(annotation, ctx);
        }
    });

    @Test
    void replaceClassDeclarationAnnotation() {
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
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  if (method.getLeadingAnnotations().isEmpty()) {
                      return JavaTemplate.apply("@SuppressWarnings(\"other\")",
                        getCursor(), method.getCoordinates().replaceAnnotations());
                  }
                  return super.visitMethodDeclaration(method, ctx);
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
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                  if (classDecl.getLeadingAnnotations().isEmpty() && !"Test".equals(classDecl.getSimpleName())) {
                      return JavaTemplate.apply("@SuppressWarnings(\"other\")", getCursor(), classDecl.getCoordinates().replaceAnnotations());
                  }
                  return super.visitClassDeclaration(classDecl, ctx);
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
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getLeadingAnnotations().isEmpty()) {
                      return JavaTemplate.apply("@SuppressWarnings(\"other\")", getCursor(), multiVariable.getCoordinates().replaceAnnotations());
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
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
                      @SuppressWarnings("other") int m;
                      @SuppressWarnings("other") final int n;
                  }
              }
              """
          )
        );
    }

    @Test
    void addFieldAnnotationKeepsLocalIndentAndJavadoc() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations field, ExecutionContext ctx) {
                  if (field.getVariables().getFirst().getSimpleName().equals("value") && field.getLeadingAnnotations().isEmpty()) {
                      return JavaTemplate.apply("@Deprecated", getCursor(),
                        field.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                  }
                  return field;
              }
          })),
          java(
            """
              class T {
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
              class T {
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
    void addVariableAnnotationsToVariableAlreadyAnnotated() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                  if (multiVariable.getLeadingAnnotations().size() == 1) {
                      return JavaTemplate.apply("@Deprecated", getCursor(), multiVariable.getCoordinates().addAnnotation(comparing(a -> 0)));
                  }
                  return super.visitVariableDeclarations(multiVariable, ctx);
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
                      @SuppressWarnings("ALL") @Deprecated /* hello */
                      Boolean z;
                      // comment n
                      @SuppressWarnings("ALL") @Deprecated
                      int n;
                      @SuppressWarnings("ALL") @Deprecated final Boolean b;
                      @SuppressWarnings("ALL") @Deprecated
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
              @Override
              public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext p) {
                  if (multiVariable.getLeadingAnnotations().isEmpty()) {
                      return JavaTemplate.apply("@SuppressWarnings(\"ALL\")",
                        getCursor(), multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
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
                      @SuppressWarnings("ALL") final int m;
                      @SuppressWarnings("ALL") int n;
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
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext p) {
                  if (method.getLeadingAnnotations().isEmpty()) {
                      return JavaTemplate.apply("@SuppressWarnings(\"other\")",
                        getCursor(), method.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
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
              @Override
              public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext p) {
                  if (classDecl.getLeadingAnnotations().isEmpty() && !"Test".equals(classDecl.getSimpleName())) {
                      return JavaTemplate.apply("@SuppressWarnings(\"other\")",
                        getCursor(), classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
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
              @Override
              public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext p) {
                  if ("SuppressWarnings".equals(annotation.getSimpleName())) {
                      return JavaTemplate.apply("@Deprecated", getCursor(), annotation.getCoordinates().replace());
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
    void annotateMethodInAnonymousClassAsFieldInitializerArgument() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                  if (m.getSimpleName().equals("compare") && m.getLeadingAnnotations().stream().noneMatch(
                    a -> a.getSimpleName().equals("Deprecated"))) {
                      return JavaTemplate.builder("@Deprecated")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), m.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                  }
                  return m;
              }
          })),
          java(
            """
              import java.util.Comparator;
              import java.util.TreeSet;
              class Test {
                  TreeSet<String> set = new TreeSet<>(new Comparator<String>() {
                      public int compare(String a, String b) {
                          return a.compareTo(b);
                      }
                  });
              }
              """,
            """
              import java.util.Comparator;
              import java.util.TreeSet;
              class Test {
                  TreeSet<String> set = new TreeSet<>(new Comparator<String>() {
                      @Deprecated
                      public int compare(String a, String b) {
                          return a.compareTo(b);
                      }
                  });
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6520")
    @Test
    void annotateMethodInAnonymousClassWithMultipleArgs() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                  J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                  if (m.getSimpleName().equals("toString") && m.getLeadingAnnotations().stream().noneMatch(
                    a -> a.getSimpleName().equals("SuppressWarnings"))) {
                      return JavaTemplate.builder("@SuppressWarnings(\"all\")")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), m.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                  }
                  return m;
              }
          })),
          java(
            """
              class A {
                  A(Object o, String name) {}
              }
              class B {
                  void test() {
                      new A(new Object() {
                          @Override
                          public String toString() { return null; }
                      }, "Elvar Fridriksson");
                  }
              }
              """,
            """
              class A {
                  A(Object o, String name) {}
              }
              class B {
                  void test() {
                      new A(new Object() {
                          @Override
                          @SuppressWarnings("all")
                          public String toString() { return null; }
                      }, "Elvar Fridriksson");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5712")
    @Nested
    class NestedAnnotationsTests {
        @Language("java")
        private final String annotations = """
          package foo;

          import java.lang.annotation.*;

          @Repeatable(NestedAnnotations.class)
          public @interface NestedAnnotation {
              String a() default "";
              String b() default "";
          }

          public @interface NestedAnnotations {
              NestedAnnotation[] value();
          }
          """;

        private final Recipe recipe = toRecipe(() -> new JavaIsoVisitor<>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if ("NestedAnnotation".equals(annotation.getSimpleName()) &&
                  !annotation.getArguments().isEmpty()) {
                    // Check if this annotation still has the 'a' attribute that needs to be replaced
                    var arg = (J.Assignment) annotation.getArguments().get(0);
                    if (arg.getVariable() instanceof J.Identifier &&
                      "a".equals(((J.Identifier) arg.getVariable()).getSimpleName())) {
                        // Only apply the template if we haven't already transformed this annotation
                        var value = (J.Literal) arg.getAssignment();

                        // Replace 'a' with 'b' in the annotation
                        return JavaTemplate.builder("@NestedAnnotation(b = \"#{}\")")
                          .javaParser(JavaParser.fromJavaVersion().dependsOn(annotations))
                          .imports("foo.*")
                          .build()
                          .apply(getCursor(), annotation.getCoordinates().replace(), value.getValue());
                    }
                }
                return super.visitAnnotation(annotation, ctx);
            }
        });

        @Test
        void replaceWhenFieldAnnotatedNoIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations({
                      @NestedAnnotation(a = "first"),
                      @NestedAnnotation(a = "second")
                    })
                    String field;
                    void method() {}
                  }
                  """,
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations({
                      @NestedAnnotation(b = "first"),
                      @NestedAnnotation(b = "second")
                    })
                    String field;
                    void method() {}
                  }
                  """
              )
            );
        }

        @Test
        void replaceWhenFieldAnnotatedWithIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations(value = {
                      @NestedAnnotation(a = "first"),
                      @NestedAnnotation(a = "second")
                    })
                    String field;
                    void method() {}
                  }
                  """,
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations(value = {
                      @NestedAnnotation(b = "first"),
                      @NestedAnnotation(b = "second")
                    })
                    String field;
                    void method() {}
                  }
                  """
              )
            );
        }

        @Test
        void replaceWhenMethodAnnotatedNoIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations({
                      @NestedAnnotation(a = "first"),
                      @NestedAnnotation(a = "second")
                    })
                    void method() {}
                  }
                  """,
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations({
                      @NestedAnnotation(b = "first"),
                      @NestedAnnotation(b = "second")
                    })
                    void method() {}
                  }
                  """
              )
            );
        }

        @Test
        void replaceWhenMethodAnnotatedWithIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations(value = {
                      @NestedAnnotation(a = "first"),
                      @NestedAnnotation(a = "second")
                    })
                    void method() {}
                  }
                  """,
                """
                  import foo.*;

                  class A {
                    @NestedAnnotations(value = {
                      @NestedAnnotation(b = "first"),
                      @NestedAnnotation(b = "second")
                    })
                    void method() {}
                  }
                  """
              )
            );
        }

        @Test
        void replaceWhenOuterClassAnnotatedNoIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  @NestedAnnotations({
                    @NestedAnnotation(a = "first"),
                    @NestedAnnotation(a = "second")
                  })
                  class A {
                    void method() {}
                  }
                  """,
                """
                  import foo.*;

                  @NestedAnnotations({
                    @NestedAnnotation(b = "first"),
                    @NestedAnnotation(b = "second")
                  })
                  class A {
                    void method() {}
                  }
                  """
              )
            );
        }

        @Test
        void replaceWhenOuterClassAnnotatedWithIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  @NestedAnnotations(value = {
                    @NestedAnnotation(a = "first"),
                    @NestedAnnotation(a = "second")
                  })
                  class A {
                    void method() {}
                  }
                  """,
                """
                  import foo.*;

                  @NestedAnnotations(value = {
                    @NestedAnnotation(b = "first"),
                    @NestedAnnotation(b = "second")
                  })
                  class A {
                    void method() {}
                  }
                  """
              )
            );
        }

        @Test
        void replaceWhenInnerClassAnnotatedNoIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  class A {
                      @NestedAnnotations({
                        @NestedAnnotation(a = "first"),
                        @NestedAnnotation(a = "second")
                      })
                      class B {
                          void method() {}
                      }
                  }
                  """,
                """
                  import foo.*;

                  class A {
                      @NestedAnnotations({
                        @NestedAnnotation(b = "first"),
                        @NestedAnnotation(b = "second")
                      })
                      class B {
                          void method() {}
                      }
                  }
                  """
              )
            );
        }

        @Test
        void replaceWhenInnerClassAnnotatedWithIdentifier() {
            rewriteRun(
              spec -> spec
                .parser(JavaParser.fromJavaVersion().dependsOn(annotations))
                .recipe(recipe),
              //language=java
              java(
                """
                  import foo.*;

                  class A {
                      @NestedAnnotations(value = {
                        @NestedAnnotation(a = "first"),
                        @NestedAnnotation(a = "second")
                      })
                      class B {
                          void method() {}
                      }
                  }
                  """,
                """
                  import foo.*;

                  class A {
                      @NestedAnnotations(value = {
                        @NestedAnnotation(b = "first"),
                        @NestedAnnotation(b = "second")
                      })
                      class B {
                          void method() {}
                      }
                  }
                  """
              )
            );
        }
    }
}
