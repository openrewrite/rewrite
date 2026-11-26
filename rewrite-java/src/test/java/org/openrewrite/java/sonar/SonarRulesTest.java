/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.sonar;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Validated;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class SonarRulesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.sonar.SonarRules");
    }

    @Test
    void compositesResolve() {
        assertThat(Environment.builder().scanRuntimeClasspath().build()
          .activateRecipes("org.openrewrite.java.sonar.SonarRules")
          .validateAll())
          .filteredOn(Validated::isInvalid)
          .isEmpty();
    }

    @DocumentExample
    @Test
    void runsSonarRules() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Set;

              class A {
                  Set<String> names;
              }
              """,
            """
              import java.util.Set;

              class A {
                  Set<String> names;
              }
              """,
            // keep the trailing newline, which EmptyNewlineAtEndOfFile would otherwise add
            spec -> spec.noTrim()
          )
        );
    }
}
