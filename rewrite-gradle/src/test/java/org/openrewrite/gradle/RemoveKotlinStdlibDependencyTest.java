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

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.properties.Assertions.properties;

class RemoveKotlinStdlibDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveKotlinStdlibDependency());
    }

    @DocumentExample
    @Test
    void kotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.2.0"
              }

              dependencies {
                  implementation(kotlin("stdlib"))
                  implementation("com.google.guava:guava:33.4.8-jre")
              }
              """,
            """
              plugins {
                  kotlin("jvm") version "2.2.0"
              }

              dependencies {
                  implementation("com.google.guava:guava:33.4.8-jre")
              }
              """
          )
        );
    }

    @Test
    void groovyDslWithPluginVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.2.0'
              }

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.2.0'
                  implementation 'com.google.guava:guava:33.4.8-jre'
              }
              """,
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.2.0'
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.4.8-jre'
              }
              """
          )
        );
    }

    @Test
    void olderVersionThanPlugin() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.2.0"
              }

              dependencies {
                  implementation(kotlin("stdlib", "2.0.0"))
                  implementation("com.google.guava:guava:33.4.8-jre")
              }
              """,
            """
              plugins {
                  kotlin("jvm") version "2.2.0"
              }

              dependencies {
                  implementation("com.google.guava:guava:33.4.8-jre")
              }
              """
          )
        );
    }

    @Test
    void keepNewerVersionThanPlugin() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.0.0"
              }

              dependencies {
                  implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
              }
              """
          )
        );
    }

    @Test
    void keepVersionWhenPluginVersionIsUnknown() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm'
              }

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.2.0'
              }
              """
          )
        );
    }

    @Test
    void versionlessWhenPluginVersionIsUnknown() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: 'kotlin'

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                  testImplementation 'junit:junit:4.13.2'
              }
              """,
            """
              apply plugin: 'kotlin'

              dependencies {
                  testImplementation 'junit:junit:4.13.2'
              }
              """
          )
        );
    }

    @Test
    void keepWithoutKotlinPlugin() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.2.0'
              }
              """
          )
        );
    }

    @Test
    void keepWhenPluginPredatesDefaultDependency() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '1.3.72'
              }

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib'
              }
              """
          )
        );
    }

    @Test
    void keepWhenDefaultDependencyIsDisabled() {
        rewriteRun(
          properties(
            """
              kotlin.stdlib.default.dependency=false
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.2.0"
              }

              dependencies {
                  implementation(kotlin("stdlib"))
              }
              """,
            spec -> spec.path("app/build.gradle.kts")
          )
        );
    }

    @Test
    void keepDependencyConstraintsAndExclusions() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.2.0"
              }

              dependencies {
                  constraints {
                      implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
                  }
                  implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0") {
                      exclude(group = "org.jetbrains", module = "annotations")
                  }
              }
              """
          )
        );
    }
}
