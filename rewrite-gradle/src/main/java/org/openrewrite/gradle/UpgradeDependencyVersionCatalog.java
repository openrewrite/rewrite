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
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.toml.tree.Toml;

final class UpgradeDependencyVersionCatalog implements GradleVersionCatalog.VersionCatalogUpdate {
    private final @Nullable String newVersion;
    private final @Nullable String versionPattern;
    private final MavenMetadataFailures metadataFailures;
    private final @Nullable GradleProject gradleProject;
    private final DependencyMatcher dependencyMatcher;

    UpgradeDependencyVersionCatalog(
            String groupId,
            String artifactId,
            @Nullable String newVersion,
            @Nullable String versionPattern,
            MavenMetadataFailures metadataFailures,
            @Nullable GradleProject gradleProject) {
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.metadataFailures = metadataFailures;
        this.gradleProject = gradleProject;
        this.dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
    }

    @Override
    public @Nullable String selectReferencedVersion(GradleVersionCatalog.VersionRefConsumer consumer,
                                                    String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
        GradleVersionCatalogDependency dependency = consumer.getDependency();
        if (dependency == null || !dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
            return null;
        }
        return selectVersion(currentVersion, dependency.getGroupId(), dependency.getArtifactId(), ctx);
    }

    @Override
    public Toml.KeyValue updateDependency(GradleVersionCatalogDependency dependency,
                                          @Nullable String referencedVersion, ExecutionContext ctx)
            throws MavenDownloadingException {
        if (!dependencyMatcher.matches(dependency.getGroupId(), dependency.getArtifactId())) {
            return dependency.getTree();
        }
        if (dependency.getVersionRef() != null) {
            return dependency.getTree();
        }
        String selected = selectVersion(dependency.getVersion(), dependency.getGroupId(), dependency.getArtifactId(), ctx);
        return selected == null ? dependency.getTree() : dependency.withVersion(selected);
    }

    private @Nullable String selectVersion(@Nullable String currentVersion, String dependencyGroupId,
                                           String dependencyArtifactId, ExecutionContext ctx) throws MavenDownloadingException {
        if (currentVersion == null) {
            return null;
        }
        return new DependencyVersionSelector(metadataFailures, gradleProject, null)
                .select(new GroupArtifactVersion(dependencyGroupId, dependencyArtifactId, currentVersion), null,
                        newVersion, versionPattern, ctx);
    }
}
