/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scala.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.scala.Assertions.scala;

class SelfTypeTest implements RewriteTest {

    @Test
    void namedAndTyped() {
        rewriteRun(
          scala(
            """
              trait A { self: Runnable =>
                def f = self.run()
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.Block body = ((J.ClassDeclaration) cu.getStatements().getFirst()).getBody();
                assertThat(body.getStatements()).hasSize(2);
                J.VariableDeclarations declaration = ((S.SelfType) body.getStatements().getFirst()).getDeclaration();
                assertThat(declaration.getTypeExpression()).isNotNull();
                assertThat(TypeUtils.isOfClassType(declaration.getTypeExpression().getType(), "java.lang.Runnable")).isTrue();
                JavaType.Variable self = declaration.getVariables().getFirst().getVariableType();
                assertThat(self).isNotNull();
                assertThat(self.getName()).isEqualTo("self");
                assertThat(TypeUtils.isOfClassType(self.getOwner(), "A")).isTrue();
            })
          )
        );
    }

    @Test
    void namedOnly() {
        rewriteRun(
          scala(
            """
              class A { self => }
              """
          )
        );
    }

    @Test
    void quotedName() {
        rewriteRun(
          scala(
            """
              trait A { `my self`: Runnable => }
              """
          )
        );
    }

    @Test
    void thisTyped() {
        rewriteRun(
          scala(
            """
              class A { this: Runnable => }
              """
          )
        );
    }

    @Test
    void objectBody() {
        rewriteRun(
          scala(
            """
              object O { self => }
              """
          )
        );
    }

    @Test
    void anonymousClassBody() {
        rewriteRun(
          scala(
            """
              class B {
                val r = new Runnable { outer =>
                  def run() = ()
                }
              }
              """
          )
        );
    }
}
