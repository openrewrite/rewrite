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

class EffectiveGradlePluginRepositoriesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
            .recipe(new EffectiveGradlePluginRepositories(true));
    }

    @DocumentExample
    @Test
    void pluginRepositories() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-project'
              """,
            """
              /*~~(https://plugins.gradle.org/m2)~~>*/rootProject.name = 'my-project'
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            """
              /*~~(https://plugins.gradle.org/m2)~~>*/plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void multiplePluginRepositoriesInSettingsGradle() {
        rewriteRun(
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                      gradlePluginPortal()
                  }
              }
              """,
            """
              /*~~(https://repo.spring.io/milestone
              https://plugins.gradle.org/m2)~~>*/pluginManagement {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                      gradlePluginPortal()
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
              /*~~(https://repo.spring.io/milestone
              https://plugins.gradle.org/m2)~~>*/plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void pluginRepositoryInSettingsGradleKts() {
        rewriteRun(
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      maven { url = uri("https://repo.spring.io/milestone") }
                  }
              }
              """,
            """
              /*~~(https://repo.spring.io/milestone)~~>*/pluginManagement {
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
            .recipe(new EffectiveGradlePluginRepositories(false))
            .dataTable(Row.class, rows -> assertThat(rows).containsExactlyInAnyOrder(
              new Row("settings.gradle", "https://repo.spring.io/milestone"),
              new Row("settings.gradle", "https://plugins.gradle.org/m2"),
              new Row("build.gradle", "https://repo.spring.io/milestone"),
              new Row("build.gradle", "https://plugins.gradle.org/m2")
            )),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                      gradlePluginPortal()
                  }
              }

              include 'module'
              """,
            spec -> spec.path("settings.gradle")
          ),
          buildGradle(
            """
              buildscript {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                  }
              }

              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }
              """,
            spec -> spec.path("build.gradle")
          )
        );
    }

    @Test
    void noMarkersWhenDisabled() {
        rewriteRun(
          spec -> spec.recipe(new EffectiveGradlePluginRepositories(false)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                  }
              }
              """
          )
        );
    }

    @Test
    void buildscriptRepositoriesInBuildGradle() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                  }
              }

              plugins {
                  id 'java'
              }
              """,
            """
              /*~~(https://repo.spring.io/milestone
              https://plugins.gradle.org/m2)~~>*/buildscript {
                  repositories {
                      maven { url 'https://repo.spring.io/milestone' }
                  }
              }

              plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void buildscriptRepositoriesInBuildGradleKts() {
        rewriteRun(
          buildGradleKts(
            """
              buildscript {
                  repositories {
                      maven { url = uri("https://repo.spring.io/milestone") }
                  }
              }

              plugins {
                  id("java")
              }
              """,
            """
              /*~~(https://repo.spring.io/milestone
              https://plugins.gradle.org/m2)~~>*/buildscript {
                  repositories {
                      maven { url = uri("https://repo.spring.io/milestone") }
                  }
              }

              plugins {
                  id("java")
              }
              """
          )
        );
    }

    @Test
    void doesNotAffectProjectRepositoriesInBuildGradle() {
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
              /*~~(https://plugins.gradle.org/m2)~~>*/plugins {
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
