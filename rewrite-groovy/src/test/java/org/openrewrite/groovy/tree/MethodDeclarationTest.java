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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

class MethodDeclarationTest implements RewriteTest {

    @Test
    void methodDeclarationDeclaringType() {
        rewriteRun(
          groovy(
            """
              class A {
                  void method() {}
              }
              """,
            spec -> spec.beforeRecipe(cu -> {
                var method = (J.MethodDeclaration) cu.getClasses().get(0).getBody().getStatements().get(0);
                JavaType.Method methodType = method.getMethodType();
                assertThat(methodType).isNotNull();
                assertThat(methodType.getName()).isEqualTo("method");
                var declaring = methodType.getDeclaringType();
                assertThat(declaring.getFullyQualifiedName()).isEqualTo("A");
                assertThat(declaring.getMethods().stream()
                  .filter(m -> m == methodType)
                  .findAny()).isPresent();
            })
          )
        );
    }

    @Test
    void methodDeclaration() {
        rewriteRun(
          groovy(
            """
              def accept(Map m) {
              }
              """
          )
        );
    }

    @Test
    void primitiveReturn() {
        rewriteRun(
          groovy(
            """
              static int accept(Map m) {
                  return 0
              }
              """
          )
        );
    }

    @Test
    void emptyArguments() {
        rewriteRun(
          groovy("def foo( ) {}")
        );
    }

    @Test
    void methodThrows() {
        rewriteRun(
          groovy(
            """
              def foo(int a) throws Exception , RuntimeException {
              }
              """
          )
        );
    }

    @Test
    void dynamicTypedArguments() {
        rewriteRun(
          groovy(
            """
              def foo(bar, baz) {
              }
              """
          )
        );
    }

    @Test
    void defaultArgumentValues() {
        rewriteRun(
          groovy(
            """
              def confirmNextStepWithCredentials(String message /* = prefix */ = /* hello prefix */ "Hello" ) {
              }
              """
          )
        );
    }

    @Test
    void returnNull() {
        rewriteRun(
          groovy(
            """
              static def foo() {
                  return null
              }
              """
          )
        );
    }
}
