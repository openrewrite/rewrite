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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class RemoveNonReproducibleArchiveSettingsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveNonReproducibleArchiveSettings());
    }

    @DocumentExample("Let archives take Gradle's reproducible defaults")
    @Test
    void removesBothOverrides() {
        rewriteRun(
          buildGradle(
            """
              tasks.withType(Jar) {
                  preserveFileTimestamps = true
                  reproducibleFileOrder = false
                  archiveBaseName = 'app'
              }
              """,
            """
              tasks.withType(Jar) {
                  archiveBaseName = 'app'
              }
              """
          )
        );
    }

    @Test
    void removesKotlinDslOverrides() {
        rewriteRun(
          buildGradleKts(
            """
              tasks.jar {
                  isPreserveFileTimestamps = true
                  isReproducibleFileOrder = false
              }
              """,
            """
              tasks.jar {
              }
              """
          )
        );
    }

    /**
     * Only the override that defeats reproducibility is removed; a value written explicitly to agree with the
     * default is left alone.
     */
    @Test
    void noChangeWhenAlreadySetReproducibly() {
        rewriteRun(
          buildGradle(
            """
              tasks.withType(Jar) {
                  preserveFileTimestamps = false
                  reproducibleFileOrder = true
              }
              """
          )
        );
    }

    @Test
    void noChangeToUnrelatedAssignments() {
        rewriteRun(
          buildGradle(
            """
              tasks.withType(Jar) {
                  archiveBaseName = 'app'
                  duplicatesStrategy = 'exclude'
              }
              """
          )
        );
    }
}
