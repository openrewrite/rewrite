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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.trait.MavenDependency;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.openrewrite.internal.StringUtils.matchesGlob;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddManagedDependency extends ScanningRecipe<AddManagedDependency.Scanned> {
    @EqualsAndHashCode.Exclude
    transient MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate 'org.apache.logging.log4j:ARTIFACT_ID:VERSION'.",
            example = "org.apache.logging.log4j")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate 'org.apache.logging.log4j:log4j-bom:VERSION'.",
            example = "log4j-bom")
    String artifactId;

    @Option(displayName = "Version",
            description = "An exact version number or node-style semver selector used to select the version number.",
            example = "latest.release")
    String version;

    @Option(displayName = "Scope",
            description = "An optional scope to use for the dependency management tag.",
            example = "import",
            valid = {"import", "runtime", "provided", "test"},
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Type",
            description = "An optional type to use for the dependency management tag.",
            valid = {"jar", "pom", "war"},
            example = "pom",
            required = false)
    @Nullable
    String type;

    @Option(displayName = "Classifier",
            description = "An optional classifier to use for the dependency management tag",
            example = "test",
            required = false)
    @Nullable
    String classifier;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'version' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Option(displayName = "Releases only",
            description = "Whether to exclude snapshots from consideration when using a semver selector",
            required = false)
    @Nullable
    Boolean releasesOnly;

    @Option(displayName = "Only if using glob expression for group:artifact",
            description = "Only add managed dependencies to projects having a dependency matching the expression.",
            example = "org.apache.logging.log4j:log4j*",
            required = false)
    @Nullable
    String onlyIfUsing;

    @Option(displayName = "Add to the root pom",
            description = "Add to the root pom where root is the eldest parent of the pom within the source set.",
            required = false)
    @Nullable
    Boolean addToRootPom;

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        //noinspection ConstantConditions
        if (version != null) {
            validated = validated.or(Semver.validate(version, versionPattern));
        }
        if (!StringUtils.isNullOrEmpty(onlyIfUsing)) {
            validated = validated.and(Validated.test("onlyIfUsing", "invalid group:artifact glob pattern", onlyIfUsing, s -> {
                try {
                    return onlyIfUsing.matches("[\\w.-]+\\*?:([\\w-]+\\*?|\\*)");
                } catch (Throwable t) {
                    return false;
                }
            }));
        }
        return validated;
    }

    @Override
    public String getDisplayName() {
        return "Add managed Maven dependency";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s:%s:%s`", groupId, artifactId, version);
    }

    @Override
    public String getDescription() {
        return "Add a managed Maven dependency to a `pom.xml` file.";
    }

    public static class Scanned {
        boolean usingType;
        List<SourceFile> rootPoms = new ArrayList<>();
    }

    @Override
    public Scanned getInitialValue(ExecutionContext ctx) {
        Scanned scanned = new Scanned();
        scanned.usingType = onlyIfUsing == null;
        return scanned;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Scanned acc) {
        return Preconditions.check(acc.usingType || (!StringUtils.isNullOrEmpty(onlyIfUsing) && onlyIfUsing.contains(":")), new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                document.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(mavenResolutionResult -> {
                    if (mavenResolutionResult.getParent() == null) {
                        acc.rootPoms.add(document);
                    }
                });
                if (acc.usingType) {
                    return document;
                }
                super.visitDocument(document, ctx);
                return document;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if (isDependencyTag()) {
                    ResolvedDependency dependency = findDependency(t, null);
                    if (dependency != null) {
                        String[] ga = requireNonNull(onlyIfUsing).split(":");
                        ResolvedDependency match = dependency.findDependency(ga[0], ga[1]);
                        if (match != null) {
                            acc.usingType = true;
                        }
                    }
                }

                return t;
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Scanned acc) {
        return Preconditions.check(acc.usingType, new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml maven = super.visitDocument(document, ctx);

                if (!Boolean.TRUE.equals(addToRootPom) || acc.rootPoms.contains(document)) {
                    ResolvedPom pom = getResolutionResult().getPom();
                    String convertedVersion = pom.getValue(version);
                    if (convertedVersion == null) {
                        return maven;
                    }
                    String convertedGroup = requireNonNull(pom.getValue(groupId));
                    String convertedArtifact = requireNonNull(pom.getValue(artifactId));
                    Validated<VersionComparator> versionValidation = Semver.validate(convertedVersion, versionPattern);
                    if (versionValidation.isValid()) {
                        VersionComparator versionComparator = requireNonNull(versionValidation.getValue());
                        try {
                            // Find the current highest version among existing dependencies
                            String currentVersion = findCurrentHighestVersion(convertedGroup, convertedArtifact, 
                                    Scope.fromName(scope), versionComparator);
                            
                            // Find what version we should use based on the version pattern
                            String versionToUse = MavenDependency.findNewerVersion(convertedGroup, convertedArtifact, 
                                    currentVersion, getResolutionResult(), metadataFailures, versionComparator, ctx);
                            
                            // Check if we should prevent a downgrade
                            if (wouldCauseVersionDowngrade(convertedGroup, convertedArtifact, currentVersion,
                                    versionToUse, versionComparator, Scope.fromName(scope))) {
                                return maven;
                            }
                            
                            // Add the managed dependency if it's different from what already exists
                            if (versionToUse != null && !versionToUse.equals(pom.getValue(existingManagedDependencyVersion()))) {
                                if (ResolvedPom.placeholderHelper.hasPlaceholders(version) && Objects.equals(convertedVersion, versionToUse)) {
                                    // revert back to the original version if the version has a placeholder
                                    versionToUse = version;
                                }
                                doAfterVisit(new AddManagedDependencyVisitor(groupId, artifactId,
                                        versionToUse, scope, type, classifier));
                                maybeUpdateModel();
                            }
                        } catch (MavenDownloadingException e) {
                            return e.warn(document);
                        }
                    }
                }

                return maven;
            }

            /**
             * Find the current highest version of a dependency in the project.
             * For transitive dependencies, finds the highest among all transitive versions.
             * For direct dependencies, uses the first found version.
             */
            private String findCurrentHighestVersion(String groupId, String artifactId, 
                                                     @Nullable Scope scope, VersionComparator versionComparator) {
                List<ResolvedDependency> dependencies = getResolutionResult()
                        .findDependencies(groupId, artifactId, scope);
                
                boolean hasTransitiveDeps = dependencies.stream()
                        .anyMatch(ResolvedDependency::isTransitive);
                
                if (hasTransitiveDeps) {
                    // Find the highest version among transitive dependencies
                    return dependencies.stream()
                            .filter(ResolvedDependency::isTransitive)
                            .map(ResolvedDependency::getVersion)
                            .reduce((v1, v2) -> compareVersions(v1, v2, versionComparator) >= 0 ? v1 : v2)
                            .orElse(existingManagedDependencyVersion());
                }

                // For direct dependencies, use the first found version
                return dependencies.stream()
                        .map(ResolvedDependency::getVersion)
                        .findFirst()
                        .orElse(existingManagedDependencyVersion());
            }
            
            /**
             * Determine if adding a managed dependency would cause a downgrade.
             * Takes into account both current versions and potential versions from related dependencies.
             */
            private boolean wouldCauseVersionDowngrade(String groupId, String artifactId,
                                                       @Nullable String currentVersion, @Nullable String versionToUse,
                                                       VersionComparator versionComparator, @Nullable Scope scope) {
                if (versionToUse == null || currentVersion == null) {
                    return false;
                }
                
                // Check if this is a transitive dependency
                boolean hasTransitiveDeps = getResolutionResult()
                        .findDependencies(groupId, artifactId, scope).stream()
                        .anyMatch(ResolvedDependency::isTransitive);
                
                if (hasTransitiveDeps) {
                    // For transitive dependencies, also consider versions from related direct dependencies
                    String highestPotentialVersion = findHighestPotentialVersion(groupId, currentVersion, versionComparator);
                    return compareVersions(highestPotentialVersion, versionToUse, versionComparator) > 0;
                }

                // For direct dependencies, just compare current vs new
                return compareVersions(currentVersion, versionToUse, versionComparator) > 0;
            }
            
            /**
             * Find the highest potential version by checking direct dependencies from the same organization.
             * This helps prevent downgrades when related libraries typically use aligned versions.
             */
            private String findHighestPotentialVersion(String targetGroup, String currentVersion, 
                                                       VersionComparator versionComparator) {
                String highestVersion = currentVersion;
                
                // Get all direct dependencies
                List<ResolvedDependency> directDeps = getResolutionResult().getDependencies().values().stream()
                        .flatMap(List::stream)
                        .filter(d -> !d.isTransitive())
                        .collect(Collectors.toList());
                
                for (ResolvedDependency directDep : directDeps) {
                    // Check if this dependency is from the same organization
                    if (areFromSameOrganization(directDep.getGroupId(), targetGroup)) {
                        // Libraries from the same organization typically use aligned versions
                        if (compareVersions(highestVersion, directDep.getVersion(), versionComparator) < 0) {
                            highestVersion = directDep.getVersion();
                        }
                    }
                }
                
                return highestVersion;
            }
            
            /**
             * Compare two versions using the provided comparator, with fallback to string comparison.
             * @return negative if v1 < v2, zero if equal, positive if v1 > v2
             */
            private int compareVersions(String v1, String v2, VersionComparator versionComparator) {
                return versionComparator.compare(null, v1, v2);
            }
            
            private @Nullable String existingManagedDependencyVersion() {
                return getResolutionResult().getPom().getDependencyManagement().stream()
                        .map(resolvedManagedDep -> {
                            if (resolvedManagedDep.matches(groupId, artifactId, type, classifier)) {
                                return resolvedManagedDep.getGav().getVersion();
                            } else if (resolvedManagedDep.getRequestedBom() != null &&
                                       resolvedManagedDep.getRequestedBom().getGroupId().equals(groupId) &&
                                       resolvedManagedDep.getRequestedBom().getArtifactId().equals(artifactId)) {
                                return resolvedManagedDep.getRequestedBom().getVersion();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
            
            /**
             * Check if two group IDs are from the same organization by comparing their prefixes.
             * Libraries from the same organization typically use aligned versions.
             */
            private boolean areFromSameOrganization(String group1, String group2) {
                if (group1 == null || group2 == null) {
                    return false;
                }
                if (group1.equals(group2)) {
                    return true;
                }
                
                // Check if they share a common organizational prefix using glob patterns
                int lastDot1 = group1.lastIndexOf('.');
                int lastDot2 = group2.lastIndexOf('.');
                
                if (lastDot1 > 0 && lastDot2 > 0) {
                    String prefix1 = group1.substring(0, lastDot1);
                    String prefix2 = group2.substring(0, lastDot2);
                    
                    // Check if one matches the other's prefix pattern
                    if (matchesGlob(group1, prefix2 + ".*") || matchesGlob(group2, prefix1 + ".*")) {
                        return true;
                    }
                    
                    // Check for deeper organizational structure
                    int nextDot1 = prefix1.lastIndexOf('.');
                    int nextDot2 = prefix2.lastIndexOf('.');
                    if (nextDot1 > 0 && nextDot2 > 0) {
                        String orgPrefix1 = prefix1.substring(0, nextDot1);
                        String orgPrefix2 = prefix2.substring(0, nextDot2);
                        return orgPrefix1.equals(orgPrefix2);
                    }
                }
                
                return false;
            }
        });
    }
}
