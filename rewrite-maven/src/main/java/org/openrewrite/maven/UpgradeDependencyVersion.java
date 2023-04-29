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
import org.openrewrite.maven.tree.*;
import org.openrewrite.maven.utilities.RetainVersions;
import org.openrewrite.semver.LatestPatch;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
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
@EqualsAndHashCode(callSuper = true)
public class UpgradeDependencyVersion extends ScanningRecipe<Set<GroupArtifact>> {
    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    // there are several implicitly defined version properties that we should never attempt to update
    private static final Collection<String> implicitlyDefinedVersionProperties = Arrays.asList(
            "${version}", "${project.version}", "${pom.version}", "${project.parent.version}"
    );

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
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

    @Option(displayName = "Override managed version",
            description = "This flag can be set to explicitly override a managed dependency's version. The default for this flag is `false`.",
            required = false)
    @Nullable
    Boolean overrideManagedVersion;

    @Option(displayName = "Retain versions",
            description = "Accepts a list of GAVs. For each GAV, if it is a project direct dependency, and it is removed "
                          + "from dependency management after the changes from this recipe, then it will be retained with an explicit version. "
                          + "The version can be omitted from the GAV to use the old value from dependency management",
            example = "com.jcraft:jsch",
            required = false)
    @Nullable
    List<String> retainVersions;

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
        return "Upgrade the version of a dependency by specifying a group and (optionally) an artifact using Node Semver " +
               "advanced range selectors, allowing more precise control over version updates to patch or minor releases.";
    }

    @Override
    public Set<GroupArtifact> getInitialValue() {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<GroupArtifact> projectArtifacts) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
                ResolvedPom pom = getResolutionResult().getPom();
                projectArtifacts.add(new GroupArtifact(pom.getGroupId(), pom.getArtifactId()));
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<GroupArtifact> projectArtifacts) {
        VersionComparator versionComparator = Semver.validate(newVersion, versionPattern).getValue();
        assert versionComparator != null;

        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                try {
                    if (isDependencyTag(groupId, artifactId)) {
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
                    }
                } catch (MavenDownloadingException e) {
                    return e.warn(t);
                }

                if (t != tag && PROJECT_MATCHER.matches(getCursor())) {
                    maybeUpdateModel();
                    doAfterVisit(new RemoveRedundantDependencyVersions(groupId, artifactId, true, null));
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
                                doAfterVisit(new RemoveRedundantDependencyVersions(null, null, true, retainVersions));
                                doAfterVisit(upgradeManagedDependency);
                                maybeUpdateModel();
                                doAfterVisit(new RemoveRedundantDependencyVersions(null, null, true, null));
                            }
                        }
                    } catch (MavenDownloadingException e) {
                        return e.warn(t);
                    }
                    return t;
                }

                private void retainVersions() {
                    for (Recipe retainVersionRecipe : RetainVersions.plan(this, retainVersions == null ?
                            emptyList() : retainVersions)) {
                        doAfterVisit(retainVersionRecipe);
                    }
                }
            }

            private Xml.Tag upgradeDependency(ExecutionContext ctx, Xml.Tag t) throws MavenDownloadingException {
                ResolvedDependency d = findDependency(t);
                if (d != null && d.getRepository() != null) {
                    // if the resolved dependency exists AND it does not represent an artifact that was parsed
                    // as a source file, attempt to find a new version.
                    String newerVersion = findNewerVersion(d.getGroupId(), d.getArtifactId(), d.getVersion(), ctx);
                    if (newerVersion != null) {
                        Optional<Xml.Tag> version = t.getChild("version");
                        if (version.isPresent()) {
                            String requestedVersion = d.getRequested().getVersion();
                            if (requestedVersion != null && requestedVersion.startsWith("${") && !implicitlyDefinedVersionProperties.contains(requestedVersion)) {
                                doAfterVisit(new ChangePropertyValue(requestedVersion.substring(2, requestedVersion.length() - 1), newerVersion, overrideManagedVersion, false));
                            } else {
                                t = (Xml.Tag) new ChangeTagValueVisitor<>(version.get(), newerVersion).visitNonNull(t, 0, getCursor().getParentOrThrow());
                            }
                        } else if (Boolean.TRUE.equals(overrideManagedVersion)) {
                            ResolvedManagedDependency dm = findManagedDependency(t);
                            // if a managed dependency is expressed as a property, change the property value
                            if (dm != null &&
                                dm.getRequested().getVersion() != null &&
                                dm.getRequested().getVersion().startsWith("${") &&
                                !implicitlyDefinedVersionProperties.contains(dm.getRequested().getVersion())) {
                                doAfterVisit(new ChangePropertyValue(dm.getRequested().getVersion().substring(2,
                                        dm.getRequested().getVersion().length() - 1),
                                        newerVersion, overrideManagedVersion, false));
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

            @Nullable
            private TreeVisitor<Xml, ExecutionContext> upgradeManagedDependency(Xml.Tag tag, ExecutionContext ctx, Xml.Tag t) throws MavenDownloadingException {
                ResolvedManagedDependency managedDependency = findManagedDependency(t);
                if (managedDependency != null) {
                    String groupId = managedDependency.getGroupId();
                    String artifactId = managedDependency.getArtifactId();
                    String version = managedDependency.getVersion();
                    if (version != null &&
                        !projectArtifacts.contains(new GroupArtifact(groupId, artifactId)) &&
                        matchesGlob(groupId, UpgradeDependencyVersion.this.groupId) &&
                        matchesGlob(artifactId, UpgradeDependencyVersion.this.artifactId)) {
                        return upgradeVersion(ctx, t, managedDependency.getRequested().getVersion(), groupId, artifactId, version);
                    }
                } else {
                    for (ResolvedManagedDependency dm : getResolutionResult().getPom().getDependencyManagement()) {
                        if (dm.getBomGav() != null) {
                            String group = getResolutionResult().getPom().getValue(tag.getChildValue("groupId").orElse(getResolutionResult().getPom().getGroupId()));
                            String artifactId = getResolutionResult().getPom().getValue(tag.getChildValue("artifactId").orElse(""));
                            if (!projectArtifacts.contains(new GroupArtifact(group, artifactId))) {
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

            @Nullable
            public TreeVisitor<Xml, ExecutionContext> upgradeVersion(ExecutionContext ctx, Xml.Tag tag, @Nullable String requestedVersion, String groupId, String artifactId, String version2) throws MavenDownloadingException {
                String newerVersion = findNewerVersion(groupId, artifactId, version2, ctx);
                if (newerVersion == null) {
                    return null;
                } else if (requestedVersion != null && requestedVersion.startsWith("${")) {
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

            @Nullable
            private String findNewerVersion(String groupId, String artifactId, String version, ExecutionContext ctx) throws MavenDownloadingException {
                String finalVersion = !Semver.isVersion(version) ? "0.0.0" : version;

                // in the case of "latest.patch", a new version can only be derived if the
                // current version is a semantic version
                if (versionComparator instanceof LatestPatch && !versionComparator.isValid(finalVersion, finalVersion)) {
                    return null;
                }

                try {
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    List<String> versions = new ArrayList<>();
                    for (String v : mavenMetadata.getVersioning().getVersions()) {
                        if (versionComparator.isValid(finalVersion, v)) {
                            versions.add(v);
                        }
                    }
                    // handle upgrades from non semver versions like "org.springframework.cloud:spring-cloud-dependencies:Camden.SR5"
                    if (!Semver.isVersion(finalVersion) && !versions.isEmpty()) {
                        versions.sort(versionComparator);
                        return versions.get(versions.size() - 1);
                    }
                    return versionComparator.upgrade(finalVersion, versions).orElse(null);
                } catch (IllegalStateException e) {
                    // this can happen when we encounter exotic versions
                    return null;
                }
            }
        };
    }

    private static final XPathMatcher PROJECT_MATCHER = new XPathMatcher("/project");
    private static final String DEPENDENCIES_XPATH = "/project/dependencies";
    private static final String DEPENDENCY_MANAGEMENT_XPATH = "/project/dependencyManagement";
}
