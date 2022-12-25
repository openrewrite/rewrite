/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantConditions")
class TypeUtilsTest implements RewriteTest {

    static Consumer<SourceSpec<J.CompilationUnit>> typeIsPresent() {
        return s -> s.afterRecipe(cu -> {
            var fooMethodType = ((J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0)).getMethodType();
            assertThat(TypeUtils.findOverriddenMethod(fooMethodType)).isPresent();
        });
    }

    @Test
    void isOverrideBasicInterface() {
        rewriteRun(
          java(
            """
              interface Interface {
                  void foo();
              }
              """
          ),
          java(
            """
              class Clazz implements Interface {
                  @Override void foo() { }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Test
    void isOverrideBasicInheritance() {
        rewriteRun(
          java(
            """
              class Superclass {
                  void foo();
              }
              """
          ),
          java(
            """
              class Clazz extends Superclass {
                  @Override void foo() { }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1759")
    @Test
    void isOverrideParameterizedInterface() {
        rewriteRun(
          java(
            """
              import java.util.Comparator;
                            
              class TestComparator implements Comparator<String> {
                  @Override public int compare(String o1, String o2) {
                      return 0;
                  }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Test
    void isOverrideParameterizedMethod() {
        rewriteRun(
          java(
            """
              interface Interface {
                  <T> void foo(T t);
              }
              """
          ),
          java(
            """
              class Clazz implements Interface {
                  @Override <T> void foo(T t) { }
              }
              """,
            typeIsPresent()
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1782")
    @Test
    void isOverrideConsidersTypeParameterPositions() {
        rewriteRun(
          java(
            """
              interface Interface <T, Y> {
                   void foo(Y y, T t);
              }
              """
          ),
          java(
            """
              class Clazz implements Interface<Integer, String> {
                  void foo(Integer t, String y) { }
                  
                  @Override
                  void foo(String y, Integer t) { }
              }
              """,
            s -> s.afterRecipe(cu -> {
                var methods = cu.getClasses().get(0).getBody().getStatements();
                assertThat(TypeUtils.findOverriddenMethod(((J.MethodDeclaration) methods.get(0)).getMethodType())).isEmpty();
                assertThat(TypeUtils.findOverriddenMethod(((J.MethodDeclaration) methods.get(1)).getMethodType())).isPresent();
            })
          )
        );
    }

    @Test
    void isFullyQualifiedOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  Integer integer1;
                  Integer integer2;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    assertThat(variable.getVariableType().getType()).isInstanceOf(JavaType.Class.class);
                    return super.visitVariable(variable, o);
                }
            })
          )
        );
    }

    @Test
    void isParameterizedTypeOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  java.util.List<Integer> integer1;
                  java.util.List<Integer> integer2;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    assertThat(variable.getVariableType().getType()).isInstanceOf(JavaType.Parameterized.class);
                    return super.visitVariable(variable, o);
                }
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1857")
    @Test
    void isParameterizedTypeWithShallowClassesOfType() {
        rewriteRun(
          java(
            """
              class Test {
                  java.util.List<Integer> integer1;
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Object o) {
                    JavaType varType = variable.getVariableType().getType();
                    assertThat(varType).isInstanceOf(JavaType.Parameterized.class);
                    var shallowParameterizedType = new JavaType.Parameterized(null, JavaType.ShallowClass.build("java.util.List"),
                      singletonList(JavaType.ShallowClass.build("java.lang.Integer")));
                    assertThat(TypeUtils.isOfType(varType, shallowParameterizedType)).isTrue();
                    return super.visitVariable(variable, o);
                }
            })
          )
        );
    }
}
