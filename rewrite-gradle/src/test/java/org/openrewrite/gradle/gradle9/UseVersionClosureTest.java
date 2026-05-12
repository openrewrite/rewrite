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
package org.openrewrite.gradle.gradle9;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UseVersionClosureTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVersionClosure());
    }

    @DocumentExample
    @Test
    void convertsVersionAssignmentToClosure() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation('org.example:lib') {
                      version = {
                          strictly '1.0'
                      }
                  }
              }
              """,
            """
              dependencies {
                  implementation('org.example:lib') {
                      version {
                          strictly '1.0'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void convertsMultipleVersionAssignments() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation('org.example:lib') {
                      version = {
                          strictly '1.0'
                      }
                  }
                  implementation('org.example:other') {
                      version = {
                          require '2.0'
                      }
                  }
              }
              """,
            """
              dependencies {
                  implementation('org.example:lib') {
                      version {
                          strictly '1.0'
                      }
                  }
                  implementation('org.example:other') {
                      version {
                          require '2.0'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void rewritesEmptyClosure() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  implementation('org.example:lib') {
                      version = { }
                  }
              }
              """,
            """
              dependencies {
                  implementation('org.example:lib') {
                      version { }
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void alreadyUsingClosureForm() {
            rewriteRun(
              buildGradle(
                """
                  dependencies {
                      implementation('org.example:lib') {
                          version {
                              strictly '1.0'
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void unrelatedAssignmentLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  ext {
                      foo = {
                          bar '1.0'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void stringLiteralAssignmentLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  dependencies {
                      implementation('org.example:lib') {
                          version = '1.0'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void identifierAssignmentLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  def libVersion = '1.0'
                  dependencies {
                      implementation('org.example:lib') {
                          version = libVersion
                      }
                  }
                  """
              )
            );
        }

        @Test
        void fieldAccessVersionAssignmentLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  allprojects {
                      project.version = {
                          strictly '1.0'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void kotlinDsl() {
            rewriteRun(
              buildGradleKts(
                """
                  dependencies {
                      implementation("org.example:lib") {
                          version {
                              strictly("1.0")
                          }
                      }
                  }
                  """
              )
            );
        }
    }
}