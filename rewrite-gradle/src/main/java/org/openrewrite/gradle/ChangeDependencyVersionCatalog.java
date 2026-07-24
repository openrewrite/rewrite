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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Toml;

import java.util.HashMap;
import java.util.Map;

final class ChangeDependencyVersionCatalog extends TomlIsoVisitor<ExecutionContext> {
    private final String oldGroupId;
    private final String oldArtifactId;
    private final @Nullable String newGroupId;
    private final @Nullable String newArtifactId;
    private final @Nullable String newVersion;
    private final @Nullable String versionPattern;
    private final @Nullable Boolean overrideManagedVersion;
    private final MavenMetadataFailures metadataFailures;
    private final @Nullable GradleProject gradleProject;
    private final DependencyMatcher dependencyMatcher;

    /**
     * Maps {@code version.ref} key names → selected new version string.
     * Populated during {@link #visitDocument} and consumed during {@link #visitKeyValue}.
     */
    private final Map<String, String> versionRefUpgrades = new HashMap<>();

    ChangeDependencyVersionCatalog(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean overrideManagedVersion, MavenMetadataFailures metadataFailures, @Nullable GradleProject gradleProject) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.overrideManagedVersion = overrideManagedVersion;
        this.metadataFailures = metadataFailures;
        this.gradleProject = gradleProject;
        this.dependencyMatcher = new DependencyMatcher(oldGroupId, oldArtifactId, null);
    }

    @Override
    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
        return sourceFile instanceof Toml.Document &&
                sourceFile.getSourcePath().endsWith(VersionCatalogToml.FILE_NAME);
    }

    @Override
    public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
        versionRefUpgrades.clear();
        Toml.Table libraries = VersionCatalogToml.findTable(document, "libraries");
        Map<String, Integer> referenceCounts = new HashMap<>();
        Map<String, Integer> matchingReferenceCounts = new HashMap<>();

        if (libraries != null && !StringUtils.isBlank(newVersion)) {
            for (Toml value : libraries.getValues()) {
                if (!(value instanceof Toml.KeyValue)) {
                    continue;
                }
                Toml.KeyValue kv = (Toml.KeyValue) value;
                GradleVersionCatalogDependency dep = GradleVersionCatalogDependency.Matcher.extract(kv, null, null);
                if (dep == null || dep.getVersionRef() == null) {
                    continue;
                }
                referenceCounts.merge(dep.getVersionRef(), 1, Integer::sum);
                if (!dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
                    continue;
                }
                matchingReferenceCounts.merge(dep.getVersionRef(), 1, Integer::sum);
                String replacementGroupId = StringUtils.isBlank(newGroupId) ? dep.getGroupId() : newGroupId;
                String replacementArtifactId = StringUtils.isBlank(newArtifactId) ? dep.getArtifactId() : newArtifactId;
                try {
                    String selected = new DependencyVersionSelector(metadataFailures, gradleProject, null).select(new GroupArtifact(replacementGroupId, replacementArtifactId), null, newVersion, versionPattern, ctx);
                    if (selected != null) {
                        versionRefUpgrades.put(dep.getVersionRef(), selected);
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(document);
                }
            }
            versionRefUpgrades.entrySet().removeIf(entry ->
                    !referenceCounts.getOrDefault(entry.getKey(), 0).equals(matchingReferenceCounts.get(entry.getKey())));
        }

        return super.visitDocument(document, ctx);
    }

    @Override
    public Toml.KeyValue visitKeyValue(Toml.KeyValue keyValue, ExecutionContext ctx) {
        Toml.KeyValue kv = super.visitKeyValue(keyValue, ctx);

        GradleVersionCatalogDependency dep = new GradleVersionCatalogDependency.Matcher().groupPattern(oldGroupId).artifactPattern(oldArtifactId).get(getCursor()).orElse(null);

        if (dep != null) {
            String replacementGroupId = StringUtils.isBlank(newGroupId) ? dep.getGroupId() : newGroupId;
            String replacementArtifactId = StringUtils.isBlank(newArtifactId) ? dep.getArtifactId() : newArtifactId;
            String selectedVersion = null;
            if (!StringUtils.isBlank(newVersion)) {
                try {
                    selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null).select(new GroupArtifact(replacementGroupId, replacementArtifactId), null, newVersion, versionPattern, ctx);
                } catch (MavenDownloadingException e) {
                    return e.warn(kv);
                }
            }
            return dep.withInlineCoordinatesAndVersion(replacementGroupId, replacementArtifactId, selectedVersion, Boolean.TRUE.equals(overrideManagedVersion));
        }

        return VersionCatalogToml.updateReferencedVersion(kv, getCursor(), versionRefUpgrades);
    }
}
