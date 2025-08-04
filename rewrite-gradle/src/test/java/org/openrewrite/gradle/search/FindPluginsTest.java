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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.text;

class FindPluginsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindPlugins("org.openrewrite.rewrite", "org.openrewrite.gradle.RewritePlugin"));
    }

    @DocumentExample
    @Test
    void findPlugin() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.openrewrite.rewrite' version '6.18.0'
              }
              """,
            """
              plugins {
                  id 'java'
                  /*~~>*/id 'org.openrewrite.rewrite' version '6.18.0'
              }
              """,
            spec -> spec.beforeRecipe(cu -> assertThat(FindPlugins.find(cu, "org.openrewrite.rewrite"))
              .isNotEmpty()
              .anySatisfy(p -> {
                  assertThat(p.getPluginId()).isEqualTo("org.openrewrite.rewrite");
                  assertThat(p.getVersion()).isEqualTo("6.18.0");
              }))
          )
        );
    }

    @Test
    void settingsResolutionStrategy() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          settingsGradle(
              """
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                }
                resolutionStrategy {
                    eachPlugin {
                        if (requested.id.id == 'org.openrewrite.rewrite') {
                            useVersion('6.22.0')
                        }
                    }
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
                  /*~~>*/id "org.openrewrite.rewrite"
              }
              """,
            spec -> spec.beforeRecipe(cu -> assertThat(FindPlugins.find(cu, "org.openrewrite.rewrite"))
              .isNotEmpty()
              .anySatisfy(p -> assertThat(p.getPluginId()).isEqualTo("org.openrewrite.rewrite")))
          )
        );
    }

    @Test
    void property() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          properties("rewritePluginVersion=6.22.0", spec -> spec.path("gradle.properties")),
          buildGradle(
            """
              plugins {
                  id "org.openrewrite.rewrite" version "${rewritePluginVersion}"
              }
              """,
            """
              plugins {
                  /*~~>*/id "org.openrewrite.rewrite" version "${rewritePluginVersion}"
              }
              """,
            spec -> spec.beforeRecipe(cu -> assertThat(FindPlugins.find(cu, "org.openrewrite.rewrite"))
              .isNotEmpty()
              .anySatisfy(p -> assertThat(p.getPluginId()).isEqualTo("org.openrewrite.rewrite")))
          )
        );
    }

    @Test
    void buildscriptId() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
             buildscript {
                 repositories {
                     gradlePluginPortal()
                 }
                 dependencies {
                     classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.1.3")
                 }
             }
             apply plugin: "org.openrewrite.rewrite"
             """,
            """
             /*~~>*/buildscript {
                 repositories {
                     gradlePluginPortal()
                 }
                 dependencies {
                     classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.1.3")
                 }
             }
             apply plugin: "org.openrewrite.rewrite"
             """
          )
        );
    }

    @Test
    void buildscriptClassname() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
             buildscript {
                 repositories {
                     gradlePluginPortal()
                 }
                 dependencies {
                     classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.1.3")
                 }
             }
             apply plugin: org.openrewrite.gradle.RewritePlugin
             """,
            """
             /*~~>*/buildscript {
                 repositories {
                     gradlePluginPortal()
                 }
                 dependencies {
                     classpath("org.openrewrite.rewrite:org.openrewrite.rewrite.gradle.plugin:7.1.3")
                 }
             }
             apply plugin: org.openrewrite.gradle.RewritePlugin
             """
          )
        );
    }

    @Test
    void findPluginFromGradleProjectMarker() {
        rewriteRun(
          text(
            "stand-in for a kotlin gradle script",
            "~~>stand-in for a kotlin gradle script",
            spec -> spec.markers(GradleProject.builder()
              .group("group")
              .name("name")
              .version("version")
              .path("path")
              .plugins(singletonList(new GradlePluginDescriptor("org.openrewrite.gradle.GradlePlugin", "org.openrewrite.rewrite")))
              .build()
            )
          )
        );
    }
}
