/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.marker.AnonymousFunction;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.kotlin.Assertions.kotlin;

class AnonymousFunctionTest implements RewriteTest {

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/287")
    void noArgs() {
        rewriteRun(
          kotlin(
            """
              val alwaysTrue = fun() = true
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements()).hasSize(1);
                J.VariableDeclarations alwaysTrue = (J.VariableDeclarations) cu.getStatements().get(0);
                assertThat(alwaysTrue.getVariables()).hasSize(1);
                J.VariableDeclarations.NamedVariable alwaysTrueVar = alwaysTrue.getVariables().get(0);
                assertThat(((JavaType.FullyQualified) alwaysTrueVar.getType()).getFullyQualifiedName()).isEqualTo("kotlin.Function0");
                assertThat(alwaysTrueVar.getInitializer()).isInstanceOf(J.Lambda.class);
                J.Lambda lambda = (J.Lambda) alwaysTrueVar.getInitializer();
                assertThat(lambda.getMarkers().findFirst(AnonymousFunction.class)).isPresent();
                assertThat(lambda.getParameters().getParameters()).satisfiesExactly(
                    empty -> assertThat(empty).isInstanceOf(J.Empty.class)
                );
            })
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-kotlin/issues/287")
    void singleArg() {
        rewriteRun(
          kotlin(
            """
              val positive = fun(i: Int) = i > 0
              """
          )
        );
    }

    @Test
    void nestedWithWhitespace() {
        rewriteRun(
          kotlin(
            """
              val filter = null?.let { _ ->
                  { false
                  }
              }
              """
          )
        );
    }

}
