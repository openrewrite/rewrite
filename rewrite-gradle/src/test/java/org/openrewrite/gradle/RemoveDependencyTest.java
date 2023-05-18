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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class RemoveDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDependency("org.springframework.boot", "spring-boot-starter-web", null));
    }

    @DocumentExample
    @Test
    void removeGradleDependencyUsingStringNotation() {
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
                  implementation "org.springframework.boot:spring-boot-starter-web:2.7.0"
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removeGradleDependencyUsingStringNotationWithExclusion() {
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
                  implementation("org.springframework.boot:spring-boot-starter-web:2.7.0") {
                      exclude group: "junit"
                  }
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removeGradleDependencyUsingMapNotation() {
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
                  implementation group: "org.springframework.boot", name: "spring-boot-starter-web", version: "2.7.0"
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removeGradleDependencyUsingMapNotationWithExclusion() {
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
                  implementation(group: "org.springframework.boot", name: "spring-boot-starter-web", version: "2.7.0") {
                      exclude group: "junit"
                  }
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
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
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removeWhenUsingVariableReplacement() {
        rewriteRun(
          buildGradle(
            """
              def springBootVersion = "2.7.0"
              dependencies {
                  implementation "org.springframework.boot:spring-boot-starter-web:${springBootVersion}"
                  implementation group: "org.springframework.boot", name: "spring-boot-starter-web", version: springBootVersion
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """,
            """
              def springBootVersion = "2.7.0"
              dependencies {
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void removePlatformDependency() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation platform("org.springframework.boot:spring-boot-starter-web:2.7.0")
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """,
            """
              dependencies {
                  testImplementation "org.jupiter.vintage:junit-vintage-engine:5.6.2"
              }
              """
          )
        );
    }

    @Test
    void onlyRemoveFromSpecifiedConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("org.springframework.boot", "spring-boot-starter-test", "implementation")),
          buildGradle(
            """
              dependencies {
                  implementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
                  testImplementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
              }
              """,
            """
              dependencies {
                  testImplementation "org.springframework.boot:spring-boot-starter-test:2.7.0"
              }
              """
          )
        );
    }
}
