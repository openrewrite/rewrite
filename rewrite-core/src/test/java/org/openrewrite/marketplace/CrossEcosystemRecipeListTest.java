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
package org.openrewrite.marketplace;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.Recipe;
import org.openrewrite.config.ClasspathScanningLoader;
import org.openrewrite.config.DeclarativeRecipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A declarative recipe packaged inside a recipe JAR whose {@code recipeList} names a recipe
 * contributed by another package ecosystem (npm, pip, NuGet, Go). Those names have no class to
 * find on the JAR's classpath, so they have to resolve through the marketplace.
 * See moderneinc/customer-requests#2953.
 */
class CrossEcosystemRecipeListTest {

    private static final String NPM_RECIPE = "org.openrewrite.primeng.UpgradeComponentsTo18";

    private static final RecipeBundle NPM_BUNDLE =
            new RecipeBundle("npm", "@openrewrite/recipes-angular", null, "1.6.0", null);

    private static final RecipeBundle MAVEN_BUNDLE =
            new RecipeBundle("maven", "com.example:angular-pass2-recipes", null, "0.1.0", null);

    @TempDir
    Path tempDir;

    @Test
    void jarBorneYamlResolvesRecipeFromAnotherEcosystem() throws IOException {
        Path jar = jarWithYaml(NPM_RECIPE);
        try (URLClassLoader classLoader = classLoaderOver(jar)) {
            StubResolver npm = new StubResolver("npm");

            Recipe composite = activate(classLoader, marketplaceWith(NPM_RECIPE, NPM_BUNDLE), npm);

            assertThat(composite.validate().failures()).isEmpty();
            assertThat(composite.getRecipeList())
                    .extracting(Recipe::getName)
                    .containsExactly(NPM_RECIPE);
        }
    }

    @Test
    void jarBorneYamlResolvesAConfiguredRecipeFromAnotherEcosystem() throws IOException {
        Path jar = jarWithYaml(NPM_RECIPE + ":\n      newVersion: 18");
        try (URLClassLoader classLoader = classLoaderOver(jar)) {
            StubResolver npm = new StubResolver("npm");

            Recipe composite = activate(classLoader, marketplaceWith(NPM_RECIPE, NPM_BUNDLE), npm);

            assertThat(composite.validate().failures()).isEmpty();
            assertThat(composite.getRecipeList())
                    .extracting(Recipe::getName)
                    .containsExactly(NPM_RECIPE);
            assertThat(npm.optionsSeen).containsEntry("newVersion", 18);
        }
    }

    @Test
    void aNameThatNoEcosystemContributesIsStillReportedMissing() throws IOException {
        Path jar = jarWithYaml("com.example.NoSuchRecipe");
        try (URLClassLoader classLoader = classLoaderOver(jar)) {
            Recipe composite = activate(classLoader, new RecipeMarketplace(), new StubResolver("npm"));

            assertThat(failureMessages(composite))
                    .contains("com.example.NoSuchRecipe: refers to a recipe that doesn't exist.");
        }
    }

    /**
     * A listing from the ecosystem whose classpath is already being searched resolves back through
     * the reader that is asking, so the marketplace lookup skips it rather than recursing into it.
     */
    @Test
    void aListingFromTheEcosystemBeingReadIsNotResolvedThroughTheMarketplace() throws IOException {
        Path jar = jarWithYaml("com.example.NotOnTheClasspath");
        try (URLClassLoader classLoader = classLoaderOver(jar)) {
            RecipeMarketplace marketplace = marketplaceWith("com.example.NotOnTheClasspath", MAVEN_BUNDLE);

            Recipe composite = activate(classLoader, marketplace, new StubResolver("maven"));

            assertThat(failureMessages(composite))
                    .contains("com.example.NotOnTheClasspath: refers to a recipe that doesn't exist.");
        }
    }

    /**
     * The asking environment re-initializes any {@link DeclarativeRecipe} it is handed, against its
     * own classpath, which empties the one the marketplace just prepared. Declining leaves the name
     * reported as unresolved rather than silently running an empty recipe.
     */
    @Test
    void aDeclarativeRecipePreparedFromTheMarketplaceIsDeclinedRatherThanEmptied() throws IOException {
        Path jar = jarWithYaml("com.example.Declarative");
        try (URLClassLoader classLoader = classLoaderOver(jar)) {
            RecipeBundle yamlBundle = new RecipeBundle("yaml", "loose.yml", null, null, null);
            RecipeMarketplace marketplace = marketplaceWith("com.example.Declarative", yamlBundle);
            StubResolver yaml = new StubResolver("yaml");
            yaml.prepared = new DeclarativeRecipe("com.example.Declarative", "Declarative", "A declarative recipe.",
                    emptySet(), null, null, false, emptyList());

            Recipe composite = activate(classLoader, marketplace, yaml);

            assertThat(failureMessages(composite))
                    .contains("com.example.Declarative: refers to a recipe that doesn't exist.");
        }
    }

    @Test
    void withoutAMarketplaceNothingChanges() throws IOException {
        Path jar = jarWithYaml(NPM_RECIPE);
        try (URLClassLoader classLoader = classLoaderOver(jar)) {
            Recipe composite = Environment.builder()
                    .load(new ClasspathScanningLoader(new Properties(), classLoader))
                    .build()
                    .activateRecipes("com.example.Composite");

            assertThat(failureMessages(composite))
                    .contains(NPM_RECIPE + ": refers to a recipe that doesn't exist.");
        }
    }

    /** Builds the environment the way {@code MavenRecipeBundleReader} does. */
    private Recipe activate(URLClassLoader classLoader, RecipeMarketplace marketplace, StubResolver resolver) {
        MarketplaceRecipeLoader marketplaceRecipeLoader = new MarketplaceRecipeLoader(
                marketplace, singletonList(resolver), MAVEN_BUNDLE.getPackageEcosystem());
        return Environment.builder()
                .load(new ClasspathScanningLoader(new Properties(), classLoader, marketplaceRecipeLoader))
                .load(marketplaceRecipeLoader)
                .build()
                .activateRecipes("com.example.Composite");
    }

    private static Iterable<String> failureMessages(Recipe recipe) {
        return recipe.validate().failures().stream()
                .map(failure -> failure.getInvalidValue() + ": " + failure.getMessage())
                .collect(java.util.stream.Collectors.toList());
    }

    private RecipeMarketplace marketplaceWith(String recipeName, RecipeBundle bundle) {
        RecipeMarketplace marketplace = new RecipeMarketplace();
        marketplace.install(new RecipeListing(marketplace, recipeName, recipeName, "",
                Duration.ZERO, emptyList(), emptyList(), 1, bundle), emptyList());
        return marketplace;
    }

    private URLClassLoader classLoaderOver(Path jar) throws IOException {
        return new URLClassLoader(new URL[]{jar.toUri().toURL()}, getClass().getClassLoader());
    }

    private Path jarWithYaml(String listed) throws IOException {
        Path jar = tempDir.resolve("angular-pass2-recipes-0.1.0.jar");
        String yaml = "---\n" +
                      "type: specs.openrewrite.org/v1beta/recipe\n" +
                      "name: com.example.Composite\n" +
                      "displayName: Composite\n" +
                      "description: Names a recipe contributed by another ecosystem.\n" +
                      "recipeList:\n" +
                      "  - " + listed + "\n";
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("META-INF/rewrite/composite.yml"));
            jos.write(yaml.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        return jar;
    }

    /** Answers with a real recipe for any listing, the way the npm resolver answers with an RPC recipe. */
    private static class StubResolver implements RecipeBundleResolver {
        private final String ecosystem;
        final Map<String, Object> optionsSeen = new HashMap<>();
        @Nullable Recipe prepared;

        StubResolver(String ecosystem) {
            this.ecosystem = ecosystem;
        }

        @Override
        public String getEcosystem() {
            return ecosystem;
        }

        @Override
        public RecipeBundleReader resolve(RecipeBundle bundle) {
            return new RecipeBundleReader() {
                @Override
                public RecipeBundle getBundle() {
                    return bundle;
                }

                @Override
                public RecipeMarketplace read() {
                    return new RecipeMarketplace();
                }

                @Override
                public RecipeDescriptor describe(RecipeListing listing) {
                    return prepare(listing, emptyMap()).getDescriptor();
                }

                @Override
                public Recipe prepare(RecipeListing listing, @Nullable Map<String, Object> options) {
                    if (options != null) {
                        optionsSeen.putAll(options);
                    }
                    return prepared == null ? new NamedNoop(listing.getName()) : prepared;
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public void close() {
        }
    }

    private static class NamedNoop extends Recipe {
        private final String name;

        NamedNoop(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDisplayName() {
            return "Stand-in for " + name;
        }

        @Override
        public String getDescription() {
            return "Stand-in for " + name + ".";
        }
    }
}
