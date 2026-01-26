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

class RemoveRedundantDirectDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveRedundantDirectDependency(null, null, null, null));
    }

    @DocumentExample
    @Test
    void removesDirectDependencyWhenAvailableTransitivelyWithSameOrNewerVersion() {
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
                  // This direct dependency (tomcat-embed-core:10.1.0) is also available transitively
                  // from spring-boot-starter-tomcat which brings in a newer version (10.1.28)
                  implementation "org.apache.tomcat.embed:tomcat-embed-core:10.1.0"
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
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
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
              }
              """
          )
        );
    }

    @Test
    void keepsDirectDependencyWhenVersionIsNewerThanTransitive() {
        // spring-boot-starter-tomcat:3.2.0 brings tomcat-embed-core:10.1.16 transitively
        // We declare 10.1.28 directly which is NEWER, so it should be kept
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
                  // This direct dependency version is NEWER than the transitive, keep it
                  implementation "org.apache.tomcat.embed:tomcat-embed-core:10.1.28"
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.2.0"
              }
              """
          )
        );
    }

    @Test
    void keepsDirectDependencyWhenNotAvailableTransitively() {
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
                  implementation "com.google.guava:guava:32.1.3-jre"
              }
              """
          )
        );
    }

    @Test
    void respectsGroupPattern() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            "org.apache.tomcat.embed", null, null, null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "org.apache.tomcat.embed:tomcat-embed-core:10.1.0"
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
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
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
              }
              """
          )
        );
    }

    @Test
    void respectsExceptList() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, null, java.util.List.of("org.apache.tomcat.embed:tomcat-embed-core"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // Excepted from removal even though transitive is newer
                  implementation "org.apache.tomcat.embed:tomcat-embed-core:10.1.0"
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
              }
              """
          )
        );
    }

    @Test
    void removesWithComparatorAny() {
        // spring-boot-starter-tomcat:3.2.0 brings tomcat-embed-core:10.1.16 transitively
        // We declare 10.1.28 directly which is NEWER, but ANY mode removes it anyway
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, RemoveRedundantDirectDependency.Comparator.ANY, null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // Even though this is newer, ANY removes it
                  implementation "org.apache.tomcat.embed:tomcat-embed-core:10.1.28"
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.2.0"
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
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.2.0"
              }
              """
          )
        );
    }

    @Test
    void removesWithComparatorEq() {
        // spring-boot-starter-tomcat:3.3.4 brings tomcat-embed-core:10.1.30
        // We declare the exact same version, so EQ comparator should remove it
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, RemoveRedundantDirectDependency.Comparator.EQ, null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // Versions must be exactly equal for removal with EQ
                  implementation "org.apache.tomcat.embed:tomcat-embed-core:10.1.30"
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
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
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
              }
              """
          )
        );
    }

    @Test
    void keepsWhenComparatorEqAndVersionsDiffer() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDirectDependency(
            null, null, RemoveRedundantDirectDependency.Comparator.EQ, null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // Version differs, EQ comparator should keep it
                  implementation "org.apache.tomcat.embed:tomcat-embed-core:10.1.0"
                  implementation "org.springframework.boot:spring-boot-starter-tomcat:3.3.4"
              }
              """
          )
        );
    }
}
