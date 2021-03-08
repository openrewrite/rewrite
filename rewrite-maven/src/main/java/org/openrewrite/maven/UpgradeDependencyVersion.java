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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.DependencyDescriptor;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Upgrade the version a group or group and artifact using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeDependencyVersion extends Recipe {

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "com.google.guava")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'com.google.guava:guava:VERSION'.",
            example = "guava")
    @Nullable
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
        return "Upgrade Maven dependency version";
    }

    @Override
    public String getDescription() {
        return "Upgrade the version a group or group and artifact using Node Semver advanced range selectors, " +
                "allowing more precise control over version updates to patch or minor releases.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new UpgradeDependencyVersionVisitor();
    }

    private class UpgradeDependencyVersionVisitor extends MavenVisitor {
        @Nullable
        private Collection<String> availableVersions;

        private final VersionComparator versionComparator;

        public UpgradeDependencyVersionVisitor() {
            //noinspection ConstantConditions
            versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        }

        @Override
        public Maven visitMaven(Maven maven, ExecutionContext ctx) {
            maybeChangeDependencyVersion(maven.getModel(), ctx);

            for (Pom module : maven.getModules()) {
                maybeChangeDependencyVersion(module, ctx);
            }

            return super.visitMaven(maven, ctx);
        }

        private void maybeChangeDependencyVersion(Pom model, ExecutionContext ctx) {
            for (DependencyDescriptor dependency : model.getDependencies()) {
                maybeChangeDependencyVersionForDescriptor(ctx, dependency);
            }

            for (DependencyDescriptor dependency : model.getDependencyManagement().getDependencies()) {
                maybeChangeDependencyVersionForDescriptor(ctx, dependency);
            }
        }

        private void maybeChangeDependencyVersionForDescriptor(ExecutionContext ctx, DependencyDescriptor dependency) {
            if (dependency.getGroupId().equals(groupId) && (artifactId == null || dependency.getArtifactId().equals(artifactId))) {
                findNewerDependencyVersion(groupId, dependency.getArtifactId(), dependency.getVersion(), ctx).ifPresent(newer -> {
                    ChangeDependencyVersionVisitor changeDependencyVersion = new ChangeDependencyVersionVisitor(newer, dependency.getArtifactId());
                    doAfterVisit(changeDependencyVersion);
                });
            }
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


    private class ChangeDependencyVersionVisitor extends MavenVisitor {
        private final String newVersion;
        private final String artifactId;

        private ChangeDependencyVersionVisitor(String newVersion, String artifactId) {
            this.newVersion = newVersion;
            this.artifactId = artifactId;
        }

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag(groupId, artifactId) || isManagedDependencyTag(groupId, artifactId)) {
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
                // In this case a transitive dependency has been removed and the dependency now requires a version
                else if (!versionTag.isPresent() && !isManagedDependencyTag(groupId, artifactId)) {
                    Xml.Tag newVersionTag = Xml.Tag.build("<version>" + newVersion + "</version>");
                    doAfterVisit(new AddToTagVisitor<>(getCursor().getValue(), newVersionTag));
                }
            } else if (!modules.isEmpty() && isPropertyTag()) {
                String propertyKeyRef = "${" + tag.getName() + "}";

                OUTER:
                for (Pom module : modules) {
                    for (Pom.Dependency dependency : module.getDependencies()) {
                        if (propertyKeyRef.equals(dependency.getRequestedVersion())) {
                            doAfterVisit(new ChangeTagValueVisitor<>(tag, newVersion));
                            break OUTER;
                        }
                    }

                }
            }

            return super.visitTag(tag, ctx);
        }
    }
}
