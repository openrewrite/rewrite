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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.MavenRepositoryOrder;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.search.EffectiveRepositoryAssertions.expectedWithUrls;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class FindRepositoryOrderTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
            .recipe(new FindRepositoryOrder());
    }

    @DocumentExample
    @Test
    void repositoryInBuildGradle() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
              }
              """,
            spec -> spec.after(actual -> expectedWithUrls(actual, List.of("https://repo.spring.io/milestone"), """
              plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
              }
              """))
          )
        );
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "REWRITE_GRADLE_MIRROR_URL", matches = ".+",
            disabledReason = "An injected mirror adds a repository, defeating the empty-repositories scenario.")
    void emptyRepositories() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void multipleRepositoriesInBuildGradle() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
                  mavenCentral()
              }
              """,
            spec -> spec.after(actual -> expectedWithUrls(actual,
              List.of("https://repo.spring.io/milestone", "https://repo.maven.apache.org/maven2/"), """
              plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
                  mavenCentral()
              }
              """))
          )
        );
    }

    @Test
    void repositoryInBuildGradleKts() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  id("java")
              }

              repositories {
                  maven { url = uri("https://repo.spring.io/milestone") }
              }
              """,
            spec -> spec.after(actual -> expectedWithUrls(actual, List.of("https://repo.spring.io/milestone"), """
              plugins {
                  id("java")
              }

              repositories {
                  maven { url = uri("https://repo.spring.io/milestone") }
              }
              """))
          )
        );
    }

    @Test
    void repositoryInSettingsGradle() {
        rewriteRun(
          settingsGradle(
            """
              dependencyResolutionManagement {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                  }
              }
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.after(actual -> expectedWithUrls(actual, List.of("https://repo.spring.io/milestone"), """
              plugins {
                  id 'java'
              }
              """))
          )
        );
    }

    @Test
    void producesDataTable() {
        rewriteRun(
          spec -> spec
            .dataTable(MavenRepositoryOrder.Row.class, rows -> {
                int springIdx = indexOfUri(rows, "https://repo.spring.io/milestone");
                int centralIdx = indexOfUri(rows, "https://repo.maven.apache.org/maven2/");
                assertThat(springIdx).as("spring milestone repo present").isNotNegative();
                assertThat(centralIdx).as("maven central repo present").isNotNegative();
                assertThat(springIdx)
                  .as("spring milestone declared before maven central")
                  .isLessThan(centralIdx);
                assertThat(rows.get(springIdx).getRank())
                  .as("spring milestone rank precedes maven central rank")
                  .isLessThan(rows.get(centralIdx).getRank());
            }),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
                  mavenCentral()
              }
              """,
            spec -> spec.after(actual -> expectedWithUrls(actual,
              List.of("https://repo.spring.io/milestone", "https://repo.maven.apache.org/maven2/"), """
              plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
                  mavenCentral()
              }
              """))
          )
        );
    }

    private static int indexOfUri(List<MavenRepositoryOrder.Row> rows, String uri) {
        for (int i = 0; i < rows.size(); i++) {
            if (uri.equals(rows.get(i).getUri())) {
                return i;
            }
        }
        return -1;
    }
}
