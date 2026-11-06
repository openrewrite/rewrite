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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class IsBuildGradleTest implements RewriteTest {

    private static final GradleProject RENAMED = GradleProject.builder()
            .buildFilePath("api.gradle")
            .build();

    private static final GradleProject CONVENTIONAL = GradleProject.builder()
            .buildFilePath("build.gradle")
            .build();

    private static Recipe isBuildGradle(boolean exactMatch) {
        return RewriteTest.toRecipe(() -> new IsBuildGradle<ExecutionContext>(exactMatch));
    }

    @Test
    void exactMatchFindsRenamedBuildFileRecordedOnMarker() {
        rewriteRun(
          spec -> spec.recipe(isBuildGradle(true)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            """
              /*~~>*/plugins {
                  id 'java'
              }
              """,
            spec -> spec.path("api.gradle").markers(RENAMED)
          )
        );
    }

    @Test
    void exactMatchResolvesProjectRelativeBuildFileAgainstSubprojectPath() {
        rewriteRun(
          spec -> spec.recipe(isBuildGradle(true)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            """
              /*~~>*/plugins {
                  id 'java'
              }
              """,
            spec -> spec.path("api/api.gradle").markers(RENAMED)
          )
        );
    }

    @Test
    void exactMatchPrefersMarkerOverConventionalName() {
        rewriteRun(
          spec -> spec.recipe(isBuildGradle(true)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            spec -> spec.path("build.gradle").markers(RENAMED)
          )
        );
    }

    @Test
    void exactMatchFallsBackToConventionalNameWithoutMarker() {
        rewriteRun(
          spec -> spec.recipe(isBuildGradle(true)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              """,
            """
              /*~~>*/plugins {
                  id 'java'
              }
              """
          ),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }

    @Test
    void exactMatchSkipsScriptPluginSharingTheProjectMarker() {
        rewriteRun(
          spec -> spec.recipe(isBuildGradle(true)),
          buildGradle(
            """
              dependencies {
              }
              """,
            spec -> spec.path("gradle/publishing.gradle").markers(CONVENTIONAL)
          )
        );
    }

    /**
     * The broad mode deliberately covers scripts applied with {@code apply from:} so that dependency recipes
     * reach declarations living outside the build file. The marker must not narrow it.
     */
    @Test
    void nonExactMatchStillFindsScriptPluginSharingTheProjectMarker() {
        rewriteRun(
          spec -> spec.recipe(isBuildGradle(false)),
          buildGradle(
            """
              dependencies {
              }
              """,
            """
              /*~~>*/dependencies {
              }
              """,
            spec -> spec.path("gradle/publishing.gradle").markers(CONVENTIONAL)
          )
        );
    }

    @Test
    void nonExactMatchSkipsSettingsFile() {
        rewriteRun(
          spec -> spec.recipe(isBuildGradle(false)),
          settingsGradle(
            """
              rootProject.name = 'sample'
              """
          )
        );
    }
}
