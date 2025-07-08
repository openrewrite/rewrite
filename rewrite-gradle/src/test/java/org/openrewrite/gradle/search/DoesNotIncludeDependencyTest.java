/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.CsvSources;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class DoesNotIncludeDependencyTest implements RewriteTest {
    @ParameterizedTest
    @ValueSource(strings = {"api", "implementation", "compileOnly", "runtimeOnly", "testImplementation", "testCompileOnly", "testRuntimeOnly"})
    void dependencyPresentWithSpecificConfigurationFailsApplicability(String configuration) {
        rewriteRun(
          spec -> spec
            .beforeRecipe(withToolingApi())
            .recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null)),
          buildGradle(String.format("""
            plugins {
                id 'java-library'
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                %s 'org.springframework:spring-beans:6.0.0'
            }
            """, configuration),
            spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"api", "implementation", "compileOnly", "runtimeOnly", "testImplementation", "testCompileOnly", "testRuntimeOnly"})
    void dependencyPresentTransitivelyWithSpecificConfigurationFailsApplicability(String configuration) {
        rewriteRun(
          spec -> spec
            .beforeRecipe(withToolingApi())
            .recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null)),
          buildGradle(String.format("""
            plugins {
                id 'java-library'
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                %s 'org.springframework.boot:spring-boot-starter-actuator:3.0.0'
            }
            """, configuration),
            spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
          )
        );
    }

    @ParameterizedTest
    @CsvSource({"compileOnly,runtimeClasspath", "compileOnly,testCompileClasspath", "compileOnly,testRuntimeClasspath"})
    @CsvSource({"runtimeOnly,compileClasspath", "runtimeOnly,testCompileClasspath"})
    @CsvSource({"testImplementation,compileClasspath", "testImplementation,runtimeClasspath"})
    @CsvSource({"testCompileOnly,compileClasspath", "testCompileOnly,runtimeClasspath", "testCompileOnly,testRuntimeClasspath"})
    @CsvSource({"testRuntimeOnly,compileClasspath", "testRuntimeOnly,runtimeClasspath", "testRuntimeOnly,testCompileClasspath"})
    void dependencyPresentButNotInSpecifiedConfigurationPassesApplicability(String existingConfiguration, String searchingConfiguration) {
        rewriteRun(
          spec -> spec
            .beforeRecipe(withToolingApi())
            .recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", searchingConfiguration)),
          buildGradle(
            String.format("""
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  %s 'org.springframework:spring-beans:6.0.0'
              }
              """, existingConfiguration),
            String.format("""
              /*~~>*/plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  %s 'org.springframework:spring-beans:6.0.0'
              }
              """, existingConfiguration)
          )
        );
    }

    @ParameterizedTest
    @CsvSource({"api,compileClasspath", "api,runtimeClasspath", "api,testCompileClasspath", "api,testRuntimeClasspath"})
    @CsvSource({"implementation,compileClasspath", "implementation,runtimeClasspath", "implementation,testCompileClasspath", "implementation,testRuntimeClasspath"})
    @CsvSource({"compileOnly,compileClasspath"})
    @CsvSource({"runtimeOnly,runtimeClasspath", "runtimeOnly,testRuntimeClasspath"})
    @CsvSource({"testImplementation,testCompileClasspath", "testImplementation,testRuntimeClasspath"})
    @CsvSource({"testCompileOnly,testCompileClasspath"})
    @CsvSource({"testRuntimeOnly,testRuntimeClasspath"})
    void dependencyPresentInSpecifiedConfigurationFailsApplicability(String existingConfiguration, String searchingConfiguration) {
        rewriteRun(
          spec -> spec
            .beforeRecipe(withToolingApi())
            .recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", searchingConfiguration)),
          buildGradle(
            String.format("""
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  %s 'org.springframework:spring-beans:6.0.0'
              }
              """, existingConfiguration),
            spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
          )
        );
    }
}