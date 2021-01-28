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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
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
import static java.util.Collections.emptyMap;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpgradeParentVersion extends Recipe {
    private final String groupId;
    private final String artifactId;
    private final String newVersion;

    @Nullable
    private final String versionPattern;

    @Override
    public Validated validate() {
        Validated validated = super.validate();
        //noinspection ConstantConditions
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpgradeParentVersionVisitor(groupId, artifactId, newVersion, versionPattern);
    }

    private class UpgradeParentVersionVisitor extends MavenVisitor<ExecutionContext> {

        @Nullable
        private Collection<String> availableVersions;

        private VersionComparator versionComparator;

        public UpgradeParentVersionVisitor(String groupId, String artifactId, String toVersion, @Nullable String metadataPattern) {
            //noinspection ConstantConditions
            versionComparator = Semver.validate(toVersion, metadataPattern).getValue();
            setCursoringOn();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            return super.visitMaven(maven, ctx);
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isParentTag()) {
                if (groupId.equals(tag.getChildValue("groupId").orElse(null)) &&
                        artifactId.equals(tag.getChildValue("artifactId").orElse(null))) {
                    tag.getChildValue("version")
                            .flatMap(parentVersion -> findNewerDependencyVersion(groupId, artifactId, parentVersion))
                            .ifPresent(newer -> {
                                ChangeParentVersion changeParentVersion = new ChangeParentVersion(groupId, artifactId, newer);
                                doAfterVisit(changeParentVersion);
                            });
                }
            }

            return super.visitTag(tag, ctx);
        }

        private Optional<String> findNewerDependencyVersion(String groupId, String artifactId, String currentVersion) {
            if (availableVersions == null) {
                MavenMetadata mavenMetadata = new MavenDownloader(new NoopCache(), emptyMap(), settings)
                        .downloadMetadata(groupId, artifactId, emptyList());
                availableVersions = mavenMetadata.getVersioning().getVersions().stream()
                        .filter(versionComparator::isValid)
                        .collect(Collectors.toList());
            }

            LatestRelease latestRelease = new LatestRelease(versionPattern);
            return availableVersions.stream()
                    .filter(v -> latestRelease.compare(currentVersion, v) < 0)
                    .max(versionComparator);
        }
    }
}
