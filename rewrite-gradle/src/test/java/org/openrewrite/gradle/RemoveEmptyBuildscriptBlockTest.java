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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class RemoveEmptyBuildscriptBlockTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveEmptyBuildscriptBlock());
    }

    @Test
    void removeEmptyBuildscriptBlock() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
              }

              plugins {
                  id 'java'
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
    void removeConsecutiveEmptyBuildscriptBlocks() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
              }

              buildscript {
                  repositories {
                  }
              }

              plugins {
                  id 'java'
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
    void removeBuildscriptBlockContainingOnlyEmptyBlocks() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  dependencies {
                  }
                  repositories {
                  }
                  configurations.all {
                  }
              }

              plugins {
                  id 'java'
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
    void removeEmptyBuildscriptBlockKts() {
        rewriteRun(
          buildGradleKts(
            """
              buildscript {
              }

              plugins {
                  id("java")
              }
              """,
            """
              plugins {
                  id("java")
              }
              """
          )
        );
    }

    @Test
    void removeBuildscriptBlockBetweenOtherStatements() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              buildscript {
              }

              group = 'com.example'
              """,
            """
              plugins {
                  id 'java'
              }

              group = 'com.example'
              """
          )
        );
    }

    @Test
    void doNotRemoveBuildscriptBlockWithDependencies() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath 'com.example:plugin:1.0'
                  }
              }

              plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveBuildscriptBlockContainingComment() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  // keep this around, we will need it later
              }

              plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void doNotRemoveBuildscriptBlockWithCommentInNestedBlock() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  dependencies {
                      // classpath 'com.example:plugin:1.0'
                  }
              }

              plugins {
                  id 'java'
              }
              """
          )
        );
    }
}
