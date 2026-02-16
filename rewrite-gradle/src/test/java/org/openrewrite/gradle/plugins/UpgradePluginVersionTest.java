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
import static org.openrewrite.gradle.Assertions.*;
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
          buildGradleKts(
            """
              plugins {
                  id("org.openrewrite.rewrite") version("5.40.0")
                  id("com.github.johnrengelman.shadow") version("6.1.0")
              }
              """,
            """
              plugins {
                  id("org.openrewrite.rewrite") version("5.40.6")
                  id("com.github.johnrengelman.shadow") version("6.1.0")
              }
              """
          )
        );
    }

    @Test
    void upgradeKotlinPluginLiteralVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("kotlin", "2.3.0", null)),
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.0.0"
              }
              """,
            """
              plugins {
                  kotlin("jvm") version "2.3.0"
              }
              """
          )
        );
    }

    @Test
    void upgradeKotlinPlugin() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("kotlin", "latest.minor", null)),
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "2.0.0"
              }
              """,
            spec -> spec.after(s -> {
                  assertThat(s).doesNotContain("2.0.0");
                  assertThat(s).containsPattern("2.\\d+.\\d+(.\\d+)*");
                  return s;
              }
            )
          )
        );
    }

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
    void change() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "5.x", null)),
          settingsGradle(
            """
              pluginManagement {
                  plugins {
                      String v = '5.40.0'
                      id 'org.openrewrite.rewrite' version v
                  }
              }
              """,
            """
              pluginManagement {
                  plugins {
                      String v = '5.40.6'
                      id 'org.openrewrite.rewrite' version v
                  }
              }
              """
          )
        );
    }

    @Test
    void dontDowngradeWhenExactVersionVariable() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "5.39.9", null)),
          settingsGradle(
            """
              pluginManagement {
                  plugins {
                      String v = '5.40.0'
                      id 'org.openrewrite.rewrite' version v
                  }
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

    @Test
    void upgradeSpringBootPluginWithoutDependencyManagementEnabled() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.springframework.boot", "3.2.4", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '2.7.0'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'javax.servlet:javax.servlet-api:4.0.1'
                  implementation 'org.apache.activemq:activemq-client-jakarta:5.18.2'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.2.4'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'javax.servlet:javax.servlet-api:4.0.1'
                  implementation 'org.apache.activemq:activemq-client-jakarta:5.18.2'
              }
              """
          )
        );
    }

    @Test
    void springBootPluginsAreDependencyManagedVersionAware() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradePluginVersion("org.springframework.boot", "3.2.4", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '2.7.0'
                  id 'io.spring.dependency-management' version '1.1.6'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'javax.servlet:javax.servlet-api'
                  implementation 'org.apache.activemq:activemq-client-jakarta:5.18.2'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.2.4'
                  id 'io.spring.dependency-management' version '1.1.6'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'javax.servlet:javax.servlet-api:4.0.1'
                  implementation 'org.apache.activemq:activemq-client-jakarta'
              }
              """
          )
        );
    }

    @Test
    void doesNotPinPropertyManagedVersions() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradePluginVersion("org.springframework.boot", "2.5.x", null)),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '2.5.14'
                  id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  runtimeOnly 'mysql:mysql-connector-java'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '2.5.15'
                  id 'io.spring.dependency-management' version '1.0.11.RELEASE'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  runtimeOnly 'mysql:mysql-connector-java'
              }
              """
          )
        );
    }

    @Test
    void dontDowngradeKotlinDslSharedVariable() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.jetbrains.kotlin.*", "1.5.31", null)),
          buildGradleKts(
            """
              plugins {
                  val kotlinVersion = "1.8.21"
                  id("org.jetbrains.kotlin.jvm") version kotlinVersion
                  id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
                  id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
                  id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
                  id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
              }
              """
          )
        );
    }

    @Test
    void dontDowngradeKotlinDslSinglePluginVariable() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.jetbrains.kotlin.*", "1.5.31", null)),
          buildGradleKts(
            """
              plugins {
                  val kotlinVersion = "1.8.21"
                  id("org.jetbrains.kotlin.jvm") version kotlinVersion
              }
              """
          )
        );
    }

    @Test
    void upgradeKotlinDslSharedVariable() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.jetbrains.kotlin.*", "2.0.0", null)),
          buildGradleKts(
            """
              plugins {
                  val kotlinVersion = "1.9.24"
                  id("org.jetbrains.kotlin.jvm") version kotlinVersion
                  id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
              }
              """,
            """
              plugins {
                  val kotlinVersion = "2.0.0"
                  id("org.jetbrains.kotlin.jvm") version kotlinVersion
                  id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
              }
              """
          )
        );
    }
}
