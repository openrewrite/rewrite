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
import org.openrewrite.maven.tree.Profile;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Option(displayName = "Trust parent POM",
            description = "Whether to preserve an inherited managed version when the plugin declaration has no version. " +
                          "Set to false with `addVersionIfMissing` to override an older inherited version. " +
                          "When unset, inherited plugin management is preserved.",
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

                            String profileId = getCursor().getPathAsStream()
                                    .filter(Xml.Tag.class::isInstance).map(Xml.Tag.class::cast)
                                    .filter(parent -> "profile".equals(parent.getName())).findFirst()
                                    .flatMap(profile -> profile.getChildValue("id")).orElse(null);
                            boolean overrideInheritedVersion = Boolean.FALSE.equals(trustParent) &&
                                    !hasLocallyManagedPluginVersion(resolvedPom, tagGroupId, tagArtifactId, profileId);
                            final String versionLookup;
                            if (maybeVersionValue.isPresent()) {
                                versionLookup = resolvedPom.getValue(maybeVersionValue.get().trim());
                            } else {
                                String managedVersion = getManagedPluginVersion(resolvedPom, tagGroupId, tagArtifactId);
                                if (managedVersion != null && !overrideInheritedVersion) {
                                    return tag;
                                }
                                versionLookup = managedVersion == null ? "0.0.0" : resolvedPom.getValue(managedVersion);
                                if (versionLookup == null) {
                                    return tag;
                                }
                            }

                            findNewerDependencyVersion(tagGroupId, tagArtifactId, versionLookup, ctx).ifPresent(newer ->
                                    doAfterVisit(new ChangePluginVersionVisitor(tagGroupId, tagArtifactId, newer,
                                            Boolean.TRUE.equals(addVersionIfMissing), overrideInheritedVersion, tag.getId()))
                            );
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                    return tag;
                }
                return super.visitTag(tag, ctx);
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

    private static boolean hasManagedPluginVersion(ResolvedPom resolvedPom, String groupId, String artifactId) {
        return getManagedPluginVersion(resolvedPom, groupId, artifactId) != null;
    }

    private static boolean hasLocallyManagedPluginVersion(ResolvedPom resolvedPom, String groupId, String artifactId,
                                                         @Nullable String profileId) {
        if (hasManagedPluginVersion(resolvedPom.getRequested().getPluginManagement(), groupId, artifactId)) {
            return true;
        }
        if (profileId != null) {
            for (Profile profile : resolvedPom.getRequested().getProfiles()) {
                if (profileId.equals(profile.getId()) && hasManagedPluginVersion(profile.getPluginManagement(), groupId, artifactId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasManagedPluginVersion(List<Plugin> plugins, String groupId, String artifactId) {
        for (Plugin p : plugins) {
            if (p.getGroupId().equals(groupId) && p.getArtifactId().equals(artifactId) && p.getVersion() != null) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable String getManagedPluginVersion(ResolvedPom resolvedPom, String groupId, String artifactId) {
        for (Plugin p : ListUtils.concatAll(resolvedPom.getPluginManagement(),
                resolvedPom.getRequested().getPluginManagement())) {
            if (p.getGroupId().equals(groupId) &&
                    p.getArtifactId().equals(artifactId) &&
                    p.getVersion() != null) {
                return p.getVersion();
            }
        }
        return null;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class ChangePluginVersionVisitor extends MavenVisitor<ExecutionContext> {
        String groupId;
        String artifactId;
        String newVersion;
        boolean addVersionIfMissing;
        boolean overrideInheritedVersion;
        UUID tagId;

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (tag.getId().equals(tagId) && isPluginTag(groupId, artifactId)) {
                Optional<Xml.Tag> versionTag = tag.getChild("version");
                if (versionTag.isPresent()) {
                    String version = versionTag.get().getValue().orElse(null);
                    if (version != null) {
                        // Only a whole property reference can be updated without changing other components of an expression.
                        if (version.trim().matches("\\$\\{[^}]+}")) {
                            if (!newVersion.equals(getResolutionResult().getPom().getValue(version.trim()))) {
                                doAfterVisit(new ChangePropertyValue(version.trim(), newVersion, false, false).getVisitor());
                            }
                        } else if (!newVersion.equals(version)) {
                            doAfterVisit(new ChangeTagValueVisitor<>(versionTag.get(), newVersion));
                        }
                    }
                } else if (addVersionIfMissing && (overrideInheritedVersion ||
                        !hasManagedPluginVersion(getResolutionResult().getPom(), groupId, artifactId))) {
                    Xml.Tag newTag = Xml.Tag.build("<version>" + newVersion + "</version>");
                    doAfterVisit(new AddToTagVisitor<>(tag, newTag, new MavenTagInsertionComparator(tag.getChildren())));
                }
            }
            return super.visitTag(tag, ctx);
        }
    }
}
