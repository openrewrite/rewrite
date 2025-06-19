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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;

import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.toml.Assertions.toml;

class GradlePluginTest implements RewriteTest {
    @Nested
    class GroovyDsl {
        @Test
        void corePlugin() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  plugins {
                      id "java"
                  }
                  """,
                """
                  plugins {
                      /*~~>*/id "java"
                  }
                  """
              )
            );
        }

        @Test
        void externalPlugin() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion())))),
              buildGradle(
                """
                  plugins {
                      id "org.openrewrite.rewrite" version "7.0.0"
                  }
                  """,
                """
                  plugins {
                      /*~~(org.openrewrite.rewrite:7.0.0)~~>*/id "org.openrewrite.rewrite" version "7.0.0"
                  }
                  """
              )
            );
        }

        @Test
        void externalPluginWithoutVersion() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion())))),
              settingsGradle(
                """
                  pluginManagement {
                      plugins {
                          id "org.openrewrite.rewrite" version "7.0.0"
                      }
                  }
                  """,
                """
                  pluginManagement {
                      plugins {
                          /*~~(org.openrewrite.rewrite:7.0.0)~~>*/id "org.openrewrite.rewrite" version "7.0.0"
                      }
                  }
                  """
              ),
              buildGradle(
                """
                  plugins {
                      id "org.openrewrite.rewrite"
                  }
                  """,
                """
                  plugins {
                      /*~~(org.openrewrite.rewrite:null)~~>*/id "org.openrewrite.rewrite"
                  }
                  """
              )
            );
        }

        @Test
        void externalPluginApplyFalse() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion() + ":" + plugin.isApplied())))),
              buildGradle(
                """
                  plugins {
                      id "org.openrewrite.rewrite" version "7.0.0" apply false
                  }
                  """,
                """
                  plugins {
                      /*~~(org.openrewrite.rewrite:7.0.0:false)~~>*/id "org.openrewrite.rewrite" version "7.0.0" apply false
                  }
                  """
              )
            );
        }

        @Test
        void apply() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath "org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0"
                      }
                  }
                  apply plugin: "org.openrewrite.rewrite"
                  """,
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath "org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0"
                      }
                  }
                  /*~~>*/apply plugin: "org.openrewrite.rewrite"
                  """
              )
            );
        }

        @Test
        void property() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              properties("rewritePluginVersion=7.0.0", spec -> spec.path("gradle.properties")),
              buildGradle(
                """
                  plugins {
                      id "java"
                      id "org.openrewrite.rewrite" version "${rewritePluginVersion}"
                  }
                  """,
                """
                  plugins {
                      /*~~>*/id "java"
                      /*~~>*/id "org.openrewrite.rewrite" version "${rewritePluginVersion}"
                  }
                  """
              )
            );
        }

        @Test
        void fullyQualifiedClassName() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  apply plugin: org.openrewrite.gradle.RewritePlugin
                  """,
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  /*~~>*/apply plugin: org.openrewrite.gradle.RewritePlugin
                  """
              )
            );
        }

        @Test
        void className() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  import org.openrewrite.gradle.RewritePlugin
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  apply plugin: RewritePlugin
                  """,
                """
                  import org.openrewrite.gradle.RewritePlugin
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  /*~~>*/apply plugin: RewritePlugin
                  """
              )
            );
        }

        @Test
        void markerId() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().pluginIdPattern("org.openrewrite.rewrite").acceptTransitive(true).asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                """
                  /*~~>*/plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                spec -> spec.markers(GradleProject.builder()
                  .group("group")
                  .name("name")
                  .version("version")
                  .path("path")
                  .plugins(Collections.singletonList(new GradlePluginDescriptor("org.openrewrite.gradle.GradlePlugin", "org.openrewrite.rewrite")))
                  .build()
                )
              )
            );
        }

        @Test
        void markerClass() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().pluginClass("org.openrewrite.gradle.GradlePlugin").acceptTransitive(true).asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                """
                  /*~~>*/plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                spec -> spec.markers(GradleProject.builder()
                  .group("group")
                  .name("name")
                  .version("version")
                  .path("path")
                  .plugins(Collections.singletonList(new GradlePluginDescriptor("org.openrewrite.gradle.GradlePlugin", "org.openrewrite.rewrite")))
                  .build()
                )
              )
            );
        }

        @Test
        void patternMatching() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().pluginIdPattern("org.openrewrite.*").asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  plugins {
                      id "java"
                      id "org.openrewrite.rewrite" version "7.0.0"
                  }
                  """,
                """
                  plugins {
                      id "java"
                      /*~~>*/id "org.openrewrite.rewrite" version "7.0.0"
                  }
                  """
              )
            );
        }

        @Test
        void alias() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              toml(
                """
                  [plugins]
                  openrewrite = { id = "org.openrewrite.rewrite", version = "7.0.0" }
                  """,
                spec -> spec.path("gradle/libs.versions.toml")
              ),
              buildGradle(
                """
                  plugins {
                      id "java"
                      alias libs.plugins.openrewrite
                  }
                  """,
                """
                  plugins {
                      /*~~>*/id "java"
                      /*~~>*/alias libs.plugins.openrewrite
                  }
                  """
              )
            );
        }

        @Test
        void settingsPlugin() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion() + ":" + plugin.isApplied())))),
              settingsGradle(
                """
                  plugins {
                      id "com.gradle.develocity" version "4.0.2"
                  }
                  """,
                """
                  plugins {
                      /*~~(com.gradle.develocity:4.0.2:true)~~>*/id "com.gradle.develocity" version "4.0.2"
                  }
                  """
              )
            );
        }
    }

    @Nested
    class KotlinDsl {
        @Test
        void corePlugin() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradleKts(
                """
                  plugins {
                      id("java")
                  }
                  """,
                """
                  plugins {
                      /*~~>*/id("java")
                  }
                  """
              )
            );
        }

        @Test
        void corePluginShorthand() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree()))))
                .beforeRecipe(withToolingApi()),
              buildGradleKts(
                """
                  plugins {
                      java
                  }
                  """,
                """
                  plugins {
                      /*~~>*/java
                  }
                  """
              )
            );
        }

        @Test
        void externalPlugin() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion())))),
              buildGradleKts(
                """
                  plugins {
                      id("org.openrewrite.rewrite") version "7.0.0"
                  }
                  """,
                """
                  plugins {
                      /*~~(org.openrewrite.rewrite:7.0.0)~~>*/id("org.openrewrite.rewrite") version "7.0.0"
                  }
                  """
              )
            );
        }

        @Test
        void externalPluginWithoutVersion() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion())))),
              settingsGradleKts(
                """
                  pluginManagement {
                      plugins {
                          id("org.openrewrite.rewrite") version "7.0.0"
                      }
                  }
                  """,
                """
                  pluginManagement {
                      plugins {
                          /*~~(org.openrewrite.rewrite:7.0.0)~~>*/id("org.openrewrite.rewrite") version "7.0.0"
                      }
                  }
                  """
              ),
              buildGradleKts(
                """
                  plugins {
                      id("org.openrewrite.rewrite")
                  }
                  """,
                """
                  plugins {
                      /*~~(org.openrewrite.rewrite:null)~~>*/id("org.openrewrite.rewrite")
                  }
                  """
              )
            );
        }

        @Test
        void externalPluginApplyFalse() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion() + ":" + plugin.isApplied())))),
              buildGradleKts(
                """
                  plugins {
                      id("org.openrewrite.rewrite") version "7.0.0" apply false
                  }
                  """,
                """
                  plugins {
                      /*~~(org.openrewrite.rewrite:7.0.0:false)~~>*/id("org.openrewrite.rewrite") version "7.0.0" apply false
                  }
                  """
              )
            );
        }

        @Test
        void apply() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradleKts(
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  apply(plugin="org.openrewrite.rewrite")
                  """,
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  /*~~>*/apply(plugin="org.openrewrite.rewrite")
                  """
              )
            );
        }

        @Test
        void kotlinPlugin() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion())))),
              buildGradleKts(
                """
                  plugins {
                      kotlin("jvm") version "2.1.21"
                  }
                  """,
                """
                  plugins {
                      /*~~(org.jetbrains.kotlin.jvm:2.1.21)~~>*/kotlin("jvm") version "2.1.21"
                  }
                  """
              )
            );
        }

        @Test
        void kotlinPluginWithoutVersion() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion())))),
              settingsGradleKts(
                """
                  pluginManagement {
                      plugins {
                          kotlin("jvm") version "2.1.21"
                      }
                  }
                  """,
                """
                  pluginManagement {
                      plugins {
                          /*~~(org.jetbrains.kotlin.jvm:2.1.21)~~>*/kotlin("jvm") version "2.1.21"
                      }
                  }
                  """
              ),
              buildGradleKts(
                """
                  plugins {
                      kotlin("jvm")
                  }
                  """,
                """
                  plugins {
                      /*~~(org.jetbrains.kotlin.jvm:null)~~>*/kotlin("jvm")
                  }
                  """
              )
            );
        }

        @Test
        void propertyViaSettings() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree()))))
                .beforeRecipe(withToolingApi()),
              properties("rewritePluginVersion=7.0.0", spec -> spec.path("gradle.properties")),
              settingsGradleKts(
                """
                  pluginManagement {
                      val rewritePluginVersion: String by settings
                      plugins {
                          id("org.openrewrite.rewrite") version "${rewritePluginVersion}"
                      }
                  }
                  """,
                """
                  pluginManagement {
                      val rewritePluginVersion: String by settings
                      plugins {
                          /*~~>*/id("org.openrewrite.rewrite") version "${rewritePluginVersion}"
                      }
                  }
                  """
              ),
              buildGradleKts(
                """
                  plugins {
                      id("java")
                      id("org.openrewrite.rewrite")
                  }
                  """,
                """
                  plugins {
                      /*~~>*/id("java")
                      /*~~>*/id("org.openrewrite.rewrite")
                  }
                  """
              )
            );
        }

        @Test
        void fullyQualifiedClassName() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradleKts(
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  apply<org.openrewrite.gradle.RewritePlugin>()
                  """,
                """
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  /*~~>*/apply<org.openrewrite.gradle.RewritePlugin>()
                  """
              )
            );
        }

        @Test
        void className() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradleKts(
                """
                  import org.openrewrite.gradle.RewritePlugin
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  apply<RewritePlugin>()
                  """,
                """
                  import org.openrewrite.gradle.RewritePlugin
                  buildscript {
                      repositories {
                          gradlePluginPortal()
                      }
                      dependencies {
                          classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.0.0")
                      }
                  }
                  /*~~>*/apply<RewritePlugin>()
                  """
              )
            );
        }

        @Test
        void markerId() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().pluginIdPattern("org.openrewrite.rewrite").acceptTransitive(true).asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                """
                  /*~~>*/plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                spec -> spec.markers(GradleProject.builder()
                  .group("group")
                  .name("name")
                  .version("version")
                  .path("path")
                  .plugins(Collections.singletonList(new GradlePluginDescriptor("org.openrewrite.gradle.GradlePlugin", "org.openrewrite.rewrite")))
                  .build()
                )
              )
            );
        }

        @Test
        void markerClass() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().pluginClass("org.openrewrite.gradle.GradlePlugin").acceptTransitive(true).asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradle(
                """
                  plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                """
                  /*~~>*/plugins {
                      id("com.example.my-plugin") version "1.0.0"
                  }
                  """,
                spec -> spec.markers(GradleProject.builder()
                  .group("group")
                  .name("name")
                  .version("version")
                  .path("path")
                  .plugins(Collections.singletonList(new GradlePluginDescriptor("org.openrewrite.gradle.GradlePlugin", "org.openrewrite.rewrite")))
                  .build()
                )
              )
            );
        }

        @Test
        void patternMatching() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().pluginIdPattern("org.openrewrite.*").asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              buildGradleKts(
                """
                  plugins {
                      id("java")
                      id("org.openrewrite.rewrite") version "7.0.0"
                  }
                  """,
                """
                  plugins {
                      id("java")
                      /*~~>*/id("org.openrewrite.rewrite") version "7.0.0"
                  }
                  """
              )
            );
        }

        @Test
        void alias() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree())))),
              toml(
                """
                  [plugins]
                  openrewrite = { id = "org.openrewrite.rewrite", version = "7.0.0" }
                  """,
                spec -> spec.path("gradle/libs.versions.toml")
              ),
              buildGradleKts(
                """
                  plugins {
                      id("java")
                      alias(libs.plugins.openrewrite)
                  }
                  """,
                """
                  plugins {
                      /*~~>*/id("java")
                      /*~~>*/alias(libs.plugins.openrewrite)
                  }
                  """
              )
            );
        }

        @Test
        void settingsPlugin() {
            rewriteRun(
              spec -> spec.recipe(RewriteTest.toRecipe(() -> new GradlePlugin.Matcher().asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId() + ":" + plugin.getVersion() + ":" + plugin.isApplied())))),
              settingsGradleKts(
                """
                  plugins {
                      id("com.gradle.develocity") version "4.0.2"
                  }
                  """,
                """
                  plugins {
                      /*~~(com.gradle.develocity:4.0.2:true)~~>*/id("com.gradle.develocity") version "4.0.2"
                  }
                  """
              )
            );
        }
    }
}
