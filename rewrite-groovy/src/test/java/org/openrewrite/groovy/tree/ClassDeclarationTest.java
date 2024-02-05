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
import org.openrewrite.test.RewriteTest;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings("GrUnnecessaryPublicModifier")
class ClassDeclarationTest implements RewriteTest {

    @Test
    void multipleClassDeclarationsInOneCompilationUnit() {
        rewriteRun(
          groovy(
            """
              public class A {
                  int n
                  
                  def sum(int m) {
                      n+m
                  }
              }
              class B {}
              """
          )
        );
    }

    @Test
    void classImplements() {
        rewriteRun(
          groovy(
            """
              public interface B {}
              interface C {}
              class A implements B, C {}
              """
          )
        );
    }

    @Test
    void classExtends() {
        rewriteRun(
          groovy(
            """
              public class Test {}
              class A extends Test {}
              """
          )
        );
    }

    @Test
    void modifierOrdering() {
        rewriteRun(
          groovy(
            """
              public abstract class A {}
              """
          )
        );
    }

    @Test
    void interfaceExtendsInterface() {
        rewriteRun(
          groovy(
            """
              interface A {}
              interface C {}
              interface B extends A , C {}
              """
          )
        );
    }

    @Test
    void transitiveInterfaces() {
        rewriteRun(
          groovy(
            """
              interface A {}
              interface B extends A {}
              interface C extends B {}
              """
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          groovy(
            """
              @interface A{}
              """
          )
        );
    }

    @SuppressWarnings("GrPackage")
    @Test
    void hasPackage() {
        rewriteRun(
          groovy(
            """ 
              package org.openrewrite
               
              public class A{}
               """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1736")
    @Test
    void parameterizedFieldDoesNotAffectClassType() {
        rewriteRun(
          groovy(
            """
              class A {
                  List<String> a
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var aType = cu.getClasses().get(0).getType();
                assertThat(aType).isNotNull();
                assertThat(aType).isInstanceOf(JavaType.Class.class);
                assertThat(((JavaType.Class) aType).getFullyQualifiedName()).isEqualTo("A");
            })
          )
        );
    }

    @Test
    void implicitlyPublic() {
        rewriteRun(
          groovy("class A{}")
        );
    }

    @Test
    void packagePrivate() {
        rewriteRun(
          groovy(
            """
              import groovy.transform.PackageScope
                            
              @PackageScope
              class A {}
              """,
            spec ->
              spec.beforeRecipe(cu -> {
                  var clazz = cu.getClasses().get(0);
                  assertThat(clazz.getModifiers())
                    .as("Groovy's default visibility is public, applying @PackageScope should prevent the public modifier from being present")
                    .hasSize(0);
                  var annotations = cu.getClasses().get(0).getAllAnnotations();
                  assertThat(annotations).hasSize(1);
              })
          )
        );
    }

    @Test
    void typeParameters() {
        rewriteRun(
          groovy(
            """
              class A <T, S extends PT<S> & C> {
                  T t
                  S s
              }
              interface PT<T> {}
              interface C {}
              """,
            spec -> spec.beforeRecipe(cu -> {
                var typeParameters = cu.getClasses().get(0).getTypeParameters();
                assertThat(typeParameters).isNotNull();
                assertThat(typeParameters).hasSize(2);
                assertThat(requireNonNull(typeParameters.get(0)).getBounds()).isNull();
                var sParam = typeParameters.get(1);
                assertThat(sParam.getBounds()).isNotNull();
                assertThat(sParam.getBounds()).hasSize(2);
                assertThat(sParam.getBounds().get(0)).isInstanceOf(J.ParameterizedType.class);
            })
          )
        );
    }

    @Test
    void innerClass() {
        rewriteRun(
          groovy(
            """
              interface C {
                  class Inner {
                  }
              }
              """
          )
        );
    }

    @Test
    void newParameterizedConstructor() {
        rewriteRun(
          groovy(
            """
              class Outer {
                  PT<TypeA> parameterizedField = new PT<TypeA>() {
                  }
              }
              interface TypeA {}
              interface PT<T> {
              }
              """
          )
        );
    }

    @Test
    void parameterizedField() {
        rewriteRun(
          groovy(
            """
              class A {
                  List<String> a
                  Map<Object, Object> b
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                var statements = cu.getClasses().get(0).getBody().getStatements();
                assertThat(statements).hasSize(2);
                var a = ((J.VariableDeclarations) statements.get(0)).getVariables().get(0);
                assertThat(requireNonNull(TypeUtils.asParameterized(a.getType())).toString())
                  .isEqualTo("java.util.List<java.lang.String>");
                var b = ((J.VariableDeclarations) statements.get(1)).getVariables().get(0);
                assertThat(requireNonNull(TypeUtils.asParameterized(b.getType())).toString())
                  .isEqualTo("java.util.Map<java.lang.Object, java.lang.Object>");
            })
          )
        );
    }

    @Test
    void singleLineCommentBeforeModifier() {
        rewriteRun(
          groovy(
            """
              @Deprecated
              // Some comment
              public final class A {}
              """,
            spec -> spec.beforeRecipe(cu -> {
                var annotations = cu.getClasses().get(0).getAllAnnotations();
                assertThat(annotations.size()).isEqualTo(1);
                var annotation = annotations.get(0);
                var type = annotation.getType();
                assertThat(type).isNotNull();
                assertThat(type).isInstanceOf(JavaType.FullyQualified.class);
                assertThat(requireNonNull(TypeUtils.asFullyQualified(type)).getFullyQualifiedName())
                  .isEqualTo("java.lang.Deprecated");
            })
          )
        );
    }
}
