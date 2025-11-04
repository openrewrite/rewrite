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
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
public class YamlRecipeBundle implements RecipeBundle {
    private final URI uri;

    @Getter
    private final @Nullable String version;

    private final Properties properties;

    @Getter
    private final @Nullable String team;

    @Override
    public String getPackageEcosystem() {
        return "yaml";
    }

    @Override
    public String getPackageName() {
        return uri.toString();
    }

    @Override
    public RecipeDescriptor describe(RecipeListing listing) {
        return prepare(listing, emptyMap()).getDescriptor();
    }

    @Override
    public Recipe prepare(RecipeListing listing, Map<String, Object> options) {
        try (InputStream inputStream = uri.toURL().openStream()) {
            YamlResourceLoader loader = new YamlResourceLoader(inputStream, uri, properties);
            return requireNonNull(loader.loadRecipe(listing.getName()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open stream from " + uri, e);
        }
    }
}
