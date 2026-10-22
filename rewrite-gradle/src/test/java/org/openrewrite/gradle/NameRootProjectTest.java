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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static org.openrewrite.gradle.Assertions.*;

class NameRootProjectTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NameRootProject());
    }

    @DocumentExample
    @Test
    void nameFromGradleProject() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.markers(rootProject("inventory-service"))
          ),
          settingsGradle(
            """
              include 'api'
              include 'core'
              """,
            """
              rootProject.name = 'inventory-service'

              include 'api'
              include 'core'
              """
          )
        );
    }

    @Test
    void afterPluginManagementAndPlugins() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }
              """,
            spec -> spec.markers(rootProject("inventory-service"))
          ),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              plugins {
                  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
              }

              include("api")
              """,
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              plugins {
                  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
              }

              rootProject.name = "inventory-service"

              include("api")
              """
          )
        );
    }

    @Test
    void nameFromRepository() {
        rewriteRun(
          settingsGradle(
            """
              include 'api'
              """,
            """
              rootProject.name = 'inventory-service'

              include 'api'
              """,
            spec -> spec.markers(repository("inventory-service"))
          )
        );
    }

    @Test
    void nameFromDirectoryOfNestedBuild() {
        rewriteRun(
          settingsGradle(
            """
              include 'plugin'
              """,
            """
              rootProject.name = 'build-logic'

              include 'plugin'
              """,
            spec -> spec.path("build-logic/settings.gradle")
          )
        );
    }

    @Test
    void keepExistingName() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'legacy-name'
              include 'api'
              """,
            spec -> spec.markers(repository("inventory-service"))
          )
        );
    }

    @Test
    void keepWhenNameIsUnknown() {
        rewriteRun(
          settingsGradle(
            """
              include 'api'
              """
          )
        );
    }

    @Test
    void createSettingsForRootBuild() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.markers(rootProject("inventory-service"))
          ),
          settingsGradle(
            null,
            """
              rootProject.name = 'inventory-service'
              """
          )
        );
    }

    @Test
    void doNotCreateSettingsForNestedBuild() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.path("samples/hello/build.gradle").markers(repository("inventory-service"))
          )
        );
    }

    private static GradleProject rootProject(String name) {
        return GradleProject.builder().name(name).path(":").build();
    }

    private static GitProvenance repository(String name) {
        return new GitProvenance(UUID.randomUUID(), "https://github.com/acme/" + name + ".git", null, null, null, null, null);
    }
}
