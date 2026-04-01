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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class JvmTestSuiteTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(RewriteTest.toRecipe(() -> new JvmTestSuite.Matcher().asVisitor(suite ->
            SearchResult.found(suite.getTree()))));
    }

    @Nested
    class GroovyDsl {
        @DocumentExample
        @Test
        void all() {
            rewriteRun(
              buildGradle(
                """
                  plugins {
                      id "java"
                      id "jvm-test-suite"
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          test {
                              useJUnitJupiter()
                          }

                          integrationTest(JvmTestSuite) {
                              dependencies {
                                  implementation project()
                              }
                          }
                      }
                  }
                  """,
                """
                  plugins {
                      id "java"
                      id "jvm-test-suite"
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          /*~~>*/test {
                              useJUnitJupiter()
                          }

                          /*~~>*/integrationTest(JvmTestSuite) {
                              dependencies {
                                  implementation project()
                              }
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void findByName() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new JvmTestSuite.Matcher().name("integrationTest").asVisitor(suite ->
                SearchResult.found(suite.getTree())))),
              buildGradle(
                """
                  plugins {
                      id "java"
                      id "jvm-test-suite"
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          test {
                              useJUnitJupiter()
                          }

                          integrationTest(JvmTestSuite) {
                              dependencies {
                                  implementation project()
                              }
                          }
                      }
                  }
                  """,
                """
                  plugins {
                      id "java"
                      id "jvm-test-suite"
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          test {
                              useJUnitJupiter()
                          }

                          /*~~>*/integrationTest(JvmTestSuite) {
                              dependencies {
                                  implementation project()
                              }
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void addDependency() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe((recipe) -> new JvmTestSuite.Matcher().asVisitor((suite, ctx) ->
                suite.addDependency("implementation", "com.google.guava", "guava", "29.0-jre", null, null, null, new MavenMetadataFailures(recipe), null, ctx).visitNonNull(suite.getTree(), ctx, suite.getCursor().getParentOrThrow())))),
              buildGradle(
                """
                  plugins {
                      id "java"
                      id "jvm-test-suite"
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          test {
                              useJUnitJupiter()
                          }

                          integrationTest(JvmTestSuite) {
                              dependencies {
                                  implementation project()
                              }
                          }
                      }
                  }
                  """,
                """
                  plugins {
                      id "java"
                      id "jvm-test-suite"
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          test {
                              useJUnitJupiter()

                              dependencies {
                                  implementation "com.google.guava:guava:29.0-jre"
                              }
                          }

                          integrationTest(JvmTestSuite) {
                              dependencies {
                                  implementation project()
                                  implementation "com.google.guava:guava:29.0-jre"
                              }
                          }
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class KotlinDsl {
        @DocumentExample
        @Test
        void all() {
            rewriteRun(
              buildGradleKts(
                """
                  plugins {
                      java
                      `jvm-test-suite`
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          val test by getting(JvmTestSuite::class) {
                              useJUnitJupiter()
                          }

                          register<JvmTestSuite>("integrationTest") {
                              dependencies {
                                  implementation(project())
                              }
                          }

                          val functionalTest by registering(JvmTestSuite::class) {
                              dependencies {
                                  implementation(project())
                              }
                          }
                      }
                  }
                  """,
                """
                  plugins {
                      java
                      `jvm-test-suite`
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          /*~~>*/val test by getting(JvmTestSuite::class) {
                              useJUnitJupiter()
                          }

                          /*~~>*/register<JvmTestSuite>("integrationTest") {
                              dependencies {
                                  implementation(project())
                              }
                          }

                          /*~~>*/val functionalTest by registering(JvmTestSuite::class) {
                              dependencies {
                                  implementation(project())
                              }
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void findByName() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new JvmTestSuite.Matcher().name("integrationTest").asVisitor(suite ->
                SearchResult.found(suite.getTree())))),
              buildGradleKts(
                """
                  plugins {
                      java
                      `jvm-test-suite`
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          val test by getting(JvmTestSuite::class) {
                              useJUnitJupiter()
                          }

                          register<JvmTestSuite>("integrationTest") {
                              dependencies {
                                  implementation(project())
                              }
                          }
                      }
                  }
                  """,
                """
                  plugins {
                      java
                      `jvm-test-suite`
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          val test by getting(JvmTestSuite::class) {
                              useJUnitJupiter()
                          }

                          /*~~>*/register<JvmTestSuite>("integrationTest") {
                              dependencies {
                                  implementation(project())
                              }
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void addDependency() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe((recipe) -> new JvmTestSuite.Matcher().asVisitor((suite, ctx) ->
                suite.addDependency("implementation", "com.google.guava", "guava", "29.0-jre", null, null, null, new MavenMetadataFailures(recipe), null, ctx).visitNonNull(suite.getTree(), ctx, suite.getCursor().getParentOrThrow())))),
              buildGradleKts(
                """
                  plugins {
                      java
                      `jvm-test-suite`
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          val test by getting(JvmTestSuite::class) {
                              useJUnitJupiter()
                          }

                          register<JvmTestSuite>("integrationTest") {
                              dependencies {
                                  implementation(project())
                              }
                          }
                      }
                  }
                  """,
                """
                  plugins {
                      java
                      `jvm-test-suite`
                  }

                  repositories {
                      mavenCentral()
                  }

                  testing {
                      suites {
                          val test by getting(JvmTestSuite::class) {
                              useJUnitJupiter()

                              dependencies {
                                  implementation("com.google.guava:guava:29.0-jre")
                              }
                          }

                          register<JvmTestSuite>("integrationTest") {
                              dependencies {
                                  implementation(project())
                                  implementation("com.google.guava:guava:29.0-jre")
                              }
                          }
                      }
                  }
                  """
              )
            );
        }
    }
}
