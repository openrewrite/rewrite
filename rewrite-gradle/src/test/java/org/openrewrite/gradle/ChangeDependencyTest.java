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

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class ChangeDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Test
    void relocateDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null)),
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
    void changeGroupIdOnly() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", null, null, null, null)),
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
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", null, "commons-lang3", null, null, null)),
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
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null)),
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
          spec -> spec.recipe(new ChangeDependency("commons-lang", "commons-lang", "org.apache.commons", "commons-lang3", "3.11.x", null, null)),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              def version = '2.6'
              dependencies {
                  implementation platform("commons-lang:commons-lang:${version}")
              }
              """,
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }

              def version = '2.6'
              dependencies {
                  implementation platform("org.apache.commons:commons-lang3:3.11")
              }
              """
          )
        );
    }

    @Test
    void changeDependencyWithLowerVersionAfter() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("org.openrewrite", "plugin", "io.moderne", "moderne-gradle-plugin", "0.x", null, null)),
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
          spec -> spec.recipe(new ChangeDependency("mysql", "mysql-connector-java", "com.mysql", "mysql-connector-j", "8.0.x", null, null)),
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
              """)
        );
    }

    @Test
    void doNotPinWhenNotVersionedOnMap() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("mysql", "mysql-connector-java", "com.mysql", "mysql-connector-j", "8.0.x", null, null)),
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
              """)
        );
    }

    @Test
    void pinWhenOverrideManagedVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("mysql", "mysql-connector-java", "com.mysql", "mysql-connector-j", "8.0.x", null, true)),
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
              """)
        );
    }
}
