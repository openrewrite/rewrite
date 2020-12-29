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
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Validated.required;

public class UpgradeParentVersion extends MavenRefactorVisitor {
    @Nullable
    private Collection<String> availableVersions;

    private String groupId;
    private String artifactId;
    private String toVersion;

    @Nullable
    private String metadataPattern;

    private VersionComparator versionComparator;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setToVersion(String toVersion) {
        this.toVersion = toVersion;
    }

    public void setMetadataPattern(@Nullable String metadataPattern) {
        this.metadataPattern = metadataPattern;
    }

    public UpgradeParentVersion() {
        setCursoringOn();
    }

    @Override
    public Validated validate() {
        return required("groupId", groupId)
                .and(required("artifactId", artifactId))
                .and(required("toVersion", toVersion))
                .and(Semver.validate(toVersion, metadataPattern));
    }

    @Override
    public Maven visitMaven(Maven maven) {
        versionComparator = Semver.validate(toVersion, metadataPattern).getValue();
        return super.visitMaven(maven);
    }

    @Override
    public Xml visitTag(Xml.Tag tag) {
        if (isParentTag()) {
            if (groupId.equals(tag.getChildValue("groupId").orElse(null)) &&
                    artifactId.equals(tag.getChildValue("artifactId").orElse(null))) {
                tag.getChildValue("version")
                        .flatMap(parentVersion -> findNewerDependencyVersion(groupId, artifactId, parentVersion))
                        .ifPresent(newer -> {
                            ChangeParentVersion changeParentVersion = new ChangeParentVersion();
                            changeParentVersion.setGroupId(groupId);
                            changeParentVersion.setArtifactId(artifactId);
                            changeParentVersion.setToVersion(newer);
                            andThen(changeParentVersion);
                        });
            }
        }

        return super.visitTag(tag);
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
