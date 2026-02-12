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
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Collections.max;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.Validated.required;
import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.isBlank;
import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.maven.utilities.MavenDependencyPropertyUsageOverlap.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeDependencyGroupIdAndArtifactId extends Recipe {
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

    String displayName = "Change Maven dependency";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s`", oldGroupId, oldArtifactId);
    }

    String description = "Change a Maven dependency coordinates. The `newGroupId` or `newArtifactId` **MUST** be different from before. " +
                "Matching `<dependencyManagement>` coordinates are also updated if a `newVersion` or `versionPattern` is provided. " +
                "Exclusions that reference the old dependency coordinates will also be updated to match the new coordinates.";

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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Nullable
            final VersionComparator versionComparator = newVersion != null ? Semver.validate(newVersion, versionPattern).getValue() : null;
            @Nullable
            private Collection<String> availableVersions;
            private boolean isNewDependencyPresent;
            private Set<String> safeVersionPlaceholdersToChange = new HashSet<>();
            private final boolean configuredToOverrideManagedVersion = overrideManagedVersion != null && overrideManagedVersion; // False by default
            private final boolean configuredToChangeManagedDependency = changeManagedDependency == null || changeManagedDependency;  // True by default

            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                isNewDependencyPresent = checkIfNewDependencyPresent(newGroupId, newArtifactId, newVersion);
                safeVersionPlaceholdersToChange = getSafeVersionPlaceholdersToChange(oldGroupId, oldArtifactId, ctx);
                if (configuredToChangeManagedDependency) {
                    doAfterVisit(new ChangeManagedDependencyGroupIdAndArtifactId(
                            oldGroupId, oldArtifactId,
                            Optional.ofNullable(newGroupId).orElse(oldGroupId),
                            Optional.ofNullable(newArtifactId).orElse(oldArtifactId),
                            newVersion, versionPattern).getVisitor());
                }
                // Update any exclusions that reference the old coordinates
                if (newGroupId != null || newArtifactId != null) {
                    doAfterVisit(new ChangeExclusion(
                            oldGroupId, oldArtifactId,
                            newGroupId, newArtifactId).getVisitor());
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                boolean isOldDependencyTag = isDependencyTag(oldGroupId, oldArtifactId);
                if (isOldDependencyTag && isNewDependencyPresent) {
                    doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                    maybeUpdateModel();
                    return t;
                }
                boolean isPluginDependency = isPluginDependencyTag(oldGroupId, oldArtifactId);
                boolean isAnnotationProcessorPath = isAnnotationProcessorPathTag(oldGroupId, oldArtifactId);
                boolean deferUpdate = false;
                if (isOldDependencyTag || isPluginDependency || isAnnotationProcessorPath) {
                    if (newVersion != null) {
                        String currentVersionValue = t.getChildValue("version").orElse(null);
                        if (isImplicitlyDefinedVersionProperty(currentVersionValue)) {
                            return t;
                        }
                    }

                    String groupId = newGroupId;
                    if (groupId != null) {
                        t = (Xml.Tag) new ChangeTagValueVisitor<>(t.getChild("groupId").orElse(null), groupId).visitNonNull(t, ctx);
                    } else {
                        groupId = t.getChildValue("groupId").orElseThrow(NoSuchElementException::new);
                    }
                    String artifactId = newArtifactId;
                    if (artifactId != null) {
                        t = (Xml.Tag) new ChangeTagValueVisitor<>(t.getChild("artifactId").orElse(null), artifactId).visitNonNull(t, ctx);
                    } else {
                        artifactId = t.getChildValue("artifactId").orElseThrow(NoSuchElementException::new);
                    }

                    String currentVersion = t.getChildValue("version").orElse(null);
                    if (newVersion != null) {
                        try {
                            String resolvedNewVersion = resolveSemverVersion(ctx, groupId, artifactId, currentVersion);
                            Optional<Xml.Tag> scopeTag = t.getChild("scope");
                            Scope scope = scopeTag.map(xml -> Scope.fromName(xml.getValue().orElse("compile"))).orElse(Scope.Compile);
                            Optional<Xml.Tag> versionTag = t.getChild("version");

                            boolean versionTagPresent = versionTag.isPresent();
                            // dependencyManagement does not apply to plugin dependencies or annotation processor paths
                            boolean oldDependencyDefinedManaged = isOldDependencyTag && canAffectManagedDependency(getResolutionResult(), scope, oldGroupId, oldArtifactId);
                            boolean newDependencyManaged = isOldDependencyTag && isDependencyManaged(scope, groupId, artifactId);
                            if (versionTagPresent) {
                                // If the previous dependency had a version but the new artifact is managed, removed the version tag.
                                if (!configuredToOverrideManagedVersion && newDependencyManaged || (oldDependencyDefinedManaged && configuredToChangeManagedDependency)) {
                                    t = (Xml.Tag) new RemoveContentVisitor<>(versionTag.get(), false, true).visit(t, ctx);
                                } else {
                                    String versionTagValue = t.getChildValue("version").orElse(null);
                                    if (versionTagValue == null || !safeVersionPlaceholdersToChange.contains(versionTagValue)) {
                                        t = (Xml.Tag) new ChangeTagValueVisitor<>(versionTag.get(), resolvedNewVersion).visitNonNull(t, ctx);
                                    } else {
                                        t = changeChildTagValue(t, "version", resolvedNewVersion, ctx);
                                    }
                                }
                            } else if (configuredToOverrideManagedVersion || (!newDependencyManaged && !(oldDependencyDefinedManaged && configuredToChangeManagedDependency))) {
                                // If the version is not present, add the version if we are explicitly overriding a managed version or if no managed version exists.
                                Xml.Tag newVersionTag = Xml.Tag.build("<version>" + resolvedNewVersion + "</version>");
                                //noinspection ConstantConditions
                                t = (Xml.Tag) new AddToTagVisitor<ExecutionContext>(t, newVersionTag, new MavenTagInsertionComparator(t.getChildren())).visitNonNull(t, ctx, getCursor().getParent());
                            } else if (!newDependencyManaged) {
                                // We leave it up to the managed dependency update to call `maybeUpdateModel()` instead to avoid interim dependency resolution failure
                                deferUpdate = true;
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(tag);
                        }
                    }

                    if (t != tag && !deferUpdate) {
                        maybeUpdateModel();
                    }
                }

                //noinspection ConstantConditions
                return t;
            }

            private boolean checkIfNewDependencyPresent(@Nullable String groupId, @Nullable String artifactId, @Nullable String version) {
                if ((groupId == null) || (artifactId == null)) {
                    return false;
                }
                List<ResolvedDependency> dependencies = findDependencies(groupId, artifactId);
                return dependencies.stream()
                        .filter(ResolvedDependency::isDirect)
                        .anyMatch(rd -> version == null ||
                                versionComparator != null && versionComparator.compare(null, version, rd.getVersion()) <= 0);
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

            private boolean canAffectManagedDependency(MavenResolutionResult result, Scope scope, String groupId, String artifactId) {
                // We're only going to be able to effect managed dependencies that are either direct or are brought in as direct via a local parent
                // `ChangeManagedDependencyGroupIdAndArtifactId` cannot manipulate BOM imported managed dependencies nor direct dependencies from remote parents
                Pom requestedPom = result.getPom().getRequested();
                for (ManagedDependency requestedManagedDependency : requestedPom.getDependencyManagement()) {
                    if (matchesGlob(requestedManagedDependency.getGroupId(), groupId) && matchesGlob(requestedManagedDependency.getArtifactId(), artifactId)) {
                        if (requestedManagedDependency instanceof ManagedDependency.Defined) {
                            return scope.isInClasspathOf(Scope.fromName(((ManagedDependency.Defined) requestedManagedDependency).getScope()));
                        }
                    }
                }
                if (result.parentPomIsProjectPom() && result.getParent() != null) {
                    return canAffectManagedDependency(result.getParent(), scope, groupId, artifactId);
                }
                return false;
            }

            private Set<String> getSafeVersionPlaceholdersToChange(String groupId, String artifactId, ExecutionContext ctx) {
                MavenResolutionResult result = getResolutionResult();
                ResolvedPom resolvedPom = result.getPom();
                Pom requestedPom = resolvedPom.getRequested();
                Set<String> relevantProperties = requestedPom.getDependencies().stream()
                        .filter(d -> isProperty(d.getVersion()) &&
                                matchesGlob(resolvedPom.getValue(d.getGroupId()), groupId) &&
                                matchesGlob(resolvedPom.getValue(d.getArtifactId()), artifactId))
                        .map(Dependency::getVersion)
                        .collect(toSet());
                relevantProperties = filterPropertiesWithOverlapInDependencies(relevantProperties, groupId, artifactId, requestedPom, resolvedPom, configuredToChangeManagedDependency);
                relevantProperties = filterPropertiesWithOverlapInChildren(relevantProperties, groupId, artifactId, result, configuredToChangeManagedDependency);
                return filterPropertiesWithOverlapInParents(relevantProperties, groupId, artifactId, result, configuredToChangeManagedDependency, ctx);
            }


            @SuppressWarnings("ConstantConditions")
            private String resolveSemverVersion(ExecutionContext ctx, String groupId, String artifactId, @Nullable String currentVersion) throws MavenDownloadingException {
                if (versionComparator == null) {
                    return newVersion;
                }
                String finalCurrentVersion = currentVersion != null ? currentVersion : newVersion;
                if (availableVersions == null) {
                    availableVersions = new ArrayList<>();
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    for (String v : mavenMetadata.getVersioning().getVersions()) {
                        if (versionComparator.isValid(finalCurrentVersion, v)) {
                            availableVersions.add(v);
                        }
                    }

                }
                return availableVersions.isEmpty() ? newVersion : max(availableVersions, versionComparator);
            }
        };
    }
}
