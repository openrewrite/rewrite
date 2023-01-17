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
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Upgrade the version of a plugin using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
@Incubating(since = "7.7.0")
@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradePluginVersion extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
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
            example = "false",
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
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new FindPlugin(groupId, artifactId).getVisitor();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = requireNonNull(Semver.validate(newVersion, versionPattern).getValue());
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isPluginTag(groupId, artifactId)) {
                    Optional<Xml.Tag> versionTag = tag.getChild("version");
                    Optional<String> maybeVersionValue = versionTag.flatMap(Xml.Tag::getValue);
                    if (maybeVersionValue.isPresent()) {
                        String versionValue = maybeVersionValue.get();
                        String versionLookup = versionValue.startsWith("${")
                                ? super.getResolutionResult().getPom().getValue(versionValue.trim())
                                : versionValue;

                        if (versionLookup != null) {
                            try {
                                String tagGroupId = getResolutionResult().getPom().getValue(
                                        tag.getChildValue("groupId").orElse(groupId)
                                );
                                String tagArtifactId = getResolutionResult().getPom().getValue(
                                        tag.getChildValue("artifactId").orElse(artifactId)
                                );
                                assert tagGroupId != null;
                                assert tagArtifactId != null;
                                findNewerDependencyVersion(tagGroupId, tagArtifactId, versionLookup, ctx).ifPresent(newer -> {
                                    ChangePluginVersionVisitor changeDependencyVersion = new ChangePluginVersionVisitor(groupId, artifactId, newer);
                                    doAfterVisit(changeDependencyVersion);
                                });
                            } catch (MavenDownloadingException e) {
                                return e.warn(tag);
                            }
                        }
                    }
                    return tag;
                }
                return super.visitTag(tag, ctx);
            }

            private Optional<String> findNewerDependencyVersion(String groupId, String artifactId,
                                                                String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
                MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                Collection<String> availableVersions = new ArrayList<>();
                for (String v : mavenMetadata.getVersioning().getVersions()) {
                    if (versionComparator.isValid(currentVersion, v)) {
                        availableVersions.add(v);
                    }
                }
                return versionComparator.upgrade(currentVersion, availableVersions);
            }
        };
    }

    private static class ChangePluginVersionVisitor extends MavenVisitor<ExecutionContext> {
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
                        if (version.trim().startsWith("${") && !newVersion.equals(getResolutionResult().getPom().getValue(version.trim()))) {
                            doAfterVisit(new ChangePropertyValue(version, newVersion, false, false));
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
