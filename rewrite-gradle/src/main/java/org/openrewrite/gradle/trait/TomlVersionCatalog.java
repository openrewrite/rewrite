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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A catalog declared as a TOML file, conventionally {@code gradle/libs.versions.toml}.
 */
@Value
class TomlVersionCatalog implements VersionCatalog {
    private static final List<String> CATALOG_TABLES = Arrays.asList("versions", "libraries", "bundles", "plugins");

    Cursor cursor;

    /**
     * A cheaper test than {@link Matcher}, for narrowing which documents a visitor accepts.
     */
    static boolean isVersionCatalog(SourceFile sourceFile) {
        return sourceFile instanceof Toml.Document && sourceFile.getSourcePath().toString().endsWith(".versions.toml");
    }

    @Override
    public Toml.Document getTree() {
        return cursor.getValue();
    }

    @Override
    public Map<GroupArtifact, VersionCatalogLibrary> getLibraryVersions() {
        Map<GroupArtifact, VersionCatalogLibrary> libraries = new LinkedHashMap<>();
        new VersionCatalogLibrary.Matcher().lower(cursor).forEach(library -> libraries.putIfAbsent(library.getGroupArtifact(), library));
        return libraries;
    }

    @Override
    public Map<String, VersionCatalogPlugin> getPluginVersions() {
        Map<String, VersionCatalogPlugin> plugins = new LinkedHashMap<>();
        new VersionCatalogPlugin.Matcher().lower(cursor).forEach(plugin -> plugins.putIfAbsent(plugin.getPluginId(), plugin));
        return plugins;
    }

    @Override
    public Map<String, String> getVersionDeclarations() {
        Map<String, String> versions = new LinkedHashMap<>();
        for (Toml value : getTree().getValues()) {
            if (value instanceof Toml.Table && isVersionsTable((Toml.Table) value)) {
                for (Toml entry : ((Toml.Table) value).getValues()) {
                    if (entry instanceof Toml.KeyValue) {
                        Toml.KeyValue keyValue = (Toml.KeyValue) entry;
                        if (keyValue.getKey() instanceof Toml.Identifier && keyValue.getValue() instanceof Toml.Literal &&
                            ((Toml.Literal) keyValue.getValue()).getValue() instanceof String) {
                            versions.put(((Toml.Identifier) keyValue.getKey()).getName(), (String) ((Toml.Literal) keyValue.getValue()).getValue());
                        }
                    }
                }
            }
        }
        return versions;
    }

    @Override
    public TomlVersionCatalog withLibraryVersion(GroupArtifact ga, String newVersion) {
        return withLibrary(ga, library -> library.withVersion(newVersion));
    }

    @Override
    public TomlVersionCatalog withDetachedLibraryVersion(GroupArtifact ga, String newVersion) {
        return withLibrary(ga, library -> library.withDetachedVersion(newVersion));
    }

    @Override
    public TomlVersionCatalog withVersionDeclarationValue(String alias, String newVersion) {
        Toml newTree = new TomlIsoVisitor<ExecutionContext>() {
            @Override
            public Toml.Table visitTable(Toml.Table table, ExecutionContext ctx) {
                Toml.Table t = super.visitTable(table, ctx);
                return isVersionsTable(t) ? TomlTableValue.withString(t, alias, newVersion) : t;
            }
        }.visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());
        return withTree((Toml.Document) newTree);
    }

    private TomlVersionCatalog withLibrary(GroupArtifact ga, UnaryOperator<VersionCatalogLibrary> edit) {
        Toml newTree = (Toml) new VersionCatalogLibrary.Matcher().<ExecutionContext>asVisitor((library, ctx) ->
                ga.equals(library.getGroupArtifact()) ? edit.apply(library).getTree() : library.getTree()
        ).visit(getTree(), new InMemoryExecutionContext(), cursor.getParent());
        return withTree((Toml.Document) newTree);
    }

    private TomlVersionCatalog withTree(Toml.Document newTree) {
        return newTree == getTree() ? this : new TomlVersionCatalog(new Cursor(cursor.getParent(), newTree));
    }

    private static boolean isVersionsTable(Toml.Table table) {
        return table.getName() != null && "versions".equals(table.getName().getName());
    }

    static class Matcher extends SimpleTraitMatcher<TomlVersionCatalog> {
        @Override
        protected @Nullable TomlVersionCatalog test(Cursor cursor) {
            if (cursor.getValue() instanceof Toml.Document) {
                for (Toml value : ((Toml.Document) cursor.getValue()).getValues()) {
                    if (value instanceof Toml.Table && ((Toml.Table) value).getName() != null &&
                        CATALOG_TABLES.contains(((Toml.Table) value).getName().getName())) {
                        return new TomlVersionCatalog(cursor);
                    }
                }
            }
            return null;
        }
    }
}
