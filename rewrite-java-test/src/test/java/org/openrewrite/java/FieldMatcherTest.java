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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class FieldMatcherTest implements RewriteTest {

    @Test
    void fieldMatch() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.atomic.AtomicReference;
              class Test {
                  AtomicReference<Integer> n = new AtomicReference<>();
              }
              """,
            spec -> spec.afterRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Integer integer) {
                    assertThat(new FieldMatcher("java.util..*", true)
                      .matches(requireNonNull(variable.getVariableType()).getOwner())).isTrue();
                    return super.visitVariable(variable, integer);
                }
            }.visitNonNull(cu, 0))
          )
        );
    }
}
