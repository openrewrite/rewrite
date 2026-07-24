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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.toml.tree.Toml;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.toml.Assertions.toml;

class GradleVersionCatalogTest implements RewriteTest {
    private static final AtomicInteger selections = new AtomicInteger();
    private static final AtomicReference<String> consumerVersion = new AtomicReference<>();

    @Test
    void rejectsConflictingReplacementForCrossSectionConsumers() throws Exception {
        GradleVersionCatalogDependency dependency =
          new GradleVersionCatalogDependency(null, "org.example", "library", null, "shared");
        GradleVersionCatalogPlugin plugin =
          new GradleVersionCatalogPlugin(null, "org.example.plugin", null, "shared");
        List<GradleVersionCatalog.VersionRefConsumer> consumers = Arrays.asList(
          new GradleVersionCatalog.VersionRefConsumer("shared", dependency, null),
          new GradleVersionCatalog.VersionRefConsumer("shared", null, plugin));
        GradleVersionCatalog catalog = new GradleVersionCatalog(null,
          Collections.singletonList(dependency), Collections.singletonList(plugin),
          Collections.singletonMap("shared", "1.0"),
          Collections.singletonMap("shared", consumers));

        Map<String, String> replacements = catalog.safeVersionRefReplacements((consumer, currentVersion) ->
          consumer.getDependency() == null ? "3.0" : "2.0");

        assertThat(replacements).isEmpty();
    }

    @Test
    void selectsReferencedVersionOnceAndSuppliesItToConsumerUpdate() {
        selections.set(0);
        consumerVersion.set(null);
        rewriteRun(
          spec -> spec.recipe(new CountingVersionCatalogUpdateRecipe()),
          toml(
            """
              [versions]
              shared = "2.0"
              
              [libraries]
              library = { group = "org.example", name = "library", version.ref = "shared" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
        assertThat(selections).hasValue(1);
        assertThat(consumerVersion).hasValue("2.0");
    }

    static class CountingVersionCatalogUpdateRecipe extends Recipe {
        @Override
        public String getDisplayName() {
            return "Update catalog version";
        }

        @Override
        public String getDescription() {
            return "Updates a version catalog through its document-level API.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return GradleVersionCatalog.visitor(new GradleVersionCatalog.VersionCatalogUpdate() {
                @Override
                public String selectReferencedVersion(GradleVersionCatalog.VersionRefConsumer consumer,
                                                      String currentVersion, ExecutionContext ctx) {
                    selections.incrementAndGet();
                    return "2.0";
                }

                @Override
                public Toml.KeyValue updateDependency(GradleVersionCatalogDependency dependency,
                                                      String referencedVersion, ExecutionContext ctx) {
                    consumerVersion.set(referencedVersion);
                    return dependency.getTree();
                }
            });
        }
    }
}
