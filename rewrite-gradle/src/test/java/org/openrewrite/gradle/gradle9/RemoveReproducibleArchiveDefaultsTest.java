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

class RemoveReproducibleArchiveDefaultsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveReproducibleArchiveDefaults());
    }

    @DocumentExample
    @Test
    void removeEmptiedWithTypeBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              tasks.withType(AbstractArchiveTask).configureEach {
                  preserveFileTimestamps = false
                  reproducibleFileOrder = true
              }
              """,
            """
              plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void keepOtherArchiveSettings() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              jar {
                  preserveFileTimestamps false
                  archiveClassifier = 'core'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              jar {
                  archiveClassifier = 'core'
              }
              """
          )
        );
    }

    @Test
    void keepOptOutOfReproducibility() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              tasks.withType(AbstractArchiveTask).configureEach {
                  preserveFileTimestamps = true
                  reproducibleFileOrder = false
              }
              """
          )
        );
    }

    @Test
    void keepTaskRegistrationLeftEmpty() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('bundle', Zip) {
                  preserveFileTimestamps = false
              }
              """,
            """
              tasks.register('bundle', Zip) {
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
              plugins {
                  java
              }

              tasks.withType<AbstractArchiveTask>().configureEach {
                  isPreserveFileTimestamps = false
                  isReproducibleFileOrder = true
              }

              tasks.jar {
                  isReproducibleFileOrder = true
                  archiveClassifier.set("core")
              }
              """,
            """
              plugins {
                  java
              }

              tasks.jar {
                  archiveClassifier.set("core")
              }
              """
          )
        );
    }
}
