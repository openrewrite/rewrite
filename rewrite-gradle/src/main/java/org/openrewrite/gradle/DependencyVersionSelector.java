/*
 * Copyright 2024 the original author or authors.
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

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Selects versions for new or existing dependencies based on a node-semver selector
 * by inspecting available versions in the Maven metadata from a set of Maven repositories.
 */
@Incubating(since = "8.17.0")
@Value
public class DependencyVersionSelector {
    @Nullable
    MavenMetadataFailures metadataFailures;

    @Nullable
    GradleProject gradleProject;

    @Nullable
    GradleSettings gradleSettings;

    /**
     * Used to select a version for a new dependency that has no prior version.
     *
     * @param ga             The group and artifact of the new dependency.
     * @param configuration  The configuration to select the version for. The configuration influences
     *                       which set of Maven repositories (either plugin repositories or regular repositories)
     *                       are used to resolve Maven metadata from.
     * @param version        The version to select, in node-semver format.
     * @param versionPattern The version pattern to select, if any.
     * @param ctx            The execution context, which can influence dependency resolution.
     * @return The selected version, if any.
     * @throws MavenDownloadingException If there is a problem downloading metadata for the dependency.
     */
    @Nullable
    public String select(GroupArtifact ga,
                         String configuration,
                         @Nullable String version,
                         @Nullable String versionPattern,
                         ExecutionContext ctx) throws MavenDownloadingException {
        return select(
                new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), "0"),
                configuration,
                // we don't want to select the latest patch in the 0.x line...
                "latest.patch".equalsIgnoreCase(version) ? "latest.release" : version,
                versionPattern,
                ctx
        );
    }

    /**
     * Used to upgrade a version for a dependency that already has a version.
     *
     * @param gav            The group, artifact, and version of the existing dependency.
     * @param configuration  The configuration to select the version for. The configuration influences
     *                       which set of Maven repositories (either plugin repositories or regular repositories)
     *                       are used to resolve Maven metadata from.     * @param version        The version to select, in node-semver format.
     * @param versionPattern The version pattern to select, if any.
     * @param ctx            The execution context, which can influence dependency resolution.
     * @return The selected version, if any.
     * @throws MavenDownloadingException If there is a problem downloading metadata for the dependency.
     */
    @Nullable
    public String select(ResolvedGroupArtifactVersion gav,
                         String configuration,
                         @Nullable String version,
                         @Nullable String versionPattern,
                         ExecutionContext ctx) throws MavenDownloadingException {
        return select(new GroupArtifactVersion(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()),
                configuration, version, versionPattern, ctx);
    }

    /**
     * Used to upgrade a version for a dependency that already has a version.
     *
     * @param gav            The group, artifact, and version of the existing dependency.
     * @param configuration  The configuration to select the version for. The configuration influences
     *                       which set of Maven repositories (either plugin repositories or regular repositories)
     *                       are used to resolve Maven metadata from.     * @param version        The version to select, in node-semver format.
     * @param versionPattern The version pattern to select, if any.
     * @param ctx            The execution context, which can influence dependency resolution.
     * @return The selected version, if any.
     * @throws MavenDownloadingException If there is a problem downloading metadata for the dependency.
     */
    @Nullable
    public String select(GroupArtifactVersion gav,
                         @Nullable String configuration,
                         @Nullable String version,
                         @Nullable String versionPattern,
                         ExecutionContext ctx) throws MavenDownloadingException {
        if (gav.getVersion() == null) {
            throw new IllegalArgumentException("Version must be specified. Call the select method " +
                                               "that accepts a GroupArtifact instead if there is no " +
                                               "current version.");
        }

        VersionComparator versionComparator = StringUtils.isBlank(version) ?
                new LatestRelease(versionPattern) :
                requireNonNull(Semver.validate(version, versionPattern).getValue());

        if (versionComparator instanceof ExactVersion) {
            return versionComparator.upgrade(gav.getVersion(), singletonList(version)).orElse(null);
        } else if (versionComparator instanceof LatestPatch &&
                   !versionComparator.isValid(gav.getVersion(), gav.getVersion())) {
            // in the case of "latest.patch", a new version can only be derived if the
            // current version is a semantic version
            return null;
        } else {
            return findNewerVersion(gav, configuration, versionComparator, ctx).orElse(null);
        }
    }

    private Optional<String> findNewerVersion(GroupArtifactVersion gav,
                                              @Nullable String configuration,
                                              VersionComparator versionComparator,
                                              ExecutionContext ctx) throws MavenDownloadingException {
        try {
            if(gav.getGroupId() == null) {
                return Optional.empty();
            }
            List<MavenRepository> repos = determineRepos(configuration);
            MavenMetadata mavenMetadata = metadataFailures == null ?
                    downloadMetadata(gav.getGroupId(), gav.getArtifactId(), repos, ctx) :
                    metadataFailures.insertRows(ctx, () -> downloadMetadata(gav.getGroupId(), gav.getArtifactId(),
                            repos, ctx));
            return versionComparator.upgrade(requireNonNull(gav.getVersion()),
                    mavenMetadata.getVersioning().getVersions());
        } catch (IllegalStateException e) {
            // this can happen when we encounter exotic versions
            return Optional.empty();
        }
    }

    private MavenMetadata downloadMetadata(String groupId, String artifactId, List<MavenRepository> repositories, ExecutionContext ctx) throws MavenDownloadingException {
        return new MavenPomDownloader(ctx).downloadMetadata(
                new GroupArtifact(groupId, artifactId), null, repositories);
    }

    private List<MavenRepository> determineRepos(@Nullable String configuration) {
        if (gradleSettings != null) {
            return gradleSettings.getPluginRepositories();
        }
        Objects.requireNonNull(gradleProject);
        return "classpath".equals(configuration) ?
                gradleProject.getMavenPluginRepositories() :
                gradleProject.getMavenRepositories();
    }
}
