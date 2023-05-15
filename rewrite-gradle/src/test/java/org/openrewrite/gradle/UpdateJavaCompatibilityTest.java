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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class UpdateJavaCompatibilityTest implements RewriteTest {
    @ParameterizedTest
    @CsvSource(textBlock = """
      1.8,1.8,11,11
      '1.8','1.8','11','11'
      "1.8","1.8","11","11"
      JavaVersion.VERSION_1_8,JavaVersion.VERSION_1_8,JavaVersion.VERSION_11,JavaVersion.VERSION_11
      1.8,"1.8",11,"11"
      JavaVersion.VERSION_1_8,"1.8",JavaVersion.VERSION_11,"11",
      """, quoteCharacter = '`')
    void sourceAndTarget(String beforeSourceCompatibility, String beforeTargetCompatibility, String afterSourceCompatibility, String afterTargetCompatibility) {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = %s
              targetCompatibility = %s
              """.formatted(beforeSourceCompatibility, beforeTargetCompatibility),
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = %s
              targetCompatibility = %s
              """.formatted(afterSourceCompatibility, afterTargetCompatibility)
          )
        );
    }

    @Test
    void sourceOnly() {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, UpdateJavaCompatibility.CompatibilityType.source, null)),
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
          spec -> spec.recipe(new UpdateJavaCompatibility(11, UpdateJavaCompatibility.CompatibilityType.target, null)),
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

    @ParameterizedTest
    @CsvSource(textBlock = """
      Enum,1.8,JavaVersion.VERSION_1_8
      Enum,'1.8',JavaVersion.VERSION_1_8
      Enum,"1.8",JavaVersion.VERSION_1_8
      Number,'1.8',1.8
      Number,"1.8",1.8
      Number,JavaVersion.VERSION_1_8,1.8
      String,1.8,'1.8'
      String,JavaVersion.VERSION_1_8,'1.8'
      """, quoteCharacter = '`')
    void styleChange(String declarationStyle, String beforeCompatibility, String afterCompatibility) {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(8, null, UpdateJavaCompatibility.DeclarationStyle.valueOf(declarationStyle))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = %s
              targetCompatibility = %s
              """.formatted(beforeCompatibility, beforeCompatibility),
            """
              plugins {
                  id "java"
              }

              sourceCompatibility = %s
              targetCompatibility = %s
              """.formatted(afterCompatibility, afterCompatibility)
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

    @ParameterizedTest
    @CsvSource(textBlock = """
      source,Enum,JavaVersion.VERSION_11,1.8
      source,Number,11,1.8
      source,String,'11',1.8
      target,Enum,1.8,JavaVersion.VERSION_11
      target,Number,1.8,11
      target,String,1.8,'11'
      """, quoteCharacter = '`')
    void allOptions(String compatibilityType, String declarationStyle, String expectedSourceCompatibility, String expectedTargetCompatibility) {
        rewriteRun(
          spec -> spec.recipe(new UpdateJavaCompatibility(11, UpdateJavaCompatibility.CompatibilityType.valueOf(compatibilityType), UpdateJavaCompatibility.DeclarationStyle.valueOf(declarationStyle))),
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

              sourceCompatibility = %s
              targetCompatibility = %s
              """.formatted(expectedSourceCompatibility, expectedTargetCompatibility)
          )
        );
    }
}
