/*
 * Copyright 2024 the original author or authors.
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

class RemoveRedundantDependencyVersionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveRedundantDependencyVersions(null, null, null, null));
    }

    @DocumentExample
    @Test
    void literal() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void mapEntry() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation(group: "org.apache.commons", name: "commons-lang3", version: "3.14.0")
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation(group: "org.apache.commons", name: "commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void mapLiteral() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation([group: "org.apache.commons", name: "commons-lang3", version: "3.14.0"])
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation([group: "org.apache.commons", name: "commons-lang3"])
              }
              """
          )
        );
    }

    @Test
    void enforcedPlatform() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void platformUsingMapEntry() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(enforcedPlatform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(enforcedPlatform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void freestandingScript() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """,
            """
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              
              apply from: 'dependencies.gradle'
              """
          )
        );
    }

    @Test
    void removeUnneededConstraint() {
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
                  constraints {
                      implementation('org.springframework:spring-core:6.2.1') {
                          because 'Gradle is resolving 6.2.2 already, this constraint has no effect and can be removed'
                      }
                  }
                  implementation 'org.springframework.boot:spring-boot:3.4.2'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.springframework.boot:spring-boot:3.4.2'
              }
              """
          )
        );
    }

    @Test
    void keepStrictConstraint() {
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
                  constraints {
                      implementation('org.springframework:spring-core:6.2.1!!') {
                          because 'The !! forces the usage of 6.2.1'
                      }
                  }
                  implementation 'org.springframework.boot:spring-boot:3.4.2'
              }
              """
          )
        );
    }

    @Test
    void transitiveConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
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
                  api(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void unmanagedDependency() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              
                  testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
              }
              """
          )
        );
    }
}
