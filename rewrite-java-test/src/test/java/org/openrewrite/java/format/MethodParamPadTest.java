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
package org.openrewrite.java.format;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.MethodParamPadStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.TypeValidation;

import java.util.Collection;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings("InfiniteRecursion")
class MethodParamPadTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MethodParamPad());
    }

    static Iterable<NamedStyles> namedStyles(Collection<Style> styles) {
        return singletonList(new NamedStyles(Tree.randomId(), "Test", "test", "test", emptySet(), styles));
    }

    @DocumentExample
    @Test
    void addSpacePadding() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(
            new MethodParamPadStyle(true, false)
          )))),
          java(
            """
              enum E {
                  E1()
              }

              class B {
              }

              class A extends B {
                  A() {
                      super();
                  }

                  static void method(int x, int y) {
                      A a = new A();
                      method(0, 1);
                  }
              }
              """,
            """
              enum E {
                  E1 ()
              }

              class B {
              }

              class A extends B {
                  A () {
                      super ();
                  }

                  static void method (int x, int y) {
                      A a = new A ();
                      method (0, 1);
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void removeSpacePadding() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(
            new MethodParamPadStyle(false, false)
          )))),
          java(
            """
              enum E {
                  E1 ()
              }

              class B {
              }

              class A extends B {
                  A () {
                      super ();
                  }

                  static void method (int x, int y) {
                      A a = new A ();
                      method (0, 1);
                  }
              }
              """,
            """
              enum E {
                  E1()
              }

              class B {
              }

              class A extends B {
                  A() {
                      super();
                  }

                  static void method(int x, int y) {
                      A a = new A();
                      method(0, 1);
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void allowLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(
            new MethodParamPadStyle(true, true)
          )))),
          java(
            """
              enum E {
                  E1
                          ()
              }
                            
              class B {
              }
                            
              class A extends B {
                  A
                          () {
                      super
                              ();
                  }
                            
                  static void method
                          (int x, int y) {
                      A a = new A
                              ();
                      method
                              (0, 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeLineBreaks() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(
            new MethodParamPadStyle(false, false)
          )))),
          java(
            """
              enum E {
                  E1
                          ()
              }
                            
              class B {
              }
                            
              class A extends B {
                  A
                          () {
                      super
                              ();
                  }
                            
                  static void method
                          (int x, int y) {
                      A a = new A
                              ();
                      method
                              (0, 1);
                  }
              }
              """,
            """
              enum E {
                  E1()
              }

              class B {
              }

              class A extends B {
                  A() {
                      super();
                  }

                  static void method(int x, int y) {
                      A a = new A();
                      method(0, 1);
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void removeLineBreaksAndAddSpaces() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().styles(namedStyles(singletonList(
            new MethodParamPadStyle(true, false)
          )))),
          java(
            """
              enum E {
                  E1
                          ()
              }

              class B {
              }

              class A extends B {
                  A
                          () {
                      super
                              ();
                  }

                  static void method
                          (int x, int y) {
                      A a = new A
                              ();
                      method
                              (0, 1);
                  }
              }
              """,
            """
              enum E {
                  E1 ()
              }

              class B {
              }

              class A extends B {
                  A () {
                      super ();
                  }

                  static void method (int x, int y) {
                      A a = new A ();
                      method (0, 1);
                  }
              }
              """,
            autoFormatIsIdempotent()
          )
        );
    }

    @Test
    void initializeStyleWhenOtherwiseNotProvided() {
        rewriteRun(
          java(
            """
              enum E {
                  E1()
              }
              """
          )
        );
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Test
    void bodyNotFormatted() {
        rewriteRun(
          java(
            """
              class A {
                  void test() {
                      for  (int i=0;i<10;i++) {}
                  }
              }
              """
          )
        );
    }

    @Test
    void javadocNotFormatted() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              class A {
                  /**
                   * @see java.sql.Connection#createArrayOf(String, Object[])
                   */
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void recordWithCompactConstructor() {
        rewriteRun(
          version(java(
            """
              import java.util.Objects;

              public record HttpClientTrafficLogData(
                  String request,
                  String response
              )
              {
                  public HttpClientTrafficLogData
                  {
                      Objects.requireNonNull(request, "request must not be null");
                      Objects.requireNonNull(response, "response must not be null");
                  }
              }
              """
          ), 17)
        );
    }

    private static Consumer<SourceSpec<J.CompilationUnit>> autoFormatIsIdempotent() {
        return spec -> spec.afterRecipe(cu ->
          assertThat(new AutoFormatVisitor<>().visit(cu, 0)).isEqualTo(cu));
    }
}
