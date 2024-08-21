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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class ClassDeclarationTest implements RewriteTest {

    /**
     * @see <a href="https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html>Sealed classes</a> documentation.
     */
    @MinimumJava17
    @Issue("https://github.com/openrewrite/rewrite/pull/2569")
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

    @MinimumJava17
    @Issue("https://github.com/openrewrite/rewrite/pull/2569")
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

    @Test
    @Disabled("class A {}~~(non-whitespace)~~>;<~~")
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
}
