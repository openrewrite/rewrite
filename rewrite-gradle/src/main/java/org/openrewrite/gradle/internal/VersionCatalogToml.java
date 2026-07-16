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
package org.openrewrite.gradle.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.toml.tree.Toml;

/**
 * Structural operations shared by Gradle version-catalog TOML visitors.
 */
public final class VersionCatalogToml {
    public static final String FILE_NAME = "libs.versions.toml";

    private VersionCatalogToml() {
    }

    public static Toml.@Nullable Table findTable(Toml.Document document, String name) {
        for (Toml value : document.getValues()) {
            if (value instanceof Toml.Table && ((Toml.Table) value).getName() != null &&
                    name.equals(((Toml.Table) value).getName().getName())) {
                return (Toml.Table) value;
            }
        }
        return null;
    }

    public static @Nullable String getVersion(Toml.@Nullable Table versions, String key) {
        if (versions == null) {
            return null;
        }
        for (Toml value : versions.getValues()) {
            if (value instanceof Toml.KeyValue && ((Toml.KeyValue) value).getKey() instanceof Toml.Identifier &&
                    key.equals(((Toml.Identifier) ((Toml.KeyValue) value).getKey()).getName()) &&
                    ((Toml.KeyValue) value).getValue() instanceof Toml.Literal) {
                Object version = ((Toml.Literal) ((Toml.KeyValue) value).getValue()).getValue();
                return version instanceof String ? (String) version : null;
            }
        }
        return null;
    }

    public static String quoted(Toml.Literal literal, String value) {
        String source = literal.getSource();
        String quote = source.isEmpty() ? "\"" : source.substring(0, 1);
        return quote + value + quote;
    }
}
