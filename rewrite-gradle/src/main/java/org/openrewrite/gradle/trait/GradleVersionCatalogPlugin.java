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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.internal.VersionCatalogToml;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Represents a plugin entry in a Gradle version catalog TOML file ({@code [plugins]} table).
 */
@Value
public class GradleVersionCatalogPlugin implements Trait<Toml.KeyValue> {
    Cursor cursor;
    String pluginId;
    @Nullable String version;
    @Nullable String versionRef;

    /**
     * Returns a new catalog entry with its direct version updated.
     * Entries using {@code version.ref} are intentionally unchanged; their shared version
     * entry is updated by the recipe after selecting a version.
     */
    public Toml.KeyValue withVersion(String newVersion) {
        if (newVersion.equals(version) || versionRef != null) {
            return getTree();
        }
        Toml.KeyValue keyValue = getTree();
        if (keyValue.getValue() instanceof Toml.Literal) {
            Toml.Literal literal = (Toml.Literal) keyValue.getValue();
            return keyValue.withValue(literal
                    .withSource(VersionCatalogToml.quoted(literal, pluginId + ":" + newVersion))
                    .withValue(pluginId + ":" + newVersion));
        }
        if (!(keyValue.getValue() instanceof Toml.Table)) {
            return keyValue;
        }
        Toml.Table inline = (Toml.Table) keyValue.getValue();
        if (!TomlTableValue.has(inline, "version")) {
            return keyValue;
        }
        return keyValue.withValue(TomlTableValue.withString(inline, "version", newVersion));
    }

    public static class Matcher extends SimpleTraitMatcher<GradleVersionCatalogPlugin> {
        @Nullable
        private String pluginIdPattern;

        public Matcher pluginIdPattern(@Nullable String pluginIdPattern) {
            this.pluginIdPattern = pluginIdPattern;
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradleVersionCatalogPlugin, P> visitor) {
            return new TomlIsoVisitor<P>() {
                @Override
                public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, P p) {
                    GradleVersionCatalogPlugin plugin = test(getCursor());
                    return plugin != null ?
                            (Toml.KeyValue) visitor.visit(plugin, p) :
                            super.visitKeyValue(keyValue, p);
                }
            };
        }

        @Override
        protected @Nullable GradleVersionCatalogPlugin test(Cursor cursor) {
            if (!(cursor.getValue() instanceof Toml.KeyValue)) {
                return null;
            }
            Cursor parent = cursor.getParent();
            if (parent == null || !(parent.getValue() instanceof Toml.Table)) {
                return null;
            }
            Toml.Table table = parent.getValue();
            if (table.getName() == null || !"plugins".equals(table.getName().getName())) {
                return null;
            }
            return extract(cursor, cursor.getValue(), pluginIdPattern);
        }

        public static @Nullable GradleVersionCatalogPlugin extract(
                Toml.KeyValue keyValue, @Nullable String pluginIdPattern) {
            Cursor cursor = new Cursor(new Cursor(null, Cursor.ROOT_VALUE), keyValue);
            return extract(cursor, keyValue, pluginIdPattern);
        }

        private static @Nullable GradleVersionCatalogPlugin extract(
                Cursor cursor, Toml.KeyValue keyValue, @Nullable String pluginIdPattern) {
            if (keyValue.getValue() instanceof Toml.Literal) {
                Toml.Literal literal = (Toml.Literal) keyValue.getValue();
                if (!(literal.getValue() instanceof String)) {
                    return null;
                }
                String[] parts = ((String) literal.getValue()).split(":", 2);
                if (parts.length != 2 || !matches(parts[0], pluginIdPattern)) {
                    return null;
                }
                return new GradleVersionCatalogPlugin(cursor, parts[0], parts[1], null);
            }
            if (!(keyValue.getValue() instanceof Toml.Table)) {
                return null;
            }
            Toml.Table table = (Toml.Table) keyValue.getValue();
            String pluginId = TomlTableValue.getString(table, "id");
            if (pluginId == null || !matches(pluginId, pluginIdPattern)) {
                return null;
            }
            return new GradleVersionCatalogPlugin(cursor, pluginId,
                    TomlTableValue.getString(table, "version"),
                    TomlTableValue.getString(table, "version.ref"));
        }

        private static boolean matches(String pluginId, @Nullable String pattern) {
            return StringUtils.isBlank(pattern) || matchesGlob(pluginId, pattern);
        }
    }
}
