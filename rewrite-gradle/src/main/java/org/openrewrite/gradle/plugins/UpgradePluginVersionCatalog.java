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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.internal.VersionCatalogToml;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.trait.GradleVersionCatalogPlugin;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.toml.TomlIsoVisitor;
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
    private final Map<String, String> referencedVersions = new HashMap<>();

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
        referencedVersions.clear();
        Toml.Table plugins = VersionCatalogToml.findTable(document, "plugins");
        Toml.Table versions = VersionCatalogToml.findTable(document, "versions");
        if (plugins == null) {
            return document;
        }
        for (Toml value : plugins.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            GradleVersionCatalogPlugin plugin = GradleVersionCatalogPlugin.Matcher.extract(
                    (Toml.KeyValue) value, pluginIdPattern);
            if (plugin == null || plugin.getVersionRef() == null) {
                continue;
            }
            try {
                String selected = select(VersionCatalogToml.getVersion(versions, plugin.getVersionRef()),
                        plugin.getPluginId(), ctx);
                if (selected != null) {
                    referencedVersions.put(plugin.getVersionRef(), selected);
                }
            } catch (MavenDownloadingException e) {
                return e.warn(document);
            }
        }

        return super.visitDocument(document, ctx);
    }

    @Override
    public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
        Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);
        GradleVersionCatalogPlugin plugin = new GradleVersionCatalogPlugin.Matcher()
                .pluginIdPattern(pluginIdPattern)
                .get(getCursor())
                .orElse(null);
        if (plugin != null && plugin.getVersionRef() == null && plugin.getVersion() != null) {
            try {
                String selected = select(plugin.getVersion(), plugin.getPluginId(), ctx);
                return selected == null ? kv : plugin.withVersion(selected);
            } catch (MavenDownloadingException e) {
                return e.warn(kv);
            }
        }
        return VersionCatalogToml.updateReferencedVersion(kv, getCursor(), referencedVersions);
    }

    private @Nullable String select(@Nullable String current, String id, ExecutionContext ctx) throws MavenDownloadingException {
        if (current == null) {
            return null;
        }
        return new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                .select(new GroupArtifactVersion(id, id + ".gradle.plugin", current), "classpath", newVersion, versionPattern, ctx);
    }
}
