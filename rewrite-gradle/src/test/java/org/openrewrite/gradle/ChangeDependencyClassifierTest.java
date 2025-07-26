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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class ChangeDependencyClassifierTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.openrewrite", "*", "classified", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-gradle:latest.release:javadoc'
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
                  api 'org.openrewrite:rewrite-gradle:latest.release:classified'
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void findDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:javadoc'
                  api "org.openrewrite:rewrite-core:latest.release:javadoc"
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
                  api 'org.openrewrite:rewrite-core:latest.release:classified'
                  api "org.openrewrite:rewrite-core:latest.release:classified"
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void findMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc"
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
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified"
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void worksWithoutVersion(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
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
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'javadoc'
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'javadoc'
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
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'classified'
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'classified'
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void worksWithClassifier(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:javadoc'
                  api "org.openrewrite:rewrite-core:latest.release:javadoc"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc"
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
                  api 'org.openrewrite:rewrite-core:latest.release:classified'
                  api "org.openrewrite:rewrite-core:latest.release:classified"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified"
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void worksWithExt(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:javadoc@jar'
                  api "org.openrewrite:rewrite-core:latest.release:javadoc@jar"
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", classifier: "javadoc"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc', ext: 'jar'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc", ext: "jar"
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
                  api 'org.openrewrite:rewrite-core:latest.release:classified@jar'
                  api "org.openrewrite:rewrite-core:latest.release:classified@jar"
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", classifier: "classified"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified', ext: 'jar'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified", ext: "jar"
              }
              """
          )
        );
    }

    @Test
    void noPreviousClassifier_1() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.openrewrite", "*", "classified", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-gradle:latest.release'
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
                  api 'org.openrewrite:rewrite-gradle:latest.release:classified'
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void noPreviousClassifier_2(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
                  api(group: "org.openrewrite", name: "rewrite-core", version: "latest.release")
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
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified')
                  api(group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified")
              }
              """
          )
        );
    }

    @Test
    void noNewClassifier_1() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.openrewrite", "*", null, "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-gradle:latest.release:classified'
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
                  api 'org.openrewrite:rewrite-gradle:latest.release'
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void noNewClassifier_2(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, null, null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified')
                  api(group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified")
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
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
                  api(group: "org.openrewrite", name: "rewrite-core", version: "latest.release")
              }
              """
          )
        );
    }

    @Test
    void worksWithDependencyDefinedInJvmTestSuite() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.openrewrite", "*", "classified", "")),
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
                              implementation 'org.openrewrite:rewrite-gradle:latest.release:javadoc'
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
                              implementation 'org.openrewrite:rewrite-gradle:latest.release:classified'
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
          spec -> spec.recipe(new ChangeDependencyClassifier("org.openrewrite", "*", "classified", "")),
          buildGradle(
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath 'org.openrewrite:rewrite-gradle:latest.release:javadoc'
                  }
              }
              """,
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath 'org.openrewrite:rewrite-gradle:latest.release:classified'
                  }
              }
              """
          )
        );
    }

    @Test
    void dependenciesBlockInFreestandingScript() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.openrewrite", "*", "classified", "")),
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
                  implementation("org.openrewrite:rewrite-gradle:latest.release:javadoc")
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
                  implementation("org.openrewrite:rewrite-gradle:latest.release:classified")
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
