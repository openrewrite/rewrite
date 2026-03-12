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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;

class AddSettingsPluginRepositoryTest implements RewriteTest {
    @DocumentExample
    @Test
    void emptySettingsFile() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradle(
            "",
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = "https://repo.example.com/snapshots"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noPluginManagementBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradle(
            """
              rootProject.name = "demo"
              """,
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = "https://repo.example.com/snapshots"
                      }
                  }
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void existingPluginManagementBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                  }
              }
              """,
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                      maven {
                          url = "https://repo.example.com/snapshots"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExists() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      maven { url = "https://repo.example.com/snapshots" }
                  }
              }
              """
          )
        );
    }

    @Test
    void addMavenLocal() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("mavenLocal", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = "https://repo.example.com/snapshots"
                      }
                  }
              }
              """,
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = "https://repo.example.com/snapshots"
                      }
                      mavenLocal()
                  }
              }
              """
          )
        );
    }

    @Test
    void existingRepositoryUsingMethod() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      maven { url "https://repo.example.com/snapshots" }
                  }
              }
              """
          )
        );
    }

    @Test
    void existingRepositoryUsingMethodAndGroovyString() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "${NEXUS_URL}/snapshots")),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      maven { url "${NEXUS_URL}/snapshots" }
                  }
              }
              """
          )
        );
    }

    @Test
    void emptySettingsFileKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradleKts(
            "",
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = uri("https://repo.example.com/snapshots")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noPluginManagementBlockKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradleKts(
            """
              rootProject.name = "demo"
              """,
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = uri("https://repo.example.com/snapshots")
                      }
                  }
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void existingPluginManagementBlockKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                  }
              }
              """,
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                      maven {
                          url = uri("https://repo.example.com/snapshots")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExistsKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      maven { url = uri("https://repo.example.com/snapshots") }
                  }
              }
              """
          )
        );
    }

    @Test
    void addMavenLocalKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("mavenLocal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = uri("https://repo.example.com/snapshots")
                      }
                  }
              }
              """,
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = uri("https://repo.example.com/snapshots")
                      }
                      mavenLocal()
                  }
              }
              """
          )
        );
    }

    @Test
    void existingRepositoryUsingMethodKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "https://repo.example.com/snapshots")),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      maven { url = uri("https://repo.example.com/snapshots") }
                  }
              }
              """
          )
        );
    }

    @Test
    void existingRepositoryUsingMethodAndGroovyStringKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("maven", "${NEXUS_URL}/snapshots")),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      maven { url = uri("${NEXUS_URL}/snapshots") }
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExistsGradlePluginPortal() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExistsGradlePluginPortalKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExistsMavenLocal() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("mavenLocal", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExistsMavenLocalKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("mavenLocal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                  }
              }
              """
          )
        );
    }

    @Test
    void noDuplicatePluginManagementBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void noDuplicatePluginManagementBlockKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void skipWhenExistsGradlePluginPortalWithOtherRepos() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExistsGradlePluginPortalWithOtherReposKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void skipWhenExistsWithPluginsBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              plugins {
                  id "com.example.plugin" version "1.0"
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void skipWhenExistsWithPluginsBlockKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              plugins {
                  id("com.example.plugin") version "1.0"
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void addGradlePluginPortalToExistingPluginManagement() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                  }
              }
              """,
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void addGradlePluginPortalToExistingPluginManagementKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                  }
              }
              """,
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void addGradlePluginPortalToEmptyFile() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradle(
            "",
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void addGradlePluginPortalToEmptyFileKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            "",
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentAfterAddingGradlePluginPortal() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null))
            .expectedCyclesThatMakeChanges(1).cycles(3),
          settingsGradle(
            "",
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentAfterAddingGradlePluginPortalKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null))
            .expectedCyclesThatMakeChanges(1).cycles(3),
          settingsGradleKts(
            "",
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentAfterAddingGradlePluginPortalWithExistingContent() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null))
            .expectedCyclesThatMakeChanges(1).cycles(3),
          settingsGradle(
            """
              rootProject.name = "demo"
              """,
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void idempotentAfterAddingGradlePluginPortalWithExistingContentKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null))
            .expectedCyclesThatMakeChanges(1).cycles(3),
          settingsGradleKts(
            """
              rootProject.name = "demo"
              """,
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void skipWhenExistsComplexSettingsKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      mavenLocal()
                      gradlePluginPortal()
                  }
              }

              plugins {
                  id("com.gradle.develocity") version "latest.release"
              }

              rootProject.name = "my-project"

              include("module-a")
              include("module-b")
              """
          )
        );
    }

    @Test
    void skipWhenExistsWithMavenRepoAndPluginsBlockKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = uri("https://repo.example.com/releases")
                      }
                      gradlePluginPortal()
                  }
              }

              plugins {
                  id("com.gradle.develocity") version "latest.release"
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    @Test
    void addToExistingPluginManagementWithPluginsBlockKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null))
            .expectedCyclesThatMakeChanges(1).cycles(3),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = uri("https://repo.example.com/releases")
                      }
                  }
              }

              plugins {
                  id("com.gradle.develocity") version "latest.release"
              }

              rootProject.name = "demo"
              """,
            """
              pluginManagement {
                  repositories {
                      maven {
                          url = uri("https://repo.example.com/releases")
                      }
                      gradlePluginPortal()
                  }
              }

              plugins {
                  id("com.gradle.develocity") version "latest.release"
              }

              rootProject.name = "demo"
              """
          )
        );
    }
}
