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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class MigrateToGradle9Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.gradle.MigrateToGradle9");
    }

    @Test
    void convertMapNotationToStringNotation() {
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
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
                  implementation group: 'com.google.guava', name: 'guava', version: '31.1-jre'
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
                  api("org.openrewrite:rewrite-core:latest.release")
                  implementation "com.google.guava:guava:31.1-jre"
              }
              """
          )
        );
    }

    @Test
    void jacocoReportDeprecations() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.enabled = false
                      csv.enabled = true
                      html.destination = layout.buildDirectory.dir('jacocoHtml')
                  }
              }
              """,
            """
              plugins {
                  id "java"
                  id "jacoco"
              }

              jacocoTestReport {
                  reports {
                      xml.required = false
                      csv.required = true
                      html.outputLocation = layout.buildDirectory.dir('jacocoHtml')
                  }
              }
              """
          )
        );
    }

    @Test
    void removeDeprecatedFeaturePreviews() {
        rewriteRun(
          settingsGradle(
            """
              rootProject.name = 'my-project'
              enableFeaturePreview('ONE_LOCKFILE_PER_PROJECT')
              enableFeaturePreview('VERSION_ORDERING_V2')
              enableFeaturePreview('VERSION_CATALOGS')
              """,
            """
              rootProject.name = 'my-project'
              """
          )
        );
    }

    @Test
    void removesSingleDeprecatedFeaturePreview() {
        rewriteRun(
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = 'my-project'
              enableFeaturePreview('ONE_LOCKFILE_PER_PROJECT')

              include 'subproject-a'
              """,
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = 'my-project'

              include 'subproject-a'
              """
          )
        );
    }

    @Test
    void jacocoReportDeprecationsKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
                  jacoco
              }

              tasks.jacocoTestReport {
                  reports {
                      xml.isEnabled = false
                      html.isEnabled(true)
                      html.destination = layout.buildDirectory.dir("jacocoHtml")
                  }
              }
              """,
            """
              plugins {
                  java
                  jacoco
              }

              tasks.jacocoTestReport {
                  reports {
                      xml.required = false
                      html.required = true
                      html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
                  }
              }
              """
          )
        );
    }

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

    @Test
    void noChangeWhenJacocoAlreadyMigratedKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
                  jacoco
              }

              tasks.jacocoTestReport {
                  reports {
                      xml.required = false
                      html.required = true
                      html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
                  }
              }
              """
          )
        );
    }

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

              jacocoTestReport {
                  reports {
                      xml.enabled = true
                  }
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

              jacocoTestReport {
                  reports {
                      xml.required = true
                  }
              }
              """
          )
        );
    }
}
