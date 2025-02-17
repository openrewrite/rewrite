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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class FindJMVTestSuitesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @Nested
    class NoDependency {
        @Test
        void configNull() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(null)),
              buildGradle(
                withoutDependency,
                withoutDependencyFound,
                spec -> spec.path("build.gradle")));
        }

        @Test
        void configFalse() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(false)),
              buildGradle(
                withoutDependency,
                withoutDependencyFound,
                spec -> spec.path("build.gradle")));
        }

        @Test
        void configTrue() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(true)),
              buildGradle(
                withoutDependency,
                spec -> spec.path("build.gradle")));
        }
    }

    @Nested
    class WithDependency {
        @Test
        void configNull() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(null)),
              buildGradle(
                withDependency,
                withDependencyFound,
                spec -> spec.path("build.gradle")));
        }

        @Test
        void configFalse() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(false)),
              buildGradle(
                withDependency,
                withDependencyFound,
                spec -> spec.path("build.gradle")));
        }

        @Test
        void configTrue() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(true)),
              buildGradle(
                withDependency,
                withDependencyFound,
                spec -> spec.path("build.gradle")));
        }
    }

    @Nested
    class NoPlugin {
        @Test
        void configNull() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(null)),
              buildGradle(
                noSuiteDefined,
                spec -> spec.path("build.gradle")));
        }

        @Test
        void configFalse() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(false)),
              buildGradle(
                noSuiteDefined,
                spec -> spec.path("build.gradle")));
        }

        @Test
        void configTrue() {
            rewriteRun(
              spec -> spec.recipe(new FindJMVTestSuites(true)),
              buildGradle(
                noSuiteDefined,
                spec -> spec.path("build.gradle")));
        }
    }

    //language=groovy
    private static final String withDependency = """
          plugins {
              id "java-library"
              id 'jvm-test-suite'
          }
          
          repositories {
              mavenCentral()
          }
          
          testing {
              suites {
                  integrationTest(JvmTestSuite) {
                      dependencies {}
                  }
              }
          }
          """;

    //language=groovy
    private static final String withDependencyFound = """
          plugins {
              id "java-library"
              id 'jvm-test-suite'
          }
          
          repositories {
              mavenCentral()
          }
          
          testing {
              suites {
                  /*~~>*/integrationTest(JvmTestSuite) {
                      dependencies {}
                  }
              }
          }
          """;

    //language=groovy
    private static final String withoutDependency = """
          plugins {
              id "java-library"
              id 'jvm-test-suite'
          }
          
          repositories {
              mavenCentral()
          }
          
          testing {
              suites {
                  integrationTest(JvmTestSuite) {
                  }
              }
          }
          """;

    //language=groovy
    private static final String withoutDependencyFound = """
          plugins {
              id "java-library"
              id 'jvm-test-suite'
          }
          
          repositories {
              mavenCentral()
          }
          
          testing {
              suites {
                  /*~~>*/integrationTest(JvmTestSuite) {
                  }
              }
          }
          """;

    //language=groovy
    private static final String noSuiteDefined = """
          plugins {
              id "java-library"
              id 'jvm-test-suite'
          }
          
          repositories {
              mavenCentral()
          }
          
          testing {
              suites {
              }
          }
          """;
}