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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.*;

import java.util.List;
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
    MavenMetadataFailures metadataFailures;

    @Nullable
    GradleProject gradleProject;

    @Nullable
    GradleSettings gradleSettings;

    /**
     * Used to select a version for a new dependency that has no prior version, or the caller is not sure what the prior version is.
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
    public @Nullable String select(GroupArtifact ga,
                         @Nullable String configuration,
                         @Nullable String version,
                         @Nullable String versionPattern,
                         ExecutionContext ctx) throws MavenDownloadingException {
        String currentVersion = "0";
        if (gradleProject != null && configuration != null) {
            GradleDependencyConfiguration gdc = gradleProject.getConfiguration(configuration);
            if(gdc != null) {
                Dependency requested = gdc.findRequestedDependency(ga.getGroupId(), ga.getArtifactId());
                if(requested != null && requested.getVersion() != null) {
                    currentVersion = requested.getVersion();
                }
            }
        }
        return select(
                new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), currentVersion),
                configuration,
                // we don't want to select the latest patch in the 0.x line...
                "latest.patch".equalsIgnoreCase(version) && "0".equals(currentVersion) ? "latest.release" : version,
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
     *                       are used to resolve Maven metadata from.
     * @param version        The version to select, in node-semver format.
     * @param versionPattern The version pattern to select, if any.
     * @param ctx            The execution context, which can influence dependency resolution.
     * @return The selected version, if any.
     * @throws MavenDownloadingException If there is a problem downloading metadata for the dependency.
     */
    public @Nullable String select(ResolvedGroupArtifactVersion gav,
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
    public @Nullable String select(
            GroupArtifactVersion gav,
            @Nullable String configuration,
            @Nullable String version,
            @Nullable String versionPattern,
            ExecutionContext ctx) throws MavenDownloadingException {
        try {
            VersionComparator versionComparator = StringUtils.isBlank(version) ?
                    new LatestRelease(versionPattern) :
                    requireNonNull(Semver.validate(version, versionPattern).getValue());
            return select(gav, configuration, version, versionComparator, ctx);
        } catch (IllegalStateException e) {
            // this can happen when we encounter exotic versions
            return null;
        }
    }

    /**
     * Used to upgrade a version for a dependency that already has a version.
     *
     * @param gav            The group, artifact, and version of the existing dependency.
     * @param configuration  The configuration to select the version for. The configuration influences
     *                       which set of Maven repositories (either plugin repositories or regular repositories)
     *                       are used to resolve Maven metadata from.     * @param version        The version to select, in node-semver format.
     * @param versionComparator the comparator used to establish the validity of a potential upgrade
     * @param ctx            The execution context, which can influence dependency resolution.
     * @return The selected version, if any.
     * @throws MavenDownloadingException If there is a problem downloading metadata for the dependency.
     */
    public @Nullable String select(
            GroupArtifactVersion gav,
            @Nullable String configuration,
            @Nullable String version,
            VersionComparator versionComparator,
            ExecutionContext ctx) throws MavenDownloadingException {
        if (gav.getVersion() == null) {
            throw new IllegalArgumentException("Version must be specified. Call the select method " +
                                               "that accepts a GroupArtifact instead if there is no " +
                                               "current version.");
        }
        try {
            if (versionComparator instanceof ExactVersion) {
                return versionComparator.upgrade(gav.getVersion(), singletonList(version)).orElse(null);
            } else if (versionComparator instanceof LatestPatch && !Semver.isVersion(gav.getVersion())) {
                // in the case of "latest.patch", a new version can only be derived if the
                // current version is a semantic version
                return null;
            } else {
                return findNewerVersion(gav, configuration, versionComparator, ctx).orElse(null);
            }
        } catch (IllegalStateException e) {
            // this can happen when we encounter exotic versions
            return null;
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
            // There is still at least one place where this is null that remains to be fixed
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
            return gradleSettings.getBuildscript().getMavenRepositories();
        }
        if (gradleProject == null) {
            throw new IllegalStateException("Gradle project must be set to determine repositories."); // Caught by caller
        }
        return "classpath".equals(configuration) ?
                gradleProject.getBuildscript().getMavenRepositories() :
                gradleProject.getMavenRepositories();
    }
}
