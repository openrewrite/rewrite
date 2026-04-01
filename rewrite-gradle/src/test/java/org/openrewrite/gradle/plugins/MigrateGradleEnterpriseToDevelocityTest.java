/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class MigrateGradleEnterpriseToDevelocityTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateGradleEnterpriseToDevelocity("3.17.x"))
          .beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite/issues/4135")
    @Test
    void migrate() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.16'
              }
              gradleEnterprise {
                  server = 'https://ge.sam.com/'
                  allowUntrustedServer = true
                  buildScan {
                      publishAlways()
                      uploadInBackground = true
                      capture {
                          taskInputFiles = true
                      }
                  }
                  buildCache {
                      remote(gradleEnterprise.buildCache) {
                          enabled = true
                          push = System.getenv("CI") != null
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://ge.sam.com/'
                  allowUntrustedServer = true
                  buildScan {
                      uploadInBackground = true
                      capture {
                          fileFingerprints = true
                      }
                  }
                  buildCache {
                      remote(develocity.buildCache) {
                          enabled = true
                          push = System.getenv("CI") != null
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void publishAlwaysIf() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.16'
              }
              gradleEnterprise {
                  server = 'https://ge.sam.com/'
                  buildScan {
                      publishAlwaysIf(System.getenv("CI") != null)
                  }
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://ge.sam.com/'
                  buildScan {
                      publishing.onlyIf { System.getenv("CI") != null }
                  }
              }
              """
          )
        );
    }

    @Test
    void publishOnFailure() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.16'
              }
              gradleEnterprise {
                  server = 'https://ge.sam.com/'
                  buildScan {
                      publishOnFailure()
                  }
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://ge.sam.com/'
                  buildScan {
                      publishing.onlyIf { !it.buildResult.failures.empty }
                  }
              }
              """
          )
        );
    }

    @Test
    void publishOnFailureIf() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.16'
              }
              gradleEnterprise {
                  server = 'https://ge.sam.com/'
                  buildScan {
                      publishOnFailureIf(System.getenv("CI") != null)
                  }
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }
              develocity {
                  server = 'https://ge.sam.com/'
                  buildScan {
                      publishing.onlyIf { !it.buildResult.failures.empty && System.getenv("CI") != null }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNothingForTooOldVersions() {
        rewriteRun(
          spec -> spec.recipe(new MigrateGradleEnterpriseToDevelocity("3.16.x")),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.16'
              }
              gradleEnterprise {
                  server = 'https://ge.sam.com/'
                  allowUntrustedServer = true
                  buildScan {
                      publishAlways()
                      uploadInBackground = true
                      capture {
                          taskInputFiles = true
                      }
                  }
              }
              """
          )
        );
    }
}
