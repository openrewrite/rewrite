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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.java.MinimumJava25;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ClassDeclarationTest implements RewriteTest {

    /**
     * @see <a href="https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html>Sealed classes</a> documentation.
     */
    @Issue("https://github.com/openrewrite/rewrite/pull/2569")
    @MinimumJava17
    @Test
    void sealedClasses() {
        rewriteRun(
          java(
            """
              public sealed class Shape
                  permits Square, Rectangle {
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getClasses().get(0).getPermits()).hasSize(2))
          ),
          java(
            """
              public non-sealed class Square extends Shape {
                 public double side;
              }
              """
          ),
          java(
            """
              public sealed class Rectangle extends Shape {
                  public double length, width;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/pull/2569")
    @MinimumJava17
    @Test
    void sealedInterfaces() {
        rewriteRun(
          java(
            """
              public sealed interface Shape { }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getClasses().get(0).getPermits()).isNull())
          ),
          java(
            """
              public sealed interface HasFourCorners extends Shape
                  permits Square, Rectangle {
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getClasses().get(0).getPermits()).hasSize(2))
          ),
          java(
            """
              public non-sealed class Square extends HasFourCorners {
                 public double side;
              }
              """
          ),
          java(
            """
              public sealed class Rectangle extends HasFourCorners {
                  public double length, width;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/70")
    @Test
    void singleLineCommentBeforeModifier() {
        rewriteRun(
          java(
            """
              @Deprecated
              // Some comment
              public final class A {}
              """
          )
        );
    }

    @Disabled("class A {}~~(non-whitespace)~~>;<~~")
    @Test
    void trailingSemicolon() {
        rewriteRun(
          java(
            """
              class A {};
              """
          )
        );
    }

    @Test
    void multipleClassDeclarationsInOneCompilationUnit() {
        rewriteRun(
          java(
            """
              public class A {}
              class B {}
              """
          )
        );
    }

    @Test
    void implementsInterface() {
        rewriteRun(
          java(
            """
              public interface B {}
              class A implements B {}
              """
          )
        );
    }

    @Test
    void extendsClass() {
        rewriteRun(
          java(
            """
              public interface B {}
              class A extends B {}
              """
          )
        );
    }

    @Test
    void typeArgumentsAndAnnotation() {
        rewriteRun(
          java(
            """
              public class B<T> {}
              @Deprecated public class A < T > extends B < T > {}
              """
          )
        );
    }

    /**
     * OpenJDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    void modifierOrdering() {
        rewriteRun(
          java(
            """
              public /* abstract */ final abstract class A {}
              """
          )
        );
    }

    @Test
    void innerClass() {
        rewriteRun(
          java(
            """
              public class A {
                  public enum B {
                      ONE,
                      TWO
                  }

                  private B b;
              }
              """
          )
        );
    }

    @SuppressWarnings("UnnecessaryModifier")
    @Test
    void strictfpModifier() {
        rewriteRun(
          java(
            """
              public strictfp class A {}
              """
          )
        );
    }

    @SuppressWarnings("UnnecessaryModifier")
    @Test
    void hasModifier() {
        rewriteRun(
          java(
            """
              public strictfp class A {}
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Object o) {
                    assertThat(classDecl.getModifiers()).hasSize(2);
                    assertThat(classDecl.hasModifier(J.Modifier.Type.Public)).isTrue();
                    assertThat(classDecl.hasModifier(J.Modifier.Type.Strictfp)).isTrue();
                    return classDecl;
                }
            }.visit(cu, 0))
          )
        );
    }

    @SuppressWarnings("UnnecessarySemicolon")
    @Test
    void unnecessarySemicolonInBody() {
        rewriteRun(
          java(
            """
              class A {
                  int i = 0;;
                  int j = 0;
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      ";",
      ";;;",
      "; // comment",
      "; // comment;with;semicolons",
      "; /* comment;with;semicolons */",
      "; /* comment\n*/",
      "; // comment1\n// comment2\n;",
      "static String method() { return null; };"
    })
    void unnecessaryLeadingOrEndingSemicolons(String suffix) {
        rewriteRun(
          java(
            """
              class A {
                  /*@@*/
                  int i = 0;
                  /*@@*/
              }
            """.replaceAll("/[*]@@[*]/", suffix)
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithMainMethod() {
        rewriteRun(
          java(
            """
              void main() {
                  System.out.println("Hello from implicit class!");
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithMultipleMethods() {
        rewriteRun(
          java(
            """
              void main() {
                  greet("World");
              }
              
              void greet(String name) {
                  System.out.println("Hello, " + name + "!");
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithFields() {
        rewriteRun(
          java(
            """
              String greeting = "Hello";
              
              void main() {
                  System.out.println(greeting + ", World!");
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithStaticFields() {
        rewriteRun(
          java(
            """
              static int counter = 0;
              
              void main() {
                  counter++;
                  System.out.println("Counter: " + counter);
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithImports() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.ArrayList;
              
              void main() {
                  List<String> names = new ArrayList<>();
                  names.add("Alice");
                  names.add("Bob");
                  System.out.println(names);
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithPackageStatement() {
        rewriteRun(
          java(
            """
              package com.example;
              
              void main() {
                  System.out.println("Hello from package!");
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithStaticInitializer() {
        rewriteRun(
          java(
            """
              static {
                  System.out.println("Static initializer");
              }
              
              void main() {
                  System.out.println("Main method");
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithInstanceInitializer() {
        rewriteRun(
          java(
            """
              {
                  System.out.println("Instance initializer");
              }
              
              void main() {
                  System.out.println("Main method");
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithConstructor() {
        rewriteRun(
          java(
            """
              private String name;
              
              Main(String name) {
                  this.name = name;
              }
              
              void main() {
                  System.out.println("Name: " + name);
              }
              """
          )
        );
    }

    @Issue("https://openjdk.org/jeps/512")
    @MinimumJava25
    @Test
    void implicitClassWithInnerClass() {
        rewriteRun(
          java(
            """
              void main() {
                  Helper helper = new Helper();
                  helper.help();
              }
              
              class Helper {
                  void help() {
                      System.out.println("Helping!");
                  }
              }
              """
          )
        );
    }
}
