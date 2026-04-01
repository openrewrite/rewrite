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
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.test.SourceSpecs;
import org.openrewrite.test.TypeValidation;

import java.nio.file.Paths;
import java.util.Collections;

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
    void skipWhenExistsGradlePluginPortalAlone() {
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
    void skipWhenExistsGradlePluginPortalAloneKts() {
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
    void addToExistingPluginManagementNotFirstStatementKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null))
            .expectedCyclesThatMakeChanges(1).cycles(3),
          settingsGradleKts(
            """
              rootProject.name = "demo"

              pluginManagement {
                  repositories {
                      mavenLocal()
                  }
              }
              """,
            """
              rootProject.name = "demo"

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
    void skipWhenExistsPluginManagementNotFirstStatementKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              rootProject.name = "demo"

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
    void noPluginManagementBlockWithBuildCacheKts() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("mavenCentral", null)),
          settingsGradleKts(
            """
              buildCache {
                  local {
                      isEnabled = false
                  }
              }

              rootProject.name = "demo"
              """,
            """
              pluginManagement {
                  repositories {
                      mavenCentral()
                  }
              }

              buildCache {
                  local {
                      isEnabled = false
                  }
              }

              rootProject.name = "demo"
              """
          )
        );
    }

    /**
     * Simulate platform behavior where KTS-parsed settings.gradle.kts has incomplete type
     * attribution — method types have a wrong declaring type instead of being null.
     */
    @SuppressWarnings("unchecked")
    private static <T extends SourceFile> T corruptMethodTypes(T sourceFile) {
        JavaType.FullyQualified wrongType = JavaType.ShallowClass.build("kotlin.Unit");
        return (T) new JavaIsoVisitor<Integer>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer p) {
                J.MethodInvocation m = super.visitMethodInvocation(method, p);
                if (m.getMethodType() != null) {
                    return m.withMethodType(m.getMethodType().withDeclaringType(wrongType));
                }
                return m;
            }
        }.visitNonNull(sourceFile, 0);
    }

    @Test
    void skipWhenExistsGradlePluginPortalKtsWithoutTypeAttribution() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null)),
          settingsGradleKts(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """,
            spec -> spec.mapBeforeRecipe(AddSettingsPluginRepositoryTest::corruptMethodTypes)
          )
        );
    }

    @Test
    void skipWhenExistsGradlePluginPortalWithOtherReposKtsWithoutTypeAttribution() {
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
              """,
            spec -> spec.mapBeforeRecipe(AddSettingsPluginRepositoryTest::corruptMethodTypes)
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

    /**
     * Helper to create settingsGradleKts specs using a GradleParser with empty settingsClasspath.
     * When settingsClasspath is explicitly set to empty (e.g. no custom plugins in the settings
     * buildscript), the Gradle API stubs must still be included for correct type attribution.
     */
    private static SourceSpecs settingsGradleKtsWithEmptyClasspath(String before) {
        GradleParser.Builder parser = GradleParser.builder()
                .kotlinParser(KotlinParser.builder().logCompilationWarningsAndErrors(true))
                .settingsClasspath(Collections.emptyList());
        SourceSpec<K.CompilationUnit> gradle = new SourceSpec<>(K.CompilationUnit.class, "gradle", parser, before, null);
        gradle.path(Paths.get("settings.gradle.kts"));
        return gradle;
    }

    @Test
    void skipWhenExistsGradlePluginPortalKtsWithEmptyClasspath() {
        rewriteRun(
          spec -> spec.recipe(new AddSettingsPluginRepository("gradlePluginPortal", null))
            .typeValidationOptions(TypeValidation.none()),
          settingsGradleKtsWithEmptyClasspath(
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
}
