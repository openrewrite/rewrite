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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;

class DoesNotIncludeDependencyTest implements RewriteTest {
    private static final String marker = "/*~~>*/";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    private Recipe defaultRecipeWithConfiguration(@Nullable String configuration) {
        return new DoesNotIncludeDependency("org.springframework", "spring-beans", configuration);
    }

    @Test
    void nonGradleFilesNotMarked() {
        rewriteRun(
          spec -> spec.recipe(defaultRecipeWithConfiguration(null)),
          java(
            """
              class SomeClass {}
              """
          )
        );
    }

    @Nested
    class DirectDependencyPresent {
        //language=groovy
        private static final String directDependencyTemplate = """
          plugins {
              id 'java-library'
          }
          repositories {
              mavenCentral()
          }
          dependencies {
              %s 'org.springframework:spring-beans:6.0.0'
          }
          """;

        @ParameterizedTest
        @ValueSource(strings = {"api", "implementation", "compileOnly", "runtimeOnly", "testImplementation", "testCompileOnly", "testRuntimeOnly"})
        void withoutDesiredConfigurationSpecifiedNotMarked(String configuration) {
            rewriteRun(
              spec -> spec.recipe(defaultRecipeWithConfiguration(null)),
              buildGradle(String.format(directDependencyTemplate, configuration))
            );
        }

        @CsvSource({"compileOnly,runtimeClasspath", "compileOnly,testCompileClasspath", "compileOnly,testRuntimeClasspath"})
        @CsvSource({"runtimeOnly,compileClasspath", "runtimeOnly,testCompileClasspath"})
        @CsvSource({"testImplementation,compileClasspath", "testImplementation,runtimeClasspath"})
        @CsvSource({"testCompileOnly,compileClasspath", "testCompileOnly,runtimeClasspath", "testCompileOnly,testRuntimeClasspath"})
        @CsvSource({"testRuntimeOnly,compileClasspath", "testRuntimeOnly,runtimeClasspath", "testRuntimeOnly,testCompileClasspath"})
        @ParameterizedTest
        void notInDesiredConfigurationMarked(String existingConfiguration, String searchingConfiguration) {
            rewriteRun(
              spec -> spec.recipe(defaultRecipeWithConfiguration(searchingConfiguration)),
              buildGradle(
                String.format(directDependencyTemplate, existingConfiguration),
                String.format(marker + directDependencyTemplate, existingConfiguration)
              )
            );
        }

        @CsvSource({"api,compileClasspath", "api,runtimeClasspath", "api,testCompileClasspath", "api,testRuntimeClasspath"})
        @CsvSource({"implementation,compileClasspath", "implementation,runtimeClasspath", "implementation,testCompileClasspath", "implementation,testRuntimeClasspath"})
        @CsvSource({"compileOnly,compileClasspath"})
        @CsvSource({"runtimeOnly,runtimeClasspath", "runtimeOnly,testRuntimeClasspath"})
        @CsvSource({"testImplementation,testCompileClasspath", "testImplementation,testRuntimeClasspath"})
        @CsvSource({"testCompileOnly,testCompileClasspath"})
        @CsvSource({"testRuntimeOnly,testRuntimeClasspath"})
        @ParameterizedTest
        void inDesiredConfigurationNotMarked(String existingConfiguration, String searchingConfiguration) {
            rewriteRun(
              spec -> spec.recipe(defaultRecipeWithConfiguration(searchingConfiguration)),
              buildGradle(String.format(directDependencyTemplate, existingConfiguration))
            );
        }
    }

    @Nested
    class TransitiveDependencyPresent {
        //language=groovy
        private static final String transitiveDependencyTemplate = """
          plugins {
              id 'java-library'
          }
          repositories {
              mavenCentral()
          }
          dependencies {
              %s 'org.springframework.boot:spring-boot-starter-actuator:3.0.0'
          }
          """;

        @ParameterizedTest
        @ValueSource(strings = {"api", "implementation", "compileOnly", "runtimeOnly", "testImplementation", "testCompileOnly", "testRuntimeOnly"})
        void withoutDesiredConfigurationSpecifiedNotMarked(String configuration) {
            rewriteRun(
              spec -> spec.recipe(defaultRecipeWithConfiguration(null)),
              buildGradle(String.format(transitiveDependencyTemplate, configuration))
            );
        }

        @CsvSource({"compileOnly,runtimeClasspath", "compileOnly,testCompileClasspath", "compileOnly,testRuntimeClasspath"})
        @CsvSource({"runtimeOnly,compileClasspath", "runtimeOnly,testCompileClasspath"})
        @CsvSource({"testImplementation,compileClasspath", "testImplementation,runtimeClasspath"})
        @CsvSource({"testCompileOnly,compileClasspath", "testCompileOnly,runtimeClasspath", "testCompileOnly,testRuntimeClasspath"})
        @CsvSource({"testRuntimeOnly,compileClasspath", "testRuntimeOnly,runtimeClasspath", "testRuntimeOnly,testCompileClasspath"})
        @ParameterizedTest
        void notInDesiredConfigurationMarked(String existingConfiguration, String searchingConfiguration) {
            rewriteRun(
              spec -> spec.recipe(defaultRecipeWithConfiguration(searchingConfiguration)),
              buildGradle(
                String.format(transitiveDependencyTemplate, existingConfiguration),
                String.format(marker + transitiveDependencyTemplate, existingConfiguration)
              )
            );
        }

        @CsvSource({"api,compileClasspath", "api,runtimeClasspath", "api,testCompileClasspath", "api,testRuntimeClasspath"})
        @CsvSource({"implementation,compileClasspath", "implementation,runtimeClasspath", "implementation,testCompileClasspath", "implementation,testRuntimeClasspath"})
        @CsvSource({"compileOnly,compileClasspath"})
        @CsvSource({"runtimeOnly,runtimeClasspath", "runtimeOnly,testRuntimeClasspath"})
        @CsvSource({"testImplementation,testCompileClasspath", "testImplementation,testRuntimeClasspath"})
        @CsvSource({"testCompileOnly,testCompileClasspath"})
        @CsvSource({"testRuntimeOnly,testRuntimeClasspath"})
        @ParameterizedTest
        void inDesiredConfigurationNotMarked(String existingConfiguration, String searchingConfiguration) {
            rewriteRun(
              spec -> spec.recipe(defaultRecipeWithConfiguration(searchingConfiguration)),
              buildGradle(String.format(transitiveDependencyTemplate, existingConfiguration))
            );
        }
    }
}
