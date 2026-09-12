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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.toml.ChangeValue;
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
    public GradleVersionCatalogPlugin withVersion(String newVersion) {
        if (newVersion.equals(version) || versionRef != null) {
            return this;
        }
        Toml.KeyValue keyValue = getTree();
        if (keyValue.getValue() instanceof Toml.Literal) {
            Toml.Literal literal = (Toml.Literal) keyValue.getValue();
            Toml.KeyValue updated = (Toml.KeyValue) new ChangeValue(keyName(keyValue),
                    TomlTableValue.quoted(literal, pluginId + ":" + newVersion))
                    .getVisitor()
                    .visitNonNull(keyValue, new InMemoryExecutionContext());
            return new GradleVersionCatalogPlugin(new Cursor(cursor.getParent(), updated),
                    pluginId, newVersion, null);
        }
        if (!(keyValue.getValue() instanceof Toml.Table)) {
            return this;
        }
        Toml.Table inline = (Toml.Table) keyValue.getValue();
        Toml.KeyValue versionKey = TomlTableValue.find(inline, "version");
        if (versionKey == null) {
            return this;
        }
        String currentVersion = TomlTableValue.getString(inline, "version");
        if (currentVersion == null) {
            return this;
        }
        if (!(versionKey.getValue() instanceof Toml.Literal)) {
            return this;
        }
        Toml.Literal versionLiteral = (Toml.Literal) versionKey.getValue();
        Toml.Table updatedInline = (Toml.Table) new ChangeValue("version",
                TomlTableValue.quoted(versionLiteral, newVersion))
                .getVisitor()
                .visitNonNull(inline, new InMemoryExecutionContext());
        Toml.KeyValue updated = keyValue.withValue(updatedInline);
        return new GradleVersionCatalogPlugin(new Cursor(cursor.getParent(), updated),
                pluginId, newVersion, null);
    }

    private static String keyName(Toml.KeyValue keyValue) {
        return ((Toml.Identifier) keyValue.getKey()).getName();
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
            Toml.Identifier tableName = table.getName();
            if (tableName == null || !"plugins".equals(tableName.getName())) {
                return null;
            }
            Toml.KeyValue keyValue = cursor.getValue();
            if (keyValue.getValue() instanceof Toml.Literal) {
                return matchLiteral(cursor, (Toml.Literal) keyValue.getValue());
            }
            if (!(keyValue.getValue() instanceof Toml.Table)) {
                return null;
            }
            return matchTable(cursor, (Toml.Table) keyValue.getValue());
        }

        private @Nullable GradleVersionCatalogPlugin matchLiteral(Cursor cursor, Toml.Literal literal) {
            if (!(literal.getValue() instanceof String)) {
                return null;
            }
            String[] parts = ((String) literal.getValue()).split(":", 2);
            if (parts.length != 2 || doesNotMatch(parts[0], pluginIdPattern)) {
                return null;
            }
            return new GradleVersionCatalogPlugin(cursor, parts[0], parts[1], null);
        }

        private @Nullable GradleVersionCatalogPlugin matchTable(Cursor cursor, Toml.Table plugin) {
            String pluginId = TomlTableValue.getString(plugin, "id");
            if (pluginId == null || doesNotMatch(pluginId, pluginIdPattern)) {
                return null;
            }
            return new GradleVersionCatalogPlugin(cursor, pluginId,
                    TomlTableValue.getString(plugin, "version"),
                    TomlTableValue.getString(plugin, "version.ref"));
        }

        private static boolean doesNotMatch(String pluginId, @Nullable String pattern) {
            return pattern != null && !matchesGlob(pluginId, pattern);
        }
    }
}
