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
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlValue;
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
    public Map<GroupArtifact, Entry> getLibraryVersions() {
        Map<GroupArtifact, Entry> libraries = new LinkedHashMap<>();
        new VersionCatalogLibrary.Matcher().lower(cursor).forEach(library -> libraries.putIfAbsent(library.getGroupArtifact(), library));
        return libraries;
    }

    @Override
    public Map<String, Entry> getPluginVersions() {
        Map<String, Entry> plugins = new LinkedHashMap<>();
        new VersionCatalogPlugin.Matcher().lower(cursor).forEach(plugin -> plugins.putIfAbsent(plugin.getPluginId(), plugin));
        return plugins;
    }

    @Override
    public Map<String, String> getVersionDeclarations() {
        Map<String, String> versions = new LinkedHashMap<>();
        for (TomlValue value : getTree().getValues()) {
            if (value instanceof Toml.Table && isVersionsTable((Toml.Table) value)) {
                Toml.Table table = (Toml.Table) value;
                for (Toml entry : table.getValues()) {
                    if (entry instanceof Toml.KeyValue && ((Toml.KeyValue) entry).getKey() instanceof Toml.Identifier) {
                        String alias = ((Toml.Identifier) ((Toml.KeyValue) entry).getKey()).getName();
                        String version = VersionConstraint.getVersion(table, alias);
                        if (version != null) {
                            versions.put(alias, version);
                        }
                    }
                }
            }
        }
        return versions;
    }

    @Override
    public TomlVersionCatalog withLibraryCoordinates(GroupArtifact ga, String newGroupId, String newArtifactId) {
        return withLibrary(ga, library -> library.withGroup(newGroupId).withName(newArtifactId));
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
                return isVersionsTable(t) ? VersionConstraint.withVersion(t, alias, newVersion) : t;
            }
        }.visitNonNull(getTree(), new InMemoryExecutionContext(), cursor.getParentOrThrow());
        return withTree((Toml.Document) newTree);
    }

    @Override
    public TomlVersionCatalog withPluginVersion(String pluginId, String newVersion) {
        Toml newTree = (Toml) new VersionCatalogPlugin.Matcher().<ExecutionContext>asVisitor((plugin, ctx) ->
                pluginId.equals(plugin.getPluginId()) ? plugin.withVersion(newVersion).getTree() : plugin.getTree()
        ).visitNonNull(getTree(), new InMemoryExecutionContext(), cursor.getParentOrThrow());
        return withTree((Toml.Document) newTree);
    }

    private TomlVersionCatalog withLibrary(GroupArtifact ga, UnaryOperator<VersionCatalogLibrary> edit) {
        Toml newTree = (Toml) new VersionCatalogLibrary.Matcher().<ExecutionContext>asVisitor((library, ctx) ->
                ga.equals(library.getGroupArtifact()) ? edit.apply(library).getTree() : library.getTree()
        ).visitNonNull(getTree(), new InMemoryExecutionContext(), cursor.getParentOrThrow());
        return withTree((Toml.Document) newTree);
    }

    private TomlVersionCatalog withTree(Toml.Document newTree) {
        return newTree == getTree() ? this : new TomlVersionCatalog(new Cursor(cursor.getParentOrThrow(), newTree));
    }

    private static boolean isVersionsTable(Toml.Table table) {
        Toml.Identifier name = table.getName();
        return name != null && "versions".equals(name.getName());
    }

    static class Matcher extends SimpleTraitMatcher<TomlVersionCatalog> {
        @Override
        protected @Nullable TomlVersionCatalog test(Cursor cursor) {
            if (cursor.getValue() instanceof Toml.Document) {
                for (TomlValue value : ((Toml.Document) cursor.getValue()).getValues()) {
                    if (value instanceof Toml.Table) {
                        Toml.Identifier name = ((Toml.Table) value).getName();
                        if (name != null && CATALOG_TABLES.contains(name.getName())) {
                            return new TomlVersionCatalog(cursor);
                        }
                    }
                }
            }
            return null;
        }
    }
}
