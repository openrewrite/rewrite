/*
 * Copyright 2022 the original author or authors.
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
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

/**
 * Test cases for UpgradeDependencyVersion with Spring Dependency Management plugin BOM imports.
 */
class UpgradeDependencyVersionBomTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new UpgradeDependencyVersion("com.google.cloud", "spring-cloud-gcp-dependencies", "7.2.x", null));
    }

    /**
     * This test demonstrates that direct 'implementation' dependencies work correctly.
     */
    @Test
    void upgradesDirectImplementationDependency() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  implementation 'com.google.cloud:spring-cloud-gcp-dependencies:7.1.0'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
                  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  implementation 'com.google.cloud:spring-cloud-gcp-dependencies:7.2.0'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
                  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
              }
              """
          )
        );
    }

    /**
     * This test verifies that direct dependencies with property-based versions work correctly.
     */
    @Test
    void upgradesDirectImplementationDependencyWithProperty() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext {
                  springCloudGcpVersion = '7.1.0'
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  implementation "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
                  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext {
                  springCloudGcpVersion = '7.2.0'
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  implementation "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
                  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
              }
              """
          )
        );
    }

    /**
     * This test verifies that direct dependencies with set() syntax for properties work correctly.
     */
    @Test
    void upgradesDirectImplementationDependencyWithSetSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext.set("springCloudGcpVersion", "7.1.0")

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  implementation "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
                  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext.set("springCloudGcpVersion", "7.2.0")

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  implementation "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
                  testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
              }
              """
          )
        );
    }

    /**
     * Verifies that BOM imports with literal versions are upgraded correctly.
     */
    @Test
    void upgradesBomImportWithLiteralVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
                  id 'io.spring.dependency-management' version '1.1.0'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'com.google.cloud:spring-cloud-gcp-dependencies:7.1.0'
                  }
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
                  id 'io.spring.dependency-management' version '1.1.0'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'com.google.cloud:spring-cloud-gcp-dependencies:7.2.0'
                  }
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """
          )
        );
    }

    /**
     * Verifies that BOM imports with property-based versions are upgraded correctly.
     */
    @Test
    void upgradesBomImportWithInterpolatedString() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
                  id 'io.spring.dependency-management' version '1.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext {
                  springCloudGcpVersion = '7.1.0'
              }

              dependencyManagement {
                  imports {
                      mavenBom "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  }
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
                  id 'io.spring.dependency-management' version '1.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext {
                  springCloudGcpVersion = '7.2.0'
              }

              dependencyManagement {
                  imports {
                      mavenBom "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  }
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """
          )
        );
    }

    /**
     * Verifies that BOM imports work correctly when properties are defined using ext.set() syntax.
     */
    @Test
    void upgradesBomImportWithSetSyntax() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
                  id 'io.spring.dependency-management' version '1.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext.set("springCloudGcpVersion", "7.1.0")

              dependencyManagement {
                  imports {
                      mavenBom "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  }
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.1.0'
                  id 'io.spring.dependency-management' version '1.1.0'
              }

              repositories {
                  mavenCentral()
              }

              ext.set("springCloudGcpVersion", "7.2.0")

              dependencyManagement {
                  imports {
                      mavenBom "com.google.cloud:spring-cloud-gcp-dependencies:${springCloudGcpVersion}"
                  }
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'com.google.cloud:spring-cloud-gcp-starter'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """
          )
        );
    }
}
