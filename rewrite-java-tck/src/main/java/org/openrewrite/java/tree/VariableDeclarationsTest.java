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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MinimumJava11;
import org.openrewrite.java.MinimumJava17;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class VariableDeclarationsTest implements RewriteTest {

    @Test
    void generic() {
        rewriteRun(
          java(
            """
              import java.util.Collections;
              import java.util.ArrayList;
                            
              class Test {
                  void test() {
                      ArrayList<String> categories = new ArrayList<>();
                      Collections.sort(categories);
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldDefinition() {
        rewriteRun(
          java(
            """
              class Test {
                  public String a = "";
              }
              """
          )
        );
    }

    @Test
    @MinimumJava11
    void finalVar() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      final var a = "";
                  }
              }
              """
          )
        );
    }

    @Test
    void localVariableDefinition() {
        rewriteRun(
          java(
            """
              class Test {
                  String a = "";
              }
              """
          )
        );
    }

    @Test
    void fieldWithNoInitializer() {
        rewriteRun(
          java(
            """
              class Test {
                  public String a;
              }
              """
          )
        );
    }

    @SuppressWarnings("CStyleArrayDeclaration")
    @Test
    void arrayVariables() {
        rewriteRun(
          java(
            """
               class Test {
                  int n [ ];
                  String s [ ] [ ];
                  int [ ] n2;
                  String [ ] [ ] s2;
              }
               """
          )
        );
    }

    @Test
    void multipleDeclarationOneAssignment() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      int i , j = 0;
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("CStyleArrayDeclaration")
    @Test
    void multipleDeclaration() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      Integer[] m = { 0 }, n[] = { { 0 } };
                  }
              }
              """
          )
        );
    }

    /**
     * The JDK does NOT preserve the order of modifiers in its AST representation
     */
    @Test
    void modifierOrdering() {
        rewriteRun(
          java(
            """
              class Test {
                  public /* static */ final static Integer n = 0;
              }
              """
          )
        );
    }

    @Test
    void primitiveClassType() {
        rewriteRun(
          java(
            """
              class Test {
                  public String fred;
                  public Class<?> a = boolean.class;
              }
              """
          )
        );
    }

    @Test
    void voidClassType() {
        rewriteRun(
          java(
            """
              @interface Test {
                  Class<?> interfaceClass() default void.class;
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void implicitlyDeclaredLocalVariable() {
        rewriteRun(
          java(
            """
              class Test {
                  void test() {
                      var a = "";
                      var/* comment */b = "";
                      /*comment*/var c = "";
                      var     d = "";
                      long /* yep */ i /* comments */, /*everywhere*/ j;
                  }
              }
              """
          )
        );
    }

    @MinimumJava17
    @Test
    void string() {
        rewriteRun(
          java(
            """
              public class Test {
                  static {
                      var a = "";
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    assertThat(requireNonNull(multiVariable.getTypeAsFullyQualified()).getFullyQualifiedName())
                      .isEqualTo("java.lang.String");
                    return multiVariable;
                }
            })
          )
        );
    }

    @MinimumJava17
    @Test
    void typeOnVarKeyword() {
        rewriteRun(
          java(
            """
              import java.util.Date;
              public class Test {
                  static {
                      var a = new Date();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    assertThat(multiVariable.getMarkers().findFirst(JavaVarKeyword.class)).isPresent();
                    TypeTree typeExpression = multiVariable.getTypeExpression();
                    assertThat(typeExpression).isNotNull();
                    assertThat(requireNonNull(TypeUtils.asFullyQualified(typeExpression.getType()))
                      .getFullyQualifiedName()).isEqualTo("java.util.Date");
                    return multiVariable;
                }
            })
          )
        );
    }

    @Test
    @MinimumJava11
    void unknownVar() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          java(
            """
              class Test {
                  void test(Unknown b) {
                      final var a = b;
                  }
              }
              """
          )
        );
    }
}

