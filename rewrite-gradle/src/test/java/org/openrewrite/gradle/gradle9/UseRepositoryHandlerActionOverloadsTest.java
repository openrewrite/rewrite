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

class UseRepositoryHandlerActionOverloadsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseRepositoryHandlerActionOverloads());
    }

    @DocumentExample
    @Test
    void flatDirWithSingleDirectory() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  flatDir dirs: 'libs'
              }
              """,
            """
              repositories {
                  flatDir {
                      dirs 'libs'
                  }
              }
              """
          )
        );
    }

    @Test
    void flatDirWithParentheses() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  flatDir(dirs: 'libs')
              }
              """,
            """
              repositories {
                  flatDir {
                      dirs 'libs'
                  }
              }
              """
          )
        );
    }

    @Test
    void flatDirWithDirectoryList() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  flatDir dirs: ['libs', 'moreLibs']
              }
              """,
            """
              repositories {
                  flatDir {
                      dirs 'libs', 'moreLibs'
                  }
              }
              """
          )
        );
    }

    @Test
    void flatDirWithNameAndDirs() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  flatDir name: 'localLibs', dirs: 'libs'
              }
              """,
            """
              repositories {
                  flatDir {
                      name = 'localLibs'
                      dirs 'libs'
                  }
              }
              """
          )
        );
    }

    @Test
    void mavenCentralWithName() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  mavenCentral name: 'central2'
              }
              """,
            """
              repositories {
                  mavenCentral {
                      name = 'central2'
                  }
              }
              """
          )
        );
    }

    @Test
    void insideBuildscriptRepositories() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      flatDir dirs: 'libs'
                  }
              }
              """,
            """
              buildscript {
                  repositories {
                      flatDir {
                          dirs 'libs'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesActionNotationAlone() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  mavenCentral()
                  flatDir {
                      dirs 'libs'
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesArtifactUrlsAlone() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  mavenCentral artifactUrls: ['https://repo.example.com/maven2']
              }
              """
          )
        );
    }

    @Test
    void leavesUnrelatedFlatDirAlone() {
        rewriteRun(
          buildGradle(
            """
              myExtension {
                  flatDir dirs: 'libs'
              }
              """
          )
        );
    }
}
