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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.toml.Assertions.toml;

class UpgradeDependencyVersionTomlCatalogTest implements RewriteTest {

    @DocumentExample
    @Test
    void bumpsInlineVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          toml(
            """
              [libraries]
              widgetA = { module = "com.acme:widget-a", version = "1.0" }
              widgetB = { group = "com.acme", name = "widget-b", version = "1.0" }
              """,
            """
              [libraries]
              widgetA = { module = "com.acme:widget-a", version = "2.0" }
              widgetB = { group = "com.acme", name = "widget-b", version = "1.0" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void bumpsStringNotation() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          toml(
            """
              [libraries]
              widgetA = "com.acme:widget-a:1.0"
              """,
            """
              [libraries]
              widgetA = "com.acme:widget-a:2.0"
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void targetingOneSharerDetachesItFromTheSharedVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          toml(
            """
              [versions]
              widget = "1.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version.ref = "widget" }
              widgetB = { module = "com.acme:widget-b", version.ref = "widget" }
              """,
            """
              [versions]
              widget = "1.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version = "2.0" }
              widgetB = { module = "com.acme:widget-b", version.ref = "widget" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void targetingEverySharerBumpsTheSharedVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          toml(
            """
              [versions]
              widget = "1.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version.ref = "widget" }
              widgetB = { module = "com.acme:widget-b", version.ref = "widget" }
              """,
            """
              [versions]
              widget = "2.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version.ref = "widget" }
              widgetB = { module = "com.acme:widget-b", version.ref = "widget" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void pluginReferrerKeepsTheSharedVersionFromMoving() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-*", "2.0", null)),
          toml(
            """
              [versions]
              widget = "1.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version.ref = "widget" }

              [plugins]
              widget = { id = "com.acme.widget", version.ref = "widget" }
              """,
            """
              [versions]
              widget = "1.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version = "2.0" }

              [plugins]
              widget = { id = "com.acme.widget", version.ref = "widget" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void sequentialRecipesDetachAllButTheLastReferrer() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null),
            new UpgradeDependencyVersion("com.acme", "widget-b", "2.0", null)
          ),
          toml(
            """
              [versions]
              widget = "1.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version.ref = "widget" }
              widgetB = { module = "com.acme:widget-b", version.ref = "widget" }
              """,
            """
              [versions]
              widget = "2.0"

              [libraries]
              widgetA = { module = "com.acme:widget-a", version = "2.0" }
              widgetB = { module = "com.acme:widget-b", version.ref = "widget" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void symbolicVersionResolvedThroughTheBuildsRepositories() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "30.x", "-jre")),
          settingsGradle(
            """
              rootProject.name = 'catalog-file'
              """
          ),
          toml(
            """
              [libraries]
              guava = { module = "com.google.guava:guava", version = "29.0-jre" }
              """,
            """
              [libraries]
              guava = { module = "com.google.guava:guava", version = "30.1.1-jre" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          ),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation libs.guava
              }
              """
          )
        );
    }

    @Test
    void ignoresTomlThatIsNotACatalog() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.acme", "widget-a", "2.0", null)),
          toml(
            """
              [libraries]
              widgetA = { module = "com.acme:widget-a", version = "1.0" }
              """,
            spec -> spec.path("config.toml")
          )
        );
    }
}
