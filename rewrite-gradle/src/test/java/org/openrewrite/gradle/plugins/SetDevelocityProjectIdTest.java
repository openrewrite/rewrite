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

class SetDevelocityProjectIdTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SetDevelocityProjectId("openrewrite"));
    }

    @DocumentExample
    @Test
    void addsProjectIdAfterServerKotlinDsl() {
        rewriteRun(
          settingsGradleKts(
            """
              plugins {
                  id("com.gradle.develocity") version "latest.release"
              }

              develocity {
                  server = "https://community.develocity.cloud/"

                  buildScan {
                      uploadInBackground = false
                  }
              }
              """,
            """
              plugins {
                  id("com.gradle.develocity") version "latest.release"
              }

              develocity {
                  server = "https://community.develocity.cloud/"
                  projectId = "openrewrite"

                  buildScan {
                      uploadInBackground = false
                  }
              }
              """
          )
        );
    }

    @Test
    void addsProjectIdAfterServerGroovyDsl() {
        rewriteRun(
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }

              develocity {
                  server = 'https://community.develocity.cloud/'
              }
              """,
            """
              plugins {
                  id 'com.gradle.develocity' version '3.17.6'
              }

              develocity {
                  server = 'https://community.develocity.cloud/'
                  projectId = 'openrewrite'
              }
              """
          )
        );
    }

    @Test
    void doesNothingWhenProjectIdAlreadySet() {
        rewriteRun(
          settingsGradleKts(
            """
              develocity {
                  server = "https://community.develocity.cloud/"
                  projectId = "openrewrite"
              }
              """
          )
        );
    }

    @Test
    void updatesDifferingProjectId() {
        rewriteRun(
          settingsGradleKts(
            """
              develocity {
                  server = "https://community.develocity.cloud/"
                  projectId = "old"
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

    @Test
    void doesNotTouchBuildGradleOrNonDevelocityBlocks() {
        rewriteRun(
          settingsGradleKts(
            """
              plugins {
                  id("com.gradle.develocity") version "latest.release"
              }
              """
          )
        );
    }
}
