/*
 * Copyright 2025 the original author or authors.
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

import lombok.Getter;
import org.intellij.lang.annotations.Language;
import org.openrewrite.Recipe;
import org.openrewrite.config.CategoryDescriptor;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static java.util.Collections.*;

public class YamlRecipeBundleReader implements RecipeBundleReader {
    private final @Getter RecipeBundle bundle;
    private final YamlResourceLoader yamlLoader;

    public YamlRecipeBundleReader(RecipeBundle bundle, InputStream yamlLoader, URI source, Properties properties, RecipeMarketplace marketplace, Collection<RecipeBundleResolver> resolvers) {
        this.yamlLoader = new YamlResourceLoader(yamlLoader, source, properties, marketplace, resolvers);
        this.bundle = bundle;
    }

    @Override
    public RecipeMarketplace read() {
        Environment env = Environment.builder()
                .scanYamlResources() // especially for rewrite-core provided category descriptors
                .build();

        RecipeMarketplace marketplace = new RecipeMarketplace();
        for (RecipeListing listing : yamlLoader.listRecipeListings(bundle)) {
            marketplace.install(listing, inferCategoriesFromName(env, listing.getName()));
        }

        return marketplace;
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        return prepare(listing, emptyMap()).getDescriptor();
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        return new Environment(singleton(yamlLoader))
                .activateRecipes(listing.getName());
    }

    private List<CategoryDescriptor> inferCategoriesFromName(Environment env, String name) {
        // Extract package from recipe name (everything before the last dot)
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) {
            return emptyList();
        }

        String packageName = name.substring(0, lastDot);

        String[] parts = packageName.split("\\.");
        List<CategoryDescriptor> categories = new ArrayList<>(parts.length);

        nextPart:
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            String partialPackage = String.join(".", Arrays.copyOfRange(parts, 0, i + 1));
            for (CategoryDescriptor categoryDescriptor : env.listCategoryDescriptors()) {
                String categoryPackageName = categoryDescriptor.getPackageName();
                if (categoryPackageName.equals(partialPackage)) {
                    if (categoryDescriptor.isRoot()) {
                        continue nextPart;
                    }
                    categories.add(categoryDescriptor);
                    continue nextPart;
                }
            }

            if (!part.isEmpty()) {
                @Language("markdown") String capitalized = Character.toUpperCase(part.charAt(0)) + part.substring(1);
                categories.add(new CategoryDescriptor(capitalized, partialPackage, "", emptySet(),
                        false, 0, false));
            }
        }

        return categories;
    }
}
