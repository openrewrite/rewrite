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
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.toml.Assertions.toml;

class GradleVersionCatalogDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() ->
          new GradleVersionCatalogDependency.Matcher().asVisitor(dep ->
            SearchResult.found(dep.getTree(), dep.getGroupId() + ":" + dep.getArtifactId() +
              (dep.getVersion() != null ? ":" + dep.getVersion() : "") +
              (dep.getVersionRef() != null ? " (ref=" + dep.getVersionRef() + ")" : "")))));
    }

    @DocumentExample
    @Test
    void matchesStringNotationLibrary() {
        rewriteRun(
          toml(
            """
              [libraries]
              guava = "com.google.guava:guava:29.0-jre"
              """,
            """
              [libraries]
              ~~(com.google.guava:guava:29.0-jre)~~>guava = "com.google.guava:guava:29.0-jre"
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void matchesInlineTableLibraryWithVersion() {
        rewriteRun(
          toml(
            """
              [libraries]
              guava = { group = "com.google.guava", name = "guava", version = "29.0-jre" }
              """,
            """
              [libraries]
              ~~(com.google.guava:guava:29.0-jre)~~>guava = { group = "com.google.guava", name = "guava", version = "29.0-jre" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void matchesInlineTableLibraryWithVersionRef() {
        rewriteRun(
          toml(
            """
              [versions]
              guava = "29.0-jre"

              [libraries]
              guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }
              """,
            """
              [versions]
              guava = "29.0-jre"

              [libraries]
              ~~(com.google.guava:guava (ref=guava))~~>guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void doesNotMatchNonLibraryTableEntries() {
        rewriteRun(
          toml(
            """
              [versions]
              guava = "29.0-jre"

              [libraries]
              guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }
              """,
            """
              [versions]
              guava = "29.0-jre"

              [libraries]
              ~~(com.google.guava:guava (ref=guava))~~>guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void filtersOnGroupPattern() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .groupPattern("com.google.*")
              .asVisitor(dep -> SearchResult.found(dep.getTree(), dep.getGroupId())))),
          toml(
            """
              [libraries]
              guava = "com.google.guava:guava:29.0-jre"
              junit = "junit:junit:4.13"
              """,
            """
              [libraries]
              ~~(com.google.guava)~~>guava = "com.google.guava:guava:29.0-jre"
              junit = "junit:junit:4.13"
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void filtersOnArtifactPattern() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .artifactPattern("guava")
              .asVisitor(dep -> SearchResult.found(dep.getTree(), dep.getArtifactId())))),
          toml(
            """
              [libraries]
              guava = "com.google.guava:guava:29.0-jre"
              junit = "junit:junit:4.13"
              """,
            """
              [libraries]
              ~~(guava)~~>guava = "com.google.guava:guava:29.0-jre"
              junit = "junit:junit:4.13"
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void withVersionUpdatesStringNotation() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .groupPattern("com.google.guava")
              .artifactPattern("guava")
              .asVisitor(dep -> dep.withVersion("30.1-jre")))),
          toml(
            """
              [libraries]
              guava = "com.google.guava:guava:29.0-jre"
              """,
            """
              [libraries]
              guava = "com.google.guava:guava:30.1-jre"
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void withVersionUpdatesInlineTable() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .groupPattern("com.google.guava")
              .artifactPattern("guava")
              .asVisitor(dep -> dep.withVersion("30.1-jre")))),
          toml(
            """
              [libraries]
              guava = { group = "com.google.guava", name = "guava", version = "29.0-jre" }
              """,
            """
              [libraries]
              guava = { group = "com.google.guava", name = "guava", version = "30.1-jre" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void withVersionPreservesSingleQuotes() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .groupPattern("com.google.guava")
              .artifactPattern("guava")
              .asVisitor(dep -> dep.withVersion("30.1-jre")))),
          toml(
            """
              [libraries]
              guava = 'com.google.guava:guava:29.0-jre'
              """,
            """
              [libraries]
              guava = 'com.google.guava:guava:30.1-jre'
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void withVersionDoesNotModifyVersionRefEntry() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .groupPattern("com.google.guava")
              .artifactPattern("guava")
              .asVisitor(dep -> dep.withVersion("30.1-jre")))),
          toml(
            """
              [libraries]
              guava = { group = "com.google.guava", name = "guava", version.ref = "guava" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void withInlineCoordinatesUpdatesGroupAndName() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .groupPattern("org.old")
              .artifactPattern("old-artifact")
              .asVisitor(dep -> dep.withInlineCoordinatesAndVersion("org.new", "new-artifact", null, false)))),
          toml(
            """
              [libraries]
              my-lib = { group = "org.old", name = "old-artifact" }
              """,
            """
              [libraries]
              my-lib = { group = "org.new", name = "new-artifact" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void withInlineCoordinatesAddsVersionWhenOverrideManagedVersion() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new GradleVersionCatalogDependency.Matcher()
              .groupPattern("org.old")
              .artifactPattern("old-artifact")
              .asVisitor(dep -> dep.withInlineCoordinatesAndVersion("org.new", "new-artifact", "2.0", true)))),
          toml(
            """
              [libraries]
              my-lib = { group = "org.old", name = "old-artifact" }
              """,
            """
              [libraries]
              my-lib = { group = "org.new", name = "new-artifact", version = "2.0" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }
}
