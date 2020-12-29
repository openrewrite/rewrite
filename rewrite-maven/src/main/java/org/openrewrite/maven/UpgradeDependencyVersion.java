/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven;

import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.NoopCache;
import org.openrewrite.maven.internal.MavenDownloader;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.tree.DependencyManagementDependency;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.semver.HyphenRange;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Validated.required;

/**
 * Upgrade the version a group or group and artifact using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
public class UpgradeDependencyVersion extends MavenRefactorVisitor {
    @Nullable
    private Collection<String> availableVersions;

    private String groupId;

    @Nullable
    private String artifactId;

    /**
     * Node Semver range syntax.
     */
    private String toVersion;

    @Nullable
    private String metadataPattern;

    private VersionComparator versionComparator;

    public UpgradeDependencyVersion() {
        setCursoringOn();
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(@Nullable String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    /**
     * Allows us to extend version selection beyond the original Node Semver semantics. So for example,
     * We can pair a {@link HyphenRange} of "25-29" with a metadata pattern of "-jre" to select
     * Guava 29.0-jre
     *
     * @param metadataPattern The metadata pattern extending semver selection.
     */
    public void setMetadataPattern(@Nullable String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("toVersion", toVersion))
                .and(Semver.validate(toVersion, metadataPattern));
    }

    @Override
    public boolean isIdempotent() {
        return false;
    }

    @Override
    public Maven visitMaven(Maven maven) {
        versionComparator = Semver.validate(toVersion, metadataPattern).getValue();
        downloader = maven.getDownloader();

        maybeChangeDependencyVersion(maven.getModel());

        for (Pom module : maven.getModules()) {
            maybeChangeDependencyVersion(module);
        }

        return super.visitMaven(maven);
    }

    private void maybeChangeDependencyVersion(Pom model) {
        for (Pom.Dependency dependency : model.getDependencies()) {
            if (dependency.getGroupId().equals(groupId) && (artifactId == null || dependency.getArtifactId().equals(artifactId))) {
                findNewerDependencyVersion(groupId, dependency.getArtifactId(), dependency.getVersion()).ifPresent(newer -> {
                    ChangeDependencyVersion changeDependencyVersion = new ChangeDependencyVersion();
                    changeDependencyVersion.setGroupId(groupId);
                    changeDependencyVersion.setArtifactId(dependency.getArtifactId());
                    changeDependencyVersion.setToVersion(newer);
                    andThen(changeDependencyVersion);
                });
            }
        }

        for (DependencyManagementDependency dependency : model.getDependencyManagement().getDependencies()) {
            if (dependency.getGroupId().equals(groupId) && (artifactId == null || dependency.getArtifactId().equals(artifactId))) {
                findNewerDependencyVersion(groupId, dependency.getArtifactId(), dependency.getVersion()).ifPresent(newer -> {
                    ChangeDependencyVersion changeDependencyVersion = new ChangeDependencyVersion();
                    changeDependencyVersion.setGroupId(groupId);
                    changeDependencyVersion.setArtifactId(dependency.getArtifactId());
                    changeDependencyVersion.setToVersion(newer);
                    andThen(changeDependencyVersion);
                });
            }
        }
    }

    private Optional<String> findNewerDependencyVersion(String groupId, String artifactId, String currentVersion) {
        if (availableVersions == null) {
            MavenMetadata mavenMetadata = downloader.downloadMetadata(groupId, artifactId, emptyList());
            availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                    .filter(versionComparator::isValid)
                    .collect(Collectors.toList());
        }

        LatestRelease latestRelease = new LatestRelease(metadataPattern);
        return availableVersions.stream()
                .filter(v -> latestRelease.compare(currentVersion, v) < 0)
                .max(versionComparator);
    }
}
