/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessarySemicolon"})
class AnnotationTest implements RewriteTest {

    @Test
    void simple() {
        rewriteRun(
          groovy(
            """
              @Foo
              class Test implements Runnable {
                  @java.lang.Override
                  void run() {}
              }
              """
          )
        );
    }

    @Test
    void memoizedMethod() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Memoized

              class Foo {
                  @Memoized
                  Object bar() {
                      return null
                  }
              }
              """
          )
        );
    }

    @Test
    void simpleFQN() {
        rewriteRun(
          groovy(
            """
              @org.springframework.stereotype.Service
              class Test {}
              """
          )
        );
    }

    @Test
    void withParentheses() {
        rewriteRun(
          groovy(
            """
              @Foo()
              class Test {}
              """
          )
        );
    }

    @Test
    void inline() {
        rewriteRun(
          groovy(
            """
              @Foo class Test implements Runnable {
                  @Override void run() {}
              }
              """
          )
        );
    }

    @Test
    void withProperties() {
        rewriteRun(
          groovy(
            """
              @Foo(value = "A", version = "1.0")
              class Test {}
              """
          )
        );
    }

    @Test
    void withConstantProperty() {
        rewriteRun(
          groovy(
            """
              @Foo(value = Test.VERSION)
              class Test {
                  static final String VERSION = "1.23"
              }
              """
          )
        );
    }

    @Test
    void withStaticallyImportedConstantProperty() {
        rewriteRun(
          groovy(
            """
              import static java.io.File.separator
              @Deprecated(since = separator)
              class Test {
              }
              """
          )
        );
    }

    @Test
    void withImplicitValueProperty() {
        rewriteRun(
          groovy(
            """
              @Foo( "A" )
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4055")
    @Test
    void nested() {
        rewriteRun(
          groovy(
            """
              @Foo(bar = @Bar(@Baz(baz = @Qux("1.0"))))
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/286")
    @Test
    void nestedAnnotationsInListLiteral() {
        rewriteRun(
          groovy(
            """
              @Tags(categories = [@Tag("tag1"), @Tag("tag2")])
              class Main {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-logging-frameworks/issues/291")
    @Test
    void nestedAnnotationsInListLiteralWithConstantReferenceUnderCompileStatic() {
        rewriteRun(
          groovy(
            """
              import java.lang.annotation.*
              import groovy.transform.CompileStatic

              interface TestConstants {
                  public static final String PROVIDER = "Provider1"
                  public static final String CATEGORY = "Category1"
              }

              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.TYPE)
              @interface Tag {
                  String id()
                  String category()
                  String provider()
              }

              @Retention(RetentionPolicy.RUNTIME)
              @Target(ElementType.TYPE)
              @interface Tags { Tag[] value() }

              @CompileStatic
              @Tags(value = [@Tag(id="tag1", category= TestConstants.CATEGORY, provider = "prov1"),
                             @Tag(id="tag2", category="cat2", provider = "prov2")])
              class Main {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4254")
    @Test
    void groovyTransformAnnotation() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.EqualsAndHashCode
              import groovy.transform.ToString

              @Foo
              @ToString
              @EqualsAndHashCode
              @Bar
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4254")
    @Test
    void groovyTransformImmutableAnnotation() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Immutable
              import groovy.transform.TupleConstructor

              @Foo
              @TupleConstructor
              @Immutable
              @Bar
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4254")
    @Test
    void groovyTransformImmutableFQNAnnotation() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.Immutable
              class Test {}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6302")
    @Test
    void groovyCanonicalAnnotation() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Canonical

              @Canonical
              class Person {
                  String name
                  int age
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4853")
    @Test
    void annotationOnVariable() {
        rewriteRun(
          groovy(
            """
              @Foo def a = "a"
              """
          )
        );
    }

    @Test
    void groovyTransformFieldAnnotationOnVariable() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field

              @Field def a = [1, 2, 3]
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8979")
    @Test
    void groovyTransformFieldAnnotationInsideBlock() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field
              if (true) {
                  @Field def list = []
              }
              try {
                  if (false) {
                      @Field Map<String, Integer> other = [a: 1]
                  }
              } finally {
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<J.VariableDeclarations> fields = new ArrayList<>();
                new GroovyIsoVisitor<Integer>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                        fields.add(multiVariable);
                        return multiVariable;
                    }
                }.visit(cu, 0);
                assertThat(fields).hasSize(2);
                assertThat(fields.get(0).getVariables().getFirst().getInitializer()).isInstanceOf(G.ListLiteral.class);
                assertThat(fields.get(1).getVariables().getFirst().getInitializer()).isInstanceOf(G.MapLiteral.class);
            })
          )
        );
    }

    @Test
    void groovyTransformFieldAnnotationAmongOtherAnnotations() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field
              @Deprecated @Field def x = 1
              @Field @Deprecated def y = 2
              if (true) {
                  @Deprecated
                  @groovy.transform.Field
                  @SuppressWarnings("unused") String z = "z"
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<List<String>> annotations = new ArrayList<>();
                new GroovyIsoVisitor<Integer>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                        annotations.add(multiVariable.getLeadingAnnotations().stream().map(J.Annotation::getSimpleName).toList());
                        return multiVariable;
                    }
                }.visit(cu, 0);
                assertThat(annotations).containsExactly(
                  List.of("Deprecated", "Field"),
                  List.of("Field", "Deprecated"),
                  List.of("Deprecated", "Field", "SuppressWarnings")
                );
            })
          )
        );
    }

    @Test
    void annotationsAfterModifiers() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field
              final @Field x = 1
              final @Deprecated y = 2
              final   @Deprecated   String z = "z"
              @Deprecated
              public @SuppressWarnings("unused") final class A {
                  private @Deprecated String s
                  private @Deprecated t
                  public @Deprecated A() {}
                  public @Deprecated void m(final @Deprecated String p, final @Deprecated q) {}
                  public @Deprecated <T> T n() { null }
                  def @Deprecated o() {}
              }
              public @Deprecated class B {}
              """,
            spec -> spec.beforeRecipe(cu -> {
                List<String> annotations = new ArrayList<>();
                new GroovyIsoVisitor<Integer>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Integer p) {
                        annotations.add(classDecl.getSimpleName() + " " + names(classDecl.getAllAnnotations()));
                        return super.visitClassDeclaration(classDecl, p);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer p) {
                        annotations.add(method.getSimpleName() + " " + names(method.getAllAnnotations()));
                        return super.visitMethodDeclaration(method, p);
                    }

                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Integer p) {
                        annotations.add(multiVariable.getVariables().getFirst().getSimpleName() + " " + names(multiVariable.getAllAnnotations()));
                        return super.visitVariableDeclarations(multiVariable, p);
                    }

                    private List<String> names(List<J.Annotation> annotations) {
                        return annotations.stream().map(J.Annotation::getSimpleName).toList();
                    }
                }.visit(cu, 0);
                assertThat(annotations).containsExactly(
                  "x [Field]", "y [Deprecated]", "z [Deprecated]",
                  "A [Deprecated, SuppressWarnings]",
                  "s [Deprecated]", "t [Deprecated]",
                  "A [Deprecated]",
                  "m [Deprecated]", "p [Deprecated]", "q [Deprecated]",
                  "n [Deprecated]", "o [Deprecated]",
                  "B [Deprecated]"
                );
            })
          )
        );
    }

    @Test
    void annotationsAfterModifiersInOtherDeclarations() {
        rewriteRun(
          groovy(
            """
              final @Deprecated a = 1, b = 2
              final @Deprecated def (c, d) = [1, 2]
              void m(final @Deprecated String... args) {}
              public @Deprecated trait T {}
              class C {
                  private /* c */ @Deprecated /* d */ final /* e */ @SuppressWarnings('x') /* f */ String s
                  public @groovy.transform.Memoized def m() { 1 }
              }
              """
          )
        );
    }

    @Test
    void groovyTransformFieldAnnotationFollowedBySemicolon() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Field
              // a comment
              @Field def x = 1;
              @Field def y = 2 ; println y
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/8978")
    @Test
    void baseScriptDeclaration() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.BaseScript groovy.lang.Script base
              println 1
              """,
            spec -> spec.beforeRecipe(cu -> assertThat(
              ((J.VariableDeclarations) cu.getStatements().getFirst()).getVariables().getFirst().getInitializer()).isNull())
          ),
          groovy(
            """
              import groovy.transform.BaseScript

              @BaseScript com.example.ScriptLoader baseScript
              println 1
              """
          )
        );
    }

    @Test
    void groovyTransformFieldFQNAnnotationOnVariable() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.Field def a = [1, 2, 3]
              """
          )
        );
    }

    @Test
    void groovyTransformFieldFQNAnnotationOnVariableWithReference() {
        rewriteRun(
          groovy(
            """
              def z = 1 + 2
              @groovy.transform.Field def a = z
              """
          )
        );
    }

    @Test
    void groovyTransformFieldFQNAnnotationOnVariableWithMethodInvocation() {
        rewriteRun(
          groovy(
            """
              @groovy.transform.Field def a = callSomething()
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6319")
    @Test
    void synchronizedAnnotation() {
        rewriteRun(
          groovy(
            """
            package org.dummy

            import groovy.transform.Synchronized

            class Foo {

                @Synchronized
                void bar() {
                    println('Hello World')
                }
            }
            """
          )
        );
    }

    @Test
    void immutableAndToString() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.Immutable
              import groovy.transform.ToString

              @Immutable @ToString
              class A {
                  void foo() {
                  }
              }
              """
          )
        );
    }

    @Test
    void notYetImplementedAnnotation() {
        rewriteRun(
          groovy(
            """
              import groovy.test.NotYetImplemented

              class Foo {
                  @NotYetImplemented
                  void m() {
                      println("hello")
                  }
              }
              """
          )
        );
    }
}
