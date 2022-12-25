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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantConditions")
class JavaTypeTest implements RewriteTest {

    @Test
    void resolvedSignatureOfGenericMethodDeclarations() {
        rewriteRun(
          java(
            """
              import java.util.ListIterator;
              import static java.util.Collections.singletonList;
                              
              interface MyList<E> {
                  ListIterator<E> listIterator();
              }
                              
              class Test {
                  ListIterator<Integer> s = singletonList(1).listIterator();
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Object o) {
                    assertThat(method.getMethodType().getReturnType()).isInstanceOf(JavaType.Parameterized.class);
                    return method;
                }

                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    JavaType returnType = ((J.MethodInvocation) variable.getInitializer()).getMethodType().getReturnType();
                    assertThat(TypeUtils.asFullyQualified(TypeUtils.asParameterized(returnType).getTypeParameters().get(0))
                      .getFullyQualifiedName()).isEqualTo("java.lang.Integer");
                    return variable;
                }
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/762")
    @Test
    void annotationsOnTypeAttribution() {
        rewriteRun(
          java(
            """
              import java.util.function.Consumer;
              class Test {
                  Consumer<String> c;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, Object o) {
                    List<JavaType.FullyQualified> annotations = multiVariable.getTypeAsFullyQualified().getAnnotations();
                    assertThat(annotations).hasSize(1);
                    assertThat(annotations.get(0).getFullyQualifiedName()).isEqualTo("java.lang.FunctionalInterface");
                    return multiVariable;
                }
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1267")
    @Test
    void noStackOverflow() {
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;
              class A {
                  Map<String, Map<String, Map<Integer, String>>> overflowMap = new HashMap<>();
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, Object o) {
                    assertThat(cu.getTypesInUse().getTypesInUse()).isNotEmpty();
                    return super.visitCompilationUnit(cu, o);
                }

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Object o) {
                    assertThat(classDecl.getSimpleName()).isEqualTo("A");
                    return classDecl;
                }
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2493")
    @Test
    void noNewMethodType() {
        rewriteRun(
          java(
            """
              public class Test {
              }
              """
          ),
          java(
            """
              public class A {
                  void method() {
                      Test a = test(null);
                  }
                  
                  Test test(Test test) {
                      return test;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Object o) {
                    J.MethodInvocation m = super.visitMethodInvocation(method, o);
                    if (m.getName().getPrefix().getWhitespace().isEmpty()) {
                        m = m.withName(m.getName().withPrefix(m.getName().getPrefix().withWhitespace("  ")));
                    }
                    assertThat(m.getName().getType()).isSameAs(m.getMethodType());
                    return m;
                }
            })
          )
        );
    }
}
