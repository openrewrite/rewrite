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

package org.openrewrite.groovy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@Disabled
@SuppressWarnings({"GroovyUnusedAssignment", "GrUnnecessaryPublicModifier", "ConstantConditions", "GrMethodMayBeStatic"})
class GroovyTypeAttributionTest implements RewriteTest {

    @Test
    void defTypeAttributed() {
        rewriteRun(
          groovy(
            """
              class Test {
                  static void test() {
                      def l = new ArrayList()
                  }
              }
              """,
            isAttributed(true)
          )
        );
    }

    @Test
    void defFieldNotTypeAttributed() {
        rewriteRun(
          groovy(
            """
              class Test {
                  def l = new ArrayList()
              }
              """,
            isAttributed(false)
          )
        );
    }

    @Test
    void globalTypeAttributed() {
        rewriteRun(
          groovy(
            """
              def l = new ArrayList()
              """,
            isAttributed(true)
          )
        );
    }

    @Test
    void closureImplicitParameterAttributed() {
        rewriteRun(
          groovy("""
              public <T> T register(String name, Class<T> type, Closure<T> configurationAction) {
                  return null
              }
              register("testTask", String) {
                  it.substring(0, 0)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(3);
                assertThat(m.getArguments().get(2)).isInstanceOf(J.Lambda.class);
                var it = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(2)).getBody())
                  .getStatements().get(0)).getExpression();
                assertThat(TypeUtils.asFullyQualified(it.getSelect().getType()).getFullyQualifiedName()).isEqualTo("java.lang.String");
                assertThat(it.getMethodType().getName()).isEqualTo("substring");
                assertThat(it.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    @Test
    void closureImplicitParameterAttributedZeroArgMethod() {
        rewriteRun(
          groovy("""
              public <T> T register(String name, Class<T> type, Closure<T> configurationAction) {
                  return null
              }
              register("testTask", Integer) {
                  it.byteValue()
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(3);
                assertThat(m.getArguments().get(2)).isInstanceOf(J.Lambda.class);
                var it = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(2)).getBody())
                  .getStatements().get(0)).getExpression();
                assertThat(TypeUtils.asFullyQualified(it.getSelect().getType()).getFullyQualifiedName()).isEqualTo("java.lang.Integer");
                assertThat(it.getMethodType().getName()).isEqualTo("byteValue");
                assertThat(it.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.Integer");
            })
          )
        );
    }

    @Test
    void closureNamedParameterAttributed() {
        rewriteRun(
          groovy("""
              public <T> T register(String name, Class<T> type, Closure<T> configurationAction) {
                  return null
              }
              register("testTask", String) { foo ->
                  foo
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(3);
                assertThat(m.getArguments().get(2)).isInstanceOf(J.Lambda.class);
                var foo = (J.Identifier) ((G.ExpressionStatement) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(2)).getBody()).getStatements().get(0))
                  .getExpression()).getExpression();
                assertThat(TypeUtils.asFullyQualified(foo.getType()).getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    @Test
    void closureWithDelegate() {
        rewriteRun(
          groovy("""
              public String register(@DelegatesTo(String) Closure stringAction) {
                  return null
              }
              register {
                  substring(0, 0)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().get(1);
                assertThat(m.getArguments()).hasSize(1);
                assertThat(m.getArguments().get(0)).isInstanceOf(J.Lambda.class);
                var substring = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
                assertThat(substring.getMethodType().getName()).isEqualTo("substring");
                assertThat(substring.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    @SuppressWarnings({"GrPackage", "OptionalGetWithoutIsPresent"})
    @Disabled
    @Test
    void infersDelegateViaSimilarGradleApi() {
        rewriteRun(
          groovy("""
              package org.gradle.api
              
              interface Action<T> {
                  void execute(T t);
              }
              void register(Action<String> stringAction) {
              }
              void register(Closure stringAction) {
              }
              register {
                  substring(0, 0)
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                var m = (J.MethodInvocation) cu.getStatements().stream()
                  .filter(s -> s instanceof J.MethodInvocation)
                  .findFirst()
                  .get();
                assertThat(m.getArguments()).hasSize(1);
                assertThat(m.getArguments().get(0)).isInstanceOf(J.Lambda.class);
                var substring = (J.MethodInvocation) ((J.Return) ((J.Block) ((J.Lambda) m.getArguments().get(0)).getBody()).getStatements().get(0)).getExpression();
                assertThat(substring.getMethodType().getName()).isEqualTo("substring");
                assertThat(substring.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("java.lang.String");
            })
          )
        );
    }

    private Consumer<SourceSpec<G.CompilationUnit>> isAttributed(boolean attributed) {
        return spec -> spec.afterRecipe(cu -> {
            new JavaVisitor<Integer>() {
                @SuppressWarnings("ConstantConditions")
                @Override
                public J visitVariable(J.VariableDeclarations.NamedVariable variable, Integer integer) {
                    assertThat(TypeUtils.asFullyQualified(variable.getVariableType().getType())
                      .getFullyQualifiedName())
                      .isEqualTo(attributed ? "java.util.ArrayList" : "java.lang.Object");
                    return variable;
                }
            }.visit(cu, 0);
        });
    }
}
