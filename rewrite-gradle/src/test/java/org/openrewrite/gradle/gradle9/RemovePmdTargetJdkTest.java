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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class RemovePmdTargetJdkTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemovePmdTargetJdk());
    }

    @DocumentExample
    @Test
    void removeFromExtensionBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'pmd'
              }

              pmd {
                  targetJdk = '1.7'
                  toolVersion = '7.0.0'
              }
              """,
            """
              plugins {
                  id 'pmd'
              }

              pmd {
                  toolVersion = '7.0.0'
              }
              """
          )
        );
    }

    @Test
    void removeEmptiedExtensionBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'pmd'
              }

              pmd {
                  targetJdk = '1.7'
              }
              """,
            """
              plugins {
                  id 'pmd'
              }
              """
          )
        );
    }

    @Test
    void removeLastStatementOfBlock() {
        rewriteRun(
          buildGradle(
            """
              pmd {
                  toolVersion = '7.0.0'
                  targetJdk = '1.7'
              }
              """,
            """
              pmd {
                  toolVersion = '7.0.0'
              }
              """
          )
        );
    }

    @Test
    void groovySetterCallNotation() {
        rewriteRun(
          buildGradle(
            """
              pmd {
                  targetJdk '1.7'
                  toolVersion '7.0.0'
              }
              """,
            """
              pmd {
                  toolVersion '7.0.0'
              }
              """
          )
        );
    }

    @Test
    void withTypeTaskConfiguration() {
        rewriteRun(
          buildGradle(
            """
              tasks.withType(Pmd) {
                  targetJdk = TargetJdk.VERSION_1_7
                  ignoreFailures = true
              }
              """,
            """
              tasks.withType(Pmd) {
                  ignoreFailures = true
              }
              """
          )
        );
    }

    @Test
    void namedTaskConfiguration() {
        rewriteRun(
          buildGradle(
            """
              tasks.named('pmdMain') {
                  targetJdk = TargetJdk.VERSION_1_7
                  ignoreFailures = true
              }
              """,
            """
              tasks.named('pmdMain') {
                  ignoreFailures = true
              }
              """
          )
        );
    }

    @Test
    void taskNameConfigurationBlock() {
        rewriteRun(
          buildGradle(
            """
              pmdMain {
                  targetJdk = TargetJdk.VERSION_1_7
                  ignoreFailures = true
              }
              """,
            """
              pmdMain {
                  ignoreFailures = true
              }
              """
          )
        );
    }

    @Test
    void qualifiedAssignment() {
        rewriteRun(
          buildGradle(
            """
              pmd.targetJdk = '1.7'
              """,
            ""
          )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  pmd
              }

              pmd {
                  targetJdk = TargetJdk.VERSION_1_7
                  toolVersion = "7.0.0"
              }
              """,
            """
              plugins {
                  pmd
              }

              pmd {
                  toolVersion = "7.0.0"
              }
              """
          )
        );
    }

    @Test
    void doesNotTouchUnrelatedTargetJdk() {
        rewriteRun(
          buildGradle(
            """
              myExtension {
                  targetJdk = '1.7'
              }
              """
          )
        );
    }

    @Test
    void leavesEmptyBlockThatHadNothingToMigrate() {
        rewriteRun(
          buildGradle(
            """
              pmd {
              }
              """
          )
        );
    }

    @Test
    void doesNotApplyToSettingsFiles() {
        rewriteRun(
          settingsGradle(
            """
              pmd {
                  targetJdk = '1.7'
              }
              """
          )
        );
    }
}
