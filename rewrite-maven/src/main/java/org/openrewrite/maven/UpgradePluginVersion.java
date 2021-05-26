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
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.marker.XmlSearchResult;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Upgrade the version of a plugin using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
@Incubating(since = "7.7.0")
@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradePluginVersion extends Recipe {
    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Option(displayName = "New version",
            description = "An exact version number, or node-style semver selector used to select the version number.",
            example = "29.X")
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                    "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    // needs implementation, left here as syntactic placeholder // todo
    @Option(displayName = "Trust parent POM",
            description = "Even if the parent suggests a version that is older than what we are trying to upgrade to, trust it anyway. " +
                    "Useful when you want to wait for the parent to catch up before upgrading. The parent is not trusted by default.",
            required = false)
    @Nullable
    Boolean trustParent;

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated validate() {
        Validated validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Upgrade Maven plugin version";
    }

    @Override
    public String getDescription() {
        return "Upgrade the version of a plugin using Node Semver advanced range selectors, " +
                "allowing more precise control over version updates to patch or minor releases.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new MavenVisitor() {
            @Override
            public Maven visitMaven(Maven maven, ExecutionContext ctx) {
                if (!FindPlugin.find(maven, groupId, artifactId).isEmpty()) {
                    maven = maven.withMarkers(maven.getMarkers().addIfAbsent(new XmlSearchResult(UpgradePluginVersion.this)));
                }
                return super.visitMaven(maven, ctx);
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpgradePluginVersionVisitor();
    }

    private class UpgradePluginVersionVisitor extends MavenVisitor {
        private final VersionComparator versionComparator;
        @Nullable
        private Collection<String> availableVersions;

        public UpgradePluginVersionVisitor() {
            //noinspection ConstantConditions
            versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            Maven m = super.visitMaven(maven, ctx);
            FindPlugin.find(m, groupId, artifactId).forEach(plugin -> {
                maybeChangePluginVersion(plugin, ctx);
            });

            return m;
        }

        private void maybeChangePluginVersion(Xml.Tag model, ExecutionContext ctx) {
            Optional<Xml.Tag> versionTag = model.getChild("version");
            versionTag.ifPresent(tag -> {
                tag.getValue().ifPresent(versionValue -> {

                    String versionLookup = versionValue.startsWith("${")
                            ? super.model.getValue(versionValue.trim())
                            : versionValue;

                    if (versionLookup != null) {
                        findNewerDependencyVersion(groupId, artifactId, versionLookup, ctx).ifPresent(newer -> {
                            ChangePluginVersionVisitor changeDependencyVersion = new ChangePluginVersionVisitor(groupId, artifactId, newer);
                            doAfterVisit(changeDependencyVersion);
                        });
                    }

                });
            });
        }

        private Optional<String> findNewerDependencyVersion(String groupId, String artifactId, String currentVersion, ExecutionContext ctx) {
            if (availableVersions == null) {
                MavenMetadata mavenMetadata = new MavenPomDownloader(MavenPomCache.NOOP, Collections.emptyMap(), ctx)
                        .downloadMetadata(groupId, artifactId, Collections.emptyList());
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

    private static class ChangePluginVersionVisitor extends MavenVisitor {
        private final String groupId;
        private final String artifactId;
        private final String newVersion;

        private ChangePluginVersionVisitor(String groupId, String artifactId, String newVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.newVersion = newVersion;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPluginTag(groupId, artifactId)) {
                Optional<Xml.Tag> versionTag = tag.getChild("version");
                if (versionTag.isPresent()) {
                    String version = versionTag.get().getValue().orElse(null);
                    if (version != null) {
                        if (version.trim().startsWith("${") && !newVersion.equals(model.getValue(version.trim()))) {
                            doAfterVisit(new ChangePropertyValue(version, newVersion));
                        } else if (!newVersion.equals(version)) {
                            doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), newVersion));
                        }
                    }
                }
            }

            return super.visitTag(tag, ctx);
        }
    }

}
