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
package org.openrewrite.gradle;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.internal.VersionCatalogToml;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleVersionCatalogDependency;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Toml;

import java.util.HashMap;
import java.util.Map;

final class UpgradeDependencyVersionCatalog extends TomlIsoVisitor<ExecutionContext> {
    private final String groupId;
    private final String artifactId;
    private final @Nullable String newVersion;
    private final @Nullable String versionPattern;
    private final MavenMetadataFailures metadataFailures;
    private final @Nullable GradleProject gradleProject;
    private final DependencyMatcher dependencyMatcher;

    /**
     * Populated during {@link #visitDocument} and consumed during {@link #visitKeyValue}.
     */
    private final Map<String, String> referencedVersions = new HashMap<>();

    UpgradeDependencyVersionCatalog(
            String groupId,
            String artifactId,
            @Nullable String newVersion,
            @Nullable String versionPattern,
            MavenMetadataFailures metadataFailures,
            @Nullable GradleProject gradleProject) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.metadataFailures = metadataFailures;
        this.gradleProject = gradleProject;
        this.dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        return sourceFile instanceof Toml.Document &&
                sourceFile.getSourcePath().endsWith(VersionCatalogToml.FILE_NAME);
    }

    @Override
    public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
        referencedVersions.clear();
        Toml.Table libraries = VersionCatalogToml.findTable(document, "libraries");
        Toml.Table versions = VersionCatalogToml.findTable(document, "versions");
        if (libraries == null) {
            return document;
        }

        for (Toml value : libraries.getValues()) {
            if (!(value instanceof Toml.KeyValue)) {
                continue;
            }
            Toml.KeyValue kv = (Toml.KeyValue) value;
            GradleVersionCatalogDependency dep = GradleVersionCatalogDependency.Matcher.extract(kv, groupId, artifactId);
            if (dep == null || dep.getVersionRef() == null) {
                continue;
            }
            if (!dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
                continue;
            }
            try {
                String upgraded = selectVersion(VersionCatalogToml.getVersion(versions, dep.getVersionRef()), ctx);
                if (upgraded != null) {
                    referencedVersions.put(dep.getVersionRef(), upgraded);
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

        GradleVersionCatalogDependency dep = new GradleVersionCatalogDependency.Matcher()
                .groupPattern(groupId)
                .artifactPattern(artifactId)
                .get(getCursor())
                .orElse(null);

        if (dep != null && dep.getVersionRef() == null && dep.getVersion() != null) {
            try {
                String selected = selectVersion(dep.getVersion(), ctx);
                if (selected != null) {
                    return dep.withVersion(selected);
                }
            } catch (MavenDownloadingException e) {
                return e.warn(kv);
            }
            return kv;
        }

        return VersionCatalogToml.updateReferencedVersion(kv, getCursor(), referencedVersions);
    }

    private @Nullable String selectVersion(@Nullable String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
        if (currentVersion == null) {
            return null;
        }
        return new DependencyVersionSelector(metadataFailures, gradleProject, null)
                .select(new GroupArtifactVersion(groupId, artifactId, currentVersion), null, newVersion, versionPattern, ctx);
    }
}
