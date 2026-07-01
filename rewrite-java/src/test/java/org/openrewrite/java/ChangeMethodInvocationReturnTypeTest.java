/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ChangeMethodInvocationReturnTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeMethodInvocationReturnType("java.lang.Integer parseInt(String)", "long"));
    }

    @DocumentExample
    @Test
    void replaceVariableAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int one = Integer.parseInt("1");
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      long one = Integer.parseInt("1");
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldOnlyChangeTargetMethodAssignments() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int zero = Integer.valueOf("0");
                      int one = Integer.parseInt("1");
                      int two = Integer.valueOf("2");
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      int zero = Integer.valueOf("0");
                      long one = Integer.parseInt("1");
                      int two = Integer.valueOf("2");
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotChangeVariableTypeWhenMatchIsNestedInInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int direct = Integer.parseInt("1");
                      // Integer.parseInt(...) is only an argument here, not the initializer of `s`
                      String s = String.valueOf(Integer.parseInt("2"));
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      long direct = Integer.parseInt("1");
                      // Integer.parseInt(...) is only an argument here, not the initializer of `s`
                      String s = String.valueOf(Integer.parseInt("2"));
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceParenthesizedInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int one = (Integer.parseInt("1"));
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      long one = (Integer.parseInt("1"));
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceTernaryInitializerWithMatchInBothBranches() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar(boolean flag) {
                      int one = flag ? Integer.parseInt("1") : Integer.parseInt("2");
                  }
              }
              """,
            """
              class Foo {
                  void bar(boolean flag) {
                      long one = flag ? Integer.parseInt("1") : Integer.parseInt("2");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceTernaryInitializerWithMatchInOneBranch() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar(boolean flag) {
                      int one = flag ? (Integer.parseInt("1")) : 0;
                  }
              }
              """,
            """
              class Foo {
                  void bar(boolean flag) {
                      long one = flag ? (Integer.parseInt("1")) : 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotChangeVariableTypeWhenInitializerIsTypeCast() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int direct = Integer.parseInt("1");
                      // the cast, not the matched invocation, determines the type of `one`
                      int one = (int) Integer.parseInt("2");
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      long direct = Integer.parseInt("1");
                      // the cast, not the matched invocation, determines the type of `one`
                      int one = (int) Integer.parseInt("2");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableAssignmentFullyQualified() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.math.BigInteger"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  public class Bar {
                      public static Integer bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              class Foo {
                  void foo() {
                      Integer one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;

              import java.math.BigInteger;

              class Foo {
                  void foo() {
                      BigInteger one = Bar.bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableAssignmentWithGenericReturnType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.util.Set<java.lang.String>"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  import java.util.List;
                  public class Bar {
                      public static List<String> bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              import java.util.List;
              class Foo {
                  void foo() {
                      List<String> one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;

              import java.util.Set;

              class Foo {
                  void foo() {
                      Set<java.lang.String> one = Bar.bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableAssignmentWithNestedGenericReturnType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.util.Map<java.lang.String, java.util.List<java.lang.Integer>>"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  import java.util.List;
                  public class Bar {
                      public static List<String> bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              import java.util.List;
              class Foo {
                  void foo() {
                      List<String> one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;
              import java.util.List;
              import java.util.Map;

              class Foo {
                  void foo() {
                      Map<java.lang.String, List<java.lang.Integer>> one = Bar.bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceVariableAssignmentWithDeeplyNestedGenericReturnType() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.util.Map<java.lang.String, java.util.Map<java.lang.Integer, java.lang.Long>>"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  import java.util.Map;
                  public class Bar {
                      public static Map<String, String> bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              import java.util.Map;
              class Foo {
                  void foo() {
                      Map<String, String> one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;
              import java.util.Map;
              class Foo {
                  void foo() {
                      Map<java.lang.String, Map<java.lang.Integer, java.lang.Long>> one = Bar.bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceParameterizedReturnTypeWithRaw() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.lang.Object"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  import java.util.List;
                  public class Bar {
                      public static List<String> bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              import java.util.List;
              class Foo {
                  void foo() {
                      List<String> one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;
              class Foo {
                  void foo() {
                      java.lang.Object one = Bar.bar();
                  }
              }
              """
          )
        );
    }

    @Test
    void newReturnTypeIsResolvedAgainstTheClasspath() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.util.ArrayList<java.lang.String>"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  import java.util.List;
                  public class Bar {
                      public static List<String> bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              import java.util.List;
              class Foo {
                  void foo() {
                      List<String> one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;

              import java.util.ArrayList;

              class Foo {
                  void foo() {
                      ArrayList<java.lang.String> one = Bar.bar();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    if ("one".equals(variable.getSimpleName())) {
                        JavaType.Parameterized type = (JavaType.Parameterized) variable.getType();
                        assertThat(type.getType()).isNotInstanceOf(JavaType.ShallowClass.class);
                    }
                    return super.visitVariable(variable, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void resolvesExternalRawTypeWithExternalTypeArgument() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "jakarta.validation.ConstraintViolation<org.springframework.http.ResponseEntity>"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  public class Bar {
                      public static Object bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              class Foo {
                  void foo() {
                      Object one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;
              import jakarta.validation.ConstraintViolation;
              import org.springframework.http.ResponseEntity;

              class Foo {
                  void foo() {
                      ConstraintViolation<ResponseEntity> one = Bar.bar();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer p) {
                    if ("one".equals(variable.getSimpleName())) {
                        JavaType.Parameterized type = (JavaType.Parameterized) variable.getType();
                        assertThat(type.getType()).isNotInstanceOf(JavaType.ShallowClass.class);
                        assertThat(type.getTypeParameters().get(0)).isNotInstanceOf(JavaType.ShallowClass.class);
                    }
                    return super.visitVariable(variable, p);
                }
            }.visit(cu, 0))
          )
        );
    }

    @Test
    void newReturnTypeMayUseSimpleNamesForJavaLangTypeArguments() {
        rewriteRun(
          spec -> spec.recipe(new ChangeMethodInvocationReturnType("bar.Bar bar()", "java.util.List<String>"))
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                """
                  package bar;
                  public class Bar {
                      public static Object bar() {
                          return null;
                      }
                  }
                  """
              )
            ),
          //language=java
          java(
            """
              import bar.Bar;
              class Foo {
                  void foo() {
                      Object one = Bar.bar();
                  }
              }
              """,
            """
              import bar.Bar;

              import java.util.List;

              class Foo {
                  void foo() {
                      List<String> one = Bar.bar();
                  }
              }
              """
          )
        );
    }
}
