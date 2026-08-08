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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;
import org.openrewrite.trait.VisitFunction2;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A document-level semantic model for the root Gradle version catalog.
 * <p>
 * In addition to indexing catalog entries, this model coordinates changes to shared
 * {@code version.ref} declarations. A referenced version can be changed only when every
 * consumer is supported and selects the same replacement.
 */
public class GradleVersionCatalog implements Trait<Toml.Document> {
    private static final String FILE_NAME = "libs.versions.toml";
    private static final Path DEFAULT_CATALOG_PATH = Paths.get("gradle", FILE_NAME);

    private final Cursor cursor;
    private final Map<String, String> declaredVersions;
    private final Map<String, List<VersionRefConsumer>> versionRefConsumers;

    private GradleVersionCatalog(Cursor cursor, Map<String, String> declaredVersions,
                                 Map<String, List<VersionRefConsumer>> versionRefConsumers) {
        this.cursor = cursor;
        this.declaredVersions = declaredVersions;
        this.versionRefConsumers = versionRefConsumers;
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Matches Gradle version catalog documents.
     * <p>
     * By default, this matches the conventional {@code gradle/libs.versions.toml} catalog.
     */
    public static class Matcher extends SimpleTraitMatcher<GradleVersionCatalog> {
        private Path catalogPath = DEFAULT_CATALOG_PATH;

        /**
         * Restricts matching to the supplied catalog path. When not called, the matcher uses
         * the conventional {@code gradle/libs.versions.toml} path.
         */
        public Matcher catalogPath(Path catalogPath) {
            this.catalogPath = catalogPath;
            return this;
        }

        @Override
        public <P> TreeVisitor<? extends Tree, P> asVisitor(VisitFunction2<GradleVersionCatalog, P> visitor) {
            return new TomlIsoVisitor<P>() {
                @Override
                public Toml.Document visitDocument(Toml.Document document, P p) {
                    GradleVersionCatalog catalog = test(getCursor());
                    return catalog == null ?
                            super.visitDocument(document, p) :
                            (Toml.Document) visitor.visit(catalog, p);
                }
            };
        }

        @Override
        protected @Nullable GradleVersionCatalog test(Cursor cursor) {
            if (!(cursor.getValue() instanceof Toml.Document)) {
                return null;
            }
            Toml.Document document = cursor.getValue();
            if (!catalogPath.equals(document.getSourcePath())) {
                return null;
            }
            return from(cursor, document);
        }
    }

    /**
     * Creates a visitor for the root project's conventional {@code gradle/libs.versions.toml} catalog.
     * Catalogs in nested or included builds are not supported.
     * The supplied policy determines which entries are eligible and how their versions are selected;
     * this visitor applies that policy consistently to direct and shared {@code version.ref} entries.
     */
    public static TreeVisitor<?, ExecutionContext> visitor(VersionCatalogUpdate update) {
        return new TomlIsoVisitor<ExecutionContext>() {
            private @Nullable GradleVersionCatalog catalog;
            private Map<String, String> referencedVersions = java.util.Collections.emptyMap();
            private Map<String, MavenDownloadingException> resolutionFailures = java.util.Collections.emptyMap();

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof Toml.Document &&
                        sourceFile.getSourcePath().equals(DEFAULT_CATALOG_PATH);
            }

            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                catalog = GradleVersionCatalog.from(getCursor(), document);
                referencedVersions = java.util.Collections.emptyMap();
                resolutionFailures = new LinkedHashMap<>();
                referencedVersions = catalog.safeVersionRefReplacements(update, ctx, resolutionFailures);
                return super.visitDocument(document, ctx);
            }

            @Override
            public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
                Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);
                Cursor kvCursor = new Cursor(getCursor().getParent(), kv);
                String versionRef = versionRef(kv);
                MavenDownloadingException resolutionFailure = resolutionFailures.get(versionRef);
                if (resolutionFailure != null) {
                    return resolutionFailure.warn(kv);
                }

                GradleVersionCatalogDependency dependency = new GradleVersionCatalogDependency.Matcher()
                        .get(kvCursor).orElse(null);
                if (dependency != null) {
                    try {
                        return update.updateDependency(dependency,
                                dependency.getVersionRef() == null ? null : referencedVersions.get(dependency.getVersionRef()), ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(kv);
                    }
                }

                GradleVersionCatalogPlugin plugin = new GradleVersionCatalogPlugin.Matcher()
                        .get(kvCursor).orElse(null);
                if (plugin != null) {
                    try {
                        return update.updatePlugin(plugin,
                                plugin.getVersionRef() == null ? null : referencedVersions.get(plugin.getVersionRef()), ctx);
                    } catch (MavenDownloadingException e) {
                        return e.warn(kv);
                    }
                }

                return catalog == null ? kv : catalog.withReferencedVersion(kv, getCursor(), referencedVersions);
            }
        };
    }

    private static GradleVersionCatalog from(Cursor cursor, Toml.Document document) {
        Map<String, String> declaredVersions = new LinkedHashMap<>();
        Map<String, List<VersionRefConsumer>> consumers = new LinkedHashMap<>();

        Toml.Table versions = document.findTable("versions");
        if (versions != null) {
            for (Toml value : versions.getValues()) {
                if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getKey() instanceof Toml.Identifier) ||
                        !(((Toml.KeyValue) value).getValue() instanceof Toml.Literal)) {
                    continue;
                }
                Object version = ((Toml.Literal) ((Toml.KeyValue) value).getValue()).getValue();
                if (version instanceof String) {
                    declaredVersions.put(((Toml.Identifier) ((Toml.KeyValue) value).getKey()).getName(), (String) version);
                }
            }
        }

        indexConsumers(cursor, document.findTable("libraries"), consumers);
        indexConsumers(cursor, document.findTable("plugins"), consumers);
        return new GradleVersionCatalog(cursor, declaredVersions, consumers);
    }

    private static void indexConsumers(Cursor documentCursor, Toml.@Nullable Table table,
                                       Map<String, List<VersionRefConsumer>> consumers) {
        if (table == null) {
            return;
        }
        boolean library = "libraries".equals(table.getName() == null ? null : table.getName().getName());
        Cursor tableCursor = new Cursor(documentCursor, table);
        for (Toml value : table.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue keyValue = (Toml.KeyValue) value;
            Cursor keyValueCursor = new Cursor(tableCursor, keyValue);
            GradleVersionCatalogDependency dependency = library ?
                    new GradleVersionCatalogDependency.Matcher().get(keyValueCursor).orElse(null) : null;
            GradleVersionCatalogPlugin plugin = library ? null :
                    new GradleVersionCatalogPlugin.Matcher().get(keyValueCursor).orElse(null);
            String versionRef = versionRef(keyValue);
            if (versionRef == null) {
                continue;
            }
            VersionRefConsumer consumer = new VersionRefConsumer(versionRef, dependency, plugin);
            consumers.computeIfAbsent(versionRef, ignored -> new ArrayList<>()).add(consumer);
        }
    }

    private static @Nullable String versionRef(Toml.KeyValue keyValue) {
        return keyValue.getValue() instanceof Toml.Table ?
                ((Toml.Table) keyValue.getValue()).getString("version.ref") : null;
    }

    private Map<String, String> safeVersionRefReplacements(
            VersionCatalogUpdate update, ExecutionContext ctx,
            Map<String, MavenDownloadingException> resolutionFailures) {
        Map<String, String> replacements = new LinkedHashMap<>();
        for (Map.Entry<String, List<VersionRefConsumer>> entry : versionRefConsumers.entrySet()) {
            String currentVersion = declaredVersions.get(entry.getKey());
            if (currentVersion == null) {
                continue;
            }
            String replacement = null;
            boolean safe = true;
            for (VersionRefConsumer consumer : entry.getValue()) {
                if (!consumer.isSupported()) {
                    safe = false;
                    break;
                }
                String selected;
                try {
                    selected = update.selectReferencedVersion(consumer, currentVersion, ctx);
                } catch (MavenDownloadingException e) {
                    resolutionFailures.put(entry.getKey(), e);
                    safe = false;
                    break;
                }
                if (selected == null || replacement != null && !replacement.equals(selected)) {
                    safe = false;
                    break;
                }
                replacement = selected;
            }
            if (safe && replacement != null) {
                replacements.put(entry.getKey(), replacement);
            }
        }
        return replacements;
    }

    Toml.KeyValue withReferencedVersion(Toml.KeyValue keyValue, Cursor cursor,
                                        Map<String, String> replacements) {
        if (replacements.isEmpty() || !(keyValue.getKey() instanceof Toml.Identifier) ||
                !(keyValue.getValue() instanceof Toml.Literal)) {
            return keyValue;
        }
        Cursor parent = cursor.getParent();
        if (parent == null || !(parent.getValue() instanceof Toml.Table)) {
            return keyValue;
        }
        Toml.Table table = parent.getValue();
        Toml.Identifier tableName = table.getName();
        if (tableName == null || !"versions".equals(tableName.getName())) {
            return keyValue;
        }
        String replacement = replacements.get(((Toml.Identifier) keyValue.getKey()).getName());
        if (replacement == null) {
            return keyValue;
        }
        Toml.Literal literal = (Toml.Literal) keyValue.getValue();
        if (replacement.equals(literal.getValue())) {
            return keyValue;
        }
        return keyValue.withValue(literal.withSource(TomlTableValue.quoted(literal, replacement))
                .withValue(replacement));
    }

    @Value
    public static class VersionRefConsumer {
        String versionRef;
        @Nullable GradleVersionCatalogDependency dependency;
        @Nullable GradleVersionCatalogPlugin plugin;

        VersionRefConsumer(String versionRef, @Nullable GradleVersionCatalogDependency dependency,
                           @Nullable GradleVersionCatalogPlugin plugin) {
            this.versionRef = versionRef;
            this.dependency = dependency;
            this.plugin = plugin;
        }

        boolean isSupported() {
            return dependency != null || plugin != null;
        }
    }

    /**
     * Recipe-specific version-catalog policy used by {@link #visitor(VersionCatalogUpdate)}.
     */
    public interface VersionCatalogUpdate {
        @Nullable String selectReferencedVersion(VersionRefConsumer consumer, String currentVersion, ExecutionContext ctx)
                throws MavenDownloadingException;

        default Toml.KeyValue updateDependency(GradleVersionCatalogDependency dependency,
                                               @Nullable String referencedVersion, ExecutionContext ctx)
                throws MavenDownloadingException {
            return dependency.getTree();
        }

        default Toml.KeyValue updatePlugin(GradleVersionCatalogPlugin plugin,
                                           @Nullable String referencedVersion, ExecutionContext ctx)
                throws MavenDownloadingException {
            return plugin.getTree();
        }
    }
}
