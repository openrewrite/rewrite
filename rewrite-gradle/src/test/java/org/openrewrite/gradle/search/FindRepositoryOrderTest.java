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
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.MavenRepositoryOrder;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.*;
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
            """
              /*~~(https://repo.spring.io/milestone)~~>*/plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
              }
              """
          )
        );
    }

    @Test
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
            """
              /*~~(https://repo.spring.io/milestone
              https://repo.maven.apache.org/maven2/)~~>*/plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
                  mavenCentral()
              }
              """
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
            """
              /*~~(https://repo.spring.io/milestone)~~>*/plugins {
                  id("java")
              }

              repositories {
                  maven { url = uri("https://repo.spring.io/milestone") }
              }
              """
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
            """
              /*~~(https://repo.spring.io/milestone)~~>*/plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void producesDataTable() {
        rewriteRun(
          spec -> spec
            .dataTable(MavenRepositoryOrder.Row.class, rows -> {
                assertThat(rows).hasSize(2);
                assertThat(rows.get(0).getUri()).isEqualTo("https://repo.spring.io/milestone");
                assertThat(rows.get(0).getRank()).isEqualTo(0);
                assertThat(rows.get(1).getUri()).isEqualTo("https://repo.maven.apache.org/maven2/");
                assertThat(rows.get(1).getRank()).isEqualTo(1);
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
            """
              /*~~(https://repo.spring.io/milestone
              https://repo.maven.apache.org/maven2/)~~>*/plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
                  mavenCentral()
              }
              """
          )
        );
    }
}
