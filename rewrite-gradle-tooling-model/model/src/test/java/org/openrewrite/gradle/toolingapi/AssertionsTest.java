/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.toolingapi;

import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.test.SourceSpecs.text;

@DisabledIf("org.openrewrite.gradle.marker.GradleProjectTest#gradleOlderThan8")
class AssertionsTest implements RewriteTest {

    // The Gradle running this build has its distribution downloaded already, so a wrapper pointing at it downloads
    // nothing, and the tests that use one share a daemon
    private static final URI CURRENT_GRADLE_DISTRIBUTION = URI.create(
      "https://services.gradle.org/distributions/gradle-" + GradleVersion.current().getVersion() + "-bin.zip");

    @Test
    void withToolingApi() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi()),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).isPresent())
          )
        );
    }

    @Test
    void withCustomDistributionUri() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi(CURRENT_GRADLE_DISTRIBUTION)),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).isPresent())
          )
        );
    }

    @Test
    void customInitScript() throws IOException {
        // The plugin comes from the jars this build made rather than whatever snapshot a remote repository has,
        // and the group shows that this script is the one that ran
        StringBuilder jars = new StringBuilder();
        for (String jar : Files.readAllLines(Paths.get(System.getProperty("org.openrewrite.gradle.local.use-embedded-classpath")))) {
            if (!jar.trim().isEmpty()) {
                jars.append(jars.length() == 0 ? "'" : ", '").append(jar.trim().replace("\\", "\\\\")).append('\'');
            }
        }
        //language=groovy
        String alternateInit = String.format("""
          initscript {
              dependencies {
                  classpath files(%s)
              }
          }

          allprojects {
              group = 'org.openrewrite.custom.init'
              apply plugin: org.openrewrite.gradle.toolingapi.ToolingApiOpenRewriteModelPlugin
          }
          """, jars);
        GradleWrapper gradleWrapper = GradleWrapper.create(CURRENT_GRADLE_DISTRIBUTION, null);
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi(gradleWrapper, alternateInit)),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class))
              .hasValueSatisfying(gp -> assertThat(gp.getGroup()).isEqualTo("org.openrewrite.custom.init")))
          )
        );
    }

    @Test
    void multipleToolingApiCallsAddSingleMarker() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi())
            .beforeRecipe(Assertions.withToolingApi()),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findAll(GradleProject.class)).hasSize(1))
          ), text(
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              empty=
              """,
            spec -> spec
              .path("gradle.lockfile")
              .afterRecipe(cu -> assertThat(cu.getMarkers().findAll(GradleProject.class)).hasSize(1))
          )
        );
    }

    @Test
    void withLockFile() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi()),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).isPresent())
          ), text(
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              empty=
              """,
            spec -> spec
              .path("gradle.lockfile")
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).isPresent())
          )
        );
    }

    @Test
    void multimoduleLockFiles() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi()),
          //language=groovy
          settingsGradle(
            """
              rootProject.name = 'test'
              include 'subproject1', 'subproject2'
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec
              .path("build.gradle")
              .afterRecipe(cu -> {
                  Optional<GradleProject> gp = cu.getMarkers().findFirst(GradleProject.class);
                  assertThat(gp).isPresent();
                  assertThat(gp.get().getPath()).isEqualTo(":");
              })
          ), text(
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              empty=
              """,
            spec -> spec
              .path("subproject1/gradle.lockfile")
              .afterRecipe(cu -> {
                  Optional<GradleProject> gp = cu.getMarkers().findFirst(GradleProject.class);
                  assertThat(gp).isPresent();
                  assertThat(gp.get().getPath()).isEqualTo(":subproject1");
              })
          ), text(
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              empty=
              """,
            spec -> spec
              .path("subproject2/gradle.lockfile")
              .afterRecipe(cu -> {
                  Optional<GradleProject> gp = cu.getMarkers().findFirst(GradleProject.class);
                  assertThat(gp).isPresent();
                  assertThat(gp.get().getPath()).isEqualTo(":subproject2");
              })
          ),
          buildGradle("", spec -> spec.path("subproject1/build.gradle")),
          buildGradle("", spec -> spec.path("subproject2/build.gradle"))
        );
    }

    @Test
    void multimoduleSubprojectGetsItsOwnGradleProjectOnGradle9() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi(CURRENT_GRADLE_DISTRIBUTION)),
          //language=groovy
          settingsGradle(
            """
              rootProject.name = 'root'
              include 'sub'
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec
              .path("build.gradle")
              .afterRecipe(cu -> {
                  Optional<GradleProject> gp = cu.getMarkers().findFirst(GradleProject.class);
                  assertThat(gp).isPresent();
                  assertThat(gp.get().getPath()).isEqualTo(":");
              })
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
            spec -> spec
              .path("sub/build.gradle")
              .afterRecipe(cu -> {
                  Optional<GradleProject> gp = cu.getMarkers().findFirst(GradleProject.class);
                  assertThat(gp).isPresent();
                  assertThat(gp.get().getPath()).isEqualTo(":sub");
                  assertThat(gp.get().getConfiguration("compileClasspath"))
                    .as("subproject's own configurations should be resolved")
                    .isNotNull();
              })
          )
        );
    }

    @Test
    void multimoduleResolvesAgainstBuildRoot() {
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi(CURRENT_GRADLE_DISTRIBUTION)),
          //language=groovy
          settingsGradle(
            """
              rootProject.name = 'test'
              include 'web'
              include 'ear'
              """
          ),
          buildGradle(
            """
              apply plugin: 'java'
              """,
            spec -> spec.path("web/build.gradle")
          ),
          buildGradle(
            """
              apply plugin: 'java'
              
              dependencies {
                implementation project(path: ':web', configuration: 'archives')
              }
              """,
            spec -> spec.path("ear/build.gradle")
          )
        );
    }
}
