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
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;

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

    /**
     * A versionless dependency whose version a BOM derives from a property is upgraded by overriding that property,
     * which the dependency management plugin resolves against the project's properties.
     */
    @Test
    void overridesBomPropertyOfManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.yaml", "snakeyaml", "1.33", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              ext['snakeyaml.version'] = '1.33'

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """
          )
        );
    }

    @Test
    void updatesExistingBomPropertyOverride() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.yaml", "snakeyaml", "1.33", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              ext['snakeyaml.version'] = '1.28'

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              ext['snakeyaml.version'] = '1.33'

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """
          )
        );
    }

    @Test
    void updatesBomPropertyOverrideInGradleProperties() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.yaml", "snakeyaml", "1.33", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """
          ),
          properties(
            """
              snakeyaml.version=1.28
              """,
            """
              snakeyaml.version=1.33
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @Test
    void overridesBomPropertyInKotlinDsl() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.yaml", "snakeyaml", "1.33", null)),
          buildGradleKts(
            """
              plugins {
                  java
                  id("io.spring.dependency-management") version "1.1.7"
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom("org.springframework.boot:spring-boot-dependencies:2.5.7")
                  }
              }

              dependencies {
                  implementation("org.yaml:snakeyaml")
              }
              """,
            """
              plugins {
                  java
                  id("io.spring.dependency-management") version "1.1.7"
              }

              extra["snakeyaml.version"] = "1.33"

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom("org.springframework.boot:spring-boot-dependencies:2.5.7")
                  }
              }

              dependencies {
                  implementation("org.yaml:snakeyaml")
              }
              """
          )
        );
    }

    /**
     * log4j-core is managed by the log4j BOM that spring-boot-dependencies imports at {@code ${log4j2.version}},
     * so that property governs it and every other log4j artifact on the classpath.
     */
    @Test
    void overridesPropertyOfNestedBomImport() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.apache.logging.log4j", "log4j-core", "2.17.1", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }

              dependencies {
                  implementation 'org.apache.logging.log4j:log4j-core'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              ext['log4j2.version'] = '2.17.1'

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }

              dependencies {
                  implementation 'org.apache.logging.log4j:log4j-core'
              }
              """
          )
        );
    }

    /**
     * The Boot plugin imports spring-boot-dependencies programmatically, so no `mavenBom` line exists to find. The
     * import is discovered from the dependency management plugin's state on the GradleProject marker instead.
     */
    @Test
    void overridesBomPropertyOfBomImportedByBootPlugin() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.yaml", "snakeyaml", "2.3", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.3.5'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.3.5'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              ext['snakeyaml.version'] = '2.3'

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """
          )
        );
    }

    /**
     * A project property is honored no matter which script imported the BOM, so the override is written into the
     * build.gradle that owns the project rather than into the applied script.
     */
    @Test
    void overridesBomPropertyWhenBomIsImportedByAppliedScript() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.yaml", "snakeyaml", "1.33", null)),
          buildGradle(
            """
              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                  }
              }
              """,
            spec -> spec.path("dependencyManagement.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencyManagement.gradle'

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              ext['snakeyaml.version'] = '1.33'

              repositories {
                  mavenCentral()
              }

              apply from: 'dependencyManagement.gradle'

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """
          )
        );
    }

    /**
     * The root project's `subprojects` block imports the BOM on the subproject's behalf. Writing the override into
     * the subproject's own script shadows the root for that project only.
     */
    @Test
    void overridesBomPropertyImportedByRootSubprojectsBlock() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.yaml", "snakeyaml", "1.33", null)),
          settingsGradle(
            """
              rootProject.name = 'sample'
              include 'sub'
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'io.spring.dependency-management' version '1.1.7' apply false
              }

              subprojects {
                  apply plugin: 'java'
                  apply plugin: 'io.spring.dependency-management'

                  repositories {
                      mavenCentral()
                  }

                  dependencyManagement {
                      imports {
                          mavenBom 'org.springframework.boot:spring-boot-dependencies:2.5.7'
                      }
                  }
              }
              """
          ),
          buildGradle(
            """
              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """,
            """
              ext['snakeyaml.version'] = '1.33'

              dependencies {
                  implementation 'org.yaml:snakeyaml'
              }
              """,
            spec -> spec.path("sub/build.gradle")
          )
        );
    }
}
