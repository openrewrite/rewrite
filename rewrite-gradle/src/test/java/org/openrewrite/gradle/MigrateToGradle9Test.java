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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.*;

class MigrateToGradle9Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.gradle.MigrateToGradle9");
    }

    @DocumentExample
    @Test
    void multipleSubRecipesApplyTogether() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'jacoco'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation group: 'com.google.guava', name: 'guava', version: '31.1-jre'
              }

              task doSomething(type: JavaExec) {
                  main = "com.example.AppMain"
              }

              tasks.register("runEverything", JavaExec) {
                  main = "com.example.AppMain"
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'jacoco'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "com.google.guava:guava:31.1-jre"
              }

              task doSomething(type: JavaExec) {
                  mainClass = "com.example.AppMain"
              }

              tasks.register("runEverything", JavaExec) {
                  mainClass = "com.example.AppMain"
              }
              """
          )
        );
    }

    @Test
    void useMainClassPropertyInKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java`
              }

              tasks.register<JavaExec>("doSomething") {
                  main = "com.example.AppMain"
              }
              """,
            """
              plugins {
                  `java`
              }

              tasks.register<JavaExec>("doSomething") {
                  mainClass = "com.example.AppMain"
              }
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void noChangeWhenAlreadyMigrated() {
            rewriteRun(
              buildGradle(
                """
                  plugins {
                      id 'java-library'
                  }

                  repositories {
                      mavenCentral()
                  }

                  dependencies {
                      api "org.openrewrite:rewrite-core:latest.release"
                  }
                  """
              )
            );
        }

        @Test
        void noChangeWhenAlreadyMigratedKotlinDsl() {
            rewriteRun(
              buildGradleKts(
                """
                  plugins {
                      `java-library`
                  }

                  repositories {
                      mavenCentral()
                  }

                  dependencies {
                      api("org.openrewrite:rewrite-core:latest.release")
                  }
                  """
              )
            );
        }
    }
}
