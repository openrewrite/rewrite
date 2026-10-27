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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.*;

class MoveRepositoriesToSettingsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MoveRepositoriesToSettings());
    }

    @DocumentExample("Declare the build's repositories once, in settings")
    @Test
    void movesRepositoriesFromBuildScript() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-service'
              """,
            """
              rootProject.name = 'my-service'

              dependencyResolutionManagement {
                  repositories {
                      mavenCentral()
                  }
              }
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
              }
              """
          )
        );
    }

    @Test
    void movesRepositoriesInKotlinDsl() {
        rewriteRun(
          settingsGradleKts(
            """
              rootProject.name = "my-service"
              """,
            """
              rootProject.name = "my-service"

              dependencyResolutionManagement {
                  repositories {
                      mavenCentral()
                  }
              }
              """
          ),
          buildGradleKts(
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id("java")
              }
              """
          )
        );
    }

    /**
     * With more than one project the repositories of each are not interchangeable: hoisting one project's into
     * settings applies it to all of them, which can change which repository a coordinate resolves from.
     */
    @Test
    void noChangeInAMultiProjectBuild() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-service'
              include(':library')
              """
          ),
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenSettingsAlreadyManagesResolution() {
        rewriteRun(
          settingsGradle(
            """
              dependencyResolutionManagement {
                  repositories {
                      mavenCentral()
                  }
              }
              """
          ),
          buildGradle(
            """
              repositories {
                  gradlePluginPortal()
              }
              """
          )
        );
    }

    /**
     * A `buildscript` block's repositories resolve the build's own classpath, not the project's dependencies, and
     * `dependencyResolutionManagement` does not serve them.
     */
    @Test
    void noChangeToBuildscriptRepositories() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-service'
              """
          ),
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }
              """
          )
        );
    }

    /**
     * Repositories inside `publishing` are where artifacts go, not where dependencies come from.
     */
    @Test
    void noChangeToPublishingRepositories() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-service'
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'maven-publish'
              }

              publishing {
                  repositories {
                      maven {
                          url = 'https://example.com/repo'
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * A `repositories` block inside `allprojects` is multi-project configuration by nature, even when the build
     * happens to have one project today.
     */
    @Test
    void noChangeToAllprojectsRepositories() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-service'
              """
          ),
          buildGradle(
            """
              allprojects {
                  repositories {
                      mavenCentral()
                  }
              }
              """
          )
        );
    }
}
