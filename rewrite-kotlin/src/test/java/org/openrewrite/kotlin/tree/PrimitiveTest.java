/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.kotlin.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class PrimitiveTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite-spring/pull/663")
    @Test
    void intZeroValueIsInteger() {
        rewriteRun(
          kotlin(
            """
              fun foo() {
                  val bar = 0
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration foo = (J.MethodDeclaration) cu.getStatements().getFirst();
                J.VariableDeclarations bar = (J.VariableDeclarations) foo.getBody().getStatements().getFirst();
                J.Literal zero = (J.Literal) bar.getVariables().getFirst().getInitializer();
                assertThat(zero.getType().getKeyword()).isEqualTo("int");
                assertThat(zero.getValue()).isInstanceOf(Integer.class);
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-spring/pull/663")
    @Test
    void longZeroValueIsLong() {
        rewriteRun(
          kotlin(
            """
              fun foo() {
                  val bar = 0L
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                J.MethodDeclaration foo = (J.MethodDeclaration) cu.getStatements().getFirst();
                J.VariableDeclarations bar = (J.VariableDeclarations) foo.getBody().getStatements().getFirst();
                J.Literal zero = (J.Literal) bar.getVariables().getFirst().getInitializer();
                assertThat(zero.getType().getKeyword()).isEqualTo("long");
                assertThat(zero.getValue()).isInstanceOf(Long.class);
            })
          )
        );
    }

}
