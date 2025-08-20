/*
 * Copyright 2021 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class ChangeDependencyArtifactIdTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("org.springframework.boot", "spring-boot-starter", "new-starter", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter:2.5.4'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.springframework.boot:new-starter:2.5.4'
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void findDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "dewrite-core", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release'
                  api "org.openrewrite:rewrite-core:latest.release"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.openrewrite:dewrite-core:latest.release'
                  api "org.openrewrite:dewrite-core:latest.release"
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(cu.getMarkers().findFirst(GradleProject.class))
                .map(gp -> gp.getConfiguration("api"))
                .map(conf -> conf.findRequestedDependency("org.openrewrite", "dewrite-core"))
                .as("Requested dependency model should have been updated to have artifactId dewrite")
                .isPresent())
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void findMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "dewrite-core", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api group: 'org.openrewrite', name: 'dewrite-core', version: 'latest.release'
                  api group: "org.openrewrite", name: "dewrite-core", version: "latest.release"
              }
              """
          )
        );
    }

    @Test
    void worksWithoutVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("org.openrewrite", "rewrite-core", "rewrite-gradle", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))
                  api 'org.openrewrite:rewrite-core'
                  api "org.openrewrite:rewrite-core"
                  api group: 'org.openrewrite', name: 'rewrite-gradle'
                  api group: "org.openrewrite", name: "rewrite-gradle"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))
                  api 'org.openrewrite:rewrite-gradle'
                  api "org.openrewrite:rewrite-gradle"
                  api group: 'org.openrewrite', name: 'rewrite-gradle'
                  api group: "org.openrewrite", name: "rewrite-gradle"
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void worksWithClassifier(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "dewrite-core", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:tests'
                  api "org.openrewrite:rewrite-core:latest.release:tests"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'tests'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "tests"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.openrewrite:dewrite-core:latest.release:tests'
                  api "org.openrewrite:dewrite-core:latest.release:tests"
                  api group: 'org.openrewrite', name: 'dewrite-core', version: 'latest.release', classifier: 'tests'
                  api group: "org.openrewrite", name: "dewrite-core", version: "latest.release", classifier: "tests"
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.eclipse.jetty:jetty-servlet", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void worksWithExt(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "jetty-servlet-tests", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.eclipse.jetty:jetty-servlet@jar'
                  api "org.eclipse.jetty:jetty-servlet@jar"
                  api 'org.eclipse.jetty:jetty-servlet:9.4.50.v20221201@jar'
                  api "org.eclipse.jetty:jetty-servlet:9.4.50.v20221201@jar"
                  api 'org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@jar'
                  api "org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.50.v20221201', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.50.v20221201", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.50.v20221201', classifier: 'tests', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.50.v20221201", classifier: "tests", ext: "jar"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.eclipse.jetty:jetty-servlet-tests@jar'
                  api "org.eclipse.jetty:jetty-servlet-tests@jar"
                  api 'org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201@jar'
                  api "org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201@jar"
                  api 'org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201:tests@jar'
                  api "org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201:tests@jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet-tests', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet-tests", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet-tests', version: '9.4.50.v20221201', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet-tests", version: "9.4.50.v20221201", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet-tests', version: '9.4.50.v20221201', classifier: 'tests', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet-tests", version: "9.4.50.v20221201", classifier: "tests", ext: "jar"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3240")
    @Test
    void worksWithGString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("javax.validation", "validation-api", "jakarta.validation-api", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def jakartaVersion = "2.0.1.Final"
                  implementation "javax.validation:validation-api:${jakartaVersion}"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  def jakartaVersion = "2.0.1.Final"
                  implementation "javax.validation:jakarta.validation-api:${jakartaVersion}"
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3227")
    @Test
    void worksWithPlatform() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("org.optaplanner", "optaplanner-bom", "timefold-solver-bom", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("org.optaplanner:optaplanner-bom:9.37.0.Final")
                  implementation "org.optaplanner:optaplanner-core"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("org.optaplanner:timefold-solver-bom:9.37.0.Final")
                  implementation "org.optaplanner:optaplanner-core"
              }
              """
          )
        );
    }

    @Test
    void worksWithDependencyDefinedInJvmTestSuite() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("org.springframework.boot", "spring-boot-starter", "new-starter", "")),
          buildGradle(
            """
              plugins {
                  id "java-library"
                  id 'jvm-test-suite'
              }

              repositories {
                  mavenCentral()
              }

              testing {
                  suites {
                      test {
                          dependencies {
                              implementation 'org.springframework.boot:spring-boot-starter:2.5.4'
                          }
                      }
                  }
              }
              """,
            """
              plugins {
                  id "java-library"
                  id 'jvm-test-suite'
              }

              repositories {
                  mavenCentral()
              }

              testing {
                  suites {
                      test {
                          dependencies {
                              implementation 'org.springframework.boot:new-starter:2.5.4'
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void worksWithDependencyDefinedInBuildScript() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("org.springframework.boot", "spring-boot-starter", "new-starter", "")),
          buildGradle(
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath 'org.springframework.boot:spring-boot-starter:2.5.4'
                  }
              }
              """,
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath 'org.springframework.boot:new-starter:2.5.4'
                  }
              }
              """
          )
        );
    }

    @Test
    void dependenciesBlockInFreestandingScript() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("org.springframework.boot", "spring-boot-starter", "new-starter", null)),
          buildGradle(
            """
              repositories {
                  mavenLocal()
                  mavenCentral()
                  maven {
                     url = uri("https://central.sonatype.com/repository/maven-snapshots")
                  }
              }
              dependencies {
                  implementation("org.springframework.boot:spring-boot-starter:2.5.4")
              }
              """,
            """
              repositories {
                  mavenLocal()
                  mavenCentral()
                  maven {
                     url = uri("https://central.sonatype.com/repository/maven-snapshots")
                  }
              }
              dependencies {
                  implementation("org.springframework.boot:new-starter:2.5.4")
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              apply from: 'dependencies.gradle'
              """
          )
        );
    }
}
