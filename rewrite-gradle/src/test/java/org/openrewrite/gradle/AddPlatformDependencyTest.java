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
package org.openrewrite.gradle;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;

class AddPlatformDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api", "guava", "jackson-databind", "jackson-core", "lombok"));
    }

    @Language("java")
    private final String usingGuavaIntMath = """
            import com.google.common.math.IntMath;
            public class A {
                boolean getMap() {
                    return IntMath.isPrime(5);
                }
            }
      """;

    @CsvSource({
      "implementation",
      "compileOnly",
      "runtimeOnly",
      "annotationProcessor"})
    @ParameterizedTest
    void addPlatformWithExplicitConfiguration(String configuration) {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, configuration, null)),
          mavenProject("project",
            srcMainJava(java(usingGuavaIntMath)),
            buildGradle(
              //language=groovy
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
                }
                """,
              //language=groovy
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    %s platform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                }
                """.formatted(configuration)
            )));
    }

    @CsvSource({
      "implementation",
      "compileOnly",
      "runtimeOnly",
      "annotationProcessor"})
    @ParameterizedTest
    void addEnforcedPlatformWithExplicitConfiguration(String configuration) {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, configuration, true)),
          mavenProject("project",
            srcMainJava(java(usingGuavaIntMath)),
            buildGradle(
              //language=groovy
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
                }
                """,
              //language=groovy
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    %s enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                }
                """.formatted(configuration)
            )));
    }

    @Test
    void addsToTestScopeWhenNoConfigurationSpecified() {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, null, null)),
          mavenProject("project",
            srcTestJava(java(usingGuavaIntMath)),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
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
                    testImplementation platform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                }
                """
            )
          )
        );
    }

    @Test
    void addsToSmokeTestScopeWhenNoConfigurationSpecified() {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, null, null)),
          mavenProject("project",
            srcSmokeTestJava(java(usingGuavaIntMath)),
            buildGradle(
              """
                plugins {
                    id "java-library"
                    id "com.netflix.nebula.facet" version "10.1.3"
                }
                
                repositories {
                    mavenCentral()
                }
                
                facets {
                    smokeTest {
                        parentSourceSet = "test"
                    }
                }
                """,
              """
                plugins {
                    id "java-library"
                    id "com.netflix.nebula.facet" version "10.1.3"
                }
                
                repositories {
                    mavenCentral()
                }
                
                facets {
                    smokeTest {
                        parentSourceSet = "test"
                    }
                }
                
                dependencies {
                    smokeTestImplementation platform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                }
                """
            )
          )
        );
    }

    @Test
    void addsToImplementationScopeWhenNoConfigurationSpecified() {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, null, null))
            .typeValidationOptions(TypeValidation.none()),
          mavenProject("project",
            srcMainJava(java(usingGuavaIntMath)),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
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
                    implementation platform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                }
                """
            )
          )
        );
    }

    @Test
    void onlyAddsToParentConfigurationWhenNoConfigurationSpecified() {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, null, null))
            .typeValidationOptions(TypeValidation.none()),
          mavenProject("project",
            srcMainJava(java(usingGuavaIntMath)),
            srcTestJava(java(usingGuavaIntMath)),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }
                
                repositories {
                    mavenCentral()
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
                    implementation platform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                }
                """
            )
          )
        );
    }

    @CsvSource({
      "integrationTestImplementation,implementation",
      "integrationTestCompileOnly,compileOnly",
      "integrationTestRuntimeOnly,runtimeOnly",
      "integrationTestAnnotationProcessor,annotationProcessor"})
    @ParameterizedTest
    void addPlatformDependencyWithExplicitConfiguration(String recipeConfiguration, String gradleConfiguration) {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, recipeConfiguration, null)),
          mavenProject("project",
            srcMainJava(java(usingGuavaIntMath, sourceSpecs -> sourceSet(sourceSpecs, "integrationTest"))),
            buildGradle(
              //language=groovy
              """
                plugins {
                    id "java-library"
                    id 'jvm-test-suite'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "org.apache.logging.log4j:log4j-core:2.22.1"
                }
                
                testing {
                    suites {
                        integrationTest(JvmTestSuite) {
                        }
                    }
                }
                """,
              //language=groovy
              """
                plugins {
                    id "java-library"
                    id 'jvm-test-suite'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "org.apache.logging.log4j:log4j-core:2.22.1"
                }
                
                testing {
                    suites {
                        integrationTest(JvmTestSuite) {
                            dependencies {
                                %s platform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                            }
                        }
                    }
                }
                """.formatted(gradleConfiguration)
            )));
    }

    @CsvSource({
      "integrationTestImplementation,implementation",
      "integrationTestCompileOnly,compileOnly",
      "integrationTestRuntimeOnly,runtimeOnly",
      "integrationTestAnnotationProcessor,annotationProcessor"})
    @ParameterizedTest
    void addEnforcedPlatformDependencyWithExplicitConfiguration(String recipeConfiguration, String gradleConfiguration) {
        rewriteRun(
          spec -> spec.recipe(new AddPlatformDependency("org.springframework.boot", "spring-boot-dependencies", "3.2.4", null, recipeConfiguration, true)),
          mavenProject("project",
            srcMainJava(java(usingGuavaIntMath, sourceSpecs -> sourceSet(sourceSpecs, "integrationTest"))),
            buildGradle(
              //language=groovy
              """
                plugins {
                    id "java-library"
                    id 'jvm-test-suite'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "org.apache.logging.log4j:log4j-core:2.22.1"
                }
                
                testing {
                    suites {
                        integrationTest(JvmTestSuite) {
                        }
                    }
                }
                """,
              //language=groovy
              """
                plugins {
                    id "java-library"
                    id 'jvm-test-suite'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation "org.apache.logging.log4j:log4j-core:2.22.1"
                }
                
                testing {
                    suites {
                        integrationTest(JvmTestSuite) {
                            dependencies {
                                %s enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.2.4")
                            }
                        }
                    }
                }
                """.formatted(gradleConfiguration)
            )));
    }
}
