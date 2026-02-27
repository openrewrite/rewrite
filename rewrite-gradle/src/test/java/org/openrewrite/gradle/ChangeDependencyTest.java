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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;

class ChangeDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Test
    void relocateDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null, true)),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "commons-lang:commons-lang:2.6"
                  implementation group: "commons-lang", name: "commons-lang", version: "2.6"
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "org.apache.commons:commons-lang3:3.11"
                  implementation group: "org.apache.commons", name: "commons-lang3", version: "3.11"
              }
              """
          )
        );
    }

    @Test
    void removeIfExists() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeDependency(
              "javax.activation",
              "javax.activation-api",
              "jakarta.activation",
              "jakarta.activation-api",
              "1.2.X",
              null,
              null,
              true
            ),
            new ChangeDependency(
              "com.google.guava",
              "guava",
              "jakarta.activation",
              "jakarta.activation-api",
              "1.2.X",
              null,
              null,
              true
            )
          ),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "com.google.guava:guava:23.0"
                  implementation group: "javax.activation", name: "javax.activation-api", version: "1.2.0"
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation group: "jakarta.activation", name: "jakarta.activation-api", version: "1.2.2"
              }
              """
          )
        );
    }
    @Test
    void changeGroupIdOnly() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", null, null, null, null, true)),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "commons-lang:commons-lang:2.6"
                  implementation group: "commons-lang", name: "commons-lang", version: "2.6"
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "org.apache.commons:commons-lang:2.6"
                  implementation group: "org.apache.commons", name: "commons-lang", version: "2.6"
              }
              """
          )
        );
    }

    @Test
    void changeArtifactIdOnly() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", null, "commons-lang3", null, null, null, true)),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "commons-lang:commons-lang:2.6"
                  implementation group: "commons-lang", name: "commons-lang", version: "2.6"
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "commons-lang:commons-lang3:2.6"
                  implementation group: "commons-lang", name: "commons-lang3", version: "2.6"
              }
              """
          )
        );
    }

    @Test
    void worksWithPlatform() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null, true)),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("commons-lang:commons-lang:2.6")
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("org.apache.commons:commons-lang3:3.11")
              }
              """
          )
        );
    }

    @Test
    void worksWithGString() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null, true)),
          properties(
            """
              commonsLangVersion=2.6
              """,
            """
              commonsLangVersion=3.11
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("commons-lang:commons-lang:${commonsLangVersion}")
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("org.apache.commons:commons-lang3:${commonsLangVersion}")
              }
              """
          )
        );
    }

    @Test
    void changeDependencyWithLowerVersionAfter() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("org.openrewrite", "plugin", "io.moderne", "moderne-gradle-plugin", "0.x", null, null, true)),
          buildGradle(
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath "org.openrewrite:plugin:6.0.0"
                      classpath group: "org.openrewrite", name: "plugin", version: "6.0.0"
                  }
              }
              """,
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath "io.moderne:moderne-gradle-plugin:0.39.0"
                      classpath group: "io.moderne", name: "moderne-gradle-plugin", version: "0.39.0"
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotPinWhenNotVersioned() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("mysql", "mysql-connector-java", "com.mysql", "mysql-connector-j", "8.0.x", null, null, true)),
          buildGradle(
            """
              plugins {
                id 'java'
                id 'org.springframework.boot' version '2.6.1'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }

              repositories {
                 mavenCentral()
              }

              dependencies {
                  runtimeOnly 'mysql:mysql-connector-java'
              }
              """,
            """
              plugins {
                id 'java'
                id 'org.springframework.boot' version '2.6.1'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }

              repositories {
                 mavenCentral()
              }

              dependencies {
                  runtimeOnly 'com.mysql:mysql-connector-j'
              }
              """
          )
        );
    }

    @Test
    void doNotPinWhenNotVersionedOnMap() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("mysql", "mysql-connector-java", "com.mysql", "mysql-connector-j", "8.0.x", null, null, true)),
          buildGradle(
            """
              plugins {
                id 'java'
                id 'org.springframework.boot' version '2.6.1'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }

              repositories {
                 mavenCentral()
              }

              dependencies {
                  runtimeOnly group: 'mysql', name: 'mysql-connector-java'
              }
              """,
            """
              plugins {
                id 'java'
                id 'org.springframework.boot' version '2.6.1'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }

              repositories {
                 mavenCentral()
              }

              dependencies {
                  runtimeOnly group: 'com.mysql', name: 'mysql-connector-j'
              }
              """
          )
        );
    }

    @Test
    void pinWhenOverrideManagedVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("mysql", "mysql-connector-java", "com.mysql", "mysql-connector-j", "8.0.x", null, true, true)),
          buildGradle(
            """
              plugins {
                id 'java'
                id 'org.springframework.boot' version '2.6.1'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }

              repositories {
                 mavenCentral()
              }

              dependencies {
                  runtimeOnly 'mysql:mysql-connector-java'
              }
              """,
            """
              plugins {
                id 'java'
                id 'org.springframework.boot' version '2.6.1'
                id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }

              repositories {
                 mavenCentral()
              }

              dependencies {
                  runtimeOnly 'com.mysql:mysql-connector-j:8.0.33'
              }
              """
          )
        );
    }

    @Test
    void warPluginProvidedConfigurations() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null, true)),
          buildGradle(
            """
              plugins {
                  id "war"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  providedCompile "commons-lang:commons-lang:2.6"
                  providedRuntime "commons-lang:commons-lang:2.6"
                  implementation group: "commons-lang", name: "commons-lang", version: "2.6"
              }
              """,
            """
              plugins {
                  id "war"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  providedCompile "org.apache.commons:commons-lang3:3.11"
                  providedRuntime "org.apache.commons:commons-lang3:3.11"
                  implementation group: "org.apache.commons", name: "commons-lang3", version: "3.11"
              }
              """
          )
        );
    }

    @Test
    void relocateDependencyInJvmTestSuite() {
        rewriteRun(
            spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null, true)),
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
                                  implementation "commons-lang:commons-lang:2.6"
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
                                  implementation "org.apache.commons:commons-lang3:3.11"
                              }
                          }
                      }
                  }
                  """
            )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null, true)),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("commons-lang:commons-lang:2.6")
                  implementation(group = "commons-lang", name = "commons-lang", version = "2.6")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.commons:commons-lang3:3.11")
                  implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.11")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslStringInterpolation() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null, true)),
          properties(
            """
              commonsLangVersion=2.6
              """,
            """
              commonsLangVersion=3.11
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              val commonsLangVersion: String by project
              dependencies {
                  implementation("commons-lang:commons-lang:${commonsLangVersion}")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              val commonsLangVersion: String by project
              dependencies {
                  implementation("org.apache.commons:commons-lang3:${commonsLangVersion}")
              }
              """
          )
        );
    }

    @Test
    void dependencyPluginManagedDependencies() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("javax.validation", "validation-api", "jakarta.validation", "jakarta.validation-api", "3.0.x", null, null, true)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("javax.validation:validation-api")
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:validation-api:2.0.1.Final'
                      dependency group: 'javax.validation', name: 'validation-api', version: '2.0.1.Final'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("jakarta.validation:jakarta.validation-api")
              }

              dependencyManagement {
                  dependencies {
                      dependency 'jakarta.validation:jakarta.validation-api:3.0.2'
                      dependency group: 'jakarta.validation', name: 'jakarta.validation-api', version: '3.0.2'
                      dependencySet('jakarta.validation:3.0.2') {
                          entry 'jakarta.validation-api'
                      }
                      dependencySet(group:'jakarta.validation', version: '3.0.2') {
                          entry 'jakarta.validation-api'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDependencyPluginManagedDependencies() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("javax.validation", "validation-api", "jakarta.validation", "jakarta.validation-api", "3.0.x", null, null, true)),
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("javax.validation:validation-api")
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "validation-api", "version" to "2.0.1.Final"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                      }
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("jakarta.validation:jakarta.validation-api")
              }

              dependencyManagement {
                  dependencies {
                      dependency("jakarta.validation:jakarta.validation-api:3.0.2")
                      dependency(mapOf("group" to "jakarta.validation", "name" to "jakarta.validation-api", "version" to "3.0.2"))
                      dependencySet("jakarta.validation:3.0.2") {
                          entry("jakarta.validation-api")
                      }
                      dependencySet(mapOf("group" to "jakarta.validation", "version" to "3.0.2")) {
                          entry("jakarta.validation-api")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void makeChangesInDependencyManagementImports() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("io.moderne.recipe", "*", "org.openrewrite", "rewrite-core", "8.44.1", null, null, true)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("io.moderne.recipe:rewrite-spring")
              }
              dependencyManagement {
                  imports {
                      mavenBom "io.moderne.recipe:moderne-recipe-bom:0.13.0"
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform("org.openrewrite:rewrite-core")
              }
              dependencyManagement {
                  imports {
                      mavenBom "org.openrewrite:rewrite-core:8.44.1"
                  }
              }
              """
          )
        );
    }

    @Test
    void makeChangesInKotlinDependencyManagementImports() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("io.moderne.recipe", "*", "org.openrewrite", "rewrite-core", "8.44.1", null, null, true)),
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("io.moderne.recipe:rewrite-spring"))
              }
              dependencyManagement {
                  imports {
                      mavenBom("io.moderne.recipe:moderne-recipe-bom:0.13.0")
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.openrewrite:rewrite-core"))
              }
              dependencyManagement {
                  imports {
                      mavenBom("org.openrewrite:rewrite-core:8.44.1")
                  }
              }
              """
          )
        );
    }

    @Test
    void noDuplicateJacksonDatabindDependenciesInGradle() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipes(
              new ChangeDependency(
                "com.fasterxml.jackson.core",
                "jackson-databind",
                "tools.jackson.core",
                null,
                "3.0.x",
                null,
                null,
                null
              ),
              new ChangeDependency(
                "com.fasterxml.jackson.datatype",
                "jackson-datatype-jsr310",
                "tools.jackson.core",
                "jackson-databind",
                "3.0.x",
                null,
                null,
                null
              )
            ),
          buildGradle(
            //language=gradle
            """
              plugins {
                  id("java-library")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
                  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
              }
              """,
            spec -> spec.after(buildGradle ->
              assertThat(buildGradle)
                .containsOnlyOnce("tools.jackson.core:jackson-databind:3")
                .doesNotContain("datatype")
                .actual())
          )
        );
    }
}
