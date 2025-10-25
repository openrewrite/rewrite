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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.Recipe;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.YamlResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

public class YamlRecipeBundle implements RecipeBundle {
    private final URI uri;
    private final Properties properties;

    @Getter
    private final @Nullable String team;

    public YamlRecipeBundle(URI uri, Properties properties, @Nullable String team) {
        this.uri = uri;
        this.properties = properties;
        this.team = team;
    }

    @Override
    public String getPackageEcosystem() {
        return "yaml";
    }

    @Override
    public String getPackageName() {
        return uri.toString();
    }

    /**
     * @return A hash of the file's contents. This version will not have a natural
     * ordering, but is sufficient for determining whether the file has changed and
     * should therefore be reinstalled into a marketplace.
     */
    @Override
    public String getVersion() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = uri.toURL().openStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return new Checksum("SHA-256", digest.digest()).getHexValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read recipe file from " + uri, e);
        }
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
