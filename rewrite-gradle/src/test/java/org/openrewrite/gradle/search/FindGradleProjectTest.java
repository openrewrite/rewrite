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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

class FindGradleProjectTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindGradleProject(null));
    }

    @ParameterizedTest
    @EnumSource(FindGradleProject.SearchCriteria.class)
    void isGradleGroovyProject(FindGradleProject.SearchCriteria criteria) {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new FindGradleProject(criteria)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            """
              /*~~>*/plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void isGradleKotlinProject() {
        rewriteRun(
          text(
            """
              plugins {
                  id("java")
              }
              """,
            """
              ~~>plugins {
                  id("java")
              }
              """,
            spec -> spec.path("build.gradle.kts")
          )
        );
    }

    @Test
    void mavenProject() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>maven-project</artifactId>
                  <version>1</version>
              </project>
              """
          )
        );
    }
}
