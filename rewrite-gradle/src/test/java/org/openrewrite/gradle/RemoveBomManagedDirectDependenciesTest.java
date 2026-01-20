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
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

@SuppressWarnings("GroovyAssignabilityCheck")
class RemoveBomManagedDirectDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "*", "*"));
    }

    @DocumentExample
    @Test
    void removeDependencyWithDifferentMajorVersion() {
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
                  implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.50")
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
              }
              """
          )
        );
    }

    @Test
    void keepDependencyWithSameMajorVersion() {
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
                  implementation("org.apache.tomcat.embed:tomcat-embed-core:10.1.0")
              }
              """
          )
        );
    }

    @Test
    void keepDependencyWithoutExplicitVersion() {
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
                  implementation("org.apache.tomcat.embed:tomcat-embed-core")
              }
              """
          )
        );
    }

    @Test
    void keepDependencyWhenNotManagedByMatchingBom() {
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
                  implementation(platform("io.quarkus.platform:quarkus-bom:3.0.0.Final"))
                  implementation("io.quarkus:quarkus-core:2.0.0.Final")
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
                  implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.50")
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
              }
              """
          )
        );
    }

    @Test
    void mapNotation() {
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
                  implementation(group: "org.apache.tomcat.embed", name: "tomcat-embed-core", version: "9.0.50")
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
              }
              """
          )
        );
    }

    @Test
    void filterByDependencyGroupPattern() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "org.apache.tomcat.*", "*")),
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
                  implementation("org.apache.tomcat.embed:tomcat-embed-core:9.0.50")
                  implementation("com.fasterxml.jackson.core:jackson-core:2.10.0")
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
                  implementation("com.fasterxml.jackson.core:jackson-core:2.10.0")
              }
              """
          )
        );
    }

    @Test
    void keepPlatformDependency() {
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
              }
              """
          )
        );
    }
}
