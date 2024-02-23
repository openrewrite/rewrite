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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

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
          buildGradle("""
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

    @Test
    void findPluginDependencyAndAddToDependencyClosure() {
        rewriteRun(
          buildGradle("""
            plugins {
                id 'groovy-gradle-plugin'
            }
            repositories {
                gradlePluginPortal()
            }
              """, spec -> spec.path("buildSrc/build.gradle")),
          groovy("""
            plugins{
                id "java"
            }
            dependencies{
                implementation 'com.google.guava:guava:31.1-jre'
            }
            """, spec -> spec.path("buildSrc/src/main/groovy/convention-plugin.gradle")),
          buildGradle("""
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
          buildGradle("""
            plugins {
                id 'groovy-gradle-plugin'
            }
            repositories {
                gradlePluginPortal()
            }
              """, spec -> spec.path("buildSrc/build.gradle")),
          groovy("""
            plugins{
                id "java"
            }
            dependencies{
                implementation 'com.google.guava:guava:31.1-jre'
            }
            """, spec -> spec.path("buildSrc/src/main/groovy/convention-plugin.gradle")),
          buildGradle("""
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
          buildGradle("""
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
                DependenciesInUse.Row row = rows.get(0);
                assertThat(row.getGroupId()).isEqualTo("com.fasterxml.jackson.core");
                assertThat(row.getArtifactId()).isEqualTo("jackson-core");
                assertThat(row.getVersion()).isEqualTo("2.13.4");
            }),
          buildGradle("""
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
          buildGradle("""
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
}
