/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.semver.ExactVersion;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Collections.max;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.Validated.test;
import static org.openrewrite.internal.StringUtils.isBlank;
import static org.openrewrite.internal.StringUtils.matchesGlob;
import static org.openrewrite.maven.utilities.MavenDependencyPropertyUsageOverlap.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class ChangeManagedDependencyGroupIdAndArtifactId extends Recipe {
    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Old groupId",
            description = "The old groupId to replace. The groupId is the first part of a managed dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
            example = "org.openrewrite.recipe")
    String oldGroupId;

    @Option(displayName = "Old artifactId",
            description = "The old artifactId to replace. The artifactId is the second part of a managed dependency coordinate `com.google.guava:guava:VERSION`. Supports glob expressions.",
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
            description = "The new version to use.",
            example = "2.0.0",
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

    public ChangeManagedDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = null;
    }

    @JsonCreator
    public ChangeManagedDependencyGroupIdAndArtifactId(String oldGroupId, String oldArtifactId, @Nullable String newGroupId, @Nullable String newArtifactId, @Nullable String newVersion, @Nullable String versionPattern) {
        this.oldGroupId = oldGroupId;
        this.oldArtifactId = oldArtifactId;
        this.newGroupId = newGroupId;
        this.newArtifactId = newArtifactId;
        this.newVersion = newVersion;
        this.versionPattern = versionPattern;
    }

    private boolean isGlobPattern() {
        return oldGroupId.contains("*") || oldGroupId.contains("?") ||
               oldArtifactId.contains("*") || oldArtifactId.contains("?");
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
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

    String displayName = "Change Maven managed dependency groupId, artifactId and optionally the version";

    String description = "Change the groupId, artifactId and optionally the version of a specified Maven managed dependency.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            final VersionComparator versionComparator = newVersion != null ? Semver.validate(newVersion, versionPattern).getValue() : null;
            private final Map<String, List<String>> availableVersionsByArtifact = new HashMap<>();
            private Set<String> safeVersionPlaceholdersToChange = new HashSet<>();
            private List<Xml.Tag> supersededEntries = new ArrayList<>();

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                safeVersionPlaceholdersToChange = getSafeVersionPlaceholdersToChange(oldGroupId, oldArtifactId, ctx);
                supersededEntries = new ArrayList<>();
                Xml.Document d = super.visitDocument(document, ctx);
                // Removed before the model update and the redundant-version pass run, so that neither sees two
                // managed entries for the same coordinates.
                for (Xml.Tag superseded : supersededEntries) {
                    d = (Xml.Document) new RemoveContentVisitor<>(superseded, true, true).visitNonNull(d, ctx);
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (isManagedDependencyTag(oldGroupId, oldArtifactId)) {
                    if (newVersion != null) {
                        String currentVersionValue = t.getChildValue("version").orElse(null);
                        if (isImplicitlyDefinedVersionProperty(currentVersionValue)) {
                            return t;
                        }
                    }

                    if (newGroupId != null) {
                        t = (Xml.Tag) new ChangeTagValueVisitor<>(t.getChild("groupId").orElse(null), newGroupId).visitNonNull(t, ctx);
                    }
                    if (newArtifactId != null) {
                        t = (Xml.Tag) new ChangeTagValueVisitor<>(t.getChild("artifactId").orElse(null), newArtifactId).visitNonNull(t, ctx);
                    }
                    String renamedVersion = null;
                    if (newVersion != null) {
                        try {
                            Optional<Xml.Tag> versionTag = t.getChild("version");
                            if (versionTag.isPresent()) {
                                ResolvedPom pom = getResolutionResult().getPom();
                                String resolvedGroupId = t.getChildValue("groupId").orElse(newGroupId != null ? newGroupId : oldGroupId);
                                String resolvedArtifactId = t.getChildValue("artifactId").orElse(newArtifactId != null ? newArtifactId : oldArtifactId);
                                if (resolvedArtifactId.contains("${")) {
                                    resolvedArtifactId = ResolvedPom.placeholderHelper.replacePlaceholders(resolvedArtifactId, pom.getProperties()::get);
                                }
                                if (resolvedGroupId.contains("${")) {
                                    resolvedGroupId = ResolvedPom.placeholderHelper.replacePlaceholders(resolvedGroupId, pom.getProperties()::get);
                                }
                                String resolvedNewVersion = resolveSemverVersion(ctx, resolvedGroupId, resolvedArtifactId, pom.getValue(versionTag.get().getValue().orElse(null)));
                                renamedVersion = resolvedNewVersion;
                                String versionTagValue = t.getChildValue("version").orElse(null);
                                if (versionTagValue == null || !safeVersionPlaceholdersToChange.contains(versionTagValue)) {
                                    t = (Xml.Tag) new ChangeTagValueVisitor<>(versionTag.get(), resolvedNewVersion).visitNonNull(t, ctx);
                                } else {
                                    t = changeChildTagValue(t, "version", resolvedNewVersion, ctx);
                                }
                            }
                        } catch (MavenDownloadingException e) {
                            if (isGlobPattern()) {
                                return tag;
                            }
                            return e.warn(t);
                        }
                    }
                    if (t != tag) {
                        maybeUpdateModel();
                        doAfterVisit(new RemoveRedundantDependencyVersions(null, null, null, null).getVisitor());
                        GroupArtifactClassifierType renamedKey = managementKey(t);
                        Xml.Tag duplicate = null;
                        Xml.Tag block = getCursor().getParentTreeCursor().getValue();
                        for (Xml.Tag sibling : block.getChildren("dependency")) {
                            if (sibling != tag && renamedKey.equals(managementKey(sibling))) {
                                duplicate = sibling;
                                break;
                            }
                        }
                        if (duplicate != null) {
                            String duplicateVersion = getResolutionResult().getPom()
                                    .getValue(duplicate.getChildValue("version").orElse(null));
                            supersededEntries.add(renamedEntryWins(renamedVersion, duplicateVersion) ? duplicate : t);
                        } else if (isManagedElsewhereAtNewVersion(renamedKey)) {
                            supersededEntries.add(t);
                        }
                    }
                }
                return t;
            }

            private GroupArtifactClassifierType managementKey(Xml.Tag dependency) {
                ResolvedPom pom = getResolutionResult().getPom();
                return new GroupArtifactClassifierType(
                        pom.getValue(dependency.getChildValue("groupId").orElse(null)),
                        pom.getValue(dependency.getChildValue("artifactId").orElse(null)),
                        pom.getValue(dependency.getChildValue("classifier").orElse(null)),
                        pom.getValue(dependency.getChildValue("type").orElse("jar")));
            }

            /**
             * The renamed entry wins only when its resolved new version is exact and the duplicate has no version or a
             * lower exact one. An exact new version counts even when it is not published. A selector that matched
             * nothing published keeps the duplicate, as does a duplicate version that is a range or placeholder.
             *
             * @param renamedVersion the resolved new version, or null when the recipe has no new version or the
             *                       entry has no {@code <version>}
             */
            private boolean renamedEntryWins(@Nullable String renamedVersion, @Nullable String duplicateVersion) {
                if (!isExact(renamedVersion)) {
                    return false;
                }
                return duplicateVersion == null ||
                       isExact(duplicateVersion) &&
                       new Version(renamedVersion).compareTo(new Version(duplicateVersion)) > 0;
            }

            private boolean isExact(@Nullable String version) {
                return version != null && !version.contains("${") &&
                       Semver.validate(version, null).getValue() instanceof ExactVersion;
            }

            private boolean isManagedElsewhereAtNewVersion(GroupArtifactClassifierType key) {
                for (ResolvedManagedDependency managed : getResolutionResult().getPom().getDependencyManagement()) {
                    if (managed.matches(key.getGroupId(), key.getArtifactId(), key.getType(), key.getClassifier())) {
                        return compareVersions(newVersion, managed.getVersion());
                    }
                }
                return false;
            }

            private Set<String> getSafeVersionPlaceholdersToChange(String groupId, String artifactId, ExecutionContext ctx) {
                MavenResolutionResult result = getResolutionResult();
                final ResolvedPom resolvedPom = result.getPom();
                Pom requestedPom = resolvedPom.getRequested();
                Set<String> relevantProperties = requestedPom.getDependencyManagement().stream()
                        .filter(md -> isProperty(md.getVersion()) &&
                                matchesGlob(resolvedPom.getValue(md.getGroupId()), groupId) &&
                                matchesGlob(resolvedPom.getValue(md.getArtifactId()), artifactId))
                        .map(ManagedDependency::getVersion)
                        .collect(toSet());
                relevantProperties = filterPropertiesWithOverlapInDependencies(relevantProperties, groupId, artifactId, requestedPom, resolvedPom, true);
                relevantProperties = filterPropertiesWithOverlapInChildren(relevantProperties, groupId, artifactId, result, true);
                return filterPropertiesWithOverlapInParents(relevantProperties, groupId, artifactId, result, true, ctx);
            }

            private boolean compareVersions(@Nullable String targetVersion, @Nullable String foundVersion) {
                if (targetVersion == null) {
                    return true;
                }
                if ((versionComparator != null) && (foundVersion != null)) {
                    return versionComparator.isValid(foundVersion, foundVersion);
                } else {
                    return targetVersion.equals(foundVersion);
                }
            }

            @SuppressWarnings("ConstantConditions")
            private String resolveSemverVersion(ExecutionContext ctx, String groupId, String artifactId, @Nullable String currentVersion) throws MavenDownloadingException {
                if (versionComparator == null) {
                    return newVersion;
                }
                String finalCurrentVersion = currentVersion != null ? currentVersion : newVersion;
                String key = groupId + ':' + artifactId;
                List<String> versions = availableVersionsByArtifact.get(key);
                if (versions == null) {
                    MavenMetadata mavenMetadata = metadataFailures.insertRows(ctx, () -> downloadMetadata(groupId, artifactId, ctx));
                    versions = mavenMetadata.getVersioning().getVersions();
                    availableVersionsByArtifact.put(key, versions);
                }
                List<String> availableVersions = new ArrayList<>();
                for (String v : versions) {
                    if (versionComparator.isValid(finalCurrentVersion, v)) {
                        availableVersions.add(v);
                    }
                }
                return availableVersions.isEmpty() ? newVersion : max(availableVersions, versionComparator);
            }
        };
    }
}
