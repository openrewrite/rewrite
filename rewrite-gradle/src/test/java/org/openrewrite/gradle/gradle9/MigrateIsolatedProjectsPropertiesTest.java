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
package org.openrewrite.gradle.gradle9;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;

class MigrateIsolatedProjectsPropertiesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/gradle-9.yml",
          "org.openrewrite.gradle.gradle9.MigrateIsolatedProjectsProperties");
    }

    @DocumentExample
    @Test
    void renameIsolatedProjectsProperties() {
        rewriteRun(
          properties(
            """
              org.gradle.caching=true
              org.gradle.unsafe.isolated-projects=true
              org.gradle.unsafe.isolated-projects.diagnostics=true
              org.gradle.unsafe.isolated-projects.dangerously-ignore-problems=true
              """,
            """
              org.gradle.caching=true
              org.gradle.isolated-projects=true
              org.gradle.isolated-projects.diagnostics=true
              org.gradle.isolated-projects.dangerously-ignore-problems=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void leavesPromotedPropertiesAlone() {
        rewriteRun(
          properties(
            """
              org.gradle.isolated-projects=true
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void onlyAppliesToGradleProperties() {
        rewriteRun(
          properties(
            """
              org.gradle.unsafe.isolated-projects=true
              """,
            spec -> spec.path("src/main/resources/application.properties")
          )
        );
    }
}
