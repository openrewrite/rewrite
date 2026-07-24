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
import org.openrewrite.gradle.DependencyVersionSelector;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.gradle.trait.GradleVersionCatalog;
import org.openrewrite.gradle.trait.GradleVersionCatalogPlugin;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.toml.tree.Toml;

import static org.openrewrite.internal.StringUtils.matchesGlob;

final class UpgradePluginVersionCatalog implements GradleVersionCatalog.VersionCatalogUpdate {
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
    public @Nullable String selectReferencedVersion(GradleVersionCatalog.VersionRefConsumer consumer,
                                                    String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
        GradleVersionCatalogPlugin plugin = consumer.getPlugin();
        if (plugin == null || !StringUtils.isBlank(pluginIdPattern) &&
                !matchesGlob(plugin.getPluginId(), pluginIdPattern)) {
            return null;
        }
        return select(currentVersion, plugin.getPluginId(), ctx);
    }

    @Override
    public Toml.KeyValue updatePlugin(GradleVersionCatalogPlugin plugin,
                                      @Nullable String referencedVersion, ExecutionContext ctx)
            throws MavenDownloadingException {
        if (!StringUtils.isBlank(pluginIdPattern) && !matchesGlob(plugin.getPluginId(), pluginIdPattern)) {
            return plugin.getTree();
        }
        if (plugin.getVersionRef() != null) {
            return plugin.getTree();
        }
        String selected = select(plugin.getVersion(), plugin.getPluginId(), ctx);
        return selected == null ? plugin.getTree() : plugin.withVersion(selected);
    }

    private @Nullable String select(@Nullable String current, String id, ExecutionContext ctx) throws MavenDownloadingException {
        if (current == null) {
            return null;
        }
        return new DependencyVersionSelector(metadataFailures, gradleProject, gradleSettings)
                .select(new GroupArtifactVersion(id, id + ".gradle.plugin", current), "classpath", newVersion, versionPattern, ctx);
    }
}
