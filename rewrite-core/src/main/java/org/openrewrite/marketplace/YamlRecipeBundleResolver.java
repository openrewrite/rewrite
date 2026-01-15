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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@RequiredArgsConstructor
public class YamlRecipeBundleResolver implements RecipeBundleResolver {
    private final Properties properties;

    @Override
    public String getEcosystem() {
        return "yaml";
    }

    @Override
    public RecipeBundleReader resolve(RecipeBundle bundle) {
        try (InputStream is = Files.exists(Paths.get(bundle.getPackageName())) ?
                Files.newInputStream(Paths.get(bundle.getPackageName())) :
                URI.create(bundle.getPackageName()).toURL().openStream()) {
            return new YamlRecipeBundleReader(bundle, is, URI.create(bundle.getPackageName()), properties);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
