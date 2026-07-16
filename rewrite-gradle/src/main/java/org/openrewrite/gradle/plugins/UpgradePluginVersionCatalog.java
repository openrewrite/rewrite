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
package org.openrewrite.gradle.plugins;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.internal.VersionCatalogToml;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.TomlTableValue;
import org.openrewrite.toml.tree.Toml;

import java.util.HashMap;
import java.util.Map;

final class UpgradePluginVersionCatalog extends TomlIsoVisitor<ExecutionContext> {
    private final String pluginIdPattern;
    private final @Nullable String newVersion;
    private final @Nullable String versionPattern;
    private final MavenMetadataFailures metadataFailures;
    private final @Nullable GradleProject gradleProject;
    private final @Nullable GradleSettings gradleSettings;

    UpgradePluginVersionCatalog(
            String pluginIdPattern,
            @Nullable String newVersion,
            @Nullable String versionPattern,
            MavenMetadataFailures metadataFailures,
            @Nullable GradleProject gradleProject,
            @Nullable GradleSettings gradleSettings) {
        this.pluginIdPattern = pluginIdPattern;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.metadataFailures = metadataFailures;
        this.gradleProject = gradleProject;
        this.gradleSettings = gradleSettings;
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        return sourceFile instanceof Toml.Document &&
                sourceFile.getSourcePath().endsWith(VersionCatalogToml.FILE_NAME);
    }

    @Override
    public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
        Toml.Table plugins = VersionCatalogToml.findTable(document, "plugins");
        Toml.Table versions = VersionCatalogToml.findTable(document, "versions");
        if (plugins == null) {
            return document;
        }
        Map<String, String> references = new HashMap<>();
        for (Toml value : plugins.getValues()) {
            if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getValue() instanceof Toml.Table)) {
                continue;
            }
            Toml.Table plugin = (Toml.Table) ((Toml.KeyValue) value).getValue();
            String ref = TomlTableValue.getString(plugin, "version.ref");
            String id = TomlTableValue.getString(plugin, "id");
            if (ref == null || id == null || !StringUtils.matchesGlob(id, pluginIdPattern)) {
                continue;
            }
            try {
                String selected = select(VersionCatalogToml.getVersion(versions, ref), id, ctx);
                if (selected != null) {
                    references.put(ref, selected);
                }
            } catch (MavenDownloadingException e) {
                return e.warn(document);
            }
        }
        Toml.Document updated = document.withValues(ListUtils.map(document.getValues(), value -> {
            if (!(value instanceof Toml.Table)) {
                return value;
            }
            Toml.Table table = (Toml.Table) value;
            if (table.getName() != null && "plugins".equals(table.getName().getName())) {
                return updatePlugins(table, ctx);
            }
            if (table.getName() != null && "versions".equals(table.getName().getName())) {
                return updateVersions(table, references);
            }
            return value;
        }));
        return super.visitDocument(updated, ctx);
    }

    private Toml.Table updatePlugins(Toml.Table plugins, ExecutionContext ctx) {
        return plugins.withValues(ListUtils.map(plugins.getValues(), value -> {
            if (!(value instanceof Toml.KeyValue)) {
                return value;
            }
            Toml.KeyValue plugin = (Toml.KeyValue) value;
            if (plugin.getValue() instanceof Toml.Literal) {
                Toml.Literal literal = (Toml.Literal) plugin.getValue();
                if (!(literal.getValue() instanceof String)) {
                    return plugin;
                }
                String[] parts = ((String) literal.getValue()).split(":", 2);
                if (parts.length != 2 || !StringUtils.matchesGlob(parts[0], pluginIdPattern)) {
                    return plugin;
                }
                try {
                    String selected = select(parts[1], parts[0], ctx);
                    if (selected != null) {
                        return plugin.withValue(literal.withSource(VersionCatalogToml.quoted(literal, parts[0] + ":" + selected))
                                .withValue(parts[0] + ":" + selected));
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(plugin);
                }
            } else if (plugin.getValue() instanceof Toml.Table) {
                Toml.Table inline = (Toml.Table) plugin.getValue();
                String id = TomlTableValue.getString(inline, "id");
                if (id == null || !StringUtils.matchesGlob(id, pluginIdPattern) || !TomlTableValue.has(inline, "version")) {
                    return plugin;
                }
                try {
                    String selected = select(TomlTableValue.getString(inline, "version"), id, ctx);
                    if (selected != null) {
                        return plugin.withValue(TomlTableValue.withString(inline, "version", selected));
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(plugin);
                }
            }
            return plugin;
        }));
    }

    private Toml.Table updateVersions(Toml.Table versions, Map<String, String> references) {
        return versions.withValues(ListUtils.map(versions.getValues(), value -> {
            if (!(value instanceof Toml.KeyValue) || !(((Toml.KeyValue) value).getKey() instanceof Toml.Identifier) ||
                    !(((Toml.KeyValue) value).getValue() instanceof Toml.Literal)) {
                return value;
            }
            String selected = references.get(((Toml.Identifier) ((Toml.KeyValue) value).getKey()).getName());
            if (selected == null) {
                return value;
            }
            Toml.Literal literal = (Toml.Literal) ((Toml.KeyValue) value).getValue();
            return ((Toml.KeyValue) value).withValue(literal.withSource(VersionCatalogToml.quoted(literal, selected)).withValue(selected));
        }));
    }

    private @Nullable String select(@Nullable String current, String id, ExecutionContext ctx) throws MavenDownloadingException {
        if (current == null) {
            return null;
        }
        return new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                .select(new GroupArtifactVersion(id, id + ".gradle.plugin", current), "classpath", newVersion, versionPattern, ctx);
    }
}
