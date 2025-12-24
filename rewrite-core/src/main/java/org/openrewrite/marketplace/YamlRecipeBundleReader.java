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
import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

public class YamlRecipeBundleReader implements RecipeBundleReader {
    private final @Getter RecipeBundle bundle;
    private final YamlResourceLoader yamlLoader;

    public YamlRecipeBundleReader(RecipeBundle bundle, InputStream yamlLoader, URI source, Properties properties) {
        this.yamlLoader = new YamlResourceLoader(yamlLoader, source, properties);
        this.bundle = bundle;
    }

    @Override
    public RecipeMarketplace read() {
        Environment env = Environment.builder()
                .scanYamlResources() // especially for rewrite-core provided category descriptors
                .build();

        RecipeMarketplace marketplace = new RecipeMarketplace();
        for (RecipeDescriptor descriptor : yamlLoader.listRecipeDescriptors()) {
            marketplace.install(RecipeListing.fromDescriptor(descriptor, bundle),
                    descriptor.inferCategoriesFromName(env));
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
}
