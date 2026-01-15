/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;

class RemoveBuildPluginTest implements RewriteTest {
    @DocumentExample
    @Test
    void removePlugin() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.jfrog.bintray")),
          buildGradle(
            """
              plugins {
                  id "com.jfrog.bintray" version "1.0"
              }
              """,
            ""
          )
        );
    }

    @Test
    void removePluginWithoutVersion() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("java")),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """,
            ""
          )
        );
    }

    @Test
    void removeUnappliedPlugin() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.jfrog.bintray")),
          buildGradle(
            """
              plugins {
                  id "com.jfrog.bintray" version "1.0" apply false
              }
              """,
            ""
          )
        );
    }

    @Test
    void removeUnappliedPluginWithoutVersion() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.jfrog.bintray")),
          buildGradle(
            """
              plugins {
                  id "com.jfrog.bintray" apply false
              }
              """,
            ""
          ),
          settingsGradle(
            """
              pluginManagement {
                  plugins {
                      id "com.jfrog.bintray" version "1.0"
                  }
              }

              rootProject.name = "example"
              """
          )
        );
    }

    @Test
    void applySyntax() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("org.openrewrite.rewrite")),
          buildGradle(
                """
              buildscript {
                repositories {
                  maven {
                    url "https://plugins.gradle.org/m2/"
                  }
                }
                dependencies {
                  classpath "org.openrewrite:plugin:6.1.6"
                }
              }

              apply plugin: "org.openrewrite.rewrite"
              """,
            """
              buildscript {
                repositories {
                  maven {
                    url "https://plugins.gradle.org/m2/"
                  }
                }
                dependencies {
                  classpath "org.openrewrite:plugin:6.1.6"
                }
              }
              """
          )
        );
    }

    @Test
    void removePluginKts() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.jfrog.bintray")),
          buildGradleKts(
            """
              plugins {
                  id("com.jfrog.bintray") version "1.0"
              }
              """,
            ""
          )
        );
    }

    @Test
    void removePluginWithoutVersionKts() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("java")),
          buildGradleKts(
            """
              plugins {
                  id("java")
              }
              """,
            ""
          )
        );
    }

    @Test
    void removeUnappliedPluginKts() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.jfrog.bintray")),
          buildGradleKts(
            """
              plugins {
                  id("com.jfrog.bintray") version "1.0" apply false
              }
              """,
            ""
          )
        );
    }

    @Test
    void removeUnappliedPluginWithoutVersionKts() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.jfrog.bintray")),
          buildGradleKts(
            """
              plugins {
                  id("com.jfrog.bintray") apply false
              }
              """,
            ""
          ),
          settingsGradleKts(
            """
              pluginManagement {
                  plugins {
                      id("com.jfrog.bintray") version "1.0"
                  }
              }
              
              rootProject.name = "example"
              """
          )
        );
    }

    @Test
    void applySyntaxKts() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("org.openrewrite.rewrite")),
          buildGradleKts(
            """
              buildscript {
                  repositories {
                      maven {
                          url = uri("https://plugins.gradle.org/m2/")
                      }
                  }
                  dependencies {
                      classpath("org.openrewrite:plugin:6.1.6")
                  }
              }
              
              apply(plugin = "org.openrewrite.rewrite")
              """,
            """
              buildscript {
                  repositories {
                      maven {
                          url = uri("https://plugins.gradle.org/m2/")
                      }
                  }
                  dependencies {
                      classpath("org.openrewrite:plugin:6.1.6")
                  }
              }
              """
          )
        );
    }

    @Test
    void removeMultipleOccurrencesKts() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.example.plugin")),
          buildGradleKts(
            """
              plugins {
                  id("com.example.plugin")
                  id("com.example.plugin") version "1.0"
                  id("com.example.plugin") version "1.0" apply false
                  id("other.plugin") version "2.0"
              }
              """,
            """
              plugins {
                  id("other.plugin") version "2.0"
              }
              """
          )
        );
    }

    @Test
    void removePluginLeavesOthersKts() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBuildPlugin("com.gradle.develocity")),
          buildGradleKts(
            """
              plugins {
                  id("com.gradle.develocity") version "3.17"
                  id("org.springframework.boot") version "3.2.0"
              }
              
              apply(plugin = "java")
              """,
            """
              plugins {
                  id("org.springframework.boot") version "3.2.0"
              }
              
              apply(plugin = "java")
              """
          )
        );
    }
}
