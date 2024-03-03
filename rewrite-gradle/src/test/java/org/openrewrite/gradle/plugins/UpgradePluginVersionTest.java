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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;

class UpgradePluginVersionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample("Upgrading a build plugin")
    @Test
    void upgradePlugin() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "latest.patch", null)),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '5.40.0'
                  id 'com.github.johnrengelman.shadow' version '6.1.0'
              }
              """,
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '5.40.6'
                  id 'com.github.johnrengelman.shadow' version '6.1.0'
              }
              """
          )
        );
    }

    @Test
    void pluginManagementPlugin() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "latest.patch", null)),
          settingsGradle(
            """
              pluginManagement {
                  plugins {
                      id 'org.openrewrite.rewrite' version '5.40.0'
                  }
              }
              """,
            """
              pluginManagement {
                  plugins {
                      id 'org.openrewrite.rewrite' version '5.40.6'
                  }
              }
              """
          )
        );
    }

    @DocumentExample("Upgrading a settings plugin")
    @Test
    void upgradeGradleSettingsPlugin() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("com.gradle.enterprise", "3.10.x", null)),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.10'
              }
              """,
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.10.3'
              }
              """
          )
        );
    }

    @Test
    void upgradePluginGlob() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.*", "5.40.X", null)),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '5.40.0'
              }
              """,
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '5.40.6'
              }
              """
          )
        );
    }

    @Test
    void exactVersionDoesNotHaveToBeResolvable() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "999.0", null)),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '5.34.0'
              }
              """,
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '999.0'
              }
              """
          )
        );
    }

    @Test
    void defaultsToLatestRelease() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", null, null)),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '5.34.0'
              }
              """,
            spec -> spec.after(after -> {
                Matcher versionMatcher = Pattern.compile("id 'org\\.openrewrite\\.rewrite' version '(.*?)'").matcher(after);
                assertThat(versionMatcher.find()).isTrue();
                String version = versionMatcher.group(1);
                VersionComparator versionComparator = requireNonNull(Semver.validate("[6.1.16,)", null).getValue());
                assertThat(versionComparator.compare(null, "6.1.16", version)).isLessThanOrEqualTo(0);

                return """
                  plugins {
                      id 'org.openrewrite.rewrite' version '%s'
                  }
                  """.formatted(version);
            })
          )
        );
    }

    @Test
    void dontDowngradeWhenExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("io.spring.dependency-management", "1.0.15.RELEASE", null)),
          buildGradle(
            """
              plugins {
                  id 'io.spring.dependency-management' version '1.1.0'
              }
              """
          )
        );
    }

    @Test
    void dontDowngradeWhenExactVersionProperties() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("io.spring.dependency-management", "1.0.15.RELEASE", null)),
          buildGradle(
            """
              plugins {
                  id 'io.spring.dependency-management' version "$springDependencyManagementVersion"
              }
              """
          ),
          properties(
            """
              springDependencyManagementVersion=1.1.0
              """,
            spec -> spec.path("gradle.properties")
          )
        );
    }

    @DocumentExample("Upgrading a build plugin with version in gradle.properties")
    @Test
    void upgradePluginVersionInProperties() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "5.40.x", null)),
          properties(
            """
              rewriteVersion=5.40.0
              """,
            """
              rewriteVersion=5.40.6
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version "$rewriteVersion"
                  id 'com.github.johnrengelman.shadow' version '6.1.0'
              }
              """
          )
        );
    }

    @Test
    void upgradePluginVersionInPropertiesWhenUsingGlobs() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.*", "5.40.X", null)),
          properties(
            """
              rewriteVersion=5.40.0
              """,
            """
              rewriteVersion=5.40.6
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version "$rewriteVersion"
                  id 'com.github.johnrengelman.shadow' version '6.1.0'
              }
              """
          )
        );
    }

    @Test
    void upgradePluginVersionInBuildGradleNotProperties() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "5.40.7", null)),
          properties(
            """
              org.openrewrite.rewrite=5.40.0
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version "5.40.0"
              }
              """,
            """
              plugins {
                  id 'org.openrewrite.rewrite' version "5.40.7"
              }
              """
          )
        );
    }
}
