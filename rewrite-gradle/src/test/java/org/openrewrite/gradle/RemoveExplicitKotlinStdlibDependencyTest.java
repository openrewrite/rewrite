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
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class RemoveExplicitKotlinStdlibDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
                .recipeFromResources("org.openrewrite.gradle.RemoveExplicitKotlinStdlibDependency");
    }

    @DocumentExample("Remove the redundant Kotlin standard library declaration")
    @Test
    void removesStdlibWhenKotlinPluginApplied() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.0.21'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.0.21'
                  implementation 'com.google.guava:guava:33.0.0-jre'
              }
              """,
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.0.21'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
              }
              """
          )
        );
    }

    @Test
    void removesStdlibVariant() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.0.21'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.21'
              }
              """,
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.0.21'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
              }
              """
          )
        );
    }

    /**
     * The `kotlin("jvm")` shorthand names no plugin id in the script, so this only resolves through the
     * `GradleProject` marker, where Gradle has already expanded it to `org.jetbrains.kotlin.jvm`.
     */
    @Test
    void removesStdlibWhenPluginAppliedWithKotlinShorthand() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.0.21"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
                  implementation("com.google.guava:guava:33.0.0-jre")
              }
              """,
            """
              plugins {
                  kotlin("jvm") version "2.0.21"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.google.guava:guava:33.0.0-jre")
              }
              """
          )
        );
    }

    /**
     * The catalog entry names two spellings, and this is the other one: the Kotlin DSL `kotlin("stdlib")` helper,
     * which Gradle expands to `org.jetbrains.kotlin:kotlin-stdlib`, so it is the same dependency as the coordinate
     * form above.
     */
    @Test
    void removesStdlibDeclaredWithKotlinShorthand() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.0.21"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(kotlin("stdlib"))
                  implementation("com.google.guava:guava:33.0.0-jre")
              }
              """,
            """
              plugins {
                  kotlin("jvm") version "2.0.21"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.google.guava:guava:33.0.0-jre")
              }
              """
          )
        );
    }

    /**
     * The Kotlin plugin adds the standard library to `implementation` and nowhere else, so a declaration on a
     * configuration of the project's own is not redundant — removing it would take the standard library off that
     * configuration with nothing putting it back.
     */
    @Test
    void noChangeToACustomConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.0.21'
              }

              repositories {
                  mavenCentral()
              }

              configurations {
                  tooling
              }

              dependencies {
                  tooling 'org.jetbrains.kotlin:kotlin-stdlib:2.0.21'
              }
              """
          )
        );
    }

    /**
     * `api` is consumer-visible: the plugin's own contribution goes to `implementation`, so removing an `api`
     * declaration would drop the standard library out of the published compile scope for everyone downstream.
     */
    @Test
    void noChangeToAnApiDeclaration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.0.21'
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.jetbrains.kotlin:kotlin-stdlib:2.0.21'
              }
              """
          )
        );
    }

    /**
     * The precondition is load-bearing: a project with no Kotlin plugin that depends on the standard library is using
     * it as an ordinary library, and removing the declaration would stop it compiling. Before `FindPlugins` stopped
     * treating an unset `pluginClass` as a match for every plugin, this case was removed too.
     */
    @Test
    void noChangeWhenNoKotlinPluginApplied() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.0.21'
              }
              """
          )
        );
    }
}
