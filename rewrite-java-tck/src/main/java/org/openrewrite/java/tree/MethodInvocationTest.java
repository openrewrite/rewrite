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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.tree.TypeUtils.asFullyQualified;
import static org.openrewrite.test.RewriteTest.toRecipe;

@SuppressWarnings("ConstantConditions")
class MethodInvocationTest implements RewriteTest {
    @Test
    void methodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("foo")) {
                      assertThat("foo").isEqualTo(method.getSimpleName());
                      assertThat("java.lang.Integer").isEqualTo(TypeUtils.asFullyQualified(method.getType())
                        .getFullyQualifiedName());

                      var effectParams = method.getMethodType().getParameterTypes();
                      assertThat("java.lang.Integer").isEqualTo(TypeUtils.asFullyQualified(effectParams.get(0))
                        .getFullyQualifiedName());
                      assertThat("java.lang.Integer").isEqualTo(TypeUtils.asFullyQualified(TypeUtils.asArray(effectParams.get(1))
                        .getElemType()).getFullyQualifiedName());

                      assertThat(method.getMethodType().getDeclaringType().getFullyQualifiedName()).isEqualTo("A");
                  }
                  return method;
              }
          })),
          java(
            """
                  public class A {
                      Integer m = foo ( 0, 1, 2 );
                  
                      public Integer foo(Integer n, Integer... ns) { return n; }
                  }
              """
          )
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void genericMethodInvocation() {
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                  if (method.getSimpleName().equals("generic")) {
                      var methType = method.getMethodType();
                      assertThat(TypeUtils.asFullyQualified(methType.getReturnType()).getFullyQualifiedName())
                        .isEqualTo("java.lang.Integer");
                      assertThat(TypeUtils.asFullyQualified(methType.getParameterTypes().get(0)).getFullyQualifiedName())
                        .isEqualTo("java.lang.Integer");
                      assertThat(TypeUtils.asFullyQualified(TypeUtils.asArray(methType.getParameterTypes().get(1)).getElemType())
                        .getFullyQualifiedName()).isEqualTo("java.lang.Integer");
                  }
                  return method;
              }
          })),
          java(
            """
              public class A {
                  Integer o = generic ( 0, 1, 2 );
                  Integer p = this . < Integer > generic ( 0, 1, 2 );
                            
                  public <TTTT> TTTT generic(TTTT n, TTTT... ns) { return n; }
              }
              """
          )
        );
    }
}
