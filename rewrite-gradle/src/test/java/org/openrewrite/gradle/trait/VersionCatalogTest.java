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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.toml.Assertions.toml;

class VersionCatalogTest implements RewriteTest {

    @Test
    void matchesSettingsAndTomlCatalogs() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new VersionCatalog.Matcher().asVisitor(catalog -> SearchResult.found(catalog.getTree(),
              catalog.getVersion(new GroupArtifact("com.google.guava", "guava")))))),
          settingsGradle(
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      libs {
                          version('guava', '29.0-jre')
                          library('guava', 'com.google.guava', 'guava').versionRef('guava')
                      }
                  }
              }
              """,
            """
              dependencyResolutionManagement {
                  versionCatalogs {
                      /*~~(29.0-jre)~~>*/libs {
                          version('guava', '29.0-jre')
                          library('guava', 'com.google.guava', 'guava').versionRef('guava')
                      }
                  }
              }
              """
          ),
          toml(
            """
              [versions]
              guava = "30.1.1-jre"

              [libraries]
              guava = { module = "com.google.guava:guava", version.ref = "guava" }
              """,
            """
              ~~(30.1.1-jre)~~>[versions]
              guava = "30.1.1-jre"

              [libraries]
              guava = { module = "com.google.guava:guava", version.ref = "guava" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }
}
