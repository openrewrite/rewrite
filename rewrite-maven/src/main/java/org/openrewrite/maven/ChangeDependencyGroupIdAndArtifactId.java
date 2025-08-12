/*
 * Copyright 2021 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.max;
import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.isBlank;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyGroupIdAndArtifactId extends ScanningRecipe<ChangeDependencyGroupIdAndArtifactId.Accumulator> {
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "rewrite-testing-frameworks")
    String oldArtifactId;

    @Option(displayName = "New groupId",
            description = "The new groupId to use. Defaults to the existing group id.",
            example = "corp.internal.openrewrite.recipe",
            required = false)
    @Nullable
    String newGroupId;

    @Option(displayName = "New artifactId",
            description = "The new artifactId to use. Defaults to the existing artifact id.",
            example = "rewrite-testing-frameworks",
            required = false)
    @Nullable
    String newArtifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Override managed version",
            description = "If the new dependency has a managed version, this flag can be used to explicitly set the version on the dependency. The default for this flag is `false`.",
            required = false)
    @Nullable
    Boolean overrideManagedVersion;

    @Option(displayName = "Update dependency management",
            description = "Also update the dependency management section. The default for this flag is `true`.",
            required = false)
    @Nullable
    Boolean changeManagedDependency;

    public ChangeDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern) {
        this(oldGroupId, oldArtifactId, newGroupId, newArtifactId, newVersion, versionPattern, false, true);
    }

    @JsonCreator
    public ChangeDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern, @Nullable Boolean overrideManagedVersion, @Nullable Boolean changeManagedDependency) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
        this.overrideManagedVersion = overrideManagedVersion;
        this.changeManagedDependency = changeManagedDependency;
    }

    @Override
    public String getDisplayName() {
        return "Change Maven dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", oldGroupId, oldArtifactId);
    }

    @Override
    public String getDescription() {
        return "Change a Maven dependency coordinates. The `newGroupId` or `newArtifactId` **MUST** be different from before. " +
               "Matching `<dependencyManagement>` coordinates are also updated if a `newVersion` or `versionPattern` is provided.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        validated = validated.and(required("newGroupId", newGroupId).or(required("newArtifactId", newArtifactId)));
        return validated.and(test(
                "coordinates",
                "newGroupId OR newArtifactId must be different from before",
                this,
                r -> {
                    boolean sameGroupId = isBlank(r.newGroupId) || Objects.equals(r.oldGroupId, r.newGroupId);
                    boolean sameArtifactId = isBlank(r.newArtifactId) || Objects.equals(r.oldArtifactId, r.newArtifactId);
                    return !(sameGroupId && sameArtifactId);
                }
        ));
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new MavenVisitor<ExecutionContext>() {
            final @Nullable VersionComparator versionComparator = newVersion != null ? Semver.validate(newVersion, versionPattern).getValue() : null;
            private boolean isNewDependencyPresent;

            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                isNewDependencyPresent = checkIfNewDependencyPresents(newGroupId, newArtifactId, newVersion);
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                boolean isOldDependencyTag = isDependencyTag(oldGroupId, oldArtifactId);
                if (isOldDependencyTag && isNewDependencyPresent) {
                    acc.isNewDependencyPresent = true;
                    return t;
                }
                if (isOldDependencyTag || isPluginDependencyTag(oldGroupId, oldArtifactId)) {
                    Optional<String> groupIdFromTag = t.getChildValue("groupId");
                    Optional<String> artifactIdFromTag = t.getChildValue("artifactId");
                    if (!groupIdFromTag.isPresent() || !artifactIdFromTag.isPresent()) {
                        return t;
                    }

                    String groupId = groupIdFromTag.get();
                    if (newGroupId != null) {
                        storeParentPomProperty(groupId, newGroupId);
                        groupId = newGroupId;
                        acc.changeGroupId = groupId;
                    }

                    String artifactId = artifactIdFromTag.get();
                    if (newArtifactId != null) {
                        storeParentPomProperty(artifactId, newArtifactId);
                        artifactId = newArtifactId;
                        acc.changeArtifactId = artifactId;
                    }

                    if (newVersion != null) {
                        try {
                            Optional<String> currentVersion = t.getChildValue("version");
                            String resolvedNewVersion = resolveSemverVersion(groupId, artifactId, currentVersion.orElse(newVersion), ctx);
                            Scope scope = t.getChild("scope").map(xml -> Scope.fromName(xml.getValue().orElse(null))).orElse(Scope.Compile);
                            Optional<Xml.Tag> versionTag = t.getChild("version");

                            boolean configuredToOverrideManageVersion = overrideManagedVersion != null && overrideManagedVersion; // False by default
                            boolean configuredToChangeManagedDependency = changeManagedDependency == null || changeManagedDependency; // True by default

                            boolean oldDependencyManaged = isDependencyManaged(scope, oldGroupId, oldArtifactId);
                            boolean newDependencyManaged = isDependencyManaged(scope, groupId, artifactId);
                            if (versionTag.isPresent()) {
                                // If the previous dependency had a version but the new artifact is managed, removed the version tag.
                                if (!configuredToOverrideManageVersion && newDependencyManaged || (oldDependencyManaged && configuredToChangeManagedDependency)) {
                                    acc.removeVersionTag = true;
                                } else {
                                    // Otherwise, change the version to the new value.
                                    storeParentPomProperty(currentVersion.orElse(null), resolvedNewVersion);
                                    acc.changeVersion = resolvedNewVersion;
                                }
                            } else if (configuredToOverrideManageVersion || !newDependencyManaged) {
                                // If the version is not present, add the version if we are explicitly overriding a managed version or if no managed version exists.
                                acc.createVersion = resolvedNewVersion;
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }
                }

                //noinspection ConstantConditions
                return t;
            }

            private boolean checkIfNewDependencyPresents(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
                if ((groupId == null) || (artifactId == null)) {
                    return false;
                }
                List<ResolvedDependency> dependencies = findDependencies(groupId, artifactId);
                return dependencies.stream()
                        .filter(ResolvedDependency::isDirect)
                        .anyMatch(rd -> (version == null) || version.equals(rd.getVersion()));
            }

            private boolean isDependencyManaged(Scope scope, String groupId, String artifactId) {
                MavenResolutionResult result = getResolutionResult();
                for (ResolvedManagedDependency managedDependency : result.getPom().getDependencyManagement()) {
                    if (groupId.equals(managedDependency.getGroupId()) && artifactId.equals(managedDependency.getArtifactId())) {
                        return scope.isInClasspathOf(managedDependency.getScope());
                    }
                }
                return false;
            }

            @SuppressWarnings("ConstantConditions")
            private String resolveSemverVersion(String groupId, String artifactId, String version, ExecutionContext ctx) throws MavenDownloadingException {
                List<String> availableVersions = new ArrayList<>();
                MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                for (String v : mavenMetadata.getVersioning().getVersions()) {
                    if (versionComparator.isValid(version, v)) {
                        availableVersions.add(v);
                    }
                }
                return availableVersions.isEmpty() ? newVersion : max(availableVersions, versionComparator);
            }

            private void storeParentPomProperty(@Nullable String currentValue, String newValue) {
                if (isProperty(currentValue)) {
                    String name = currentValue.substring(2, currentValue.length() - 1).trim();
                    if (!getResolutionResult().getPom().getRequested().getProperties().containsKey(name)) {
                        storeParentPomProperty(getResolutionResult(), name, newValue);
                    }
                }
            }

            private void storeParentPomProperty(MavenResolutionResult resolutionResult, String name, String value) {
                Pom pom = resolutionResult.getPom().getRequested();
                if (pom.getSourcePath() != null && pom.getProperties().containsKey(name)) {
                    acc.pomProperties.add(new PomProperty(pom.getSourcePath(), name, value));
                } else if (resolutionResult.getParent() != null) {
                    storeParentPomProperty(resolutionResult.getParent(), name, value);
                }
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                // Any managed dependency change is unlikely to use the same version, so only update selectively.
                if ((changeManagedDependency == null || changeManagedDependency) && newVersion != null || versionPattern != null) {
                    doAfterVisit(new ChangeManagedDependencyGroupIdAndArtifactId(
                            oldGroupId, oldArtifactId,
                            Optional.ofNullable(newGroupId).orElse(oldGroupId),
                            Optional.ofNullable(newArtifactId).orElse(oldArtifactId),
                            newVersion, versionPattern).getVisitor());
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (isPropertyTag()) {
                    Path sourcePath = getResolutionResult().getPom().getRequested().getSourcePath();
                    for (PomProperty prop : acc.pomProperties) {
                        if (prop.filePath.equals(sourcePath)) {
                            doAfterVisit(new ChangePropertyValue(prop.name, prop.value, false, false).getVisitor());
                        }
                    }
                    return t;
                }

                boolean isOldDependencyTag = isDependencyTag(oldGroupId, oldArtifactId);
                if (isOldDependencyTag && acc.isNewDependencyPresent) {
                    doAfterVisit(new RemoveContentVisitor<>(t, true, true));
                    maybeUpdateModel();
                    return t;
                }
                if (isOldDependencyTag || isPluginDependencyTag(oldGroupId, oldArtifactId)) {
                    if (acc.changeGroupId != null) {
                        t = changeChildTagValue(t, "groupId", newGroupId, ctx);
                    }
                    if (acc.changeArtifactId != null) {
                        t = changeChildTagValue(t, "artifactId", newArtifactId, ctx);
                    }

                    if (acc.createVersion != null) {
                        Xml.Tag newVersionTag = Xml.Tag.build("<version>" + acc.createVersion + "</version>");
                        t = (Xml.Tag) new AddToTagVisitor<ExecutionContext>(t, newVersionTag, new MavenTagInsertionComparator(t.getChildren())).visitNonNull(t, ctx, getCursor().getParent());
                    } else if (acc.changeVersion != null) {
                        t = changeChildTagValue(t, "version", acc.changeVersion, ctx);
                    } else if (acc.removeVersionTag) {
                        t = (Xml.Tag) new RemoveContentVisitor<>(t.getChild("version").get(), false, true).visitNonNull(t, ctx);
                    }

                    if (t != tag) {
                        maybeUpdateModel();
                    }
                }

                return t;
            }
        };
    }

    public static class Accumulator {
        Set<PomProperty> pomProperties = new HashSet<>();
        boolean isNewDependencyPresent;

        @Nullable
        String changeGroupId;

        @Nullable
        String changeArtifactId;

        boolean removeVersionTag;

        @Nullable
        String createVersion;

        @Nullable
        String changeVersion;
    }

    @Value
    public static class PomProperty {
        Path filePath;
        String name;
        String value;
    }
}
