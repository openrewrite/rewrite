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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.util.GradleWrapper;
import org.openrewrite.test.RewriteTest;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.test.SourceSpecs.text;

@DisabledIf("org.openrewrite.gradle.marker.GradleProjectTest#gradleOlderThan8")
class AssertionsTest implements RewriteTest {

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
          spec -> spec.beforeRecipe(Assertions.withToolingApi(URI.create("https://artifactory.moderne.ninja/artifactory/gradle-distributions/gradle-8.6-bin.zip"))),
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
    void customInitScript() {
        //language=groovy
        String alternateInit = """
          initscript{
              repositories{
                  mavenLocal()
                  maven{ url = uri("https://central.sonatype.com/repository/maven-snapshots") }
                  mavenCentral()
              }

              configurations.all{
                  resolutionStrategy{
                      cacheChangingModulesFor 0, 'seconds'
                      cacheDynamicVersionsFor 0, 'seconds'
                  }
              }

              dependencies{
                  classpath 'org.openrewrite.gradle.tooling:plugin:latest.integration'
                  classpath 'org.openrewrite:rewrite-maven:latest.integration'
              }
          }

          allprojects{
              apply plugin: org.openrewrite.gradle.toolingapi.ToolingApiOpenRewriteModelPlugin
          }

          """;
        GradleWrapper gradleWrapper = GradleWrapper.create(URI.create("https://services.gradle.org/distributions/gradle-8.6-bin.zip"), null);
        rewriteRun(
          spec -> spec.beforeRecipe(Assertions.withToolingApi(gradleWrapper, alternateInit)),
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
}
