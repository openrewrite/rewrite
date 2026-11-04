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
import static org.openrewrite.gradle.Assertions.settingsGradle;

class RewriteSpreadAllInConfigurationsBlockTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RewriteSpreadAllInConfigurationsBlock());
    }

    @DocumentExample
    @Test
    void rewriteSpreadAllAtTopLevel() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  all*.exclude group: "log4j", module: "log4j"
              }
              """,
            """
              configurations.all {
                  exclude group: "log4j", module: "log4j"
              }
              """
          )
        );
    }

    @Test
    void rewriteSpreadAllInsideDependenciesBlock() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  configurations {
                      all*.exclude group: "log4j", module: "log4j"
                  }
                  implementation 'org.springframework.boot:spring-boot-starter-web'
              }
              """,
            """
              dependencies {
                  configurations.all {
                      exclude group: "log4j", module: "log4j"
                  }
                  implementation 'org.springframework.boot:spring-boot-starter-web'
              }
              """
          )
        );
    }

    @Test
    void multipleSpreadStatementsCollapseIntoOneAllBlock() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  all*.exclude group: "log4j", module: "log4j"
                  all*.exclude group: "junit", module: "junit"
              }
              """,
            """
              configurations.all {
                  exclude group: "log4j", module: "log4j"
                  exclude group: "junit", module: "junit"
              }
              """
          )
        );
    }

    @Test
    void mixedNamedConfigurationsAndSpreadGroups() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  javadoc {}
                  offlineJavadocPackages {}
                  // Shared exclusions
                  all*.exclude group: 'commons-logging'
                  // Keep this with the second exclusion
                  all*.exclude group: 'log4j'
                  create("integrationTest")
                  all*.exclude group: 'junit'
                  resolutionStrategy { failOnVersionConflict() }
              }
              """,
            """
              configurations {
                  javadoc {}
                  offlineJavadocPackages {}
                  // Shared exclusions
                  all {
                      exclude group: 'commons-logging'
                      // Keep this with the second exclusion
                      exclude group: 'log4j'
                  }
                  create("integrationTest")
                  all {
                      exclude group: 'junit'
                  }
                  resolutionStrategy { failOnVersionConflict() }
              }
              """
          )
        );
    }

    @Test
    void mixedBlockEndingInSpread() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  javadoc {}
                  all*.exclude group: 'commons-logging'
                  all*.transitive(false)
              }
              """,
            """
              configurations {
                  javadoc {}
                  all {
                      exclude group: 'commons-logging'
                      transitive(false)
                  }
              }
              """
          )
        );
    }

    @Test
    void keepControlFlowAndExplicitReturnInOuterClosure() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  all*.exclude group: 'log4j'
                  if (project.hasProperty('skip')) {
                      return
                  }
                  all*.exclude group: 'junit'
                  return all*.exclude(group: 'commons-logging')
              }
              """,
            """
              configurations {
                  all {
                      exclude group: 'log4j'
                  }
                  if (project.hasProperty('skip')) {
                      return
                  }
                  all {
                      exclude group: 'junit'
                  }
                  return all*.exclude(group: 'commons-logging')
              }
              """
          )
        );
    }

    @Test
    void preserveInlineAndTrailingComments() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  javadoc {} // named configuration
                  all*.exclude group: 'log4j' // first exclusion
                  all*.exclude group: 'junit' // last exclusion
                  // Keep the following configuration
                  offlineJavadocPackages {}
                  // End of configurations
              }
              """,
            """
              configurations {
                  javadoc {} // named configuration
                  all {
                      exclude group: 'log4j' // first exclusion
                      exclude group: 'junit'
                  } // last exclusion
                  // Keep the following configuration
                  offlineJavadocPackages {}
                  // End of configurations
              }
              """
          )
        );
    }

    @Test
    void preserveSemicolonsAndUnrelatedStatements() {
        rewriteRun(
          buildGradle(
            """
              configurations {
                  javadoc {};
                  all*.exclude group: 'log4j'; all*.exclude group: 'junit';
                  def retained = 'value'; println retained
              }
              """,
            """
              configurations {
                  javadoc {};
                  all {
                      exclude group: 'log4j'; exclude group: 'junit';
                  }
                  def retained = 'value'; println retained
              }
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void explicitReturnSpreadLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  configurations {
                      return all*.exclude(group: 'log4j')
                  }
                  """
              )
            );
        }

        @Test
        void spreadNestedInControlFlowLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  configurations {
                      javadoc {}
                      if (project.hasProperty('excludeLogging')) {
                          all*.exclude group: 'log4j'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void alreadyUsingClosureForm() {
            rewriteRun(
              buildGradle(
                """
                  configurations.all {
                      exclude group: "log4j", module: "log4j"
                  }
                  """
              )
            );
        }

        @Test
        void noSpreadInConfigurationsBlock() {
            rewriteRun(
              buildGradle(
                """
                  configurations {
                      create("integrationTest")
                  }
                  """
              )
            );
        }

        @Test
        void spreadOnIdentifierOtherThanAll() {
            rewriteRun(
              buildGradle(
                """
                  configurations {
                      compileClasspath*.exclude group: "log4j", module: "log4j"
                  }
                  """
              )
            );
        }

        @Test
        void settingsGradleNotTouched() {
            rewriteRun(
              settingsGradle(
                """
                  configurations {
                      all*.exclude group: "log4j", module: "log4j"
                  }
                  """
              )
            );
        }

        @Test
        void kotlinDslLeftAlone() {
            rewriteRun(
              buildGradleKts(
                """
                  configurations.all {
                      exclude(group = "log4j", module = "log4j")
                  }
                  """
              )
            );
        }
    }
}
