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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * A plugin entry in a Gradle version catalog TOML file, in its {@code [plugins]} table.
 */
@Value
public class VersionCatalogPlugin implements Trait<Toml.KeyValue> {
    Cursor cursor;
    String pluginId;
    @Nullable String version;
    @Nullable String versionRef;

    /**
     * An entry using {@code version.ref} is left unchanged.
     */
    public VersionCatalogPlugin withVersion(String newVersion) {
        if (newVersion.equals(version) || versionRef != null) {
            return this;
        }
        Toml.KeyValue keyValue = getTree();
        Toml.KeyValue updated;
        if (keyValue.getValue() instanceof Toml.Literal) {
            Toml.Literal literal = (Toml.Literal) keyValue.getValue();
            String notation = pluginId + ":" + newVersion;
            updated = keyValue.withValue(literal.withSource(TomlTableValue.quoted(literal, notation)).withValue(notation));
        } else if (keyValue.getValue() instanceof Toml.Table) {
            updated = keyValue.withValue(TomlTableValue.withString((Toml.Table) keyValue.getValue(), "version", newVersion));
        } else {
            return this;
        }
        return updated == keyValue ? this :
                new VersionCatalogPlugin(new Cursor(cursor.getParent(), updated), pluginId, newVersion, null);
    }

    public static class Matcher extends SimpleTraitMatcher<VersionCatalogPlugin> {
        @Nullable
        private String pluginIdPattern;

        public Matcher pluginIdPattern(@Nullable String pluginIdPattern) {
            this.pluginIdPattern = pluginIdPattern;
            return this;
        }

        @Override
        protected @Nullable VersionCatalogPlugin test(Cursor cursor) {
            if (!(cursor.getValue() instanceof Toml.KeyValue)) {
                return null;
            }
            Cursor parent = cursor.getParent();
            if (parent == null || !(parent.getValue() instanceof Toml.Table)) {
                return null;
            }
            Toml.Table table = parent.getValue();
            Toml.Identifier tableName = table.getName();
            if (tableName == null || !"plugins".equals(tableName.getName())) {
                return null;
            }
            Toml.KeyValue keyValue = cursor.getValue();
            if (keyValue.getValue() instanceof Toml.Literal) {
                return testLiteral(cursor, (Toml.Literal) keyValue.getValue());
            }
            if (keyValue.getValue() instanceof Toml.Table) {
                return testInlineTable(cursor, (Toml.Table) keyValue.getValue());
            }
            return null;
        }

        private @Nullable VersionCatalogPlugin testLiteral(Cursor cursor, Toml.Literal literal) {
            if (!(literal.getValue() instanceof String)) {
                return null;
            }
            String[] parts = ((String) literal.getValue()).split(":", 2);
            if (parts.length != 2 || !matchesPattern(parts[0])) {
                return null;
            }
            return new VersionCatalogPlugin(cursor, parts[0], parts[1], null);
        }

        private @Nullable VersionCatalogPlugin testInlineTable(Cursor cursor, Toml.Table inline) {
            String pluginId = TomlTableValue.getString(inline, "id");
            if (pluginId == null || !matchesPattern(pluginId)) {
                return null;
            }
            return new VersionCatalogPlugin(cursor, pluginId,
                    TomlTableValue.getString(inline, "version"),
                    TomlTableValue.getString(inline, "version.ref"));
        }

        private boolean matchesPattern(String pluginId) {
            return pluginIdPattern == null || matchesGlob(pluginId, pluginIdPattern);
        }
    }
}
