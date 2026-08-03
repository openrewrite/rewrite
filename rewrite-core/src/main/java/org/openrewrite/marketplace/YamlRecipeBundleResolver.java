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

import lombok.RequiredArgsConstructor;
import org.openrewrite.config.CategoryDescriptor;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class YamlRecipeBundleResolver implements RecipeBundleResolver {
    private final Properties properties;
    private final RecipeMarketplace marketplace;
    private final Collection<RecipeBundleResolver> resolvers;

    /**
     * Category path to file every recipe under, shallowest to deepest. When empty each recipe
     * takes the path inferred from its own name.
     */
    private final List<CategoryDescriptor> categoryOverride;

    public YamlRecipeBundleResolver(Properties properties, RecipeMarketplace marketplace,
                                    Collection<RecipeBundleResolver> resolvers) {
        this(properties, marketplace, resolvers, emptyList());
    }

    @Override
    public String getEcosystem() {
        return "yaml";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        try {
            Path path = Paths.get(bundle.getPackageName());
            if (Files.exists(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    return new YamlRecipeBundleReader(bundle, is, path.toUri(), properties, marketplace, resolvers, categoryOverride);
                }
            }
        } catch (Exception ignored) {
        }

        try {
            URI resource = URI.create(bundle.getPackageName());
            try (InputStream is = resource.toURL().openStream()) {
                return new YamlRecipeBundleReader(bundle, is, resource, properties, marketplace, resolvers, categoryOverride);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
