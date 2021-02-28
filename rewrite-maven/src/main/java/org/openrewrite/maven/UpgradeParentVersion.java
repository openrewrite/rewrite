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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeParentVersion extends Recipe {
    private static final XPathMatcher PARENT_VERSION_MATCHER = new XPathMatcher("/project/parent/version");

    @Option(displayName = "Group ID", description = "Group ID of parent to upgrade.")
    String groupId;

    @Option(displayName = "Artifact ID", description = "Artifact ID of parent to upgrade.")
    String artifactId;

    @Option(displayName = "New Version", description = "An exact version number, or node-style semver selector to upgrade the parent pom version to.")
    String newVersion;

    @Option(displayName = "Version Metadata Pattern", description =
            "A regular expression used to validate the metadata of a version number. " +
                    "e.g.: \"jre\" ensures that version \"1.0.0-jre\" would be selected instead of \"1.0.0-android\" ",
            required = false)
    @Nullable
    String versionPattern;

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
    public String getDisplayName() {
        return "Upgrade Maven parent project version";
    }

    @Override
    public String getDescription() {
        return "Set the parent pom version number according to a node-style semver selector or to a specific version number";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpgradeParentVersionVisitor(newVersion, versionPattern);
    }

    private class UpgradeParentVersionVisitor extends MavenVisitor {
        @Nullable
        private Collection<String> availableVersions;

        private final VersionComparator versionComparator;

        public UpgradeParentVersionVisitor(String toVersion, @Nullable String metadataPattern) {
            //noinspection ConstantConditions
            versionComparator = Semver.validate(toVersion, metadataPattern).getValue();
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
                            .flatMap(parentVersion -> findNewerDependencyVersion(groupId, artifactId, parentVersion, ctx))
                            .ifPresent(newer -> {
                                ChangeParentVersion changeParentVersion = new ChangeParentVersion(newer);
                                doAfterVisit(changeParentVersion);
                            });
                }
            }

            return super.visitTag(tag, ctx);
        }

        private Optional<String> findNewerDependencyVersion(String groupId, String artifactId, String currentVersion,
                                                            ExecutionContext ctx) {
            if (availableVersions == null) {
                MavenMetadata mavenMetadata = new MavenPomDownloader(MavenPomCache.NOOP,
                        emptyMap(), ctx).downloadMetadata(groupId, artifactId, emptyList());
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

    private class ChangeParentVersion extends MavenVisitor {

        private final String toVersion;

        private ChangeParentVersion(String toVersion) {
            this.toVersion = toVersion;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (PARENT_VERSION_MATCHER.matches(getCursor())) {
                Xml.Tag parent = getCursor().getParentOrThrow().getValue();
                if (groupId.equals(parent.getChildValue("groupId").orElse(null)) &&
                        artifactId.equals(parent.getChildValue("artifactId").orElse(null)) &&
                        !toVersion.equals(tag.getValue().orElse(null))) {
                    doAfterVisit(new ChangeTagValueVisitor<>(tag, toVersion));
                }
            }
            return super.visitTag(tag, ctx);
        }
    }
}
