/*
 * Copyright 2025 the original author or authors.
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

import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.toml.Assertions.toml;

class MigrateDependenciesToVersionCatalogKotlinDSLTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new MigrateDependenciesToVersionCatalog());
    }

    @Test
    void doesNotProcessFilesExcludedByPreconditions() {
        rewriteRun(
          java(
            """
              package com.example;

              public class Example {
                  public void method() {
                      String dep = "org.springframework:spring-core:5.3.0";
                  }
              }
              """
          )
        );
    }

    @Test
    @DocumentExample
    void migrateStringNotationDependencies() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.springframework:spring-core:5.3.0")
                  testImplementation("junit:junit:4.13.2")
                  runtimeOnly("com.h2database:h2:1.4.200")
              }
              """,
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(libs.springCore)
                  testImplementation(libs.junit)
                  runtimeOnly(libs.h2)
              }
              """,
            spec -> spec.path("build.gradle.kts")
          ),
          toml(
            doesNotExist(),
            """
              [versions]
              spring-core = "5.3.0"
              junit = "4.13.2"
              h2 = "1.4.200"

              [libraries]
              spring-core = { group = "org.springframework", name = "spring-core", version.ref = "spring-core" }
              junit = { group = "junit", name = "junit", version.ref = "junit" }
              h2 = { group = "com.h2database", name = "h2", version.ref = "h2" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void migrateMapNotationDependencies() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.12.0")
                  testImplementation(group = "org.mockito", name = "mockito-core", version = "4.6.1")
              }
              """,
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(libs.commonsLang3)
                  testImplementation(libs.mockitoCore)
              }
              """,
            spec -> spec.path("build.gradle.kts")
          ),
          toml(
            doesNotExist(),
            """
              [versions]
              commons-lang3 = "3.12.0"
              mockito-core = "4.6.1"

              [libraries]
              commons-lang3 = { group = "org.apache.commons", name = "commons-lang3", version.ref = "commons-lang3" }
              mockito-core = { group = "org.mockito", name = "mockito-core", version.ref = "mockito-core" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void doNotMigrateProjectDependencies() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = "test"
              include("core")
              include("test-utils")
              """,
            spec -> spec.path("settings.gradle.kts")
          ),
          buildGradleKts(
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(project(":core"))
                  implementation("org.springframework:spring-core:5.3.0")
                  testImplementation(project(":test-utils"))
              }
              """,
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(project(":core"))
                  implementation(libs.springCore)
                  testImplementation(project(":test-utils"))
              }
              """,
            spec -> spec.path("build.gradle.kts")
          ),
          buildGradleKts(
            """
              plugins {
                  id("java")
              }
              """,
            spec -> spec.path("core/build.gradle.kts")
          ),
          buildGradleKts(
            """
              plugins {
                  id("java")
              }
              """,
            spec -> spec.path("test-utils/build.gradle.kts")
          ),
          toml(
            doesNotExist(),
            """
              [versions]
              spring-core = "5.3.0"

              [libraries]
              spring-core = { group = "org.springframework", name = "spring-core", version.ref = "spring-core" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void handleMixedNotations() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.springframework:spring-core:5.3.0")
                  implementation(group = "org.springframework", name = "spring-context", version = "5.3.0")
                  implementation("org.springframework:spring-web:5.3.0") {
                      exclude(group = "commons-logging")
                  }
              }
              """,
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(libs.springCore)
                  implementation(libs.springContext)
                  implementation(libs.springWeb) {
                      exclude(group = "commons-logging")
                  }
              }
              """,
            spec -> spec.path("build.gradle.kts")
          ),
          toml(
            doesNotExist(),
            """
              [versions]
              spring-core = "5.3.0"
              spring-context = "5.3.0"
              spring-web = "5.3.0"

              [libraries]
              spring-core = { group = "org.springframework", name = "spring-core", version.ref = "spring-core" }
              spring-context = { group = "org.springframework", name = "spring-context", version.ref = "spring-context" }
              spring-web = { group = "org.springframework", name = "spring-web", version.ref = "spring-web" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void migrateDependenciesWithVersionsFromGradleProperties() {
        rewriteRun(
          properties(
            """
              junitVersion=4.13.2
              mockitoVersion=4.6.1
              unrelatedProperty=someValue
              """,
            """
              unrelatedProperty=someValue
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradleKts(
            """
              val junitVersion: String by project
              val mockitoVersion: String by project

              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  testImplementation("junit:junit:$junitVersion")
                  testImplementation(group = "org.mockito", name = "mockito-core", version = mockitoVersion)
              }
              """,
            """


              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  testImplementation(libs.junit)
                  testImplementation(libs.mockitoCore)
              }
              """,
            spec -> spec.path("build.gradle.kts")
          ),
          toml(
            doesNotExist(),
            """
              [versions]
              junit = "4.13.2"
              mockito-core = "4.6.1"

              [libraries]
              junit = { group = "junit", name = "junit", version.ref = "junit" }
              mockito-core = { group = "org.mockito", name = "mockito-core", version.ref = "mockito-core" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    /**
     * When a version catalog already exists, the recipe will not modify it.
     * This is the current behavior - merging is not yet implemented.
     */
    @Test
    void whenVersionCatalogAlreadyExists() {
        rewriteRun(
          toml(
            """
              [versions]
              guava-version = "31.0-jre"

              [libraries]
              guava = { group = "com.google.guava", name = "guava", version.ref = "guava-version" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          ),
          buildGradleKts(
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.springframework:spring-core:5.3.0")
                  testImplementation("junit:junit:4.13.2")
              }
              """,
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(libs.springCore)
                  testImplementation(libs.junit)
              }
              """,
            spec -> spec.path("build.gradle.kts")
          )
        );
    }
}
