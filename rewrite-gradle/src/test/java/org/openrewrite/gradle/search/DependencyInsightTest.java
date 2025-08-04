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
package org.openrewrite.gradle.search;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.groovy.Assertions.groovy;

class DependencyInsightTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new DependencyInsight("com.google.guava", "failureaccess", null, null));
    }

    @DocumentExample
    @Test
    void findTransitiveDependency() {
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
                  implementation 'com.google.guava:guava:31.1-jre'
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
                  /*~~(com.google.guava:failureaccess:1.0.1)~~>*/implementation 'com.google.guava:guava:31.1-jre'
              }
              """
          )
        );
    }

    @Nested
    class ConfigurationChecks {
        private static Stream<Arguments> configurationsAndMatches() {
            /*
            "annotationProcessor", "api", "implementation", "compileOnly", "runtimeOnly", "testImplementation", "testCompileOnly", "testRuntimeOnly"
            */
            return Stream.of(
              Arguments.of("annotationProcessor", true, false, false, false, false, false, false, false),
              Arguments.of("compileClasspath", false, true, true, true, false, false, false, false),
              Arguments.of("runtimeClasspath", false, true, true, false, true, false, false, false),
              Arguments.of("testCompileClasspath", false, true, true, false, false, true, true, false),
              Arguments.of("testRuntimeClasspath", false, true, true, false, true, true, false, true)
            );
        }

        private SourceSpecs expectationHelper(String configuration, String template, String matchComment, boolean matched) {
            @Language("groovy")
            final String before = String.format(template, configuration);
            if (matched) {
                @Language("groovy")
                final String after = String.format(template, matchComment + configuration);
                return buildGradle(before, after);
            }
            return buildGradle(before);
        }

        private void rewriteRunHelper(String existingConfiguration, String checkingConfiguration, boolean matched) {
            final String matchComment = "/*~~(com.google.guava:guava:31.1-jre)~~>*/";
            @Language("groovy")
            final String gradleTemplate = """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  %s 'com.google.guava:guava:31.1-jre'
              }
              """;
            rewriteRun(
              spec -> spec.recipe(new DependencyInsight("com.google.guava", "guava", null, checkingConfiguration)),
              expectationHelper(existingConfiguration, gradleTemplate, matchComment, matched)
            );
        }

        @MethodSource("configurationsAndMatches")
        @ParameterizedTest
        void configurationsAreMatched(
          String configuration,
          boolean annotationProcessorMatch,
          boolean apiMatch, boolean implementationMatch, boolean compileOnlyMatch, boolean runtimeOnlyMatch,
          boolean testImplementationMatch, boolean testCompileOnlyMatch, boolean testRuntimeOnlyMatch
        ) {
            rewriteRunHelper("annotationProcessor", configuration, annotationProcessorMatch);
            rewriteRunHelper("api", configuration, apiMatch);
            rewriteRunHelper("implementation", configuration, implementationMatch);
            rewriteRunHelper("compileOnly", configuration, compileOnlyMatch);
            rewriteRunHelper("runtimeOnly", configuration, runtimeOnlyMatch);
            rewriteRunHelper("testImplementation", configuration, testImplementationMatch);
            rewriteRunHelper("testCompileOnly", configuration, testCompileOnlyMatch);
            rewriteRunHelper("testRuntimeOnly", configuration, testRuntimeOnlyMatch);
        }
    }

    @Test
    void findPluginDependencyAndAddToDependencyClosure() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'groovy-gradle-plugin'
              }
              repositories {
                  gradlePluginPortal()
              }
              """,
            spec -> spec.path("buildSrc/build.gradle")),
          groovy(
            """
              plugins{
                  id "java"
              }
              dependencies{
                  implementation 'com.google.guava:guava:31.1-jre'
              }
              """,
            spec -> spec.path("buildSrc/src/main/groovy/convention-plugin.gradle")),
          buildGradle(
            """
              plugins {
                  id 'convention-plugin'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.openrewrite:rewrite-java:8.0.0'
              }
              """,
            """
              plugins {
                  id 'convention-plugin'
              }
              repositories {
                  mavenCentral()
              }
              /*~~(com.google.guava:failureaccess:1.0.1)~~>*/dependencies {
                  implementation 'org.openrewrite:rewrite-java:8.0.0'
              }
              """
          )
        );
    }

    @Test
    void findPluginDependencyAndAddToRoot() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'groovy-gradle-plugin'
              }
              repositories {
                  gradlePluginPortal()
              }
              """,
            spec -> spec.path("buildSrc/build.gradle")),
          groovy(
            """
              plugins{
                  id "java"
              }
              dependencies{
                  implementation 'com.google.guava:guava:31.1-jre'
              }
              """,
            spec -> spec.path("buildSrc/src/main/groovy/convention-plugin.gradle")),
          buildGradle(
            """
              plugins {
                  id 'convention-plugin'
              }
              repositories {
                  mavenCentral()
              }
              """,
            """
              /*~~(com.google.guava:failureaccess:1.0.1)~~>*/plugins {
                  id 'convention-plugin'
              }
              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void recursive() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("doesnotexist", "doesnotexist", null, null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'io.grpc:grpc-services:1.59.0'
              }
              """
          )
        );
    }

    @Test
    void pattern() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("*", "jackson-core", null, null))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).isNotEmpty();
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getGroupId()).isEqualTo("com.fasterxml.jackson.core");
                assertThat(row.getArtifactId()).isEqualTo("jackson-core");
                assertThat(row.getVersion()).isEqualTo("2.13.4");
            }),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.openrewrite:rewrite-core:7.39.1'
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
                  /*~~(com.fasterxml.jackson.core:jackson-core:2.13.4)~~>*/implementation 'org.openrewrite:rewrite-core:7.39.1'
              }
              """
          )
        );
    }

    @Test
    void versionSearch() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("org.openrewrite", "*", "7.0.0", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.openrewrite:rewrite-yaml:7.0.0'
                  implementation 'org.openrewrite:rewrite-java:8.0.0'
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
                  /*~~(org.openrewrite:rewrite-yaml:7.0.0)~~>*/implementation 'org.openrewrite:rewrite-yaml:7.0.0'
                  implementation 'org.openrewrite:rewrite-java:8.0.0'
              }
              """
          )
        );
    }

    @Test
    void nestedDependenciesAreTransitivelySearchedForMatchingDependencies() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("org.springframework.boot", "*", null, null))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).isNotEmpty();
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getArtifactId()).isEqualTo("spring-boot-starter-web");
                assertThat(row.getDepth()).isEqualTo(0);
                row = rows.get(4);
                assertThat(row.getArtifactId()).isEqualTo("spring-boot");
                assertThat(row.getDepth()).isEqualTo(4);
            }),
          buildGradle(
            """
              buildscript {
              	    ext {
              	    	springBootVersion = '2.6.6'
              	    }
              	    dependencies {
              	        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.6.6")
              	    }
              	    repositories {
              	        mavenCentral()
              	    }
              }
              repositories {
                  mavenCentral()
              }

              apply plugin: 'org.springframework.boot'
              apply plugin: 'io.spring.dependency-management'
              apply plugin: 'java'

              java {
                  sourceCompatibility = '11'
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'org.springframework.boot:spring-boot-starter-actuator:2.6.4'
                  implementation 'io.pivotal.cfenv:java-cfenv-boot:2.5.0'
              }
              """,
            """
              buildscript {
              	    ext {
              	    	springBootVersion = '2.6.6'
              	    }
              	    dependencies {
              	        classpath("org.springframework.boot:spring-boot-gradle-plugin:2.6.6")
              	    }
              	    repositories {
              	        mavenCentral()
              	    }
              }
              repositories {
                  mavenCentral()
              }

              apply plugin: 'org.springframework.boot'
              apply plugin: 'io.spring.dependency-management'
              apply plugin: 'java'

              java {
                  sourceCompatibility = '11'
              }

              dependencies {
                  /*~~(org.springframework.boot:spring-boot-starter-web:2.6.6,org.springframework.boot:spring-boot-starter:2.6.6,org.springframework.boot:spring-boot-autoconfigure:2.6.6,org.springframework.boot:spring-boot-starter-json:2.6.6,org.springframework.boot:spring-boot:2.6.6,org.springframework.boot:spring-boot-starter-tomcat:2.6.6,org.springframework.boot:spring-boot-starter-logging:2.6.6)~~>*/implementation 'org.springframework.boot:spring-boot-starter-web'
                  /*~~(org.springframework.boot:spring-boot-starter-actuator:2.6.4,org.springframework.boot:spring-boot-starter:2.6.6,org.springframework.boot:spring-boot-autoconfigure:2.6.6,org.springframework.boot:spring-boot:2.6.6,org.springframework.boot:spring-boot-actuator-autoconfigure:2.6.6,org.springframework.boot:spring-boot-starter-logging:2.6.6,org.springframework.boot:spring-boot-actuator:2.6.6)~~>*/implementation 'org.springframework.boot:spring-boot-starter-actuator:2.6.4'
                  /*~~(org.springframework.boot:spring-boot-dependencies:2.6.15,org.springframework.boot:spring-boot:2.6.6)~~>*/implementation 'io.pivotal.cfenv:java-cfenv-boot:2.5.0'
              }
              """
          )
        );
    }

    @Test
    void jacksonIsFoundInternally() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("com.fasterxml.jackson.*", "*", null, null))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).isNotEmpty();
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getGroupId()).isEqualTo("com.fasterxml.jackson.datatype");
                assertThat(row.getArtifactId()).isEqualTo("jackson-datatype-jsr310");
                assertThat(row.getDepth()).isEqualTo(2);
                row = rows.get(4);
                assertThat(row.getGroupId()).isEqualTo("com.fasterxml.jackson.core");
                assertThat(row.getArtifactId()).isEqualTo("jackson-core");
                assertThat(row.getDepth()).isEqualTo(3);
            }),
          buildGradle(
            """
              buildscript {
                  ext {
                      springBootVersion = '2.6.6'
                  }
                  dependencies {
                      classpath("org.springframework.boot:spring-boot-gradle-plugin:2.6.6")
                  }
                  repositories {
                      mavenCentral()
                  }
              }
              repositories {
                  mavenCentral()
              }

              apply plugin: 'org.springframework.boot'
              apply plugin: 'io.spring.dependency-management'
              apply plugin: 'java'

              java {
                  sourceCompatibility = '11'
              }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  implementation 'org.springframework.boot:spring-boot-starter-actuator:2.6.4'
                  implementation 'io.pivotal.cfenv:java-cfenv-boot:2.5.0'
              }
              """,
            """
              buildscript {
                  ext {
                      springBootVersion = '2.6.6'
                  }
                  dependencies {
                      classpath("org.springframework.boot:spring-boot-gradle-plugin:2.6.6")
                  }
                  repositories {
                      mavenCentral()
                  }
              }
              repositories {
                  mavenCentral()
              }

              apply plugin: 'org.springframework.boot'
              apply plugin: 'io.spring.dependency-management'
              apply plugin: 'java'

              java {
                  sourceCompatibility = '11'
              }

              dependencies {
                  /*~~(com.fasterxml.jackson.module:jackson-module-parameter-names:2.13.2,com.fasterxml.jackson.core:jackson-core:2.13.2,com.fasterxml.jackson.core:jackson-annotations:2.13.2,com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.2,com.fasterxml.jackson.core:jackson-databind:2.13.2.2,com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.13.2)~~>*/implementation 'org.springframework.boot:spring-boot-starter-web'
                  /*~~(com.fasterxml.jackson.core:jackson-core:2.13.2,com.fasterxml.jackson.core:jackson-annotations:2.13.2,com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.2,com.fasterxml.jackson.core:jackson-databind:2.13.2.2)~~>*/implementation 'org.springframework.boot:spring-boot-starter-actuator:2.6.4'
                  /*~~(com.fasterxml.jackson.core:jackson-core:2.13.2,com.fasterxml.jackson.core:jackson-annotations:2.13.2,com.fasterxml.jackson.core:jackson-databind:2.13.2.2)~~>*/implementation 'io.pivotal.cfenv:java-cfenv-boot:2.5.0'
              }
              """
          )
        );
    }

    @Test
    void duplicateDependencies() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()).recipe(new DependencyInsight("org.projectlombok", "lombok", null, null)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  compileOnly("org.projectlombok:lombok:1.18.38")
                  annotationProcessor("org.projectlombok:lombok:1.18.38")

                  testCompileOnly("org.projectlombok:lombok:1.18.38")
                  testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
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
                  /*~~(org.projectlombok:lombok:1.18.38)~~>*/compileOnly("org.projectlombok:lombok:1.18.38")
                  /*~~(org.projectlombok:lombok:1.18.38)~~>*/annotationProcessor("org.projectlombok:lombok:1.18.38")

                  /*~~(org.projectlombok:lombok:1.18.38)~~>*/testCompileOnly("org.projectlombok:lombok:1.18.38")
                  /*~~(org.projectlombok:lombok:1.18.38)~~>*/testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
              }
              """
          )
        );
    }
}
