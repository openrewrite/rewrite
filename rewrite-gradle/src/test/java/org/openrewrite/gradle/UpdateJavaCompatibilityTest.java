/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class UpdateJavaCompatibilityTest implements RewriteTest {
    @Test
    void sourceAndTarget() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = 1.8
              targetCompatibility = 1.8
              """,
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = 11
              targetCompatibility = 11
              """
          )
        );
    }

    @Test
    void sourceOnly() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, UpdateJavaCompatibility.CompatibilityType.Source, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = 1.8
              targetCompatibility = 1.8
              """,
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = 11
              targetCompatibility = 1.8
              """
          )
        );
    }

    @Test
    void targetOnly() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, UpdateJavaCompatibility.CompatibilityType.Target, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = 1.8
              targetCompatibility = 1.8
              """,
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = 1.8
              targetCompatibility = 11
              """
          )
        );
    }

    @Test
    void styleChange() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(8, null, UpdateJavaCompatibility.DeclarationStyle.Enum)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = 1.8
              targetCompatibility = 1.8
              """,
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = JavaVersion.VERSION_1_8
              targetCompatibility = JavaVersion.VERSION_1_8
              """
          )
        );
    }

    @Test
    void handlesJavaExtension() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              java {
                  sourceCompatibility = 1.8
                  targetCompatibility = 1.8
              }
              """,
            """
              plugins {
                  id "java"
              }

              java {
                  sourceCompatibility = 11
                  targetCompatibility = 11
              }
              """
          )
        );
    }

    @Test
    void handlesJavaToolchains() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(8)
                  }
              }
              """,
            """
              plugins {
                  id "java"
              }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(11)
                  }
              }
              """
          )
        );
    }
}
