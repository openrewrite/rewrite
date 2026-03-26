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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Upgrade the version of a plugin using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradePluginVersion extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'. " +
                          "Supports globs.",
            example = "org.openrewrite.maven")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.openrewrite.maven:rewrite-maven-plugin:VERSION'. " +
                          "Supports globs.",
            example = "rewrite-maven-plugin")
    String artifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors)",
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

    @Option(displayName = "Add version if missing",
            description = "If the plugin is missing a version, add the latest release. Defaults to false.",
            required = false)
    @Nullable
    Boolean addVersionIfMissing;

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    String displayName = "Upgrade Maven plugin version";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, newVersion);
    }

    String description = "Upgrade the version of a plugin using Node Semver advanced range selectors, " +
               "allowing more precise control over version updates to patch or minor releases.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = requireNonNull(Semver.validate(newVersion, versionPattern).getValue());
        return Preconditions.check(new FindPlugin(groupId, artifactId), new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isPluginTag(groupId, artifactId)) {
                    Optional<Xml.Tag> versionTag = tag.getChild("version");
                    Optional<String> maybeVersionValue = versionTag.flatMap(Xml.Tag::getValue);
                    if (maybeVersionValue.isPresent() || Boolean.TRUE.equals(addVersionIfMissing)) {
                        try {
                            ResolvedPom resolvedPom = getResolutionResult().getPom();
                            String tagGroupId = resolvedPom.getValue(tag.getChildValue("groupId").orElse(groupId));
                            String tagArtifactId = resolvedPom.getValue(tag.getChildValue("artifactId").orElse(artifactId));
                            assert tagGroupId != null;
                            assert tagArtifactId != null;

                            final String versionLookup;
                            if (maybeVersionValue.isPresent()) {
                                String versionValue = maybeVersionValue.get();
                                versionLookup = versionValue.startsWith("${") ?
                                        resolvedPom.getValue(versionValue.trim()) :
                                        versionValue;
                            } else {
                                if (hasManagedPluginVersion(resolvedPom, tagGroupId, tagArtifactId)) {
                                    return tag;
                                }
                                versionLookup = "0.0.0";
                            }

                            findNewerDependencyVersion(tagGroupId, tagArtifactId, versionLookup, ctx).ifPresent(newer ->
                                    doAfterVisit(new ChangePluginVersionVisitor(tagGroupId, tagArtifactId, newer, Boolean.TRUE.equals(addVersionIfMissing)))
                            );
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                    return tag;
                }
                return super.visitTag(tag, ctx);
            }

            private boolean hasManagedPluginVersion(ResolvedPom resolvedPom, String groupId, String artifactId) {
                for (Plugin p : ListUtils.concatAll(resolvedPom.getPluginManagement(),
                        resolvedPom.getRequested().getPluginManagement())) {
                    if (p.getGroupId().equals(groupId) &&
                            p.getArtifactId().equals(artifactId) &&
                            p.getVersion() != null) {
                        return true;
                    }
                }
                return false;
            }

            private Optional<String> findNewerDependencyVersion(String groupId, String artifactId,
                                                                String currentVersion, ExecutionContext ctx) throws MavenDownloadingException {
                MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadPluginMetadata(groupId, artifactId, ctx));
                Collection<String> availableVersions = new ArrayList<>();
                for (String v : mavenMetadata.getVersioning().getVersions()) {
                    if (versionComparator.isValid(currentVersion, v)) {
                        availableVersions.add(v);
                    }
                }
                return versionComparator.upgrade(currentVersion, availableVersions);
            }
        });
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ChangePluginVersionVisitor extends MavenVisitor<ExecutionContext> {
        String groupId;
        String artifactId;
        String newVersion;
        boolean addVersionIfMissing;

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isPluginTag(groupId, artifactId)) {
                Optional<Xml.Tag> versionTag = tag.getChild("version");
                if (versionTag.isPresent()) {
                    String version = versionTag.get().getValue().orElse(null);
                    if (version != null) {
                        if (version.trim().startsWith("${")) {
                            if (!newVersion.equals(getResolutionResult().getPom().getValue(version.trim()))) {
                                doAfterVisit(new ChangePropertyValue(version, newVersion, false, false).getVisitor());
                            }
                        } else if (!newVersion.equals(version)) {
                            doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), newVersion));
                        }
                    }
                } else if (addVersionIfMissing) {
                    Xml.Tag newTag = Xml.Tag.build("<version>" + newVersion + "</version>");
                    doAfterVisit(new AddToTagVisitor<>(tag, newTag, new MavenTagInsertionComparator(tag.getChildren())));
                }
            }
            return super.visitTag(tag, ctx);
        }
    }
}
