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

class SortDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new SortDependencies());
    }

    @DocumentExample
    @Test
    void sortsByConfigurationThenGroupThenArtifact() {
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
                  testImplementation "org.junit.jupiter:junit-jupiter-api:5.9.1"
                  implementation "org.springframework:spring-web:5.3.23"
                  api "com.google.guava:guava:31.1-jre"
                  implementation "com.fasterxml.jackson.core:jackson-databind:2.13.4"
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
                  api "com.google.guava:guava:31.1-jre"
                  implementation "com.fasterxml.jackson.core:jackson-databind:2.13.4"
                  implementation "org.springframework:spring-web:5.3.23"
                  testImplementation "org.junit.jupiter:junit-jupiter-api:5.9.1"
              }
              """
          )
        );
    }

    @Test
    void sortsKotlinDsl() {
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
                  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
                  implementation("org.springframework:spring-web:5.3.23")
                  api("com.google.guava:guava:31.1-jre")
                  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api("com.google.guava:guava:31.1-jre")
                  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.4")
                  implementation("org.springframework:spring-web:5.3.23")
                  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
              }
              """
          )
        );
    }

    @Test
    void sortsMapNotation() {
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
                  implementation group: 'org.springframework', name: 'spring-web', version: '5.3.23'
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
                  implementation group: 'com.google.guava', name: 'guava', version: '31.1-jre'
                  implementation group: 'org.springframework', name: 'spring-web', version: '5.3.23'
              }
              """
          )
        );
    }

    @Test
    void sortsKotlinDslMapNotation() {
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
                  implementation(group = "org.springframework", name = "spring-web", version = "5.3.23")
                  implementation(group = "com.google.guava", name = "guava", version = "31.1-jre")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group = "com.google.guava", name = "guava", version = "31.1-jre")
                  implementation(group = "org.springframework", name = "spring-web", version = "5.3.23")
              }
              """
          )
        );
    }

    @Test
    void configurationGroupingTakesPrecedence() {
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
                  implementation "org.springframework:spring-web:5.3.23"
                  api "org.springframework:spring-core:5.3.23"
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
                  api "org.springframework:spring-core:5.3.23"
                  implementation "org.springframework:spring-web:5.3.23"
              }
              """
          )
        );
    }
}
