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
import org.openrewrite.Contributor;
import org.openrewrite.Recipe;
import org.openrewrite.config.*;
import org.openrewrite.style.NamedStyles;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;

/**
 * Resolves a recipe by name out of a {@link RecipeMarketplace} rather than off a classpath, so that
 * a declarative recipe packaged in one package ecosystem's artifact can name a recipe contributed by
 * another. Lookups go through {@link RecipeListing#prepare}, which dispatches to the resolver
 * registered for that listing's ecosystem.
 * <p>
 * Meant to sit behind a classloader lookup: a name the classpath can satisfy resolves the way it did
 * before, and only what the classpath cannot satisfy reaches the marketplace. It serves two seams,
 * because a {@code recipeList} entry reaches the loader by two different routes. A bare name becomes
 * a {@code LazyLoadedRecipe} and is resolved when the recipe is activated, through the
 * {@link ResourceLoader} this implements. An entry that configures options cannot be deferred that
 * way, so {@link YamlResourceLoader} calls {@link #load} while parsing.
 */
public class MarketplaceRecipeLoader implements ResourceLoader {
    private final RecipeMarketplace marketplace;
    private final Collection<RecipeBundleResolver> resolvers;

    /**
     * The ecosystem whose classpath is already being searched. Only a listing from a different
     * ecosystem is resolved here: a listing from this one either has a class the classpath lookup
     * would have found, or is a declarative recipe that {@link #load} would have to decline anyway.
     */
    private final @Nullable String excludingEcosystem;

    public MarketplaceRecipeLoader(RecipeMarketplace marketplace, Collection<RecipeBundleResolver> resolvers) {
        this(marketplace, resolvers, null);
    }

    public MarketplaceRecipeLoader(RecipeMarketplace marketplace, Collection<RecipeBundleResolver> resolvers,
                                   @Nullable String excludingEcosystem) {
        this.marketplace = marketplace;
        this.resolvers = resolvers;
        this.excludingEcosystem = excludingEcosystem;
    }

    /**
     * @return The prepared recipe, or null when the name is not one this loader can answer for. A
     * null answer leaves the caller reporting the name the way it reports any name it cannot
     * resolve, which is what it did for every cross-ecosystem name before this loader existed.
     */
    public @Nullable Recipe load(String recipeName, @Nullable Map<String, Object> options) {
        RecipeListing listing = marketplace.findRecipe(recipeName);
        if (listing == null) {
            return null;
        }
        String ecosystem = listing.getBundle().getPackageEcosystem();
        if (ecosystem == null || ecosystem.equals(excludingEcosystem) || !hasResolver(ecosystem)) {
            return null;
        }
        Recipe prepared = listing.prepare(resolvers, options == null ? emptyMap() : options);
        if (prepared instanceof DeclarativeRecipe) {
            // The asking environment re-initializes a DeclarativeRecipe against its own classpath, emptying it.
            return null;
        }
        return prepared;
    }

    @Override
    public @Nullable Recipe loadRecipe(String recipeName, RecipeDetail... details) {
        return load(recipeName, null);
    }

    private boolean hasResolver(String ecosystem) {
        for (RecipeBundleResolver resolver : resolvers) {
            if (ecosystem.equals(resolver.getEcosystem())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<Recipe> listRecipes() {
        return emptyList();
    }

    @Override
    public Collection<RecipeDescriptor> listRecipeDescriptors() {
        return emptyList();
    }

    @Override
    public Collection<NamedStyles> listStyles() {
        return emptyList();
    }

    @Override
    public Collection<CategoryDescriptor> listCategoryDescriptors() {
        return emptyList();
    }

    @Override
    @Deprecated
    public Map<String, List<Contributor>> listContributors() {
        return emptyMap();
    }

    @Override
    public Map<String, List<RecipeExample>> listRecipeExamples() {
        return emptyMap();
    }
}
