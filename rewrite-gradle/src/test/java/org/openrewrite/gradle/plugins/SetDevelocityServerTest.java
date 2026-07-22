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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;

class SetDevelocityServerTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SetDevelocityServer("https://community.develocity.cloud/"));
    }

    @DocumentExample
    @Test
    void updatesServerKotlinDsl() {
        rewriteRun(
          settingsGradleKts(
            """
              develocity {
                  server = "https://ge.openrewrite.org/"

                  buildScan {
                      uploadInBackground = false
                  }
              }
              """,
            """
              develocity {
                  server = "https://community.develocity.cloud/"

                  buildScan {
                      uploadInBackground = false
                  }
              }
              """
          )
        );
    }

    @Test
    void updatesServerGroovyDsl() {
        rewriteRun(
          settingsGradle(
            """
              develocity {
                  server = 'https://ge.openrewrite.org/'
              }
              """,
            """
              develocity {
                  server = 'https://community.develocity.cloud/'
              }
              """
          )
        );
    }

    @Test
    void doesNothingWhenServerAlreadySet() {
        rewriteRun(
          settingsGradleKts(
            """
              develocity {
                  server = "https://community.develocity.cloud/"
              }
              """
          )
        );
    }

    @Test
    void addsServerAsFirstStatementWhenAbsent() {
        rewriteRun(
          settingsGradleKts(
            """
              develocity {
                  projectId = "openrewrite"
              }
              """,
            """
              develocity {
                  server = "https://community.develocity.cloud/"
                  projectId = "openrewrite"
              }
              """
          )
        );
    }
}
