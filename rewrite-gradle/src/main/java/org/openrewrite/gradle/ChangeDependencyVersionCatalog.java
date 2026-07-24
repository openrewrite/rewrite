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
import org.openrewrite.ExecutionContext;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradleVersionCatalog;
import org.openrewrite.gradle.trait.GradleVersionCatalogDependency;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.toml.tree.Toml;

final class ChangeDependencyVersionCatalog implements GradleVersionCatalog.VersionCatalogUpdate {
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
    public @Nullable String selectReferencedVersion(GradleVersionCatalog.VersionRefConsumer consumer,
                                                    String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
        if (StringUtils.isBlank(newVersion)) {
            return null;
        }
        GradleVersionCatalogDependency dependency = consumer.getDependency();
        if (dependency == null || !dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
            return null;
        }
        String replacementGroupId = StringUtils.isBlank(newGroupId) ? dependency.getGroupId() : newGroupId;
        String replacementArtifactId = StringUtils.isBlank(newArtifactId) ? dependency.getArtifactId() : newArtifactId;
        return new DependencyVersionSelector(metadataFailures, gradleProject, null)
                .select(new GroupArtifact(replacementGroupId, replacementArtifactId), null,
                        newVersion, versionPattern, ctx);
    }

    @Override
    public Toml.KeyValue updateDependency(GradleVersionCatalogDependency dependency,
                                          @Nullable String referencedVersion, ExecutionContext ctx)
            throws MavenDownloadingException {
        if (!dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
            return dependency.getTree();
        }
        String replacementGroupId = StringUtils.isBlank(newGroupId) ? dependency.getGroupId() : newGroupId;
        String replacementArtifactId = StringUtils.isBlank(newArtifactId) ? dependency.getArtifactId() : newArtifactId;
        String selectedVersion = referencedVersion;
        if (dependency.getVersionRef() == null && !StringUtils.isBlank(newVersion)) {
            selectedVersion = new DependencyVersionSelector(metadataFailures, gradleProject, null)
                    .select(new GroupArtifact(replacementGroupId, replacementArtifactId), null, newVersion, versionPattern, ctx);
        }
        return dependency.withInlineCoordinatesAndVersion(replacementGroupId, replacementArtifactId,
                selectedVersion, Boolean.TRUE.equals(overrideManagedVersion));
    }
}
