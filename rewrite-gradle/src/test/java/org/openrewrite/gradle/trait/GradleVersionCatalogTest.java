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

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.toml.Assertions.toml;

class GradleVersionCatalogTest implements RewriteTest {
    @Test
    void matchesTheConventionalCatalogDocument() {
        CatalogTestState state = new CatalogTestState();
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new MatchingCatalogRecipe(state).getVisitor())),
          toml(
            """
              [versions]
              shared = "1.0"
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
        assertThat(state.catalogMatches).isEqualTo(1);
    }

    @Test
    void matchesACustomCatalogPath() {
        CatalogTestState state = new CatalogTestState();
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new MatchingCatalogRecipe(state, Path.of("config/libs.versions.toml")).getVisitor())),
          toml(
            """
              [versions]
              shared = "1.0"
              """,
            spec -> spec.path("config/libs.versions.toml")
          )
        );
        assertThat(state.catalogMatches).isEqualTo(1);
    }

    @Test
    void leavesSharedVersionUnchangedWhenConsumersSelectDifferentReplacements() {
        rewriteRun(
          spec -> spec.recipe(new ConflictingVersionCatalogUpdateRecipe()),
          toml(
            """
              [versions]
              shared = "1.0"
              
              [libraries]
              library = { group = "org.example", name = "library", version.ref = "shared" }
              
              [plugins]
              plugin = { id = "org.example.plugin", version.ref = "shared" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void selectsReferencedVersionOnceAndSuppliesItToConsumerUpdate() {
        CatalogTestState state = new CatalogTestState();
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new CountingVersionCatalogUpdateRecipe(state).getVisitor())),
          toml(
            """
              [versions]
              shared = '1.0'

              [libraries]
              library = { group = "org.example", name = "library", version.ref = "shared" }
              """,
            """
              [versions]
              shared = '2.0'

              [libraries]
              library = { group = "org.example", name = "library", version.ref = "shared" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
        assertThat(state.selections).isEqualTo(1);
        assertThat(state.consumerVersion).isEqualTo("2.0");
    }

    @Test
    void doesNotUpdateConsumersWithMissingVersionDeclarations() {
        CatalogTestState state = new CatalogTestState();
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new CountingVersionCatalogUpdateRecipe(state).getVisitor())),
          toml(
            """
              [libraries]
              library = { group = "org.example", name = "library", version.ref = "missing" }
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
        assertThat(state.selections).isZero();
        assertThat(state.consumerVersion).isNull();
    }

    @Test
    void composesCatalogVisitorAfterChildTransformations() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> {
              TreeVisitor<?, ExecutionContext> catalogVisitor = GradleVersionCatalog.visitor(
                new GradleVersionCatalog.VersionCatalogUpdate() {
                    @Override
                    public String selectReferencedVersion(
                            GradleVersionCatalog.@NonNull VersionRefConsumer consumer,
                            @NonNull String currentVersion, @NonNull ExecutionContext ctx) {
                        return null;
                    }

                    @Override
                    public Toml.@NonNull KeyValue updateDependency(
                            @NonNull GradleVersionCatalogDependency dependency,
                            String referencedVersion, @NonNull ExecutionContext ctx) {
                        return dependency.withVersion("2.0");
                    }
                });
              return new org.openrewrite.toml.TomlIsoVisitor<ExecutionContext>() {
                  @Override
                  public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                      Toml.Document transformed = (Toml.Document) new org.openrewrite.toml.TomlIsoVisitor<ExecutionContext>() {
                          @Override
                          public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                              if (keyValue.getKey() instanceof Toml.Identifier &&
                                      "library".equals(((Toml.Identifier) keyValue.getKey()).getName()) &&
                                      keyValue.getValue() instanceof Toml.Literal &&
                                      "org.example:library:1.0".equals(
                                              ((Toml.Literal) keyValue.getValue()).getValue())) {
                                  Toml.Literal literal = (Toml.Literal) keyValue.getValue();
                                  return keyValue.withValue(literal.withSource("\"org.example:library:1.1\"")
                                          .withValue("org.example:library:1.1"));
                              }
                              return super.visitKeyValue(keyValue, ctx);
                          }
                      }.visit(document, ctx);
                      return (Toml.Document) catalogVisitor.visit(transformed, ctx);
                  }
              };
          })),
          toml(
            """
              [libraries]
              library = "org.example:library:1.0"
              """,
            """
              [libraries]
              library = "org.example:library:2.0"
              """,
            spec -> spec.path("gradle/libs.versions.toml")
          )
        );
    }

    @Test
    void ignoresLibsVersionsTomlOutsideGradleDirectory() {
        CatalogTestState state = new CatalogTestState();
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new CountingVersionCatalogUpdateRecipe(state).getVisitor())),
          toml(
            """
              [versions]
              shared = "1.0"

              [libraries]
              library = { group = "org.example", name = "library", version.ref = "shared" }
              """,
            spec -> spec.path("libs.versions.toml")
          )
        );
        assertThat(state.selections).isZero();
    }

    @Test
    void ignoresLibsVersionsTomlInNestedGradleBuild() {
        CatalogTestState state = new CatalogTestState();
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new CountingVersionCatalogUpdateRecipe(state).getVisitor())),
          toml(
            """
              [versions]
              shared = "1.0"

              [libraries]
              library = { group = "org.example", name = "library", version.ref = "shared" }
              """,
            spec -> spec.path("included-build/gradle/libs.versions.toml")
          )
        );
        assertThat(state.selections).isZero();
    }

    private static final class CatalogTestState {
        private int selections;
        private int catalogMatches;
        private String consumerVersion;
    }

    static class MatchingCatalogRecipe extends Recipe {
        private final CatalogTestState state;
        private final Path catalogPath;

        MatchingCatalogRecipe(CatalogTestState state) {
            this(state, Path.of("gradle/libs.versions.toml"));
        }

        MatchingCatalogRecipe(CatalogTestState state, Path catalogPath) {
            this.state = state;
            this.catalogPath = catalogPath;
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Match version catalog";
        }

        @Override
        public @NonNull String getDescription() {
            return "Matches a Gradle version catalog document.";
        }

        @Override
        public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
            return new GradleVersionCatalog.Matcher().catalogPath(catalogPath).asVisitor((catalog, ctx) -> {
                state.catalogMatches++;
                return catalog.getTree();
            });
        }
    }

    static class CountingVersionCatalogUpdateRecipe extends Recipe {
        private final CatalogTestState state;

        CountingVersionCatalogUpdateRecipe(CatalogTestState state) {
            this.state = state;
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Update catalog version";
        }

        @Override
        public @NonNull String getDescription() {
            return "Updates a version catalog through its document-level API.";
        }

        @Override
        public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
            return GradleVersionCatalog.visitor(new GradleVersionCatalog.VersionCatalogUpdate() {
                @Override
                public String selectReferencedVersion(GradleVersionCatalog.@NonNull VersionRefConsumer consumer,
                                                      @NonNull String currentVersion, @NonNull ExecutionContext ctx) {
                    if ("1.0".equals(currentVersion)) {
                        state.selections++;
                        return "2.0";
                    }
                    return null;
                }

                @Override
                public Toml.@NonNull KeyValue updateDependency(@NonNull GradleVersionCatalogDependency dependency,
                                                               String referencedVersion, @NonNull ExecutionContext ctx) {
                    if (referencedVersion != null) {
                        state.consumerVersion = referencedVersion;
                    }
                    return dependency.getTree();
                }
            });
        }
    }

    static class ConflictingVersionCatalogUpdateRecipe extends Recipe {
        @Override
        public @NonNull String getDisplayName() {
            return "Update conflicting catalog versions";
        }

        @Override
        public @NonNull String getDescription() {
            return "Verifies that conflicting shared version selections are not applied.";
        }

        @Override
        public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
            return GradleVersionCatalog.visitor((consumer, currentVersion, ctx) -> consumer.getDependency() == null ? "3.0" : "2.0");
        }
    }
}
