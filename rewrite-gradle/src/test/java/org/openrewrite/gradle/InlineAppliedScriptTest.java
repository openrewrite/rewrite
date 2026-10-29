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
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class InlineAppliedScriptTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new InlineAppliedScript(null));
    }

    @DocumentExample
    @Test
    void inlineDependenciesGradle() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencies.gradle'
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
              }
              """
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void preservesEvaluationOrder() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              // Shared dependency declarations
              apply from: 'dependencies.gradle'

              tasks.named('test') {
                  useJUnitPlatform()
              }
              """,
            """
              plugins {
                  id 'java'
              }

              // Shared dependency declarations
              ext {
                  guavaVersion = '33.0.0-jre'
              }

              dependencies {
                  implementation "com.google.guava:guava:$guavaVersion"
              }

              tasks.named('test') {
                  useJUnitPlatform()
              }
              """
          ),
          buildGradle(
            """
              ext {
                  guavaVersion = '33.0.0-jre'
              }

              dependencies {
                  implementation "com.google.guava:guava:$guavaVersion"
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void movesImportsToTop() {
        rewriteRun(
          buildGradle(
            """
              import java.time.Duration

              plugins {
                  id 'java'
              }

              apply from: 'dependencies.gradle'
              """,
            """
              import java.time.Duration
              import java.time.LocalDate

              plugins {
                  id 'java'
              }

              dependencies {
                  implementation "org.example:built:${LocalDate.now().year}"
              }
              """
          ),
          buildGradle(
            """
              import java.time.Duration
              import java.time.LocalDate

              dependencies {
                  implementation "org.example:built:${LocalDate.now().year}"
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void resolvesPathForms() {
        rewriteRun(
          settingsGradle(
            """
              include 'app'
              """
          ),
          buildGradle(
            """
              apply from: "$rootDir/gradle/repositories.gradle"
              apply from: file('dependencies.gradle')
              apply from: rootProject.file('gradle/versions.gradle')
              """,
            """
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              ext.junitVersion = '5.10.2'
              """,
            spec -> spec.path("app/build.gradle")
          ),
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }
              """,
            null,
            spec -> spec.path("gradle/repositories.gradle")
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            null,
            spec -> spec.path("app/dependencies.gradle")
          ),
          buildGradle(
            """
              ext.junitVersion = '5.10.2'
              """,
            null,
            spec -> spec.path("gradle/versions.gradle")
          )
        );
    }

    @Test
    void inlinesNestedApplyFrom() {
        rewriteRun(
          buildGradle(
            """
              apply from: 'dependencies.gradle'
              """,
            """
              ext.slf4jVersion = '2.0.13'
              dependencies {
                  implementation "org.slf4j:slf4j-api:$slf4jVersion"
              }
              """
          ),
          buildGradle(
            """
              apply from: 'gradle/versions.gradle'
              dependencies {
                  implementation "org.slf4j:slf4j-api:$slf4jVersion"
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              ext.slf4jVersion = '2.0.13'
              """,
            null,
            spec -> spec.path("gradle/versions.gradle")
          )
        );
    }

    @Test
    void doesNotInlineUnappliedScript() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void sharedScriptLeftAlone() {
        rewriteRun(
          buildGradle(
            """
              apply from: "$rootDir/gradle/common.gradle"
              """,
            spec -> spec.path("a/build.gradle")
          ),
          buildGradle(
            """
              apply from: "$rootDir/gradle/common.gradle"
              """,
            spec -> spec.path("b/build.gradle")
          ),
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }
              """,
            spec -> spec.path("gradle/common.gradle")
          )
        );
    }

    @Test
    void doesNotInlineInsideClosure() {
        rewriteRun(
          buildGradle(
            """
              subprojects {
                  apply from: 'dependencies.gradle'
              }
              """
          ),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              apply plugin: 'java'
              """,
            spec -> spec.path("sub/build.gradle")
          ),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("sub/dependencies.gradle")
          )
        );
    }

    @Test
    void doesNotInlineWithTo() {
        rewriteRun(
          buildGradle(
            """
              apply from: 'dependencies.gradle', to: project
              """
          ),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void doesNotInlineUnresolvablePath() {
        rewriteRun(
          buildGradle(
            """
              def name = 'dependencies'
              apply from: "${name}.gradle"
              apply from: 'https://example.com/dependencies.gradle'
              """
          ),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void doesNotInlineScriptWithTopLevelReturn() {
        rewriteRun(
          buildGradle(
            """
              apply from: 'dependencies.gradle'
              """
          ),
          buildGradle(
            """
              if (!project.hasProperty('withDeps')) {
                  return
              }
              dependencies {
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void inlinesScriptWithReturnInsideClosure() {
        rewriteRun(
          buildGradle(
            """
              apply from: 'dependencies.gradle'
              """,
            """
              configurations.all {
                  if (name == 'skip') {
                      return
                  }
              }
              """
          ),
          buildGradle(
            """
              configurations.all {
                  if (name == 'skip') {
                      return
                  }
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void doesNotInlineScriptWithBuildscriptBlock() {
        rewriteRun(
          buildGradle(
            """
              apply from: 'dependencies.gradle'
              """
          ),
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void doesNotInlineCollidingDeclarations() {
        rewriteRun(
          buildGradle(
            """
              def version = '1.0'
              apply from: 'dependencies.gradle'
              """
          ),
          buildGradle(
            """
              def version = '2.0'
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void doesNotInlineAcrossDsls() {
        rewriteRun(
          buildGradleKts(
            """
              apply(from = "dependencies.gradle")
              """
          ),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void inlineKotlinScript() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              apply(from = "dependencies.gradle.kts")
              """,
            """
              plugins {
                  `java-library`
              }

              dependencies {
                  "implementation"("org.slf4j:slf4j-api:2.0.13")
              }
              """
          ),
          buildGradleKts(
            """
              dependencies {
                  "implementation"("org.slf4j:slf4j-api:2.0.13")
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle.kts")
          )
        );
    }

    @Test
    void pluginAppendsScriptsInProjectDirectory() {
        rewriteRun(
          spec -> spec.recipe(new InlineAppliedScript("com.example.dependencies")),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'com.example.dependencies' version '1.0'
              }

              tasks.named('test') {
                  useJUnitPlatform()
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'com.example.dependencies' version '1.0'
              }

              tasks.named('test') {
                  useJUnitPlatform()
              }

              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }

              repositories {
                  mavenCentral()
              }
              """
          ),
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }
              """,
            null,
            spec -> spec.path("repositories.gradle")
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              ext.slf4jVersion = '2.0.13'
              """,
            spec -> spec.path("gradle/versions.gradle")
          )
        );
    }

    @Test
    void pluginAppliedWithApplyPlugin() {
        rewriteRun(
          spec -> spec.recipe(new InlineAppliedScript("com.example.dependencies")),
          buildGradle(
            """
              apply plugin: 'com.example.dependencies'
              """,
            """
              apply plugin: 'com.example.dependencies'

              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void pluginAppliedFromElsewhere() {
        rewriteRun(
          spec -> spec.recipe(new InlineAppliedScript("com.example.dependencies")),
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }
              """,
            """
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            spec -> spec.path("app/build.gradle").markers(GradleProject.builder()
              .name("app")
              .path(":app")
              .plugins(singletonList(new GradlePluginDescriptor("com.example.DependenciesPlugin", "com.example.dependencies")))
              .build())
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            null,
            spec -> spec.path("app/dependencies.gradle")
          )
        );
    }

    @Test
    void pluginAndApplyFrom() {
        rewriteRun(
          spec -> spec.recipe(new InlineAppliedScript("com.example.dependencies")),
          buildGradle(
            """
              plugins {
                  id 'com.example.dependencies' version '1.0'
              }

              apply from: 'publishing.gradle'

              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id 'com.example.dependencies' version '1.0'
              }

              apply plugin: 'maven-publish'

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """
          ),
          buildGradle(
            """
              apply plugin: 'maven-publish'
              """,
            null,
            spec -> spec.path("publishing.gradle")
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void pluginNotApplied() {
        rewriteRun(
          spec -> spec.recipe(new InlineAppliedScript("com.example.dependencies")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.slf4j:slf4j-api:2.0.13'
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void pluginAppendsKotlinScript() {
        rewriteRun(
          spec -> spec.recipe(new InlineAppliedScript("com.example.dependencies")),
          buildGradleKts(
            """
              plugins {
                  id("com.example.dependencies") version "1.0"
              }

              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id("com.example.dependencies") version "1.0"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  "implementation"("org.slf4j:slf4j-api:2.0.13")
              }
              """
          ),
          buildGradleKts(
            """
              dependencies {
                  "implementation"("org.slf4j:slf4j-api:2.0.13")
              }
              """,
            null,
            spec -> spec.path("dependencies.gradle.kts")
          )
        );
    }
}
