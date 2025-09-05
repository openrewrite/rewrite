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
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;

class ChangeDependencyConfigurationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Test
    void changeConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "*", "implementation", "")),
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
                  implementation 'org.openrewrite:rewrite-gradle:latest.release'
              }
              """
          )
        );
    }

    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "*", "implementation", "")),
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
                  implementation 'org.openrewrite:rewrite-gradle:latest.release'
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void changeStringStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
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
                  implementation 'org.openrewrite:rewrite-core:latest.release'
                  implementation "org.openrewrite:rewrite-core:latest.release"
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void changeMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
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
                  implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  implementation group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
              }
              """
          )
        );
    }

    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void changeGStringStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def version = "latest.release"
              dependencies {
                  api "org.openrewrite:rewrite-core:${version}"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def version = "latest.release"
              dependencies {
                  implementation "org.openrewrite:rewrite-core:${version}"
              }
              """
          )
        );
    }

    @CsvSource(value = {"*:project2", "*:*"}, delimiterString = ":")
    @ParameterizedTest
    void worksForProjectDependencies(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
          mavenProject("root",
            buildGradle(
              ""
            ),
            settingsGradle(
              """
                include "project1"
                include "project2"
                """
            ),
            mavenProject("project1",
              buildGradle(
                """
                  plugins {
                    id 'java-library'
                  }
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      api project(":project2")
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
                      implementation project(":project2")
                  }
                  """
              )
            ),
            mavenProject("project2",
              buildGradle(
                ""
              )
            )
          )
        );
    }

    @Test
    void onlyChangeSpecificDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "rewrite-core", "implementation", null)),
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
                  testImplementation group: "org.openrewrite", name: "rewrite-test", version: "latest.release"
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
                  implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  testImplementation group: "org.openrewrite", name: "rewrite-test", version: "latest.release"
              }
              """
          )
        );
    }

    @Test
    void worksWithDependencyDefinedInJvmTestSuite() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "*", "implementation", "")),
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
                              runtimeOnly 'org.openrewrite:rewrite-gradle:latest.release'
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
                              implementation 'org.openrewrite:rewrite-gradle:latest.release'
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void dependenciesBlockInFreestandingScript() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "*", "implementation", "")),
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
                  runtimeOnly 'org.openrewrite:rewrite-gradle:latest.release'
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
                  implementation 'org.openrewrite:rewrite-gradle:latest.release'
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

    @Test
    void onlyIfConfigurationMatches() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("*", "*", "testImplementation", "testRuntimeOnly")),
          mavenProject("root",
            settingsGradle(
              """
                rootProject.name = "root"

                include "project1"
                include "project2"
                """
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                repositories {
                    mavenCentral()
                }
                dependencies {
                    implementation 'org.openrewrite:rewrite-maven:latest.release'
                    implementation project(':project1')
                    testRuntimeOnly 'org.openrewrite:rewrite-gradle:latest.release'
                    testRuntimeOnly project(':project2')
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
                    implementation 'org.openrewrite:rewrite-maven:latest.release'
                    implementation project(':project1')
                    testImplementation 'org.openrewrite:rewrite-gradle:latest.release'
                    testImplementation project(':project2')
                }
                """
            ),
            mavenProject("project1",
              buildGradle(
                """
                  plugins {
                      id "java"
                  }
                  """
              )
            ),
            mavenProject("project2",
              buildGradle(
                """
                  plugins {
                      id "java"
                  }
                  """
              )
            )
          )
        );
    }
}
