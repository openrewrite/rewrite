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
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.toml.Assertions.toml;

class GradleVersionCatalogPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() ->
                new GradleVersionCatalogPlugin.Matcher().asVisitor(plugin ->
                        SearchResult.found(plugin.getTree(), plugin.getPluginId() +
                                (plugin.getVersion() == null ? "" : ":" + plugin.getVersion()) +
                                (plugin.getVersionRef() == null ? "" : " (ref=" + plugin.getVersionRef() + ")")))));
    }

    @Test
    void matchesStringNotation() {
        rewriteRun(
                toml(
                        """
                          [plugins]
                          kotlin = "org.jetbrains.kotlin.jvm:2.0.0"
                          """,
                        """
                          [plugins]
                          ~~(org.jetbrains.kotlin.jvm:2.0.0)~~>kotlin = "org.jetbrains.kotlin.jvm:2.0.0"
                          """,
                        spec -> spec.path("gradle/libs.versions.toml")
                )
        );
    }

    @Test
    void matchesInlineTableAndVersionRef() {
        rewriteRun(
                toml(
                        """
                          [versions]
                          kotlin = "2.0.0"

                          [plugins]
                          kotlin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
                          """,
                        """
                          [versions]
                          kotlin = "2.0.0"

                          [plugins]
                          ~~(org.jetbrains.kotlin.jvm (ref=kotlin))~~>kotlin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
                          """,
                        spec -> spec.path("gradle/libs.versions.toml")
                )
        );
    }

    @Test
    void filtersPluginId() {
        rewriteRun(
                spec -> spec.recipe(RewriteTest.toRecipe(() ->
                        new GradleVersionCatalogPlugin.Matcher()
                                .pluginIdPattern("org.jetbrains.kotlin.*")
                                .asVisitor(plugin -> SearchResult.found(plugin.getTree(), plugin.getPluginId())))),
                toml(
                        """
                          [plugins]
                          kotlin = "org.jetbrains.kotlin.jvm:2.0.0"
                          spotless = "com.diffplug.spotless:6.25.0"
                          """,
                        """
                          [plugins]
                          ~~(org.jetbrains.kotlin.jvm)~~>kotlin = "org.jetbrains.kotlin.jvm:2.0.0"
                          spotless = "com.diffplug.spotless:6.25.0"
                          """,
                        spec -> spec.path("gradle/libs.versions.toml")
                )
        );
    }

    @Test
    void updatesStringNotationAndPreservesQuoteStyle() {
        rewriteRun(
                spec -> spec.recipe(RewriteTest.toRecipe(() ->
                        new GradleVersionCatalogPlugin.Matcher()
                                .pluginIdPattern("org.jetbrains.kotlin.jvm")
                                .asVisitor(plugin -> plugin.withVersion("2.1.0")))),
                toml(
                        """
                          [plugins]
                          kotlin = 'org.jetbrains.kotlin.jvm:2.0.0'
                          """,
                        """
                          [plugins]
                          kotlin = 'org.jetbrains.kotlin.jvm:2.1.0'
                          """,
                        spec -> spec.path("gradle/libs.versions.toml")
                )
        );
    }

    @Test
    void updatesInlineTable() {
        rewriteRun(
                spec -> spec.recipe(RewriteTest.toRecipe(() ->
                        new GradleVersionCatalogPlugin.Matcher()
                                .pluginIdPattern("org.jetbrains.kotlin.jvm")
                                .asVisitor(plugin -> plugin.withVersion("2.1.0")))),
                toml(
                        """
                          [plugins]
                          kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.0.0" }
                          """,
                        """
                          [plugins]
                          kotlin = { id = "org.jetbrains.kotlin.jvm", version = "2.1.0" }
                          """,
                        spec -> spec.path("gradle/libs.versions.toml")
                )
        );
    }

    @Test
    void doesNotUpdateVersionRef() {
        rewriteRun(
                spec -> spec.recipe(RewriteTest.toRecipe(() ->
                        new GradleVersionCatalogPlugin.Matcher()
                                .pluginIdPattern("org.jetbrains.kotlin.jvm")
                                .asVisitor(plugin -> plugin.withVersion("2.1.0")))),
                toml(
                        """
                          [plugins]
                          kotlin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
                          """
                , spec -> spec.path("gradle/libs.versions.toml"))
        );
    }
}
