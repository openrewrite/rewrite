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

class AddJavaToolchainTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddJavaToolchain(17));
    }

    @DocumentExample
    @Test
    void addAfterPluginsBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              group = "com.example"
              """,
            """
              plugins {
                  id "java-library"
              }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                  }
              }

              group = "com.example"
              """
          )
        );
    }

    @Test
    void addAfterLastAppliedPluginInSubprojects() {
        rewriteRun(
          buildGradle(
            """
              subprojects {
                  apply plugin: 'java-library'
                  apply plugin: 'maven-publish'

                  group = "com.example"
              }
              """,
            """
              subprojects {
                  apply plugin: 'java-library'
                  apply plugin: 'maven-publish'

                  java {
                      toolchain {
                          languageVersion = JavaLanguageVersion.of(17)
                      }
                  }

                  group = "com.example"
              }
              """
          )
        );
    }

    @Test
    void keepFollowingStatementSpacing() {
        rewriteRun(
          buildGradle(
            """
              allprojects {
                  apply plugin: 'java'
                  group = "com.example"
              }
              """,
            """
              allprojects {
                  apply plugin: 'java'

                  java {
                      toolchain {
                          languageVersion = JavaLanguageVersion.of(17)
                      }
                  }
                  group = "com.example"
              }
              """
          )
        );
    }

    @Test
    void addToEveryScopeApplyingJava() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              subprojects {
                  apply plugin: 'java-library'
              }
              """,
            """
              plugins {
                  id "java"
              }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                  }
              }

              subprojects {
                  apply plugin: 'java-library'

                  java {
                      toolchain {
                          languageVersion = JavaLanguageVersion.of(17)
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void onlyScopeApplyingJava() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "base"
              }

              subprojects {
                  apply plugin: 'java-library'
              }
              """,
            """
              plugins {
                  id "base"
              }

              subprojects {
                  apply plugin: 'java-library'

                  java {
                      toolchain {
                          languageVersion = JavaLanguageVersion.of(17)
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void existingToolchainIsLeftAlone() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
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

    @Test
    void noJavaPlugin() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "base"
              }

              subprojects {
                  apply plugin: 'maven-publish'
              }
              """
          )
        );
    }

    @Test
    void ignorePluginsNotAppliedToThisProject() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library" apply false
              }
              """
          )
        );
    }

    @Test
    void kotlinDslIsNotChanged() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }
              """
          )
        );
    }
}
