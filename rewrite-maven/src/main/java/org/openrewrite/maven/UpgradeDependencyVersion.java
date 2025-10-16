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
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.trait.MavenDependency;
import org.openrewrite.maven.tree.*;
import org.openrewrite.maven.utilities.RetainVersions;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.StringUtils.matchesGlob;

/**
 * Upgrade the version of a dependency by specifying a group or group and artifact using Node Semver
 * <a href="https://github.com/npm/node-semver#advanced-range-syntax">advanced range selectors</a>, allowing
 * more precise control over version updates to patch or minor releases.
 * <P><P>
 * NOTES:
 * <li>If a version is defined as a property, this recipe will only change the property value if the property exists within the same pom.</li>
 * <li>This recipe will alter the managed version of the dependency if it exists in the pom.</li>
 * <li>The default behavior for managed dependencies is to leave them unaltered unless the "overrideManagedVersion" is set to true.</li>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeDependencyVersion extends ScanningRecipe<UpgradeDependencyVersion.Accumulator> {
    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
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
                          "Setting 'newVersion' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Override managed version",
            description = "This flag can be set to explicitly override a managed dependency's version. " +
                          "If the dependency has its version managed by a Bill of Materials (BOM), enabling this flag will attempt to upgrade the BOM. " +
                          "The default for this flag is `false`.",
            required = false)
    @Nullable
    Boolean overrideManagedVersion;

    @Option(displayName = "Retain versions",
            description = "Accepts a list of GAVs. For each GAV, if it is a project direct dependency, and it is removed " +
                          "from dependency management after the changes from this recipe, then it will be retained with an explicit version. " +
                          "The version can be omitted from the GAV to use the old value from dependency management",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> retainVersions;

    @SuppressWarnings("ConstantConditions")
    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
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
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, newVersion);
    }

    @Override
    public String getDescription() {
        return "Upgrade the version of a dependency by specifying a group and (optionally) an artifact using Node Semver " +
               "advanced range selectors, allowing more precise control over version updates to patch or minor releases.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator accumulator) {
        return new MavenIsoVisitor<ExecutionContext>() {
            private final VersionComparator versionComparator =
                    requireNonNull(Semver.validate(newVersion, versionPattern).getValue());

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                ResolvedPom pom = getResolutionResult().getPom();
                accumulator.projectArtifacts.add(new GroupArtifact(pom.getGroupId(), pom.getArtifactId()));
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(final Xml.Tag tag, final ExecutionContext ctx) {
                if (isDependencyTag(groupId, artifactId)) {
                    ResolvedDependency d = findDependency(tag);
                    if (d != null && d.getRepository() != null) {
                        // if the resolved dependency exists AND it does not represent an artifact that was parsed
                        // as a source file, attempt to find a new version.
                        try {
                            String newerVersion = MavenDependency.findNewerVersion(d.getGroupId(), d.getArtifactId(), d.getVersion(), getResolutionResult(), metadataFailures,
                                    versionComparator, ctx);
                            if (newerVersion != null) {
                                Optional<Xml.Tag> version = tag.getChild("version");
                                if (version.isPresent()) {
                                    String requestedVersion = d.getRequested().getVersion();
                                    if (isProperty(requestedVersion)) {
                                        String propertyName = requestedVersion.substring(2, requestedVersion.length() - 1);
                                        if (!getResolutionResult().getPom().getRequested().getProperties().containsKey(propertyName)) {
                                            storeParentPomProperty(getResolutionResult().getParent(), propertyName, newerVersion);
                                        }
                                    }
                                }
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                }
                return super.visitTag(tag, ctx);
            }

            /**
             * Recursively look for a parent POM that's still part of the sources, which contains the version property.
             * If found, store the property in the accumulator, such that we can update that source file later.
             * @param currentMavenResolutionResult the current Maven resolution result parent to search for the property
             * @param propertyName the name of the property to update, if found in any the parent pom source file
             * @param newerVersion the resolved newer version that any matching parent pom property should be updated to
             */
            private void storeParentPomProperty(@Nullable MavenResolutionResult currentMavenResolutionResult, String propertyName, String newerVersion) {
                if (currentMavenResolutionResult == null) {
                    return; // No parent contained the property; might then be in the same source file, or an import BOM
                }
                Pom pom = currentMavenResolutionResult.getPom().getRequested();
                if (pom.getSourcePath() == null) {
                    return; // Not a source file, so nothing to update
                }
                if (pom.getProperties().containsKey(propertyName)) {
                    accumulator.pomProperties.add(new PomProperty(pom.getSourcePath(), propertyName, newerVersion));
                    return; // Property found, so no further searching is needed
                }
                storeParentPomProperty(currentMavenResolutionResult.getParent(), propertyName, newerVersion);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator accumulator) {
        return new MavenIsoVisitor<ExecutionContext>() {
            private final VersionComparator versionComparator = requireNonNull(Semver.validate(newVersion, versionPattern).getValue());

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                // Check if we should attempt a BOM upgrade before processing individual dependencies
                if (Boolean.TRUE.equals(overrideManagedVersion)) {
                    MavenResolutionResult mrr = getResolutionResult();
                    for (ResolvedDependency dep : mrr.findDependencies(groupId, artifactId, null)) {
                        ResolvedManagedDependency managedDep = getResolutionResult().getPom().getManagedDependency(dep.getGroupId(), dep.getArtifactId(), dep.getType(), dep.getClassifier());
                        // If the dependency's version is managed by a BOM, first attempt to upgrade the BOM to maintain its alignment
                        if (managedDep != null && managedDep.getBomGav() != null) {
                            try {
                                String targetVersion = findNewerVersion(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), ctx);
                                if (targetVersion != null) {
                                    Xml.Document result = attemptBomUpgrade(document, managedDep, targetVersion, ctx);
                                    if (result != document) {
                                        return result;
                                    }
                                }
                            } catch (MavenDownloadingException e) {
                                document = e.warn(document);
                            }
                        }
                    }
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                try {
                    if (isPropertyTag()) {
                        Path pomSourcePath = getResolutionResult().getPom().getRequested().getSourcePath();
                        for (PomProperty pomProperty : accumulator.pomProperties) {
                            if (pomProperty.pomFilePath.equals(pomSourcePath) &&
                                pomProperty.propertyName.equals(tag.getName())) {
                                Optional<String> value = tag.getValue();
                                if (!value.isPresent() || !value.get().equals(pomProperty.propertyValue)) {
                                    doAfterVisit(new ChangeTagValueVisitor<>(tag, pomProperty.propertyValue));
                                    maybeUpdateModel();
                                }
                                break;
                            }
                        }
                    } else if (isDependencyTag(groupId, artifactId)) {
                        t = upgradeDependency(ctx, t);
                    } else if (isManagedDependencyTag(groupId, artifactId)) {
                        if (isManagedDependencyImportTag(groupId, artifactId)) {
                            doAfterVisit(new UpgradeDependencyManagementImportVisitor());
                        } else {
                            TreeVisitor<Xml, ExecutionContext> upgradeManagedDependency = upgradeManagedDependency(tag, ctx, t);
                            if (upgradeManagedDependency != null) {
                                doAfterVisit(upgradeManagedDependency);
                                maybeUpdateModel();
                            }
                        }
                    } else if (isPluginDependencyTag(groupId, artifactId) || isAnnotationProcessorPathTag(groupId, artifactId)) {
                        t = upgradeTag(ctx, t);
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(t);
                }

                if (t != tag && isProjectTag()) {
                    maybeUpdateModel();
                    doAfterVisit(new RemoveRedundantDependencyVersions(groupId, artifactId, null, null).getVisitor());
                }

                return t;
            }

            class UpgradeDependencyManagementImportVisitor extends MavenIsoVisitor<ExecutionContext> {
                @Override
                public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                    Xml.Tag t = super.visitTag(tag, ctx);
                    try {
                        if (isManagedDependencyImportTag(groupId, artifactId)) {
                            TreeVisitor<Xml, ExecutionContext> upgradeManagedDependency = upgradeManagedDependency(tag, ctx, t);
                            if (upgradeManagedDependency != null) {
                                retainVersions();
                                doAfterVisit(new RemoveRedundantDependencyVersions(null, null, null, retainVersions).getVisitor());
                                doAfterVisit(upgradeManagedDependency);
                                maybeUpdateModel();
                                doAfterVisit(new RemoveRedundantDependencyVersions(null, null, null, null).getVisitor());
                            }
                        }
                    } catch (MavenDownloadingException e) {
                        return e.warn(t);
                    }
                    return t;
                }

                private void retainVersions() {
                    RetainVersions.plan(this, retainVersions == null ? emptyList() : retainVersions)
                            .forEach(it -> doAfterVisit(it.getVisitor()));
                }
            }

            /**
             * Check if a dependency is managed by a local parent POM (in the same repository).
             * This helps avoid adding unnecessary explicit versions in child POMs when the version
             * is already managed by a parent POM in the same multi-module project.
             */
            private boolean isManagedByLocalParent(String groupId, String artifactId) {
                MavenResolutionResult current = getResolutionResult().getParent();
                while (current != null) {
                    ResolvedPom parentPom = current.getPom();
                    if (accumulator.projectArtifacts.contains(new GroupArtifact(parentPom.getGroupId(), parentPom.getArtifactId()))) {
                        for (ResolvedManagedDependency md : parentPom.getDependencyManagement()) {
                            if (groupId.equals(md.getGroupId()) && artifactId.equals(md.getArtifactId())) {
                                return true;
                            }
                        }
                    }
                    current = current.getParent();
                }
                return false;
            }

            private Xml.Tag upgradeDependency(ExecutionContext ctx, Xml.Tag t) throws MavenDownloadingException {
                ResolvedDependency d = findDependency(t);
                if (d != null && d.getRepository() != null) {
                    // if the resolved dependency exists AND it does not represent an artifact that was parsed
                    // as a source file, attempt to find a new version.
                    String newerVersion = findNewerVersion(d.getGroupId(), d.getArtifactId(), d.getVersion(), ctx);
                    if (newerVersion != null) {
                        if (t.getChild("version").isPresent()) {
                            t = changeChildTagValue(t, "version", newerVersion, overrideManagedVersion, ctx);
                        } else if (Boolean.TRUE.equals(overrideManagedVersion)) {
                            ResolvedManagedDependency dm = findManagedDependency(t);

                            // if a managed dependency is expressed as a property, change the property value
                            // do this only when a requested bom is absent, otherwise changing property has no effect
                            if (dm != null && isProperty(dm.getRequested().getVersion()) && dm.getRequestedBom() == null) {
                                doAfterVisit(new ChangePropertyValue(dm.getRequested().getVersion().substring(2,
                                        dm.getRequested().getVersion().length() - 1),
                                        newerVersion, overrideManagedVersion, false).getVisitor());
                            } else if (dm != null && dm.getBomGav() == null) {
                                // if the version is managed directly (not from a BOM) and comes from a local parent POM
                                // (in the same repository), don't add an explicit version
                                boolean isManagedByLocalParent = isManagedByLocalParent(d.getGroupId(), d.getArtifactId());
                                if (!isManagedByLocalParent) {
                                    Xml.Tag versionTag = Xml.Tag.build("<version>" + newerVersion + "</version>");
                                    //noinspection ConstantConditions
                                    t = (Xml.Tag) new AddToTagVisitor<>(t, versionTag, new MavenTagInsertionComparator(t.getChildren()))
                                            .visitNonNull(t, 0, getCursor().getParent());
                                }
                            } else {
                                // if the version is not present and the override managed version is set,
                                // add a new explicit version tag
                                Xml.Tag versionTag = Xml.Tag.build("<version>" + newerVersion + "</version>");

                                //noinspection ConstantConditions
                                t = (Xml.Tag) new AddToTagVisitor<>(t, versionTag, new MavenTagInsertionComparator(t.getChildren()))
                                        .visitNonNull(t, 0, getCursor().getParent());
                            }
                        }
                    }
                }
                return t;
            }

            private @Nullable TreeVisitor<Xml, ExecutionContext> upgradeManagedDependency(Xml.Tag tag, ExecutionContext ctx, Xml.Tag t) throws MavenDownloadingException {
                ResolvedManagedDependency managedDependency = findManagedDependency(t);
                if (managedDependency != null) {
                    String groupId = managedDependency.getGroupId();
                    String artifactId = managedDependency.getArtifactId();
                    String version = managedDependency.getVersion();
                    if (version != null &&
                        !accumulator.projectArtifacts.contains(new GroupArtifact(groupId, artifactId)) &&
                        matchesGlob(groupId, UpgradeDependencyVersion.this.groupId) &&
                        matchesGlob(artifactId, UpgradeDependencyVersion.this.artifactId)) {
                        return upgradeVersion(ctx, t, managedDependency.getRequested().getVersion(), groupId, artifactId, version);
                    }
                } else {
                    for (ResolvedManagedDependency dm : getResolutionResult().getPom().getDependencyManagement()) {
                        if (dm.getBomGav() != null) {
                            String group = ofNullable(getResolutionResult().getPom().getValue(tag.getChildValue("groupId").orElse(getResolutionResult().getPom().getGroupId()))).orElse("");
                            String artifactId = ofNullable(getResolutionResult().getPom().getValue(tag.getChildValue("artifactId").orElse(""))).orElse("");
                            if (!accumulator.projectArtifacts.contains(new GroupArtifact(group, artifactId))) {
                                ResolvedGroupArtifactVersion bom = dm.getBomGav();
                                if (Objects.equals(group, bom.getGroupId()) &&
                                    Objects.equals(artifactId, bom.getArtifactId())) {
                                    return upgradeVersion(ctx, t, requireNonNull(dm.getRequestedBom()).getVersion(), bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
                                }
                            }
                        }
                    }
                }
                return null;
            }

            private Xml.Tag upgradeTag(ExecutionContext ctx, Xml.Tag t) throws MavenDownloadingException {
                String groupId = t.getChildValue("groupId").orElse(null);
                String artifactId = t.getChildValue("artifactId").orElse(null);
                String version = t.getChildValue("version").orElse(null);
                if (groupId != null && artifactId != null && version != null) {
                    String newerVersion = findNewerVersion(groupId, artifactId, resolveVersion(version), ctx);
                    if (newerVersion != null) {
                        t = changeChildTagValue(t, "version", newerVersion, overrideManagedVersion, ctx);
                    }
                }
                return t;
            }

            private String resolveVersion(String version) {
                if (isProperty(version)) {
                    Map<String, String> properties = getResolutionResult().getPom().getProperties();
                    String property = version.substring(2, version.length() - 1);
                    return properties.getOrDefault(property, version);
                }
                return version;
            }

            public @Nullable TreeVisitor<Xml, ExecutionContext> upgradeVersion(ExecutionContext ctx, Xml.Tag tag, @Nullable String requestedVersion, String groupId, String artifactId, String version2) throws MavenDownloadingException {
                String newerVersion = findNewerVersion(groupId, artifactId, version2, ctx);
                if (newerVersion == null) {
                    return null;
                } else if (isProperty(requestedVersion)) {
                    //noinspection unchecked
                    return (TreeVisitor<Xml, ExecutionContext>) new ChangePropertyValue(requestedVersion.substring(2, requestedVersion.length() - 1), newerVersion, overrideManagedVersion, false)
                            .getVisitor();
                } else {
                    Xml.Tag childVersionTag = tag.getChild("version").orElse(null);
                    if (childVersionTag != null) {
                        return new ChangeTagValueVisitor<>(childVersionTag, newerVersion);
                    }
                }
                return null;
            }

            private @Nullable String findNewerVersion(String groupId, String artifactId, String version, ExecutionContext ctx)
                    throws MavenDownloadingException {
                return MavenDependency.findNewerVersion(groupId, artifactId, version, getResolutionResult(), metadataFailures,
                        versionComparator, ctx);
            }

            private boolean isAnnotationProcessorPathTag(String groupId, String artifactId) {
                // Runtime might still have a private version of this class parent-loaded -> remove this once we have had a few releases.
//                if (!isTag("path") || !ANNOTATION_PROCESSORS_PATH_MATCHER.matches(getCursor())) {
                if (!(getCursor().getValue() instanceof Xml.Tag && "path".equals(getCursor().<Xml.Tag>getValue().getName())) || !new XPathMatcher("//annotationProcessorPaths/path").matches(getCursor())) {
                    return false;
                }
                Xml.Tag tag = getCursor().getValue();
                return matchesGlob(tag.getChildValue("groupId").orElse(null), groupId) &&
                        matchesGlob(tag.getChildValue("artifactId").orElse(null), artifactId);
            }

            private Xml.Document attemptBomUpgrade(Xml.Document document, ResolvedManagedDependency managedDep,
                                                   String targetVersion, ExecutionContext ctx) throws MavenDownloadingException {
                ResolvedGroupArtifactVersion bomGav = managedDep.getBomGav();
                if (bomGav == null) {
                    return document;
                }

                String newerBomVersion = findNewerBomVersionWithDependency(
                        bomGav.getGroupId(),
                        bomGav.getArtifactId(),
                        bomGav.getVersion(),
                        managedDep.getGroupId(),
                        managedDep.getArtifactId(),
                        targetVersion,
                        ctx
                );

                if (newerBomVersion == null) {
                    return document;
                }

                return (Xml.Document) new UpgradeDependencyVersion(bomGav.getGroupId(), bomGav.getArtifactId(), newerBomVersion, null, true, retainVersions)
                        .getVisitor(accumulator)
                        .visitNonNull(document, ctx, getCursor().getParentTreeCursor());
            }

            /**
             * Finds a newer version of a BOM that manages a specific dependency at a target version.
             */
            private @Nullable String findNewerBomVersionWithDependency(
                    String bomGroupId,
                    String bomArtifactId,
                    String currentBomVersion,
                    String dependencyGroupId,
                    String dependencyArtifactId,
                    String targetDependencyVersion,
                    ExecutionContext ctx) throws MavenDownloadingException {

                List<String> bomVersions = getAvailableBomVersions(bomGroupId, bomArtifactId, currentBomVersion, ctx);

                for (String bomVersion : bomVersions) {
                    String managedVersion = getDependencyVersionFromBom(
                            bomGroupId, bomArtifactId, bomVersion,
                            dependencyGroupId, dependencyArtifactId, ctx
                    );

                    if (targetDependencyVersion.equals(managedVersion)) {
                        return bomVersion;
                    }
                }

                return null;
            }

            private List<String> getAvailableBomVersions(String groupId, String artifactId, String currentVersion, ExecutionContext ctx)
                    throws MavenDownloadingException {
                //noinspection SpellCheckingInspection
                MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
                MavenSettings settings = mctx.effectiveSettings(getResolutionResult());
                MavenPomDownloader downloader = new MavenPomDownloader(
                        emptyMap(), ctx, settings,
                        ofNullable(settings)
                                .map(MavenSettings::getActiveProfiles)
                                .map(MavenSettings.ActiveProfiles::getActiveProfiles)
                                .orElse(null)
                );

                MavenMetadata metadata = downloader.downloadMetadata(
                        new GroupArtifact(groupId, artifactId), null,
                        getResolutionResult().getPom().getRepositories()
                );

                return metadata.getVersioning().getVersions().stream()
                        .filter(version -> versionComparator.compare(null, currentVersion, version) < 0)
                        .sorted(versionComparator)
                        .collect(toList());
            }

            private @Nullable String getDependencyVersionFromBom(
                    String bomGroupId,
                    String bomArtifactId,
                    String bomVersion,
                    String dependencyGroupId,
                    String dependencyArtifactId,
                    ExecutionContext ctx) throws MavenDownloadingException {
                //noinspection SpellCheckingInspection
                MavenExecutionContextView mctx = MavenExecutionContextView.view(ctx);
                MavenSettings settings = mctx.effectiveSettings(getResolutionResult());
                MavenPomDownloader downloader = new MavenPomDownloader(
                        emptyMap(), mctx, settings,
                        ofNullable(settings)
                                .map(MavenSettings::getActiveProfiles)
                                .map(MavenSettings.ActiveProfiles::getActiveProfiles)
                                .orElse(null)
                );

                Pom bom = downloader.download(
                        new GroupArtifactVersion(bomGroupId, bomArtifactId, bomVersion),
                        null, null, getResolutionResult().getPom().getRepositories()
                );
                ResolvedPom resolvedBom = bom.resolve(mctx.getActiveProfiles(), downloader, mctx);
                for (ResolvedManagedDependency md : resolvedBom.getDependencyManagement()) {
                    if (dependencyGroupId.equals(md.getGroupId()) &&
                        dependencyArtifactId.equals(md.getArtifactId())) {
                        return md.getVersion();
                    }
                }

                return null;
            }
        };
    }

    @Value
    public static class Accumulator {
        Set<GroupArtifact> projectArtifacts = new HashSet<>();
        Set<PomProperty> pomProperties = new HashSet<>();
    }

    @Value
    public static class PomProperty {
        Path pomFilePath;
        String propertyName;
        String propertyValue;
    }
}
