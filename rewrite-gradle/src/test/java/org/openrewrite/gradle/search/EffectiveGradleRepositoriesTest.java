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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.maven.search.EffectiveMavenRepositoriesTable.Row;

class EffectiveGradleRepositoriesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
            .recipe(new EffectiveGradleRepositories(true));
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
    void repositoryInSettingsGradleKts() {
        rewriteRun(
          settingsGradleKts(
            """
              dependencyResolutionManagement {
                  repositories {
                      maven { url = uri("https://repo.spring.io/milestone") }
                  }
              }
              """
          ),
          buildGradleKts(
            """
              plugins {
                  id("java")
              }
              """,
            """
              /*~~(https://repo.spring.io/milestone)~~>*/plugins {
                  id("java")
              }
              """
          )
        );
    }

    @Test
    void producesDataTable() {
        rewriteRun(
          spec -> spec
            .recipe(new EffectiveGradleRepositories(false))
            .dataTable(Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
              new Row("build.gradle", "https://repo.spring.io/milestone"),
              new Row("build.gradle", "https://repo.maven.apache.org/maven2/"),
              new Row("module/build.gradle", "https://repo.spring.io/milestone"),
              new Row("module/build.gradle", "https://repo.maven.apache.org/maven2/")
            )),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              include 'module'
              """
          ),
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
            spec -> spec.path("build.gradle")
          ),
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
            spec -> spec.path("module/build.gradle")
          )
        );
    }

    @Test
    void noMarkersWhenDisabled() {
        rewriteRun(
          spec -> spec.recipe(new EffectiveGradleRepositories(false)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  maven { url 'https://repo.spring.io/milestone' }
              }
              """
          )
        );
    }
}
