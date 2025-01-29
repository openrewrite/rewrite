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
import org.openrewrite.gradle.ChangeDependency;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class ChangePluginTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new ChangePlugin("org.openrewrite.rewrite", "io.moderne.rewrite", "0.x"));
    }

    @DocumentExample
    @Test
    void changePlugin() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "org.openrewrite.rewrite" version "6.0.0"
              }
              """,
            """
              plugins {
                  id "io.moderne.rewrite" version "0.39.0"
              }
              """,
            spec -> spec.afterRecipe(g -> {
                Optional<GradleProject> maybeGp = g.getMarkers().findFirst(GradleProject.class);
                assertThat(maybeGp).isPresent()
                  .hasValueSatisfying(gp -> assertThat(gp.getPlugins()).filteredOn(plugin -> "org.openrewrite.rewrite".equals(plugin.getId())).isEmpty())
                  .hasValueSatisfying(gp -> assertThat(gp.getPlugins()).filteredOn(plugin -> "io.moderne.rewrite".equals(plugin.getId())).hasSize(1));
            })
          )
        );
    }

    @Test
    void changeApplyPluginSyntax() {
        rewriteRun(
          spec -> spec.recipes(
            new ChangeDependency("org.openrewrite", "plugin", "io.moderne", "moderne-gradle-plugin", "0.x", null, null),
            new ChangePlugin("org.openrewrite.rewrite", "io.moderne.rewrite", null)
          ),
          buildGradle(
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath "org.openrewrite:plugin:6.0.0"
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
                      classpath "io.moderne:moderne-gradle-plugin:0.39.0"
                  }
              }
              apply plugin: "io.moderne.rewrite"
              """
          )
        );
    }

    @Test
    void defaultsToLatestRelease() {
        rewriteRun(
          spec -> spec.recipe(new ChangePlugin("org.openrewrite.rewrite", "io.moderne.rewrite", null)),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '6.0.0'
              }
              """,
            spec -> spec.after(after -> {
                Matcher versionMatcher = Pattern.compile("id 'io\\.moderne\\.rewrite' version '(.*?)'").matcher(after);
                assertThat(versionMatcher.find()).isTrue();
                String version = versionMatcher.group(1);
                VersionComparator versionComparator = requireNonNull(Semver.validate("[1.0.34,)", null).getValue());
                assertThat(versionComparator.compare(null, "1.0.34", version)).isLessThanOrEqualTo(0);

                //language=gradle
                return """
                  plugins {
                      id 'io.moderne.rewrite' version '%s'
                  }
                  """.formatted(version);
            })
          )
        );
    }

    @Test
    void dontChangeExisting() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'io.moderne.rewrite' version '1.0.34'
              }
              """
          )
        );
    }
}
